package io.github.cwzmorro.alayacore.progression;

import java.util.Locale;
import net.minecraft.advancements.AdvancementType;

/**
 * A class grade (plan 13.4.1), lowest first. Each is an advancement in the "Classes" tab, per class,
 * and picks the HUD badge's art.
 */
public enum ClassGrade {
	BLACK(AdvancementType.TASK, false),
	BRONZE(AdvancementType.TASK, false),
	SILVER(AdvancementType.TASK, false),
	GOLD(AdvancementType.CHALLENGE, false),
	GRAND(AdvancementType.CHALLENGE, true);

	private final AdvancementType frame;
	private final boolean hidden;

	ClassGrade(AdvancementType frame, boolean hidden) {
		this.frame = frame;
		this.hidden = hidden;
	}

	public AdvancementType frame() {
		return this.frame;
	}

	public boolean hidden() {
		return this.hidden;
	}

	/** Lower-case name, used in advancement ids, texture paths and translation keys. */
	public String key() {
		return this.name().toLowerCase(Locale.ROOT);
	}
}
