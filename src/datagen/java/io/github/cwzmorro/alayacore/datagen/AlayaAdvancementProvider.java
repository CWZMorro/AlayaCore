package io.github.cwzmorro.alayacore.datagen;

import io.github.cwzmorro.alayacore.AlayaItems;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.progression.ClassGrades;
import io.github.cwzmorro.alayacore.progression.TrueName;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomModelData;

/** The "Throne of Heroes" tab's root (plan 13.3) and the "Classes" tab (plan 13.4.1). */
final class AlayaAdvancementProvider extends FabricAdvancementProvider {
	private static final Identifier THRONE_BACKGROUND = Identifier.withDefaultNamespace("gui/advancements/backgrounds/end");

	AlayaAdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> output) {
		output.accept(Advancement.Builder.advancement()
			.display(AlayaItems.ALAYA, Component.translatable("advancements.alayacore.throne_of_heroes.title"),
				Component.translatable("advancements.alayacore.throne_of_heroes.description"), THRONE_BACKGROUND, AdvancementType.TASK,
				false, false, false)
			.addCriterion("granted", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
			.build(TrueName.THRONE_OF_HEROES));

		AdvancementHolder classes = ClassGrades.root(new ItemStackTemplate(AlayaItems.ALL));
		output.accept(classes);
		for (ServantClass servantClass : AlayaRegistries.SERVANT_CLASS) {
			Identifier classId = AlayaRegistries.SERVANT_CLASS.getKey(servantClass);
			ClassGrades.chain(classes, classId, grade -> badge(AlayaDataGenerator.classBadge(classId, grade))).forEach(output);
		}
	}

	/** The class badge item showing one class's grade. */
	private static ItemStackTemplate badge(String classBadge) {
		return new ItemStackTemplate(AlayaItems.CLASS_BADGE, DataComponentPatch.builder()
			.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(classBadge), List.of()))
			.build());
	}
}
