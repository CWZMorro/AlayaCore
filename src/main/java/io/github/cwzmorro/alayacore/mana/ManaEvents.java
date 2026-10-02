package io.github.cwzmorro.alayacore.mana;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Everything that changes mana by itself: regeneration, kills, food, sleep, challenge advancements
 * and respawning (plan 7). Regeneration of every kind uses the class bonus (Archer +20%). Food,
 * sleep and advancements are reached through mixins, which call the {@code on…} methods here.
 */
public final class ManaEvents {
	/** Regeneration is applied this often, so a player's mana is re-sent twice a second, not every tick. */
	private static final int REGEN_INTERVAL_TICKS = 10;

	private ManaEvents() {
	}

	public static void register() {
		// Every player has mana from the first join.
		ServerPlayerEvents.JOIN.register(AlayaMana::getOrCreate);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % REGEN_INTERVAL_TICKS != 0) {
				return;
			}
			double regen = AlayaConfig.mana().regenPerSecond() * REGEN_INTERVAL_TICKS / SharedConstants.TICKS_PER_SECOND;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isAlive()) {
					AlayaMana.addCurrent(player, regen * AlayaServants.manaRegenMultiplier(player));
				}
			}
		});

		// The killer is whoever vanilla credits: the attacker, or the shooter of a projectile.
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((level, killer, killed, source) -> {
			if (killed instanceof Player) {
				return;
			}
			if (killer instanceof LivingEntity living) {
				AlayaMana.addMax(living, killed.getMaxHealth());
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register(AlayaCore.AFTER_ATTACHMENTS_COPIED, (oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				AlayaMana.update(newPlayer, AlayaConfig.mana()::afterRespawn);
			}
		});
	}

	/** A food item or cake slice was eaten; {@code food} is its item id. */
	public static void onAte(LivingEntity eater, int nutrition, Identifier food) {
		double fraction = AlayaConfig.mana().foodFraction(nutrition, food) * AlayaServants.manaRegenMultiplier(eater);
		AlayaMana.update(eater, mana -> ManaRules.addFractionOfMax(mana, fraction));
	}

	/** The player slept through and is being woken in the morning. */
	public static void onSleptThroughNight(ServerPlayer player) {
		double fraction = AlayaConfig.mana().sleepFraction() * AlayaServants.manaRegenMultiplier(player);
		AlayaMana.update(player, mana -> ManaRules.addFractionOfMax(mana, fraction));
	}

	/** The player has just completed an advancement. */
	public static void onAdvancementDone(ServerPlayer player, AdvancementHolder holder) {
		boolean challenge = holder.value().display()
			.map(display -> display.getType() == AdvancementType.CHALLENGE)
			.orElse(false);
		if (!challenge || !AlayaMana.hasMana(player)) {
			return;
		}
		Set<Identifier> paid = player.getAttachedOrCreate(ManaStorage.PAID_ADVANCEMENTS);
		if (paid.contains(holder.id())) {
			return;
		}
		Set<Identifier> updated = new HashSet<>(paid);
		updated.add(holder.id());
		player.setAttached(ManaStorage.PAID_ADVANCEMENTS, Set.copyOf(updated));
		AlayaMana.addMax(player, AlayaConfig.mana().challengeBonus(holder.value().rewards().experience()));
	}
}
