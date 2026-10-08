package io.github.cwzmorro.alayacore.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cwzmorro.alayacore.rank.Rank;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * What an attack (the beam, projectile or other entity an ability creates) carries: who fired it, its
 * rank, whether it can clash, and whether it is a noble phantasm. The ability attaches it when it creates the attack
 * ({@link Attacks}). Shields read its rank; clashes need both attacks able to clash. Attacks that never clash, such as
 * burst NPs, leave {@code canClash} false; other mods' attacks carry none.
 *
 * @param owner the entity that fired it: its mana pays for a clash; after losing it is hit only if in the winner's path
 * @param noblePhantasm whether a noble phantasm fired it, whether it can clash or not (shields treat NPs apart)
 */
public record AttackData(UUID owner, Rank rank, boolean canClash, boolean noblePhantasm) {
	public static final Codec<AttackData> CODEC = RecordCodecBuilder.create(i -> i.group(
		UUIDUtil.CODEC.fieldOf("owner").forGetter(AttackData::owner),
		Rank.CODEC.fieldOf("rank").forGetter(AttackData::rank),
		Codec.BOOL.fieldOf("can_clash").forGetter(AttackData::canClash),
		Codec.BOOL.optionalFieldOf("noble_phantasm", false).forGetter(AttackData::noblePhantasm)
	).apply(i, AttackData::new));
}
