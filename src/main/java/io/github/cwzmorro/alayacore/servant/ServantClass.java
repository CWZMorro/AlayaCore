package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.progression.ClassGrade;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A servant class (Saber, Archer, …). Registered in {@link AlayaRegistries#SERVANT_CLASS}, so other
 * mods can add their own. Its values are config defaults; the config can change them.
 */
public final class ServantClass {
	private final ServantClassConfig defaults;

	public ServantClass(ServantClassConfig defaults) {
		this.defaults = defaults;
	}

	public ServantClassConfig defaults() {
		return this.defaults;
	}

	/** Translation key {@code servant_class.<namespace>.<path>}. */
	public static Component name(Identifier id) {
		return Component.translatable(id.toLanguageKey("servant_class"));
	}

	/**
	 * The class badge for a grade, shown on the HUD: {@code <namespace>:textures/gui/servant_class/<path>/<grade>.png}.
	 * The texture holds four copies side by side, 38, 76, 114 and 152 pixels square (380×152 in all); the HUD draws
	 * the one that matches the screen.
	 */
	public static Identifier badge(Identifier id, ClassGrade grade) {
		return id.withPath(path -> "textures/gui/servant_class/" + path + "/" + grade.key() + ".png");
	}
}
