package io.github.cwzmorro.alayacore.item;

import com.mojang.serialization.Codec;
import io.github.cwzmorro.alayacore.AlayaCore;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * What weapons may share: kept through death ({@link #KEPT_ON_DEATH}), and never destroyed or despawned once dropped
 * ({@link #INDESTRUCTIBLE}). Content mods list their weapons in the tags.
 */
public final class WeaponRules {
	public static final TagKey<Item> KEPT_ON_DEATH = TagKey.create(Registries.ITEM, AlayaCore.id("kept_on_death"));
	public static final TagKey<Item> INDESTRUCTIBLE = TagKey.create(Registries.ITEM, AlayaCore.id("indestructible"));
	public static final AttachmentType<List<ItemStack>> KEPT = AttachmentRegistry.create(AlayaCore.id("kept_weapons"), builder -> builder
		.initializer(List::of)
		.persistent(Codec.list(ItemStack.CODEC))
		.copyOnDeath());

	private WeaponRules() {
	}

	public static void register() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item && item.getItem().is(INDESTRUCTIBLE)) {
				item.setInvulnerable(true);
				item.setUnlimitedLifetime();
			}
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> giveBack(newPlayer));
	}

	public static void keepOnDeath(Inventory inventory) {
		// Anything still kept from an earlier death that was never given back stays kept.
		List<ItemStack> kept = new ArrayList<>(inventory.player.getAttachedOrElse(KEPT, List.of()));
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(KEPT_ON_DEATH)) {
				kept.add(stack);
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
		if (!kept.isEmpty()) {
			inventory.player.setAttached(KEPT, List.copyOf(kept));
		}
	}

	public static void giveBack(ServerPlayer player) {
		giveBack(player, player.removeAttached(KEPT));
	}

	public static void giveBack(ServerPlayer player, @Nullable List<ItemStack> kept) {
		if (kept != null) {
			kept.forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
		}
	}
}
