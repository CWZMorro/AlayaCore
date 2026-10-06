package io.github.cwzmorro.alayacore.config;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * Holds a content mod's settings registered through {@link AlayaConfig#register}.
 * Returns defaults until the first load and refreshes with each config reload.
 */
public final class ConfigTable<T> {
	private final String key;
	private final @Nullable String comment;
	private final Function<ConfigSection, T> reader;
	private volatile T value;

	ConfigTable(String key, @Nullable String comment, T defaults, Function<ConfigSection, T> reader) {
		this.key = key;
		this.comment = comment;
		this.value = defaults;
		this.reader = reader;
	}

	public T get() {
		return this.value;
	}

	String key() {
		return this.key;
	}

	void read(ConfigSection root) {
		this.value = this.reader.apply(root.section(this.key, this.comment));
	}
}
