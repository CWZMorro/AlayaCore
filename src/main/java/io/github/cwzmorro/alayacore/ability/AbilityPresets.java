package io.github.cwzmorro.alayacore.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * A player's ability presets (plan 8): {@value #PRESETS} presets × {@value #SLOTS} slots, each slot
 * holding an ability and the mode it is used in. Plain Java, immutable.
 *
 * @param active the preset the slot keys use
 * @param slots  the filled slots only
 */
public record AbilityPresets(int active, List<Slot> slots) {
	public static final int PRESETS = 5;
	public static final int SLOTS = 3;
	public static final AbilityPresets EMPTY = new AbilityPresets(0, List.of());

	/** One filled slot. */
	public record Slot(int preset, int slot, Identifier ability, int mode) {
		static final Codec<Slot> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("preset").forGetter(Slot::preset),
			Codec.INT.fieldOf("slot").forGetter(Slot::slot),
			Identifier.CODEC.fieldOf("ability").forGetter(Slot::ability),
			Codec.INT.fieldOf("mode").forGetter(Slot::mode)
		).apply(i, Slot::new));
		static final StreamCodec<ByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Slot::preset,
			ByteBufCodecs.VAR_INT, Slot::slot,
			Identifier.STREAM_CODEC, Slot::ability,
			ByteBufCodecs.VAR_INT, Slot::mode,
			Slot::new
		);
	}

	public static final Codec<AbilityPresets> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.fieldOf("active").forGetter(AbilityPresets::active),
		Slot.CODEC.listOf().fieldOf("slots").forGetter(AbilityPresets::slots)
	).apply(i, AbilityPresets::new));
	public static final StreamCodec<ByteBuf, AbilityPresets> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, AbilityPresets::active,
		Slot.STREAM_CODEC.apply(ByteBufCodecs.list()), AbilityPresets::slots,
		AbilityPresets::new
	);

	public AbilityPresets {
		slots = List.copyOf(slots);
	}

	public static boolean isValid(int preset, int slot) {
		return preset >= 0 && preset < PRESETS && slot >= 0 && slot < SLOTS;
	}

	public Optional<Slot> slot(int preset, int slot) {
		return this.slots.stream().filter(s -> s.preset == preset && s.slot == slot).findFirst();
	}

	public AbilityPresets withActive(int preset) {
		return new AbilityPresets(Math.floorMod(preset, PRESETS), this.slots);
	}

	/** Puts the ability in the slot (first mode), or empties the slot. */
	public AbilityPresets with(int preset, int slot, Optional<Identifier> ability) {
		List<Slot> slots = new ArrayList<>(this.slots);
		slots.removeIf(s -> s.preset == preset && s.slot == slot);
		ability.ifPresent(id -> slots.add(new Slot(preset, slot, id, 0)));
		return new AbilityPresets(this.active, slots);
	}

	/** Moves the slot's mode by {@code delta}, wrapping within the ability's {@code modes}. */
	public AbilityPresets withModeShifted(int preset, int slot, int delta, int modes) {
		return this.slot(preset, slot).map(s -> {
			List<Slot> slots = new ArrayList<>(this.slots);
			slots.set(slots.indexOf(s), new Slot(preset, slot, s.ability, Math.floorMod(s.mode + delta, modes)));
			return new AbilityPresets(this.active, slots);
		}).orElse(this);
	}
}
