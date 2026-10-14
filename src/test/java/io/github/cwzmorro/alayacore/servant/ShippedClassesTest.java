package io.github.cwzmorro.alayacore.servant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShippedClassesTest {
	private static final Path DIRECTORY = Path.of("src/main/generated/data/alayacore/alayacore/servant_class");

	private record Row(String name, ServantStats real, double manaRegenBonusPercent, boolean regenWhileCasting) {
	}

	private static final List<Row> TABLE = List.of(
		new Row("saber", new ServantStats(350, 36, 4, 2, 20, 0.7), 0, false),
		new Row("archer", new ServantStats(275, 24, 2, 0, 25, 0.4), 20, true),
		new Row("lancer", new ServantStats(325, 32, 3, 1, 30, 0.6), 0, false),
		new Row("rider", new ServantStats(300, 28, 2, 1, 25, 0.5), 0, false),
		new Row("caster", new ServantStats(200, 16, 1, 0, 10, 0.3), 0, false),
		new Row("assassin", new ServantStats(225, 32, 1, 0, 30, 0.2), 0, false),
		new Row("berserker", new ServantStats(400, 40, 4, 2, 15, 1.0), 0, false),
		new Row("ruler", new ServantStats(350, 28, 3, 1, 20, 0.6), 0, false),
		new Row("shielder", new ServantStats(375, 20, 6, 4, 15, 0.9), 0, false));

	@Test
	void shippedDataMatchesTheTableInSelectionOrder() throws IOException {
		for (int order = 0; order < TABLE.size(); order++) {
			Row row = TABLE.get(order);
			String json = Files.readString(DIRECTORY.resolve(row.name() + ".json"));
			ServantClass shipped = ServantClass.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
			assertEquals(new ServantClass(order, new ServantClassConfig(row.real(), row.manaRegenBonusPercent(), row.regenWhileCasting())), shipped,
				row.name());
		}
	}
}
