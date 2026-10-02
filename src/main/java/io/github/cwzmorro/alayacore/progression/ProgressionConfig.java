package io.github.cwzmorro.alayacore.progression;

import io.github.cwzmorro.alayacore.config.ConfigSection;
import io.github.cwzmorro.alayacore.servant.ServantStats;
import io.github.cwzmorro.alayacore.util.Percent;

/**
 * The {@code [progression]} section of {@code config/alayacore.toml} (plan 13.2).
 *
 * <p>sync = log(max mana ÷ start) ÷ log(full ÷ start), 0–1. The stat bar = sync share × sync^exponent,
 * plus the true name share once the true name is realized. Every stat sits that far between its demi
 * value and its class's real value. Class grades (plan 13.4.1) follow sync and the true name.
 */
public record ProgressionConfig(
	double syncStartMana,
	double syncFullMana,
	double syncSharePercent,
	double syncExponent,
	double trueNameSharePercent,
	double bronzeSyncPercent,
	double silverSyncPercent,
	ServantStats demi
) {
	public static final ProgressionConfig DEFAULTS = new ProgressionConfig(
		1000.0,
		100_000.0,
		70.0,
		2.0,
		30.0,
		50.0,
		100.0,
		new ServantStats(30.0, 2.0, 2.0, 0.0, 5.0, 0.0)
	);

	public static ProgressionConfig read(ConfigSection s) {
		double start = s.number("sync_start_mana", " Max mana at which sync is 0%.", DEFAULTS.syncStartMana, 1.0, Double.MAX_VALUE);
		double full = s.number("sync_full_mana", " Max mana at which sync is 100%. Mana keeps growing after that; sync stays 100%.",
			DEFAULTS.syncFullMana, 1.0, Double.MAX_VALUE);
		if (full <= start) {
			s.warn("sync_full_mana", "must be above sync_start_mana; using the defaults");
			start = DEFAULTS.syncStartMana;
			full = DEFAULTS.syncFullMana;
		}
		double bronze = s.number("bronze_sync_percent", " Class grades: sync for Bronze.", DEFAULTS.bronzeSyncPercent, 0.0, Percent.WHOLE);
		double silver = s.number("silver_sync_percent", " Class grades: sync for Silver; Gold is Silver plus the true name.",
			DEFAULTS.silverSyncPercent, 0.0, Percent.WHOLE);
		if (silver < bronze) {
			s.warn("silver_sync_percent", "must not be below bronze_sync_percent; using the defaults");
			bronze = DEFAULTS.bronzeSyncPercent;
			silver = DEFAULTS.silverSyncPercent;
		}
		return new ProgressionConfig(
			start,
			full,
			s.number("sync_share_percent", " Part of the stat bar that sync fills.", DEFAULTS.syncSharePercent, 0.0, Percent.WHOLE),
			s.number("sync_exponent", " Sync is raised to this power before filling the bar (2 = slow at first, faster later).",
				DEFAULTS.syncExponent, 0.0, Double.MAX_VALUE),
			s.number("true_name_share_percent", " Part of the stat bar that true name realization fills at once.",
				DEFAULTS.trueNameSharePercent, 0.0, Percent.WHOLE),
			bronze,
			silver,
			ServantStats.read(s.section("demi", " Demi values of every class (sync 0%)."), DEFAULTS.demi)
		);
	}

	/** Sync from max mana: 0 at the start value, 1 at the full value and beyond. */
	public double sync(double maxMana) {
		if (maxMana <= this.syncStartMana) {
			return 0.0;
		}
		return Math.min(1.0, Math.log(maxMana / this.syncStartMana) / Math.log(this.syncFullMana / this.syncStartMana));
	}

	/** The least max mana that gives {@code sync} (0 to 1): the inverse of {@link #sync}. */
	public double manaForSync(double sync) {
		double mana = this.syncStartMana * Math.pow(this.syncFullMana / this.syncStartMana, sync);
		// Rounding can leave it a hair short of the sync asked for.
		while (this.sync(mana) < sync) {
			mana = Math.nextUp(mana);
		}
		return mana;
	}

	/** The sync {@code grade} needs (Gold also needs the true name; Grand, the title). */
	public double syncFor(ClassGrade grade) {
		return switch (grade) {
			case BLACK -> 0.0;
			case BRONZE -> Percent.toFraction(this.bronzeSyncPercent);
			case SILVER, GOLD, GRAND -> Percent.toFraction(this.silverSyncPercent);
		};
	}

	/** The highest grade sync and the true name have earned; Grand comes from the title instead. */
	public ClassGrade grade(double sync, boolean trueName) {
		if (sync < Percent.toFraction(this.bronzeSyncPercent)) {
			return ClassGrade.BLACK;
		}
		if (sync < Percent.toFraction(this.silverSyncPercent)) {
			return ClassGrade.BRONZE;
		}
		return trueName ? ClassGrade.GOLD : ClassGrade.SILVER;
	}

	/** How full the demi → real bar is (0–1). */
	public double barFill(double sync, boolean trueName) {
		double fill = Percent.toFraction(this.syncSharePercent) * Math.pow(sync, this.syncExponent);
		if (trueName) {
			fill += Percent.toFraction(this.trueNameSharePercent);
		}
		return Math.clamp(fill, 0.0, 1.0);
	}
}
