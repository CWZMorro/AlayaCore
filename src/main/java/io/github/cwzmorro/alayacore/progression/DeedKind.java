package io.github.cwzmorro.alayacore.progression;

import com.mojang.serialization.Codec;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import java.util.function.BiPredicate;

/**
 * A kind of true-name deed, registered in {@link AlayaRegistries#DEED_KIND}. Deed data names its kind by id (without
 * the namespace for Alaya Core's own, see {@link DeedKinds}) and gives the condition under that key. Whoever sees it
 * happen calls {@link TrueName#happened}; the player's next deed is done if the kind and condition match.
 *
 * @param codec reads a deed's condition, the value under the kind's key
 * @param met   whether a condition is met by what happened
 * @param <C>   a deed's condition
 * @param <E>   what happened
 */
public record DeedKind<C, E>(Codec<C> codec, BiPredicate<C, E> met) {
}
