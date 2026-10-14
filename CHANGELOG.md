# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0]

### Added

- Reality marbles for content mods, with shared overlap and terrain protection rules.
- Solid shields that bodies can't pass through.
- Content mods can add their own kinds of deeds.
- Passive abilities.
- Hidden + and ++ steps for every rank.
- Attacks ignore armor and Protection by their rank.
- Continuous damage that is not stopped by hit immunity.
- A servant property for melee hits that ignore hit immunity.
- Abilities can deflect projectiles.
- Shared held beams, thrown weapons, weapon block-breaking and sword dashes for content mods.
- Skill effects that give back what they replaced when they end.
- Any Archer draws bows faster.
- Weapons kept on death and indestructible weapons through item tags.
- Servant classes, servants, divinity, sweet foods, cakes, loot table groups and starting items as data.
- Servants can be hidden from selection.
- Lost items that fall into the void return to where they belong.
- Max mana stops growing at a ceiling.
- A warning in the log for every missing config key.

### Changed

- Kills give 10% of the victim's max HP as max mana, and challenge advancements give half their experience.
- Sync reaches 100% at 10,000 max mana.
- Ability costs come from a cost table per rank step, and shield repair gets cheaper at every step.
- The class attack bonus on melee depends on the weapon's attack speed, and skills and noble phantasms add it too.
- Mobs that are servants get the class attack bonus.
- Shields protect blocks and entities behind them from every attack, and take each attack once per tick.
- Solid shields stop dashes.
- Config keys that move to another section keep their old values.

### Fixed

- Players who had not chosen a servant could still fall, fly or be pushed.
- A Grand who changed class kept the old class's title.
- The previous servant's advancement branch stayed visible after changing servant.

## [0.1.1]

### Added

- True Name Realization item: instantly awakens the current servant, consumed on successful use except in creative.
- The ability settings column scrolls when it is taller than the screen.

### Changed

- Close-clash mana drain now scales between sub-ranks (e.g. A vs A+, A++), reaching the full 1.5× at a whole letter.
- Numbers in ability settings and ability descriptions show without ".0".

## [0.1.0]

### Added

- Servant and class selection, including Human and Random choices and the Spirit Origin Changer.
- Mana that grows through combat and advancements and regenerates over time, through food and sleep.
- Servant synchronization, class grades, true-name awakening and Grand titles.
- Press, tap and held abilities with modes, mana costs and cooldowns; creative casts cost no mana and have no cooldown.
- Five customizable ability presets, quick-cast keys, ability settings and a preset editor.
- Servant portrait, health, mana, class-grade and ability HUDs.
- Ranked beam clashes, directional shields and divinity-based combat support for content mods.
- Configurable balance, server game rules and mana administration commands.

[Unreleased]: https://github.com/CWZMorro/AlayaCore/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/CWZMorro/AlayaCore/compare/v0.1.1...v0.2.0
[0.1.1]: https://github.com/CWZMorro/AlayaCore/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/CWZMorro/AlayaCore/releases/tag/v0.1.0
