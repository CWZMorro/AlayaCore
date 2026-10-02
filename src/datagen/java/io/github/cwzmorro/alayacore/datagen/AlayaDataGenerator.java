package io.github.cwzmorro.alayacore.datagen;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.progression.ClassGrade;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.resources.Identifier;

/** Makes Alaya Core's generated data: advancements and item models ({@code ./gradlew runDatagen}). */
public final class AlayaDataGenerator implements DataGeneratorEntrypoint {
	/** Each of Alaya Core's classes as a custom model data string per grade, {@code <class path>/<grade>}. */
	static List<String> classBadges() {
		List<String> badges = new ArrayList<>();
		for (ServantClass servantClass : AlayaRegistries.SERVANT_CLASS) {
			Identifier classId = AlayaRegistries.SERVANT_CLASS.getKey(servantClass);
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
		pack.addProvider(AlayaAdvancementProvider::new);
		pack.addProvider(AlayaModelProvider::new);
	}
}
