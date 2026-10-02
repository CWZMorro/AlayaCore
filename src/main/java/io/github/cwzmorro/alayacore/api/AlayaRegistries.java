package io.github.cwzmorro.alayacore.api;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.servant.Servant;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Registries other mods register their content into.
 *
 * <p>All are synced, so a client only joins a server with the same content.
 * They are created when this class is first loaded, so a content mod can use them from its own
 * initializer whatever order Fabric runs the initializers in.
 */
public final class AlayaRegistries {
	public static final ResourceKey<Registry<ServantClass>> SERVANT_CLASS_KEY = ResourceKey.createRegistryKey(AlayaCore.id("servant_class"));
	public static final ResourceKey<Registry<Servant>> SERVANT_KEY = ResourceKey.createRegistryKey(AlayaCore.id("servant"));
	public static final ResourceKey<Registry<Ability>> ABILITY_KEY = ResourceKey.createRegistryKey(AlayaCore.id("ability"));

	public static final Registry<ServantClass> SERVANT_CLASS = FabricRegistryBuilder.create(SERVANT_CLASS_KEY)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();
	public static final Registry<Servant> SERVANT = FabricRegistryBuilder.create(SERVANT_KEY)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();
	public static final Registry<Ability> ABILITY = FabricRegistryBuilder.create(ABILITY_KEY)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();

	private AlayaRegistries() {
	}

	/** Loads this class, which creates the registries. */
	public static void init() {
	}
}
