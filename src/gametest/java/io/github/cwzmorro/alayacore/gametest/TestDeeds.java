package io.github.cwzmorro.alayacore.gametest;

import com.mojang.serialization.Codec;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.progression.DeedKind;
import net.minecraft.core.Registry;

final class TestDeeds {
	static final DeedKind<String, String> SAY = Registry.register(AlayaRegistries.DEED_KIND, TestServants.id("say"),
		new DeedKind<>(Codec.STRING, String::equals));

	private TestDeeds() {
	}

	static void init() {
	}
}
