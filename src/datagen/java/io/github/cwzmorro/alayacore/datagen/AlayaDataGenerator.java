package io.github.cwzmorro.alayacore.datagen;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.progression.ClassGrade;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.resources.Identifier;

public final class AlayaDataGenerator implements DataGeneratorEntrypoint {
	static List<String> classBadges() {
		List<String> badges = new ArrayList<>();
		for (Identifier classId : AlayaServantClassProvider.ids()) {
			for (ClassGrade grade : ClassGrade.values()) {
				badges.add(classBadge(classId, grade));
			}
		}
		return badges;
	}

	static String classBadge(Identifier classId, ClassGrade grade) {
		return classId.getPath() + "/" + grade.key();
	}

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider(AlayaServantClassProvider::new);
		pack.addProvider(AlayaAdvancementProvider::new);
		pack.addProvider(AlayaModelProvider::new);
	}

	@Override
	public void buildRegistry(RegistrySetBuilder registries) {
		registries.add(AlayaRegistries.SERVANT_CLASS_KEY, AlayaServantClassProvider::bootstrap);
	}
}
