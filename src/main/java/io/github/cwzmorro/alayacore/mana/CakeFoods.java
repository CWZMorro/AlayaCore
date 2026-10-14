package io.github.cwzmorro.alayacore.mana;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import java.util.function.Supplier;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Cake slices from any tagged block, including blocks unrelated to vanilla's cake class. */
public final class CakeFoods {
	// Blocks that restore cake mana when they actually feed a player.
	public static final TagKey<Block> CAKES = TagKey.create(Registries.BLOCK, AlayaCore.id("cakes"));
	public static final Identifier SLICE = AlayaCore.id("cake_slice");

	private CakeFoods() {
	}

	public static double foodFraction(RegistryAccess registries, int nutrition, Identifier food) {
		return AlayaConfig.mana().foodFraction(nutrition, food, SweetFoods.percent(registries, SLICE));
	}

	public static InteractionResult interact(BlockState state, Player player, Supplier<InteractionResult> interaction) {
		if (player.level().isClientSide() || !state.is(CAKES)) {
			return interaction.get();
		}
		Identifier food = BuiltInRegistries.ITEM.getKey(state.getBlock().asItem());
		return ((CakeConsumption) player.getFoodData()).alayacore$withCake(nutrition -> ManaEvents.onCake(player, nutrition, food), interaction);
	}
}
