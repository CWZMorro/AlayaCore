package io.github.cwzmorro.alayacore.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.rank.Rank;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * What an attack (the beam, projectile or other entity an ability creates) carries (plan 9): who fired it, its
 * rank, and whether it can clash. The ability attaches it when it creates the attack ({@link Attacks}). Shields read
 * its rank; clashes need both attacks able to clash. Attacks that never clash (burst NPs, UBW, Gate of Babylon's
 * weapons) leave {@code canClash} false; other mods' attacks carry none.
 *
 * @param owner the entity that fired it: its mana pays for a clash, and it takes the hit if it loses
 */
public record AttackData(UUID owner, Rank rank, boolean canClash) {
	public static final Codec<AttackData> CODEC = RecordCodecBuilder.create(i -> i.group(
		UUIDUtil.CODEC.fieldOf("owner").forGetter(AttackData::owner),
		Rank.CODEC.fieldOf("rank").forGetter(AttackData::rank),
		Codec.BOOL.fieldOf("can_clash").forGetter(AttackData::canClash)
	).apply(i, AttackData::new));
}
