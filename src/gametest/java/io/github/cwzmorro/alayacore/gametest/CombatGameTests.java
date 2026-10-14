package io.github.cwzmorro.alayacore.gametest;

import static io.github.cwzmorro.alayacore.gametest.TestPlayers.BESIDE;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.INSIDE;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.check;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.join;
import static io.github.cwzmorro.alayacore.gametest.TestPlayers.joinAs;

import io.github.cwzmorro.alayacore.combat.AttackData;
import io.github.cwzmorro.alayacore.combat.AttackDefence;
import io.github.cwzmorro.alayacore.combat.AttackSource;
import io.github.cwzmorro.alayacore.combat.Attacks;
import io.github.cwzmorro.alayacore.combat.ContinuousSource;
import io.github.cwzmorro.alayacore.combat.HitCooldown;
import io.github.cwzmorro.alayacore.combat.ServantBonus;
import io.github.cwzmorro.alayacore.config.AlayaConfig;
import io.github.cwzmorro.alayacore.mixin.LivingEntityAccessor;
import io.github.cwzmorro.alayacore.progression.Progression;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.servant.ServantChoice;
import io.github.cwzmorro.alayacore.servant.ServantClasses;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

public class CombatGameTests {
	private static final ResourceKey<DamageType> PLAIN = ResourceKey.create(Registries.DAMAGE_TYPE, TestServants.id("plain"));
	// In alayacore:ignores_defence by the test mod's data.
	private static final ResourceKey<DamageType> DEFIANT = ResourceKey.create(Registries.DAMAGE_TYPE, TestServants.id("defiant"));
	private static final float HIT = 15.0F;
	private static final float SLICE = 0.75F;
	private static final int CONTINUOUS_TICKS = SharedConstants.TICKS_PER_SECOND;
	private static final float BARE_HEALTH = 100.0F;
	private static final int PROTECTION_LEVEL = 4;
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
	private static final Item[] ARMOR = {Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
	// Unenchanted armor, so every wear lands (no Unbreaking).
	private static final Item[] PLAIN_ARMOR = {Items.IRON_BOOTS, Items.IRON_LEGGINGS, Items.IRON_CHESTPLATE, Items.IRON_HELMET};
	private static final int WEAR_SECONDS = 3;

	private static DamageSource attack(ServerLevel level, LivingEntity attacker, String rank, boolean noblePhantasm) {
		return new AttackSource(Attacks.damageType(level, PLAIN), attacker, attacker,
			new AttackData(attacker.getUUID(), Rank.parse(rank), false, noblePhantasm));
	}

	private static DamageSource plain(ServerLevel level, LivingEntity attacker) {
		return new DamageSource(Attacks.damageType(level, PLAIN), attacker);
	}

	private static DamageSource continuous(ServerLevel level, LivingEntity attacker) {
		return ContinuousSource.of(level, PLAIN, attacker, attacker);
	}

	private static Zombie bare(GameTestHelper helper) {
		Husk target = helper.spawn(EntityType.HUSK, INSIDE);
		target.getAttribute(Attributes.ARMOR).setBaseValue(0);
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BARE_HEALTH);
		target.setHealth(BARE_HEALTH);
		return target;
	}

	private static Zombie armored(GameTestHelper helper) {
		Zombie target = helper.spawn(EntityType.ZOMBIE, INSIDE);
		var protection = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		for (int i = 0; i < ARMOR.length; i++) {
			ItemStack stack = new ItemStack(ARMOR[i]);
			stack.enchant(protection, PROTECTION_LEVEL);
			target.setItemSlot(ARMOR_SLOTS[i], stack);
			// The armor attributes would only apply on the zombie's next tick.
			EquipmentSlot slot = ARMOR_SLOTS[i];
			stack.get(DataComponents.ATTRIBUTE_MODIFIERS).forEach(slot, (attribute, modifier) -> target.getAttribute(attribute).addTransientModifier(modifier));
		}
		return target;
	}

