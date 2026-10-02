package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.human;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAs;

import io.github.cwzmorro.alayacore.ability.AbilityPresets;
import io.github.cwzmorro.alayacore.ability.Presets;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Checks the server side of the presets (plan 8): only valid, owned changes are taken. */
public class PresetGameTests {
	private static final Identifier RECORDER = TestAbilities.RECORDER.identifier();

	@GameTest
	public void onlyOwnedAbilitiesGoInValidSlots(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		Presets.setSlot(player, 1, 2, Optional.of(RECORDER));
		check(helper, "placed", Optional.of(new AbilityPresets.Slot(1, 2, RECORDER, 0)), Presets.get(player).slot(1, 2));
		Presets.setSlot(player, AbilityPresets.PRESETS, 0, Optional.of(RECORDER));
		Presets.setSlot(player, 0, AbilityPresets.SLOTS, Optional.of(RECORDER));
		check(helper, "out of range ignored", 1, Presets.get(player).slots().size());
		Presets.setSlot(player, 1, 2, Optional.empty());
		check(helper, "cleared", Optional.empty(), Presets.get(player).slot(1, 2));

		ServerPlayer human = join(helper);
		ServantSelection.choose(human, human());
		Presets.setSlot(human, 0, 0, Optional.of(RECORDER));
		check(helper, "not owned ignored", AbilityPresets.EMPTY, Presets.get(human));
		helper.succeed();
	}

	@GameTest
	public void modesWrapAndPresetsSelect(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		Presets.setSlot(player, 0, 0, Optional.of(RECORDER));
		Presets.shiftMode(player, 0, 0, 1);
		check(helper, "next mode", 1, Presets.get(player).slot(0, 0).orElseThrow().mode());
		Presets.shiftMode(player, 0, 0, 1);
		check(helper, "wraps to the first", 0, Presets.get(player).slot(0, 0).orElseThrow().mode());
		Presets.shiftMode(player, 0, 0, -1);
		check(helper, "previous wraps to the last", TestAbilities.RECORDER_MODES - 1, Presets.get(player).slot(0, 0).orElseThrow().mode());

		Presets.select(player, 3);
		check(helper, "selected", 3, Presets.get(player).active());
		Presets.select(player, AbilityPresets.PRESETS);
		check(helper, "out of range ignored", 3, Presets.get(player).active());
		helper.succeed();
	}

	@GameTest
	public void aServantChangeClearsThePresets(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		Presets.setSlot(player, 0, 0, Optional.of(RECORDER));
		Presets.select(player, 2);
		ServantSelection.open(player);
		ServantSelection.choose(player, human());
		check(helper, "presets", AbilityPresets.EMPTY, Presets.get(player));
		helper.succeed();
	}
}
