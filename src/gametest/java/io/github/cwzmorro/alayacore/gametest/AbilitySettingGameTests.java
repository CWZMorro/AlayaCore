package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.human;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAs;

import io.github.cwzmorro.alayacore.ability.AbilitySettingValues;
import io.github.cwzmorro.alayacore.ability.AbilitySettings;
import io.github.cwzmorro.alayacore.ability.AbilityStorage;
import io.github.cwzmorro.alayacore.servant.ServantSelection;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class AbilitySettingGameTests {
	private static final Identifier CONFIGURABLE = TestAbilities.CONFIGURABLE.identifier();
	private static final Identifier RECORDER = TestAbilities.RECORDER.identifier();

	@GameTest
	public void aChoiceIsClampedToTheDeclaredRange(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		check(helper, "default", TestAbilities.REACH.defaultValue(), AbilitySettings.value(player, CONFIGURABLE, TestAbilities.REACH));
		AbilitySettings.set(player, CONFIGURABLE, TestAbilities.REACH.id(), 3.5);
		check(helper, "chosen", 3.5, AbilitySettings.value(player, CONFIGURABLE, TestAbilities.REACH));
		AbilitySettings.set(player, CONFIGURABLE, TestAbilities.REACH.id(), 99.0);
		check(helper, "clamped to the maximum", TestAbilities.REACH.max(), AbilitySettings.value(player, CONFIGURABLE, TestAbilities.REACH));
		AbilitySettings.set(player, CONFIGURABLE, TestAbilities.REACH.id(), -1.0);
		check(helper, "clamped to the minimum", TestAbilities.REACH.min(), AbilitySettings.value(player, CONFIGURABLE, TestAbilities.REACH));
		helper.succeed();
	}

	@GameTest
	public void onlyDeclaredSettingsOfOwnedAbilitiesAreTaken(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		AbilitySettings.set(player, CONFIGURABLE, "unknown", 3.0);
		AbilitySettings.set(player, RECORDER, TestAbilities.REACH.id(), 3.0);
		check(helper, "undeclared ignored", AbilitySettingValues.EMPTY, player.getAttachedOrElse(AbilityStorage.SETTINGS, AbilitySettingValues.EMPTY));

		ServerPlayer human = join(helper);
		ServantSelection.choose(human, human());
		AbilitySettings.set(human, CONFIGURABLE, TestAbilities.REACH.id(), 3.0);
		check(helper, "not owned ignored", AbilitySettingValues.EMPTY, human.getAttachedOrElse(AbilityStorage.SETTINGS, AbilitySettingValues.EMPTY));
		helper.succeed();
	}

	@GameTest
	public void aServantChangeClearsTheSettings(GameTestHelper helper) {
		ServerPlayer player = joinAs(helper, TestServants.CASTER);
		AbilitySettings.set(player, CONFIGURABLE, TestAbilities.REACH.id(), 4.0);
		ServantSelection.open(player);
		ServantSelection.choose(player, human());
		check(helper, "settings", AbilitySettingValues.EMPTY, player.getAttachedOrElse(AbilityStorage.SETTINGS, AbilitySettingValues.EMPTY));
		helper.succeed();
	}
}
