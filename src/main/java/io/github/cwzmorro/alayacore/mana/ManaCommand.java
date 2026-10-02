package io.github.cwzmorro.alayacore.mana;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.progression.Progression;
import io.github.cwzmorro.alayacore.util.Percent;
import java.util.function.UnaryOperator;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code /alayacore mana <targets> ...} (op level 2), like Tensura's magicule commands: {@code get}; {@code current set
 * <amount>}, {@code current set max} (fills it), {@code current add <amount>}; {@code max set <amount>}, {@code max add
 * <amount>}. Max mana is what sync comes from (plan 13.2); current mana stays between 0 and max.
 */
public final class ManaCommand {
	private static final String TARGETS = "targets";
	private static final String AMOUNT = "amount";

	private ManaCommand() {
	}

	/** A change of mana by an amount. */
	private interface ByAmount {
		ManaData apply(ManaData mana, double amount);
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
			Commands.literal(AlayaCore.MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("mana")
					.then(Commands.argument(TARGETS, EntityArgument.entities())
						.then(Commands.literal("get").executes(context -> apply(context, UnaryOperator.identity())))
						.then(Commands.literal("current")
							.then(Commands.literal("set")
								.then(Commands.literal("max").executes(context -> apply(context, mana -> mana.withCurrent(mana.max()))))
								.then(amount(ManaData::withCurrent)))
							.then(Commands.literal("add").then(amount((mana, amount) -> mana.withCurrent(mana.current() + amount)))))
						.then(Commands.literal("max")
							.then(Commands.literal("set").then(amount(ManaData::withMax)))
							.then(Commands.literal("add").then(amount((mana, amount) -> mana.withMax(mana.max() + amount)))))))));
	}

	/** An {@code <amount>} argument that changes mana by it. */
	private static RequiredArgumentBuilder<CommandSourceStack, Double> amount(ByAmount change) {
		return Commands.argument(AMOUNT, DoubleArgumentType.doubleArg())
			.executes(context -> apply(context, mana -> change.apply(mana, DoubleArgumentType.getDouble(context, AMOUNT))));
	}

	/** Changes every target's mana, kept in bounds, and says what it is now; the number of targets with mana. */
	private static int apply(CommandContext<CommandSourceStack> context, UnaryOperator<ManaData> change) throws CommandSyntaxException {
		int changed = 0;
		for (Entity target : EntityArgument.getEntities(context, TARGETS)) {
			if (!(target instanceof LivingEntity living) || !AlayaMana.hasMana(living)) {
				continue;
			}
			AlayaMana.update(living, mana -> {
				ManaData after = change.apply(mana);
				double max = Math.max(0.0, after.max());
				return new ManaData(Math.clamp(after.current(), 0.0, max), max);
			});
			ManaData now = AlayaMana.getOrCreate(living);
			context.getSource().sendSuccess(() -> Component.translatable("commands.alayacore.mana", living.getDisplayName(),
				Math.round(now.current()), Math.round(now.max()), Math.round(Percent.WHOLE * Progression.sync(living))), true);
			changed++;
		}
		if (changed == 0) {
			context.getSource().sendFailure(Component.translatable("commands.alayacore.mana.none"));
		}
		return changed;
	}
}
