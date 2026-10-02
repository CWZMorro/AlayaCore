package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Class grades (plan 13.4.1): per class, Black → Bronze → Silver → Gold → Grand, advancements in the
 * "Classes" tab granted here. They are permanent (only a revoke removes them), except that a Grand who
 * loses the title loses its Grand advancement too. The player's highest grade in their current class
 * is sent to them for the HUD badge.
 *
 * <p>Advancement ids: {@code <class namespace>:classes/<class path>/<grade>}. The JSON files are made
 * by data generation with {@link #root} and {@link #chain}; content mods call {@link #chain} for their
 * own classes.
 */
public final class ClassGrades {
	public static final Identifier ROOT = AlayaCore.id("classes/root");
	private static final String CRITERION = "granted";
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("block/chiseled_quartz_block_top");

	/** The player's highest grade in their current class, for the HUD badge. Worked out from the advancements; not saved. */
	public static final AttachmentType<ClassGrade> GRADE = AttachmentRegistry.create(AlayaCore.id("class_grade"), builder -> builder
		.syncWith(ByteBufCodecs.idMapper(i -> ClassGrade.values()[i], ClassGrade::ordinal), AttachmentSyncPredicate.targetOnly()));

	private ClassGrades() {
	}

	public static Identifier advancement(Identifier classId, ClassGrade grade) {
		return classId.withPath(path -> "classes/" + path + "/" + grade.key());
	}

	/** Data generation: the "Classes" tab's root, granted when a player first becomes a servant. */
	public static AdvancementHolder root(ItemStackTemplate icon) {
		return Advancement.Builder.advancement()
			.display(icon, Component.translatable("advancements.alayacore.classes.title"),
				Component.translatable("advancements.alayacore.classes.description"), BACKGROUND, AdvancementType.TASK, false, false, false)
			.addCriterion(CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
			.build(ROOT);
	}

	/** Data generation: one class's grades, Black (under {@code root}) to Grand, each with its own icon. */
	public static List<AdvancementHolder> chain(AdvancementHolder root, Identifier classId, Function<ClassGrade, ItemStackTemplate> icon) {
		List<AdvancementHolder> chain = new ArrayList<>();
		AdvancementHolder parent = root;
		for (ClassGrade grade : ClassGrade.values()) {
			Component className = ServantClass.name(classId);
			parent = Advancement.Builder.advancement()
				.parent(parent)
				.display(icon.apply(grade), Component.translatable("advancements.alayacore.grade." + grade.key() + ".title", className),
					Component.translatable("advancements.alayacore.grade." + grade.key() + ".description", className),
					null, grade.frame(), true, true, grade.hidden())
				.addCriterion(CRITERION, CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
				.build(advancement(classId, grade));
			chain.add(parent);
		}
		return chain;
	}

	/** The player just became a servant: opens the tab. Their class's grades follow with the next {@link #refresh}. */
	public static void onBecameServant(ServerPlayer player) {
		grantOnce(player, ROOT);
	}

	/**
	 * Grants every grade the player has earned in their current class (sync, true name, the Grand title),
	 * takes Grand away once the title is gone, and updates the badge. Runs with every progression refresh.
	 */
	public static void refresh(ServerPlayer player) {
		Optional<Identifier> classId = AlayaServants.classId(player);
		if (classId.isEmpty()) {
			player.removeAttached(GRADE);
			return;
		}
		ClassGrade earned = AlayaConfig.progression().grade(Progression.sync(player), TrueName.isRealized(player));
		for (ClassGrade grade : ClassGrade.values()) {
			if (grade.ordinal() <= earned.ordinal()) {
				grantOnce(player, advancement(classId.get(), grade));
			}
		}
		Identifier grand = advancement(classId.get(), ClassGrade.GRAND);
		if (Grand.isGrand(player)) {
			grantOnce(player, grand);
		} else if (GrantedAdvancements.isDone(player, grand)) {
			GrantedAdvancements.revoke(player, grand);
		}
		updateBadge(player);
	}

	/** The player's grade in their current class, as the badge shows it: the highest granted. */
	public static ClassGrade of(ServerPlayer player) {
		return player.getAttachedOrElse(GRADE, ClassGrade.BLACK);
	}

	/** Takes {@code grade} of the player's current class away (the badge follows); it is earned again as any grade is. */
	public static void revoke(ServerPlayer player, ClassGrade grade) {
		AlayaServants.classId(player).ifPresent(classId -> GrantedAdvancements.revoke(player, advancement(classId, grade)));
	}

	/** A class grade advancement was granted or revoked (also by a command): the badge follows. */
	public static void onAdvancementChanged(ServerPlayer player, Identifier advancement) {
		if (advancement.getPath().startsWith("classes/")) {
			updateBadge(player);
		}
	}

	private static void updateBadge(ServerPlayer player) {
		AlayaServants.classId(player).ifPresent(classId -> {
			ClassGrade highest = ClassGrade.BLACK;
			for (ClassGrade grade : ClassGrade.values()) {
				if (GrantedAdvancements.isDone(player, advancement(classId, grade))) {
					highest = grade;
				}
			}
			if (player.getAttached(GRADE) != highest) {
				player.setAttached(GRADE, highest);
			}
		});
	}

	private static void grantOnce(ServerPlayer player, Identifier advancement) {
		if (!GrantedAdvancements.isDone(player, advancement)) {
			GrantedAdvancements.grant(player, advancement);
		}
	}
}