	private static void checkHit(GameTestHelper helper, String what, DamageSource source, double armorIgnored, double protectionIgnored) {
		Zombie target = armored(helper);
		float afterArmor = CombatRules.getDamageAfterAbsorb(target, HIT, source, (float) (target.getArmorValue() * (1 - armorIgnored)),
			(float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		float protection = EnchantmentHelper.getDamageProtection(helper.getLevel(), target, source);
		double expected = CombatRules.getDamageAfterMagicAbsorb(afterArmor, (float) (protection * (1 - protectionIgnored)));
		float before = target.getHealth();
		target.hurtServer(helper.getLevel(), source, HIT);
		check(helper, what, expected, before - target.getHealth());
		target.discard();
	}

	@GameTest
	public void noblePhantasmsIgnoreArmorAndProtectionByTheirLetter(GameTestHelper helper) {
		ServerPlayer human = join(helper);
		checkHit(helper, "plain hit", plain(helper.getLevel(), human), 0, 0);
		checkHit(helper, "rank C noble phantasm", attack(helper.getLevel(), human, "C", true), 0, 0);
		checkHit(helper, "rank B noble phantasm", attack(helper.getLevel(), human, "B", true), 0.25, 0.5);
		checkHit(helper, "rank A noble phantasm", attack(helper.getLevel(), human, "A", true), 0.5, 0.75);
		checkHit(helper, "rank A++ noble phantasm uses its letter", attack(helper.getLevel(), human, "A++", true), 0.5, 0.75);
		checkHit(helper, "rank EX noble phantasm", attack(helper.getLevel(), human, "EX", true), 1.0, 1.0);
		helper.succeed();
	}

	@GameTest
	public void skillsIgnoreAsANoblePhantasmOneLetterLower(GameTestHelper helper) {
		ServerPlayer human = join(helper);
		checkHit(helper, "rank EX skill as an A noble phantasm", attack(helper.getLevel(), human, "EX", false), 0.5, 0.75);
		checkHit(helper, "rank A skill as a B noble phantasm", attack(helper.getLevel(), human, "A", false), 0.25, 0.5);
		checkHit(helper, "rank B skill nothing extra", attack(helper.getLevel(), human, "B", false), 0, 0);
		checkHit(helper, "rank E skill nothing extra", attack(helper.getLevel(), human, "E", false), 0, 0);
		helper.succeed();
	}

	@GameTest
	public void servantDamageIgnoresProtectionAndTheLargestShareCounts(GameTestHelper helper) {
		ServerPlayer servant = joinAs(helper, TestServants.NEW);
		double servantShare = AlayaConfig.combat().servantProtectionIgnored();
		checkHit(helper, "servant melee", plain(helper.getLevel(), servant), 0, servantShare);
		checkHit(helper, "servant rank B skill", attack(helper.getLevel(), servant, "B", false), 0, servantShare);
		checkHit(helper, "servant rank B noble phantasm", attack(helper.getLevel(), servant, "B", true), 0.25, Math.max(servantShare, 0.5));
		checkHit(helper, "servant rank A noble phantasm", attack(helper.getLevel(), servant, "A", true), 0.5, 0.75);
		helper.succeed();
	}

	@GameTest
	public void aDamageTypeInTheTagIgnoresDefenceAndWearsNoArmor(GameTestHelper helper) {
		ServerPlayer human = join(helper);
		DamageSource source = new DamageSource(Attacks.damageType(helper.getLevel(), DEFIANT), human);
		Zombie target = armored(helper);
		float afterArmor = CombatRules.getDamageAfterAbsorb(target, HIT, source, target.getArmorValue(),
			(float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
		float reduced = (float) (HIT - (1 - AlayaConfig.combat().ignoresDefenceArmor()) * (HIT - afterArmor));
		float protection = Math.min(AttackDefence.MOST_PROTECTION, EnchantmentHelper.getDamageProtection(helper.getLevel(), target, source));
		double expected = CombatRules.getDamageAfterMagicAbsorb(reduced, (float) (protection * (1 - AlayaConfig.combat().ignoresDefenceProtection())));
		float before = target.getHealth();
		target.hurtServer(helper.getLevel(), source, HIT);
		check(helper, "damage that ignores defence", expected, before - target.getHealth());
		helper.succeed();
	}

	@GameTest
	public void onlyAttacksThatIgnoreAllArmorOrDefenceWearNone(GameTestHelper helper) {
		ServerPlayer human = join(helper);
		ServerLevel level = helper.getLevel();
		check(helper, "an ordinary hit wears armor", true, AttackDefence.wearsArmor(plain(level, human)));
		check(helper, "a rank B noble phantasm wears armor", true, AttackDefence.wearsArmor(attack(level, human, "B", true)));
		check(helper, "a rank A noble phantasm wears armor", true, AttackDefence.wearsArmor(attack(level, human, "A", true)));
		check(helper, "a rank EX noble phantasm ignores all armor, so wears none", false, AttackDefence.wearsArmor(attack(level, human, "EX", true)));
		check(helper, "a hit that ignores defence wears none", false, AttackDefence.wearsArmor(new DamageSource(Attacks.damageType(level, DEFIANT), human)));
		helper.succeed();
	}

	@GameTest
	public void aServantsMeleeHitAddsItsClassAttackScaledByTheWeaponsAttackSpeed(GameTestHelper helper) {
		ServerPlayer servant = joinAs(helper, TestServants.NEW);
		Zombie target = helper.spawn(EntityType.ZOMBIE, INSIDE);
		double classAttack = Progression.stats(servant, TestPlayers.classConfig(ServantClasses.SABER)).attack();
		double reference = AlayaConfig.combat().meleeReferenceAttacksPerSecond();
		check(helper, "class attack", classAttack, ServantBonus.classAttack(servant));
		double base = servant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);

		servant.attack(target);
		check(helper, "bare hand: class attack × reference ÷ attack speed", base + classAttack * reference / servant.getAttributeValue(Attributes.ATTACK_SPEED),
			servant.getAttributeValue(Attributes.ATTACK_DAMAGE));

		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		servant.setItemInHand(InteractionHand.MAIN_HAND, sword);
		// A mock player's equipment attributes only apply on its tick, as a real player's do between swings.
		ItemAttributeModifiers swordModifiers = sword.get(DataComponents.ATTRIBUTE_MODIFIERS);
		swordModifiers.forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> servant.getAttribute(attribute).addTransientModifier(modifier));
		servant.attack(target);
		double swordDamage = swordModifiers.compute(Attributes.ATTACK_DAMAGE, base, EquipmentSlot.MAINHAND);
		double swordSpeed = servant.getAttributeValue(Attributes.ATTACK_SPEED);
		check(helper, "sword: the weapon's own attack speed", swordDamage + classAttack * reference / swordSpeed, servant.getAttributeValue(Attributes.ATTACK_DAMAGE));
		helper.succeed();
	}

	@GameTest
	public void continuousDamageLandsThroughTheHitCooldownWithoutStartingOne(GameTestHelper helper) {
		ServerPlayer attacker = join(helper);
		ServerPlayer other = join(helper);
		ServerLevel level = helper.getLevel();
		Zombie target = bare(helper);
		target.hurtServer(level, plain(level, other), HIT);
		float health = target.getHealth();
		target.hurtServer(level, plain(level, attacker), SLICE);
		check(helper, "an ordinary hit smaller than the last is stopped by the hit cooldown, so continuous damage is opt-in", health, target.getHealth());
		target.hurtServer(level, continuous(level, attacker), SLICE);
		check(helper, "a continuous tick lands through the hit cooldown", health - SLICE, target.getHealth());
		check(helper, "the hit cooldown is left as it was", HitCooldown.FULL_TICKS, target.invulnerableTime);
		check(helper, "and what it took", HIT, ((LivingEntityAccessor) target).alayacore$lastHurt());

		Zombie fresh = bare(helper);
		fresh.hurtServer(level, continuous(level, attacker), SLICE);
		check(helper, "a continuous tick starts no hit cooldown", 0, fresh.invulnerableTime);
		health = fresh.getHealth();
		fresh.hurtServer(level, plain(level, other), HIT);
		check(helper, "a second attacker's ordinary hit lands in full", health - HIT, fresh.getHealth());
		helper.succeed();
	}

	@GameTest
	public void continuousDamageMeetsArmorAsItsWholeSecond(GameTestHelper helper) {
		ServerPlayer attacker = join(helper);
		ServerLevel level = helper.getLevel();
		DamageSource source = continuous(level, attacker);
		Zombie target = armored(helper);
		float armor = target.getArmorValue();
		float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
		float perSecond = SLICE * SharedConstants.TICKS_PER_SECOND;
		float afterArmor = CombatRules.getDamageAfterAbsorb(target, perSecond, source, armor, toughness) * SLICE / perSecond;
		double expected = CombatRules.getDamageAfterMagicAbsorb(afterArmor, EnchantmentHelper.getDamageProtection(level, target, source));
		check(helper, "armor reduces a lone tick more than its second", true,
			CombatRules.getDamageAfterAbsorb(target, SLICE, source, armor, toughness) < afterArmor);
		float before = target.getHealth();
		target.hurtServer(level, source, SLICE);
		check(helper, "a tick takes the share of armor and Protection its whole second would", expected, before - target.getHealth());
		helper.succeed();
	}

	// Neither hit names an attacker, so difficulty doesn't scale them.
	@GameTest(maxTicks = WEAR_SECONDS * CONTINUOUS_TICKS + 5)
	public void continuousDamageWearsArmorOnceASecondByItsSecond(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer continuous = wearer(helper);
		ServerPlayer struck = wearer(helper);
		for (int second = 0; second < WEAR_SECONDS; second++) {
			struck.invulnerableTime = 0;
			struck.hurtServer(level, new DamageSource(Attacks.damageType(level, PLAIN)), SLICE * SharedConstants.TICKS_PER_SECOND);
		}
		for (int tick = 0; tick < WEAR_SECONDS * CONTINUOUS_TICKS; tick++) {
			helper.runAfterDelay(tick, () -> continuous.hurtServer(level, ContinuousSource.of(level, PLAIN, null, null), SLICE));
		}
		helper.runAfterDelay(WEAR_SECONDS * CONTINUOUS_TICKS, () -> {
			check(helper, "worn at all", true, continuous.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() > 0);
			for (EquipmentSlot slot : ARMOR_SLOTS) {
				check(helper, "worn as " + WEAR_SECONDS + " hits of its second: " + slot, struck.getItemBySlot(slot).getDamageValue(),
					continuous.getItemBySlot(slot).getDamageValue());
			}
			helper.succeed();
		});
	}

	// A mock player takes no damage until it says it has loaded, nor while it hasn't chosen a servant or human.
	private static ServerPlayer wearer(GameTestHelper helper) {
		ServerPlayer player = join(helper);
		player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		player.setAttached(ServantStorage.CHOICE, ServantChoice.human());
		player.setGameMode(GameType.SURVIVAL);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BARE_HEALTH);
		player.setHealth(BARE_HEALTH);
		for (int i = 0; i < ARMOR_SLOTS.length; i++) {
			player.setItemSlot(ARMOR_SLOTS[i], new ItemStack(PLAIN_ARMOR[i]));
		}
		return player;
	}

	@GameTest(maxTicks = CONTINUOUS_TICKS + 5)
	public void continuousDamageKnocksNothingBackAndFlashesAtMostTwiceASecond(GameTestHelper helper) {
		ServerPlayer attacker = join(helper);
		Zombie target = bare(helper);
		target.setNoAi(true);
		target.setNoGravity(true);
		int[] flashes = {0};
		for (int tick = 0; tick < CONTINUOUS_TICKS; tick++) {
			helper.runAfterDelay(tick, () -> {
				target.hurtServer(helper.getLevel(), continuous(helper.getLevel(), attacker), SLICE);
				if (target.hurtTime == target.hurtDuration) {
					flashes[0]++;
				}
				check(helper, "no knockback", 0.0, target.getDeltaMovement().horizontalDistance());
			});
		}
		helper.runAfterDelay(CONTINUOUS_TICKS, () -> {
			check(helper, "every tick landed", target.getMaxHealth() - CONTINUOUS_TICKS * SLICE, target.getHealth());
			check(helper, "the hurt sound and flash, once the last flash is over: twice in a second", 2, flashes[0]);
			helper.succeed();
		});
	}

	@GameTest
	public void aServantWhoseMeleeIgnoresTheHitCooldownHitsAgainAtOnceWithoutStartingOne(GameTestHelper helper) {
		ServerPlayer ordinary = joinAs(helper, TestServants.NEW);
		ServerPlayer relentless = joinAs(helper, TestServants.RELENTLESS);
		ServerPlayer other = join(helper);
		Zombie first = bare(helper);
		ordinary.attack(first);
		float health = first.getHealth();
		ordinary.attack(first);
		check(helper, "an ordinary servant's weaker second swing is stopped by the hit cooldown", health, first.getHealth());

		Zombie second = bare(helper);
		relentless.attack(second);
		health = second.getHealth();
		check(helper, "the first swing lands", true, health < second.getMaxHealth());
		relentless.attack(second);
		check(helper, "the second swing lands through the hit cooldown", true, second.getHealth() < health);
		check(helper, "and starts none", 0, second.invulnerableTime);
		health = second.getHealth();
		second.hurtServer(helper.getLevel(), plain(helper.getLevel(), other), HIT);
		check(helper, "another attacker's hit lands in full", health - HIT, second.getHealth());
		helper.succeed();
	}

	@GameTest
	public void aMobServantWhoseMeleeIgnoresTheHitCooldownHitsAgainAtOnceWithoutStartingOne(GameTestHelper helper) {
		Zombie relentless = helper.spawn(EntityType.ZOMBIE, BESIDE);
		relentless.setAttached(ServantStorage.CHOICE, ServantChoice.servant(TestServants.RELENTLESS));
		Zombie ordinary = helper.spawn(EntityType.ZOMBIE, BESIDE);
		ordinary.setAttached(ServantStorage.CHOICE, ServantChoice.servant(TestServants.NEW));
		Zombie first = bare(helper);
		ordinary.doHurtTarget(helper.getLevel(), first);
		float health = first.getHealth();
		ordinary.doHurtTarget(helper.getLevel(), first);
		check(helper, "an ordinary servant mob's second hit is stopped by the hit cooldown", health, first.getHealth());

		Zombie second = bare(helper);
		relentless.doHurtTarget(helper.getLevel(), second);
		health = second.getHealth();
		check(helper, "the first hit lands", true, health < second.getMaxHealth());
		relentless.doHurtTarget(helper.getLevel(), second);
		check(helper, "the second hit lands through the hit cooldown", true, second.getHealth() < health);
		check(helper, "and starts none", 0, second.invulnerableTime);
		helper.succeed();
	}

	@GameTest
	public void aMobWithAServantAddsItsFullClassAttackAndAMobWithoutNothing(GameTestHelper helper) {
		Zombie plain = helper.spawn(EntityType.ZOMBIE, INSIDE);
		Zombie servant = helper.spawn(EntityType.ZOMBIE, BESIDE);
		Zombie victim = helper.spawn(EntityType.ZOMBIE, BESIDE.above());
		servant.setAttached(ServantStorage.CHOICE, ServantChoice.servant(TestServants.NEW));
		double base = servant.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
		double classAttack = ServantBonus.classAttack(servant);
		check(helper, "a mob servant's class attack at no sync", AlayaConfig.progression().demi().attack(), classAttack);

		servant.doHurtTarget(helper.getLevel(), victim);
		check(helper, "the mob's melee hit adds the whole class attack", base + classAttack, servant.getAttributeValue(Attributes.ATTACK_DAMAGE));
		plain.doHurtTarget(helper.getLevel(), victim);
		check(helper, "a mob without a servant adds none", plain.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), plain.getAttributeValue(Attributes.ATTACK_DAMAGE));
		helper.succeed();
	}
}
