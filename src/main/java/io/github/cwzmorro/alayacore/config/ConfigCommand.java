package io.github.cwzmorro.alayacore.config;

import com.mojang.brigadier.Command;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.progression.Progression;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /alayacore config reload}: re-reads {@code config/alayacore.toml} (op level 2). */
public final class ConfigCommand {
	private ConfigCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
			Commands.literal(AlayaCore.MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("config")
					.then(Commands.literal("reload").executes(context -> {
						List<String> warnings = AlayaConfig.load();
						Progression.refreshAll(context.getSource().getServer());
						Component message = warnings.isEmpty()
							? Component.translatable("commands.alayacore.config.reload.success")
							: Component.translatable("commands.alayacore.config.reload.warnings", warnings.size());
						context.getSource().sendSuccess(() -> message, true);
						return Command.SINGLE_SUCCESS;
					})))));
	}
}
