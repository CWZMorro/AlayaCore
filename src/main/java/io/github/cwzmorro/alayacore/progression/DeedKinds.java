package io.github.cwzmorro.alayacore.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;

/** Alaya Core's own deed kinds. */
public final class DeedKinds {
	// "kill": "<entity type>" or "kill": "#<entity type tag>": the player kills such an entity.
	public static final DeedKind<ExtraCodecs.TagOrElementLocation, LivingEntity> KILL = register("kill",
		new DeedKind<>(ExtraCodecs.TAG_OR_ELEMENT_ID, DeedKinds::kills));
	// "win_raid": true: the player is a hero of a won raid.
	public static final DeedKind<Unit, Unit> WIN_RAID = register("win_raid", new DeedKind<>(Codec.BOOL.comapFlatMap(
		won -> won ? DataResult.success(Unit.INSTANCE) : DataResult.error(() -> "\"win_raid\" can only be true"),
		raid -> true), (condition, raid) -> true));

	private DeedKinds() {
	}

	public static void init() {
	}

	private static <C, E> DeedKind<C, E> register(String name, DeedKind<C, E> kind) {
		return Registry.register(AlayaRegistries.DEED_KIND, AlayaCore.id(name), kind);
	}

	private static boolean kills(ExtraCodecs.TagOrElementLocation target, LivingEntity killed) {
		return target.tag()
			? killed.is(TagKey.create(Registries.ENTITY_TYPE, target.id()))
			: killed.is(ResourceKey.create(Registries.ENTITY_TYPE, target.id()));
	}
}
