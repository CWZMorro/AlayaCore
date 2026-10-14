package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Moves a servant's class grade one step with the progress that grade stands for (max mana for Silver and Bronze, the
 * true name for Gold, the title for Grand); used up outside creative. See {@link Kind} for promotion, Grand and demotion.
 */
public final class GradeItem extends Item {
	/** Promotion goes Black to Gold; Grand needs Gold and a free title; demotion goes one step down, Black stays. */
	public enum Kind {
		PROMOTION, GRAND, DEMOTION
	}

	private static final int CONSUMED_PER_USE = 1;

	private final Kind kind;

	public GradeItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		Optional<Identifier> classId = AlayaServants.classId(serverPlayer);
		if (classId.isEmpty()) {
			serverPlayer.sendOverlayMessage(Component.translatable("item.alayacore.grade.not_a_servant"));
			return InteractionResult.FAIL;
		}
		Component refused = this.apply(serverPlayer, ClassGrades.of(serverPlayer));
		if (refused != null) {
			serverPlayer.sendOverlayMessage(refused);
			return InteractionResult.FAIL;
		}
		serverPlayer.sendOverlayMessage(Component.translatable("item.alayacore.grade.now",
			ClassGrades.of(serverPlayer).title(ServantClass.name(classId.get()))));
		player.getItemInHand(hand).consume(CONSUMED_PER_USE, player);
		return InteractionResult.SUCCESS_SERVER;
	}

	private @Nullable Component apply(ServerPlayer player, ClassGrade grade) {
		return switch (this.kind) {
			case PROMOTION -> {
				if (grade.ordinal() >= ClassGrade.GOLD.ordinal()) {
					yield Component.translatable("item.alayacore.grade.highest");
				}
				ClassGrade next = ClassGrade.values()[grade.ordinal() + 1];
				raiseMaxManaTo(player, AlayaConfig.progression().manaForSync(AlayaConfig.progression().syncFor(next)));
				if (next == ClassGrade.GOLD) {
					TrueName.realize(player);
				}
				yield null;
			}
			case GRAND -> {
				if (Grand.isGrand(player)) {
					yield Component.translatable("item.alayacore.grade.already_grand");
				}
				if (grade != ClassGrade.GOLD) {
					yield Component.translatable("item.alayacore.grade.needs_gold");
				}
				if (Grand.titleTaken(player)) {
					yield Component.translatable("item.alayacore.grade.title_taken");
				}
				Grand.qualify(player);
				yield null;
			}
			case DEMOTION -> {
				switch (grade) {
					case BLACK -> {
						yield Component.translatable("item.alayacore.grade.lowest");
					}
					case GRAND -> Grand.loseTitle(player);
					case GOLD -> TrueName.unrealize(player);
					case SILVER, BRONZE -> {
						ClassGrade below = ClassGrade.values()[grade.ordinal() - 1];
						lowerMaxManaTo(player, AlayaConfig.progression().manaForSync(AlayaConfig.progression().syncFor(below)));
					}
				}
				ClassGrades.revoke(player, grade);
				yield null;
			}
		};
	}

	private static void raiseMaxManaTo(ServerPlayer player, double max) {
		AlayaMana.update(player, mana -> mana.max() >= max ? mana : mana.withMax(max));
	}

	private static void lowerMaxManaTo(ServerPlayer player, double max) {
		AlayaMana.update(player, mana -> mana.max() <= max ? mana : mana.withMax(max).withCurrent(Math.min(mana.current(), max)));
	}
}
