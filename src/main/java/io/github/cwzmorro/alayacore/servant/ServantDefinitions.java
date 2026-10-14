package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Registered servants together with data definitions; hidden data never enters a selection pool. */
public final class ServantDefinitions {
	public static final ResourceKey<Registry<ServantDefinition>> KEY = ResourceKey.createRegistryKey(AlayaCore.id("servant_definition"));

	private ServantDefinitions() {
	}

	public static void register() {
		DynamicRegistries.registerSynced(KEY, ServantDefinition.CODEC);
	}

	/** Resolves data first, then code-registered content; hidden entries remain readable as data. */
	public static Optional<Servant> get(RegistryAccess registries, Identifier id) {
		Optional<Servant> data = registries.lookup(KEY).flatMap(registry -> registry.getOptional(id)).map(ServantDefinition::servant);
		return data.or(() -> AlayaRegistries.SERVANT.getOptional(id));
	}

	public static Map<Identifier, Servant> selectable(RegistryAccess registries) {
		Map<Identifier, Servant> servants = new LinkedHashMap<>();
		for (Servant servant : AlayaRegistries.SERVANT) {
			servants.put(AlayaRegistries.SERVANT.getKey(servant), servant);
		}
		registries.lookup(KEY).ifPresent(definitions -> {
			for (ServantDefinition definition : definitions) {
				Identifier id = definitions.getKey(definition);
				if (definition.hidden()) {
					servants.remove(id);
				} else {
					servants.put(id, definition.servant());
				}
			}
		});
		return Collections.unmodifiableMap(servants);
	}
}
