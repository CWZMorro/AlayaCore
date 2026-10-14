package io.github.cwzmorro.alayacore.servant;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * Servant classes as data: synced definitions in {@code data/<namespace>/alayacore/servant_class/<class path>.json}.
 * Alaya Core's own are the seven standard ones in Fate's usual order, then Ruler and Shielder, with {@code order} 0 to 8.
 */
public final class ServantClasses {
	public static final ResourceKey<ServantClass> SABER = key("saber");
	public static final ResourceKey<ServantClass> ARCHER = key("archer");
	public static final ResourceKey<ServantClass> LANCER = key("lancer");
	public static final ResourceKey<ServantClass> RIDER = key("rider");
	public static final ResourceKey<ServantClass> CASTER = key("caster");
	public static final ResourceKey<ServantClass> ASSASSIN = key("assassin");
	public static final ResourceKey<ServantClass> BERSERKER = key("berserker");
	public static final ResourceKey<ServantClass> RULER = key("ruler");
	public static final ResourceKey<ServantClass> SHIELDER = key("shielder");

	private ServantClasses() {
	}

	private static ResourceKey<ServantClass> key(String name) {
		return ResourceKey.create(AlayaRegistries.SERVANT_CLASS_KEY, AlayaCore.id(name));
	}

	public static void register() {
		DynamicRegistries.registerSynced(AlayaRegistries.SERVANT_CLASS_KEY, ServantClass.CODEC);
	}

	public static Map<Identifier, ServantClass> ordered(RegistryAccess registries) {
		Map<Identifier, ServantClass> classes = new LinkedHashMap<>();
		registries.lookup(AlayaRegistries.SERVANT_CLASS_KEY).ifPresent(registry -> registry.listElements()
			.sorted(Comparator.<Holder.Reference<ServantClass>>comparingInt(holder -> holder.value().order())
				.thenComparing(holder -> holder.key().identifier()))
			.forEach(holder -> classes.put(holder.key().identifier(), holder.value())));
		return Collections.unmodifiableMap(classes);
	}

	public static Optional<ServantClass> get(RegistryAccess registries, ResourceKey<ServantClass> key) {
		return registries.lookup(AlayaRegistries.SERVANT_CLASS_KEY).flatMap(registry -> registry.get(key)).map(Holder::value);
	}
}
