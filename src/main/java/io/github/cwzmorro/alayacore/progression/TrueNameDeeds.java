package io.github.cwzmorro.alayacore.progression;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;

/**
 * A servant's legend, from {@code data/<namespace>/alayacore/true_name/<servant>.json}; the
 * file's id is the servant's id. Library users write their own.
 *
 * @param branch     the servant's advancement branch, granted when a player becomes that servant
 * @param deeds      done in this order, each counting only once the one before it is done; all of them
 *                   before the true name can awaken
 * @param awakening  the true name advancement, granted by defeating a boss once the deeds are done
 */
public record TrueNameDeeds(Identifier branch, List<Deed> deeds, Identifier awakening) {
	public record Deed(Identifier advancement, Condition<?, ?> condition) {
		private static final String ADVANCEMENT = "advancement";

		public static final Codec<Deed> CODEC = new MapCodec<Deed>() {
			@Override
			public <T> Stream<T> keys(DynamicOps<T> ops) {
				return Stream.of(ops.createString(ADVANCEMENT));
			}

			@Override
			public <T> DataResult<Deed> decode(DynamicOps<T> ops, MapLike<T> input) {
				T advancement = input.get(ADVANCEMENT);
				if (advancement == null) {
					return DataResult.error(() -> "a deed needs \"" + ADVANCEMENT + "\"");
				}
				List<Pair<T, T>> conditions = input.entries()
					.filter(entry -> ops.getStringValue(entry.getFirst()).result().filter(ADVANCEMENT::equals).isEmpty())
					.toList();
				return Identifier.CODEC.parse(ops, advancement).flatMap(id -> conditions.size() == 1
					? Condition.read(ops, conditions.getFirst()).map(condition -> new Deed(id, condition))
					: DataResult.error(() -> "deed " + id + " needs exactly one condition, has " + conditions.size()));
			}

			@Override
			public <T> RecordBuilder<T> encode(Deed deed, DynamicOps<T> ops, RecordBuilder<T> prefix) {
				prefix.add(ADVANCEMENT, Identifier.CODEC.encodeStart(ops, deed.advancement));
				return deed.condition.write(ops, prefix);
			}
		}.codec();
	}

	public record Condition<C, E>(DeedKind<C, E> kind, C value) {
		public <F> boolean metBy(DeedKind<?, F> happened, F event) {
			if (happened != this.kind) {
				return false;
			}
			// The same kind, so F is E.
			@SuppressWarnings("unchecked")
			E same = (E) event;
			return this.kind.met().test(this.value, same);
		}

		private static <T> DataResult<Condition<?, ?>> read(DynamicOps<T> ops, Pair<T, T> entry) {
			return ops.getStringValue(entry.getFirst())
				.flatMap(key -> Identifier.read(key.indexOf(Identifier.NAMESPACE_SEPARATOR) < 0
					? AlayaCore.MOD_ID + Identifier.NAMESPACE_SEPARATOR + key
					: key))
				.flatMap(id -> AlayaRegistries.DEED_KIND.getOptional(id)
					.map(DataResult::success)
					.orElseGet(() -> DataResult.error(() -> "unknown deed kind " + id)))
				.flatMap(kind -> value(kind, ops, entry.getSecond()));
		}

		private static <C, E, T> DataResult<Condition<?, ?>> value(DeedKind<C, E> kind, DynamicOps<T> ops, T value) {
			return kind.codec().parse(ops, value).map(read -> new Condition<>(kind, read));
		}

		private <T> RecordBuilder<T> write(DynamicOps<T> ops, RecordBuilder<T> builder) {
			Identifier id = AlayaRegistries.DEED_KIND.getKey(this.kind);
			String key = id.getNamespace().equals(AlayaCore.MOD_ID) ? id.getPath() : id.toString();
			return builder.add(key, this.kind.codec().encodeStart(ops, this.value));
		}
	}

	public static final Codec<TrueNameDeeds> CODEC = RecordCodecBuilder.create(i -> i.group(
		Identifier.CODEC.fieldOf("branch").forGetter(TrueNameDeeds::branch),
		Deed.CODEC.listOf().fieldOf("deeds").forGetter(TrueNameDeeds::deeds),
		Identifier.CODEC.fieldOf("awakening").forGetter(TrueNameDeeds::awakening)
	).apply(i, TrueNameDeeds::new));
}
