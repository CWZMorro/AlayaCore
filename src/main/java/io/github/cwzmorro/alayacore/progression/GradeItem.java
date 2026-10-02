package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Moves a servant's class grade (plan 13.4.1) one step, with the progress that grade stands for, as Jujutsu Craft's
 * promotions do; used up, except in creative. Creative and commands only.
 *
 * <ul>
 * <li>{@link Kind#PROMOTION}: Black to Bronze to Silver (max mana raised to the sync each needs) to Gold (the true name
 * realized).</li>
 * <li>{@link Kind#GRAND}: the Grand title, from Gold; never while every title of the class is held by others.</li>
 * <li>{@link Kind#DEMOTION}: one step down: Grand loses the title, Gold the true name, Silver and Bronze their sync (max
 * mana down to the step below's); Black stays.</li>
 * </ul>
 */
public final class GradeItem extends Item {
	public enum Kind {
		PROMOTION, GRAND, DEMOTION
	}

	private final Kind kind;

	public GradeItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		Optional<Identifier> classId = AlayaServants.classId(serverPlayer);
		if (classId.isEmpty()) {
			serverPlayer.sendOverlayMessage(Component.translatable("item.alayacore.grade.not_a_servant"));
			return InteractionResult.FAIL;
		}
		Component refused = this.apply(serverPlayer, ClassGrades.of(serverPlayer));
		if (refused != null) {
			serverPlayer.sendOverlayMessage(refused);
			return InteractionResult.FAIL;
		}
		ClassGrade now = ClassGrades.of(serverPlayer);
		serverPlayer.sendOverlayMessage(Component.translatable("item.alayacore.grade.now",
			Component.translatable("advancements.alayacore.grade." + now.key() + ".title", ServantClass.name(classId.get()))));
		player.getItemInHand(hand).consume(1, player);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Moves the player from {@code grade} as this kind does; why not, or null once done. */
	private @Nullable Component apply(ServerPlayer player, ClassGrade grade) {
		return switch (this.kind) {
			case PROMOTION -> {
				if (grade.ordinal() >= ClassGrade.GOLD.ordinal()) {
					yield Component.translatable("item.alayacore.grade.highest");
				}
				ClassGrade next = ClassGrade.values()[grade.ordinal() + 1];
				raiseMaxManaTo(player, AlayaConfig.progression().manaForSync(AlayaConfig.progression().syncFor(next)));
				if (next == ClassGrade.GOLD) {
					TrueName.realize(player);
				}
				yield null;
			}
			case GRAND -> {
				if (Grand.isGrand(player)) {
					yield Component.translatable("item.alayacore.grade.already_grand");
				}
				if (grade != ClassGrade.GOLD) {
					yield Component.translatable("item.alayacore.grade.needs_gold");
				}
				if (Grand.titleTaken(player)) {
					yield Component.translatable("item.alayacore.grade.title_taken");
				}
				Grand.qualify(player);
				yield null;
			}
			case DEMOTION -> {
				switch (grade) {
					case BLACK -> {
						yield Component.translatable("item.alayacore.grade.lowest");
					}
					case GRAND -> Grand.loseTitle(player);
					case GOLD -> TrueName.unrealize(player);
					case SILVER, BRONZE -> {
						ClassGrade below = ClassGrade.values()[grade.ordinal() - 1];
						lowerMaxManaTo(player, AlayaConfig.progression().manaForSync(AlayaConfig.progression().syncFor(below)));
					}
				}
				ClassGrades.revoke(player, grade);
				yield null;
			}
		};
	}

	private static void raiseMaxManaTo(ServerPlayer player, double max) {
		AlayaMana.update(player, mana -> mana.max() >= max ? mana : mana.withMax(max));
	}

	private static void lowerMaxManaTo(ServerPlayer player, double max) {
		AlayaMana.update(player, mana -> mana.max() <= max ? mana : mana.withMax(max).withCurrent(Math.min(mana.current(), max)));
	}
}
