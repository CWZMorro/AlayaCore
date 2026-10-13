package io.github.cwzmorro.alayacore.servant;

import com.mojang.serialization.Codec;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One-time starting items. Each servant's are in
 * {@code data/<namespace>/alayacore/starting_items/<servant path>.json}, whose id is the servant's id, so a content mod or a
 * data pack gives any servant its own. Given on becoming the servant or on a returning join; persisted through death.
 */
public final class ServantStartingItems {
	public static final ResourceKey<Registry<StartingItems>> KEY = ResourceKey.createRegistryKey(AlayaCore.id("starting_items"));
	// The starting items already given to the player, whichever servant gave them: a lost one is never replaced.
	private static final AttachmentType<List<Identifier>> GIVEN = AttachmentRegistry.create(AlayaCore.id("starting_items_given"),
		builder -> builder.initializer(List::of).persistent(Codec.list(Identifier.CODEC)).copyOnDeath());

	private ServantStartingItems() {
	}

	public static void register() {
		DynamicRegistries.register(KEY, StartingItems.CODEC);
		AlayaServants.BECAME_SERVANT.register((player, servant) -> give(player));
		ServerPlayerEvents.JOIN.register(ServantStartingItems::give);
	}

	public static void give(ServerPlayer player) {
		List<Item> items = AlayaServants.servantId(player).flatMap(servant -> player.level().getServer().registryAccess()
			.lookup(KEY).flatMap(registry -> registry.getOptional(servant))).map(StartingItems::items).orElse(List.of());
		List<Identifier> given = new ArrayList<>(player.getAttachedOrCreate(GIVEN));
		for (Item item : items) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (!given.contains(id)) {
				player.getInventory().placeItemBackInInventory(new ItemStack(item));
				given.add(id);
				player.setAttached(GIVEN, List.copyOf(given));
			}
		}
	}

	public static void markGiven(ServerPlayer player, Collection<Item> items) {
		List<Identifier> given = new ArrayList<>(player.getAttachedOrCreate(GIVEN));
		for (Item item : items) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (!given.contains(id)) {
				given.add(id);
			}
		}
		player.setAttached(GIVEN, List.copyOf(given));
	}
}
