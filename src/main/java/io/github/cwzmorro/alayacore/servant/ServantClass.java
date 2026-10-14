package io.github.cwzmorro.alayacore.servant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.progression.ClassGrade;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A servant class (Saber, Archer, …), data in {@code data/<namespace>/alayacore/servant_class/<class path>.json} (see
 * {@link ServantClasses}), so other mods and data packs can add their own. Its values are config defaults; the config can change them.
 *
 * @param order    where the class stands on the selection screen's contents, lowest first; classes with the same order follow by id
 * @param defaults the class's built-in values
 */
public record ServantClass(int order, ServantClassConfig defaults) {
	public static final Codec<ServantClass> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("order", 0).forGetter(ServantClass::order),
		ServantClassConfig.CODEC.forGetter(ServantClass::defaults)
	).apply(i, ServantClass::new));

	public static Component name(Identifier id) {
		return Component.translatable(id.toLanguageKey("servant_class"));
	}

	/**
	 * Returns {@code <namespace>:textures/gui/servant_select/icon/<path>.png} for the selection screen.
	 * The texture must contain side-by-side square copies at 16, 32, 48 and 64 pixels.
	 */
	public static Identifier icon(Identifier id) {
		return id.withPath(path -> "textures/gui/servant_select/icon/" + path + ".png");
	}

	/**
	 * Returns {@code <namespace>:textures/gui/servant_class/<path>/<grade>.png} for the HUD.
	 * The texture must contain side-by-side square copies at 38, 76, 114 and 152 pixels.
	 */
	public static Identifier badge(Identifier id, ClassGrade grade) {
		return id.withPath(path -> "textures/gui/servant_class/" + path + "/" + grade.key() + ".png");
	}
}
