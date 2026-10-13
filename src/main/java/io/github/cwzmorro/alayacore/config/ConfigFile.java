package io.github.cwzmorro.alayacore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import io.github.cwzmorro.alayacore.AlayaCore;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

final class ConfigFile {
	private ConfigFile() {
	}

	// Leave unparseable files untouched and do not apply partial settings.
	static List<String> sync(Path file, String header, boolean useExisting, Consumer<ConfigSection> reader) {
		List<String> warnings = new ArrayList<>();
		CommentedConfig input = null;
		String existing = null;
		if (Files.exists(file)) {
			try {
				existing = Files.readString(file, StandardCharsets.UTF_8);
				if (useExisting) {
					input = TomlFormat.instance().createParser().parse(new StringReader(existing));
				}
			} catch (IOException | ParsingException e) {
				warnings.add("cannot read " + file + ", keeping the current values: " + e.getMessage());
				return report(warnings);
			}
		}

		CommentedConfig output = TomlFormat.newConfig(LinkedHashMap::new);
		Set<UnmodifiableConfig> inlineTables = Collections.newSetFromMap(new IdentityHashMap<>());
		reader.accept(new ConfigSection(input, output, "", warnings, inlineTables, input != null));

		String text = header + toToml(output, inlineTables);
		if (!text.equals(existing)) {
			write(file, text, warnings);
		}
		return report(warnings);
	}

	private static String toToml(CommentedConfig config, Set<UnmodifiableConfig> inlineTables) {
		TomlWriter writer = TomlFormat.instance().createWriter();
		writer.setWriteTableInlinePredicate(inlineTables::contains);
		StringWriter out = new StringWriter();
		writer.write(config, out);
		return out.toString();
	}

	private static void write(Path file, String text, List<String> warnings) {
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(temp, text, StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			warnings.add("cannot write " + file + ": " + e.getMessage());
		}
	}

	private static List<String> report(List<String> warnings) {
		warnings.forEach(warning -> AlayaCore.LOGGER.warn("Config: {}", warning));
		return List.copyOf(warnings);
	}
}
