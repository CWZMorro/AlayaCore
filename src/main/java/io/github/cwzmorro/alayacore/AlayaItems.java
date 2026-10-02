package io.github.cwzmorro.alayacore;

import io.github.cwzmorro.alayacore.progression.GradeItem;
import io.github.cwzmorro.alayacore.servant.SpiritOriginChangerItem;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Alaya Core's items and its creative tab. */
public final class AlayaItems {
	public static final Item SPIRIT_ORIGIN_CHANGER = register("spirit_origin_changer", SpiritOriginChangerItem::new,
		new Item.Properties().stacksTo(1));
	/** Alaya, an icon only (creative tab, Throne of Heroes tab); not in the tab and has no use. */
	public static final Item ALAYA = register("alaya", Item::new, new Item.Properties());
	/** Move the holder's class grade one step, as Jujutsu Craft's promotions do (plan 13.4.1); creative and commands only. */
	public static final Item GRADE_PROMOTION = register("grade_promotion", properties -> new GradeItem(GradeItem.Kind.PROMOTION, properties),
		new Item.Properties().rarity(Rarity.RARE));
	public static final Item GRAND_PROMOTION = register("grand_promotion", properties -> new GradeItem(GradeItem.Kind.GRAND, properties),
		new Item.Properties().rarity(Rarity.EPIC));
	public static final Item GRADE_DEMOTION = register("grade_demotion", properties -> new GradeItem(GradeItem.Kind.DEMOTION, properties),
		new Item.Properties().rarity(Rarity.RARE));
	/** The All logo, an icon only (Classes tab); not in the tab and has no use. */
	public static final Item ALL = register("all", Item::new, new Item.Properties());
	/**
	 * Icons of the class grade advancements (plan 13.4.1) only; not in the tab and has no use. Which
	 * badge it shows comes from its custom model data string, {@code <class path>/<grade>}.
	 */
	public static final Item CLASS_BADGE = register("class_badge", Item::new, new Item.Properties());

	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, AlayaCore.id("main"));

	private AlayaItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, AlayaCore.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
			.title(Component.translatable(TAB.identifier().toLanguageKey("itemGroup")))
			.icon(() -> new ItemStack(ALAYA))
			.displayItems((parameters, output) -> {
				output.accept(SPIRIT_ORIGIN_CHANGER);
				output.accept(GRADE_PROMOTION);
				output.accept(GRAND_PROMOTION);
				output.accept(GRADE_DEMOTION);
			})
			.build());
	}
}
