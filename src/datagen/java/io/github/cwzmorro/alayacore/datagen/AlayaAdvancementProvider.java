package io.github.cwzmorro.alayacore.datagen;

import io.github.cwzmorro.alayacore.AlayaItems;
import io.github.cwzmorro.alayacore.progression.ClassGrades;
import io.github.cwzmorro.alayacore.progression.TrueName;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomModelData;

final class AlayaAdvancementProvider extends FabricAdvancementProvider {
	AlayaAdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
		output.accept(TrueName.root(new ItemStackTemplate(AlayaItems.ALAYA)));

		AdvancementHolder classes = ClassGrades.root(new ItemStackTemplate(AlayaItems.ALL));
		output.accept(classes);
		for (Identifier classId : AlayaServantClassProvider.ids()) {
			ClassGrades.chain(classes, classId, grade -> badge(AlayaDataGenerator.classBadge(classId, grade))).forEach(output);
		}
	}

	private static ItemStackTemplate badge(String classBadge) {
		return new ItemStackTemplate(AlayaItems.CLASS_BADGE, DataComponentPatch.builder()
			.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(classBadge), List.of()))
			.build());
	}
}
