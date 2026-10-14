package io.github.cwzmorro.alayacore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import io.github.cwzmorro.alayacore.rank.Rank;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * One table of the config file, read and rebuilt in the same pass.
 *
 * <p>Each key is declared with its default, limits and comment; the call returns the value to use and writes it into
 * the saved file. A missing key gets its default; an invalid value falls back to it with a warning.
 */
public final class ConfigSection {
	private static final String NOT_A_RANK = "must be a rank from E to EX++";

	private final @Nullable UnmodifiableConfig input;
	private final CommentedConfig output;
	private final String path;
	private final List<String> warnings;
	private final Set<UnmodifiableConfig> inlineTables;
	// False when there was no file, or this whole table is missing (that is reported once).
	private final boolean warnMissing;

	ConfigSection(@Nullable UnmodifiableConfig input, CommentedConfig output, String path, List<String> warnings,
		Set<UnmodifiableConfig> inlineTables, boolean warnMissing) {
		this.input = input;
		this.output = output;
		this.path = path;
		this.warnings = warnings;
		this.inlineTables = inlineTables;
		this.warnMissing = warnMissing;
	}

	/** Reads a finite number within inclusive bounds, falling back with a warning if invalid. */
	public double number(String key, @Nullable String comment, double fallback, double min, double max) {
		double value = this.read(key, fallback, min, max);
		this.write(key, comment, value);
		return value;
	}

	public int integer(String key, @Nullable String comment, int fallback, int min, int max) {
		double value = this.read(key, fallback, min, max);
		int whole = (int) value;
		if (whole != value) {
			this.warn(key, "must be a whole number");
			whole = fallback;
		}
		this.write(key, comment, whole);
		return whole;
	}

	public Rank rank(String key, @Nullable String comment, Rank fallback) {
		Rank value = fallback;
		Object raw = this.raw(key);
		if (raw instanceof String text) {
			try {
				value = Rank.parse(text);
			} catch (IllegalArgumentException e) {
				this.warn(key, NOT_A_RANK);
			}
		} else if (raw != null) {
			this.warn(key, NOT_A_RANK);
		}
		this.write(key, comment, value.toString());
		return value;
	}

	public boolean bool(String key, @Nullable String comment, boolean fallback) {
		boolean value = fallback;
		Object raw = this.raw(key);
		if (raw instanceof Boolean b) {
			value = b;
		} else if (raw != null) {
			this.warn(key, "must be true or false");
		}
		this.write(key, comment, value);
		return value;
	}

	/** Reads exactly {@code fallback.size()} finite numbers within inclusive bounds. */
	public List<Double> numbers(String key, @Nullable String comment, List<Double> fallback, double min, double max) {
		List<Double> value = fallback;
		Object raw = this.raw(key);
		if (raw != null) {
			value = parseNumbers(raw, fallback.size(), min, max);
			if (value == null) {
				this.warn(key, "must be a list of " + fallback.size() + " numbers between " + min + " and " + max);
				value = fallback;
			}
		}
		this.write(key, comment, value);
		return value;
	}

	/** Reads an extensible id-to-number table; uses defaults only when the table is absent. */
	public Map<Identifier, Double> numbersById(Map<Identifier, Double> fallback, double min, double max) {
		if (this.input == null) {
			fallback.forEach((id, value) -> this.write(id.toString(), null, value));
			return fallback;
		}
		Map<Identifier, Double> values = new LinkedHashMap<>();
		for (UnmodifiableConfig.Entry entry : this.input.entrySet()) {
			Identifier id = Identifier.tryParse(entry.getKey());
			Double value = asNumber(entry.getRawValue(), min, max);
			if (id == null || value == null) {
				this.warn(entry.getKey(), "must be an id with a number between " + min + " and " + max + "; entry ignored");
				continue;
			}
			values.put(id, value);
			this.write(id.toString(), null, value);
		}
		return Collections.unmodifiableMap(values);
	}

	public ConfigSection section(String key, @Nullable String comment) {
		return this.child(key, comment, false);
	}

	/** Reads a sub-table written inline ({@code key = { … }}), where TOML cannot store child comments. */
	public ConfigSection inlineSection(String key, @Nullable String comment) {
		return this.child(key, comment, true);
	}

	public void warn(String key, String problem) {
		this.warnings.add(this.qualified(key) + " " + problem);
	}

	private ConfigSection child(String key, @Nullable String comment, boolean inline) {
		Object raw = this.lookup(key);
		UnmodifiableConfig childInput = raw instanceof UnmodifiableConfig config ? config : null;
		if (raw != null && childInput == null) {
			this.warn(key, "must be a table; using defaults");
		} else if (raw == null && this.warnMissing) {
			this.warn(key, "is missing; using defaults");
		}
		CommentedConfig childOutput = this.output.createSubConfig();
		if (inline) {
			this.inlineTables.add(childOutput);
		}
		this.write(key, comment, childOutput);
		return new ConfigSection(childInput, childOutput, this.qualified(key), this.warnings, this.inlineTables, childInput != null && this.warnMissing);
	}

	private double read(String key, double fallback, double min, double max) {
		Object raw = this.raw(key);
		if (raw == null) {
			return fallback;
		}
		Double value = asNumber(raw, min, max);
		if (value == null) {
			this.warn(key, "must be a number between " + min + " and " + max);
			return fallback;
		}
		return value;
	}

	private @Nullable Object raw(String key) {
		Object raw = this.lookup(key);
		if (raw == null && this.warnMissing) {
			this.warn(key, "is missing; using the default");
		}
		return raw;
	}

	private @Nullable Object lookup(String key) {
		return this.input == null ? null : this.input.get(List.of(key));
	}

	private void write(String key, @Nullable String comment, Object value) {
		List<String> keyPath = List.of(key);
		this.output.set(keyPath, value);
		if (comment != null) {
			this.output.setComment(keyPath, comment);
		}
	}

	private String qualified(String key) {
		return this.path.isEmpty() ? key : this.path + "." + key;
	}

	private static @Nullable Double asNumber(Object raw, double min, double max) {
		if (raw instanceof Number number) {
			double value = number.doubleValue();
			if (Double.isFinite(value) && value >= min && value <= max) {
				return value;
			}
		}
		return null;
	}

	private static @Nullable List<Double> parseNumbers(Object raw, int size, double min, double max) {
		if (!(raw instanceof List<?> list) || list.size() != size) {
			return null;
		}
		Double[] values = new Double[size];
		for (int i = 0; i < size; i++) {
			values[i] = asNumber(list.get(i), min, max);
			if (values[i] == null) {
				return null;
			}
		}
		return List.of(values);
	}
}
