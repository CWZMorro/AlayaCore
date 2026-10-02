package io.github.cwzmorro.alayacore.rank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/**
 * A rank such as B, A+ or EX++: a letter and 0–2 pluses.
 *
 * <p>Ordering goes by letter, then by pluses (A < A+ < A++ < EX). Clash and shield outcomes look
 * at letters only; see {@link RankRules}. Damage and mana numbers are in {@link RankConfig}.
 */
public record Rank(RankLetter letter, int pluses) implements Comparable<Rank> {
	public static final int MAX_PLUSES = 2;
	/** Saved as its text ("A+"). */
	public static final Codec<Rank> CODEC = Codec.STRING.comapFlatMap(text -> {
		try {
			return DataResult.success(parse(text));
		} catch (IllegalArgumentException e) {
			return DataResult.error(e::getMessage);
		}
	}, Rank::toString);

	public Rank {
		if (letter == null) {
			throw new IllegalArgumentException("rank letter is null");
		}
		if (pluses < 0 || pluses > MAX_PLUSES) {
			throw new IllegalArgumentException("pluses must be 0.." + MAX_PLUSES + ", got " + pluses);
		}
	}

	/** Reads "B", "a+", "EX++"; surrounding spaces are ignored. */
	public static Rank parse(String text) {
		String s = text.strip().toUpperCase(Locale.ROOT);
		int end = s.length();
		while (end > 0 && s.charAt(end - 1) == '+') {
			end--;
		}
		int pluses = s.length() - end;
		if (pluses > MAX_PLUSES) {
			throw new IllegalArgumentException("too many '+' in rank: " + text);
		}
		try {
			return new Rank(RankLetter.valueOf(s.substring(0, end)), pluses);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("unknown rank: " + text, e);
		}
	}

	@Override
	public int compareTo(Rank other) {
		int byLetter = this.letter.compareTo(other.letter);
		return byLetter != 0 ? byLetter : Integer.compare(this.pluses, other.pluses);
	}

	@Override
	public String toString() {
		return this.letter.name() + "+".repeat(this.pluses);
	}
}
