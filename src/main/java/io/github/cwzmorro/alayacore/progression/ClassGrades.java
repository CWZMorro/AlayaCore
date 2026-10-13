package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.util.Percent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * Grants class grade advancements and syncs the highest current grade for the HUD badge.
 * Grades persist except Grand, which follows the title; content mods generate their grades with {@link #chain}.
 */
public final class ClassGrades {
	private static final String PATH = "classes/";
	public static final Identifier ROOT = AlayaCore.id(PATH + "root");
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("block/chiseled_quartz_block_top");

	// The player's highest grade in their current class, for the HUD badge. Worked out from the advancements; not saved.
	public static final AttachmentType<ClassGrade> GRADE = AttachmentRegistry.create(AlayaCore.id("class_grade"), builder -> builder
		.syncWith(ByteBufCodecs.idMapper(i -> ClassGrade.values()[i], ClassGrade::ordinal), AttachmentSyncPredicate.targetOnly()));

	private ClassGrades() {
	}

	public static Identifier advancement(Identifier classId, ClassGrade grade) {
		return classId.withPath(path -> PATH + path + "/" + grade.key());
	}

	public static AdvancementHolder root(ItemStackTemplate icon) {
		return GrantedAdvancements.impossible(Advancement.Builder.advancement()
			.display(icon, Component.translatable("advancements.alayacore.classes.title"),
				Component.translatable("advancements.alayacore.classes.description"), BACKGROUND, AdvancementType.TASK, false, false, false))
			.build(ROOT);
	}

	/**
	 * Generates a class's grade chain under {@code root}; {@link #describe} fills config-backed descriptions at load time.
	 */
	public static List<AdvancementHolder> chain(AdvancementHolder root, Identifier classId, Function<ClassGrade, ItemStackTemplate> icon) {
		List<AdvancementHolder> chain = new ArrayList<>();
		AdvancementHolder parent = root;
		Component className = ServantClass.name(classId);
		for (ClassGrade grade : ClassGrade.values()) {
			parent = GrantedAdvancements.impossible(Advancement.Builder.advancement()
				.parent(parent)
				.display(icon.apply(grade), grade.title(className), grade.description(className), null, grade.frame(), true, true, grade.hidden()))
				.build(advancement(classId, grade));
			chain.add(parent);
		}
		return chain;
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> describe(server.getAdvancements()));
	}

	/**
	 * Updates grade descriptions from the live config; clients see changes on their next full advancement update.
	 */
	public static void describe(ServerAdvancementManager advancements) {
		ProgressionConfig config = AlayaConfig.progression();
		for (ServantClass servantClass : AlayaRegistries.SERVANT_CLASS) {
			Identifier classId = AlayaRegistries.SERVANT_CLASS.getKey(servantClass);
			Component className = ServantClass.name(classId);
			for (ClassGrade grade : ClassGrade.values()) {
				AdvancementHolder holder = advancements.get(advancement(classId, grade));
				if (holder != null) {
					GrantedAdvancements.describe(holder.value(), grade.description(className, Percent.fromFraction(config.syncFor(grade))));
				}
			}
		}
	}

	public static void onBecameServant(ServerPlayer player) {
		grantOnce(player, ROOT);
	}

	/**
	 * Grants earned grades in the current class, revokes Grand when the title is lost, and updates the badge.
	 */
	public static void refresh(ServerPlayer player) {
		Optional<Identifier> classId = AlayaServants.classId(player);
		if (classId.isEmpty()) {
			player.removeAttached(GRADE);
			return;
		}
		ClassGrade earned = Progression.grade(player);
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

	public static ClassGrade of(ServerPlayer player) {
		return player.getAttachedOrElse(GRADE, ClassGrade.BLACK);
	}

	public static void revoke(ServerPlayer player, ClassGrade grade) {
		AlayaServants.classId(player).ifPresent(classId -> GrantedAdvancements.revoke(player, advancement(classId, grade)));
	}

	public static void onAdvancementChanged(ServerPlayer player, Identifier advancement) {
		if (advancement.getPath().startsWith(PATH)) {
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
