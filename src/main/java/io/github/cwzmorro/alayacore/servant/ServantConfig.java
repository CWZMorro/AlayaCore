package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * The {@code [servants]} and {@code [classes]} sections of {@code config/alayacore.toml}.
 *
 * @param classes the values of every registered class, by class id
 */
public record ServantConfig(boolean allowDuplicates, Map<Identifier, ServantClassConfig> classes) {
	public static final ServantConfig DEFAULTS = new ServantConfig(false, Map.of());
	/** Written once above all classes instead of under each one. */
	public static final String CLASSES_COMMENT = " Real values per servant class (sync 100% + true name), added to the player's own and their gear:\n"
		+ "   max_hp, attack, armor, toughness, knockback_resistance, speed_percent (over walking speed)\n"
		+ "   mana_regen_bonus_percent = extra mana regeneration of every kind (flat, food, sleep)\n"
		+ "   regen_while_casting = mana keeps regenerating while casting or holding an ability";

	/**
	 * @param servants      the {@code [servants]} section
	 * @param classes       the {@code [classes]} section
	 * @param classDefaults every registered class with its built-in values, in registry order
	 */
	public static ServantConfig read(ConfigSection servants, ConfigSection classes, Map<Identifier, ServantClassConfig> classDefaults) {
		boolean allowDuplicates = servants.bool("allow_duplicates",
			" Let several players be the same servant. Off: one player per servant.", DEFAULTS.allowDuplicates);
		Map<Identifier, ServantClassConfig> values = new LinkedHashMap<>();
		classDefaults.forEach((id, defaults) -> values.put(id, ServantClassConfig.read(classes.section(id.toString(), null), defaults)));
		return new ServantConfig(allowDuplicates, Collections.unmodifiableMap(values));
	}

	/** This class's values; its built-in ones if the config was not read yet. */
	public ServantClassConfig classConfig(Identifier id, ServantClass servantClass) {
		return this.classes.getOrDefault(id, servantClass.defaults());
	}
}
