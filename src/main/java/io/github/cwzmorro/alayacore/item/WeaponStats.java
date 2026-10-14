package io.github.cwzmorro.alayacore.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** A weapon's own melee numbers, as its tooltip shows them. */
public final class WeaponStats {
	public static final double PLAYER_ATTACK_DAMAGE = 1.0;

	private WeaponStats() {
	}

	public static double attackDamage(ItemStack stack) {
		return stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
			.compute(Attributes.ATTACK_DAMAGE, PLAYER_ATTACK_DAMAGE, EquipmentSlot.MAINHAND);
	}
}
