package io.github.cwzmorro.alayacore.datagen;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.AlayaItems;
import java.util.List;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/** Item models: the icons drawn flat, and the class badge picking its grade art by custom model data. */
final class AlayaModelProvider extends FabricModelProvider {
	AlayaModelProvider(FabricPackOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators blocks) {
	}

	@Override
	public void generateItemModels(ItemModelGenerators items) {
		items.generateFlatItem(AlayaItems.SPIRIT_ORIGIN_CHANGER, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(AlayaItems.ALAYA, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(AlayaItems.ALL, ModelTemplates.FLAT_ITEM);
		// Placeholders until the grade items have art of their own: vanilla's sprites.
		placeholder(items, AlayaItems.GRADE_PROMOTION, "experience_bottle");
		placeholder(items, AlayaItems.GRAND_PROMOTION, "nether_star");
		placeholder(items, AlayaItems.GRADE_DEMOTION, "echo_shard");
		List<SelectItemModel.SwitchCase<String>> badges = AlayaDataGenerator.classBadges().stream().map(badge -> {
			Identifier model = AlayaCore.id("item/class_badge/" + badge);
			return ItemModelUtils.when(badge, ItemModelUtils.plainModel(ModelTemplates.FLAT_ITEM.create(model, TextureMapping.layer0(new Material(model)), items.modelOutput)));
		}).toList();
		items.itemModelOutput.accept(AlayaItems.CLASS_BADGE, ItemModelUtils.select(new CustomModelDataProperty(0), badges));
	}

	/** {@code item} drawn with vanilla's {@code sprite}. */
	private static void placeholder(ItemModelGenerators items, Item item, String sprite) {
		Identifier model = ModelLocationUtils.getModelLocation(item);
		ModelTemplates.FLAT_ITEM.create(model, TextureMapping.layer0(new Material(Identifier.withDefaultNamespace("item/" + sprite))), items.modelOutput);
		items.itemModelOutput.accept(item, ItemModelUtils.plainModel(model));
	}
}
