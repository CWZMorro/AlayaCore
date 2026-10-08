package io.github.cwzmorro.alayacore.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityPresets;
import io.github.cwzmorro.alayacore.ability.AbilitySetting;
import io.github.cwzmorro.alayacore.ability.AbilitySettings;
import io.github.cwzmorro.alayacore.ability.AbilityStorage;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.client.hud.KeyBox;
import io.github.cwzmorro.alayacore.client.hud.PresetHud;
import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import io.github.cwzmorro.alayacore.config.ClientConfig;
import io.github.cwzmorro.alayacore.mana.ManaData;
import io.github.cwzmorro.alayacore.network.SelectPresetPayload;
import io.github.cwzmorro.alayacore.network.SetAbilitySettingPayload;
import io.github.cwzmorro.alayacore.network.SetPresetSlotPayload;
import io.github.cwzmorro.alayacore.network.ShiftSlotModePayload;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class PresetScreen extends Screen {
	private static final Identifier PANEL = AlayaCore.id("panel");
	private static final Identifier INSET = AlayaCore.id("inset");

	private static final Component LABEL_ABILITIES = Component.translatable("alayacore.screen.presets.abilities");
	private static final Component LABEL_QUICK_CAST = Component.translatable("alayacore.screen.presets.quick_cast");
	private static final Component LABEL_SELECT_ABILITY = Component.translatable("alayacore.screen.presets.select_ability");
	private static final Component LABEL_DESCRIPTION = Component.translatable("alayacore.screen.presets.description");
	private static final Component LABEL_SETTINGS = Component.translatable("alayacore.screen.presets.settings");

	private static final int WIDTH = 276;
	private static final int PADDING = 8;
	private static final int TITLE_Y = 7;

	private static final int LIST_WIDTH = 138;
	private static final int LIST_LABEL_Y = 20;
	private static final int LIST_Y = 32;
	private static final int ROW_HEIGHT = 13;
	private static final int VISIBLE_ROWS = 6;
	private static final int QUICK_CAST_LABEL_Y = 116;
	private static final int QUICK_CAST_Y = 128;
	private static final int QUICK_CAST_ROW_HEIGHT = 20;
	private static final int VISIBLE_QUICK_CASTS = 3;
	private static final int KEY_BUTTON_WIDTH = 56;
	private static final int KEY_BUTTON_HEIGHT = 18;
	private static final int BUTTON_LABEL_GAP = 4;
	private static final int TEXT_INSET = 3;
	private static final int SLOT_TEXT_TOP = 2;
	private static final int PROMPT_DROP = 5;

	public static final int RIGHT_X = 156;
	public static final int PRESET_Y = 18;
	private static final int PRESET_BUTTON_SIZE = 20;
	private static final int PRESET_BUTTON_GAP = 2;
	private static final int SLOT_Y = 44;
	private static final int SLOT_SPACING = 25;
	private static final int KEY_BOX_GAP = 4;
	private static final int SLOT_BOX_X = RIGHT_X + KeyBox.SIZE + KEY_BOX_GAP;
	private static final int SLOT_BOX_WIDTH = WIDTH - PADDING - SLOT_BOX_X;
	private static final int SLOT_BOX_HEIGHT = 22;
	private static final int MANA_GAP = 4;
	private static final int MANA_TO_HINT = 15;
	private static final int MANA_Y = SLOT_Y + AbilityPresets.SLOTS * SLOT_SPACING + MANA_GAP;
	private static final int HINT_Y = MANA_Y + MANA_TO_HINT;

	private static final int DESCRIPTION_GAP = 4;
	private static final int LABEL_TO_BOX = LIST_Y - LIST_LABEL_Y;
	private static final int DESCRIPTION_LABEL_Y = QUICK_CAST_Y + VISIBLE_QUICK_CASTS * QUICK_CAST_ROW_HEIGHT + DESCRIPTION_GAP;
	private static final int DESCRIPTION_Y = DESCRIPTION_LABEL_Y + LABEL_TO_BOX;
	private static final int DESCRIPTION_LINES = 4;
	private static final int DESCRIPTION_PADDING = 5;
	private static final int DESCRIPTION_LINE_SPACING = 1;
	private static final int DESCRIPTION_SCROLL_MARGIN = 3;
	private static final int DESCRIPTION_SCROLL_GAP = 3;
	private static final int DESCRIPTION_WIDTH = WIDTH - 2 * PADDING;
	private static final int DESCRIPTION_TEXT_WIDTH = DESCRIPTION_WIDTH - DESCRIPTION_PADDING - DESCRIPTION_SCROLL_MARGIN
		- ScrollBar.WIDTH - DESCRIPTION_SCROLL_GAP;

	private static final int SETTINGS_SLIDER_WIDTH = SideTabs.SETTINGS_WIDTH - 2 * PADDING;
	private static final int SETTINGS_SLIDER_GAP = 4;
	private static final int SETTINGS_CONTROLS_Y = PADDING + SideTabs.COG_SIZE + PADDING;
	private static final int SETTINGS_TITLE_GAP = 4;
	private static final int SETTINGS_SECTION_GAP = 10;
	private static final int MARGIN = 4;

	private static final int TEXT = 0xFF404040;
	private static final int SUBTEXT = 0xFF707070;
	private static final int SLOT_TEXT = 0xFFFFFFFF;
	private static final int SLOT_SUBTEXT = 0xFFD0D0D0;
	private static final int HOVER = 0x30FFFFFF;

	private static final NumberFormat MANA_FORMAT = NumberFormat.getIntegerInstance();

	private final List<Button> presetButtons = new ArrayList<>();
	private List<Identifier> abilities = List.of();
	private @Nullable Identifier selected;
	private @Nullable KeyMapping listening;
	private boolean settingsOpen;
	// Worked out when the widgets are rebuilt, not every frame.
	private List<AbilitySetting> settings = List.of();
	private @Nullable ScreenRectangle settingsArea;
	private String settingsAbilityName = "";
	private List<FormattedCharSequence> description = List.of();
	private int descriptionScroll;
	private int descriptionLines;
	private List<List<FormattedCharSequence>> quickCastLabels = List.of();
	private List<FormattedCharSequence> hint = List.of();
	private @Nullable Component manaText;
	private int scroll;
	private int quickCastScroll;
	private int left;
	private int top;

	public PresetScreen() {
		super(Component.translatable("alayacore.screen.presets.title"));
	}

	@Override
	protected void init() {
		this.descriptionLines = Math.clamp((this.height - 2 * MARGIN - DESCRIPTION_Y - 2 * DESCRIPTION_PADDING - PADDING)
			/ (this.font.lineHeight + DESCRIPTION_LINE_SPACING), 1, DESCRIPTION_LINES);
		int right = this.settingsOpen ? SideTabs.SETTINGS_WIDTH - SideTabs.UNDER : SideTabs.WIDTH - SideTabs.UNDER;
		this.left = Math.max(MARGIN, Math.min((this.width - WIDTH) / 2, this.width - MARGIN - WIDTH - right));
		this.top = Math.max(MARGIN, (this.height - this.panelHeight()) / 2);
		this.settings = this.selected == null ? List.of() : Ability.settings(this.selected);
		this.settingsArea = this.settingsOpen ? this.openSettingsArea() : null;
		this.settingsAbilityName = this.selected == null ? "" : this.font.substrByWidth(Ability.name(this.selected), SETTINGS_SLIDER_WIDTH).getString();
		this.abilities = Abilities.owned(this.minecraft.player).stream().map(ResourceKey::identifier).toList();
		this.scroll = Math.clamp(this.scroll, 0, this.maxScroll());

		this.presetButtons.clear();
		for (int preset = 0; preset < AbilityPresets.PRESETS; preset++) {
			int index = preset;
			this.presetButtons.add(this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(preset + 1)),
					button -> ClientPlayNetworking.send(new SelectPresetPayload(index)))
				.bounds(this.left + RIGHT_X + preset * (PRESET_BUTTON_SIZE + PRESET_BUTTON_GAP), this.top + PRESET_Y,
					PRESET_BUTTON_SIZE, PRESET_BUTTON_SIZE)
				.build()));
		}

		this.quickCastLabels = List.of();
		if (this.selected != null) {
			Identifier ability = this.selected;
			int modes = Ability.modes(ability);
			this.quickCastScroll = Math.clamp(this.quickCastScroll, 0, Math.max(0, modes - VISIBLE_QUICK_CASTS));
			List<List<FormattedCharSequence>> labels = new ArrayList<>();
			for (int mode = 0; mode < modes; mode++) {
				labels.add(this.font.split(modes > 1 ? Ability.modeName(ability, mode) : Ability.name(ability),
					LIST_WIDTH - KEY_BUTTON_WIDTH - BUTTON_LABEL_GAP));
			}
			this.quickCastLabels = List.copyOf(labels);
			for (int row = 0; row < VISIBLE_QUICK_CASTS && this.quickCastScroll + row < modes; row++) {
				int y = this.top + QUICK_CAST_Y + row * QUICK_CAST_ROW_HEIGHT;
				AbilityKeys.quickCastKey(ability, this.quickCastScroll + row).ifPresent(key -> this.addRenderableWidget(
					Button.builder(this.keyLabel(key), button -> {
							this.listening = key;
							this.rebuildWidgets();
						})
						.bounds(this.left + PADDING + LIST_WIDTH - KEY_BUTTON_WIDTH, y, KEY_BUTTON_WIDTH, KEY_BUTTON_HEIGHT)
						.build()));
			}
		}
		if (this.settingsArea != null) {
			ScreenRectangle area = this.settingsArea;
			// Covered panel buttons must not draw or receive input through an overlapping settings tab.
			for (var child : this.children()) {
				if (child instanceof Button button && button.getX() + button.getWidth() > area.left()
					&& button.getY() < area.bottom() && button.getY() + button.getHeight() > area.top()) {
					button.visible = false;
				}
			}
			this.addRenderableWidget(OpacitySlider.create(area.left() + PADDING, area.top() + SETTINGS_CONTROLS_Y, SETTINGS_SLIDER_WIDTH));
			for (int index = 0; index < this.settings.size(); index++) {
				this.addRenderableWidget(this.abilitySlider(area, index));
			}
		}
		this.hint = this.font.split(Component.translatable("alayacore.screen.presets.hint"), WIDTH - PADDING - RIGHT_X);
		this.refreshDescription();
		this.tick();
	}

	private int panelHeight() {
		return DESCRIPTION_Y + this.descriptionHeight() + PADDING;
	}

	private int descriptionHeight() {
		return this.descriptionLines * (this.font.lineHeight + DESCRIPTION_LINE_SPACING) + 2 * DESCRIPTION_PADDING;
	}

	public void refreshDescription() {
		List<Component> lines = new ArrayList<>();
		if (this.selected != null) {
			lines.addAll(Ability.description(this.selected));
			// An ability with one mode has no mode names; its lines are all general.
			int modes = Ability.modes(this.selected) > 1 ? Ability.modes(this.selected) : 0;
			for (int mode = 0; mode < modes; mode++) {
				List<Component> modeLines = Ability.modeDescription(this.selected, mode);
				if (!modeLines.isEmpty()) {
					lines.add(Ability.modeName(this.selected, mode).copy().withStyle(ChatFormatting.BOLD));
					lines.addAll(modeLines);
				}
			}
		}
		this.description = lines.stream().flatMap(line -> this.font.split(line, DESCRIPTION_TEXT_WIDTH).stream()).toList();
		this.descriptionScroll = Math.clamp(this.descriptionScroll, 0, this.maxDescriptionScroll());
	}

	private int maxScroll() {
		return Math.max(0, this.abilities.size() - VISIBLE_ROWS);
	}

	private int maxDescriptionScroll() {
		return Math.max(0, this.description.size() - this.descriptionLines);
	}

	private SettingSlider abilitySlider(ScreenRectangle area, int index) {
		Identifier ability = this.selected;
		AbilitySetting setting = this.settings.get(index);
		return new SettingSlider(area.left() + PADDING, area.top() + this.abilitySettingY(index), SETTINGS_SLIDER_WIDTH, setting.min(), setting.max(),
			setting.step(), AbilitySettings.value(this.minecraft.player, ability, setting), value -> setting.label(ability, value),
			value -> ClientPlayNetworking.send(new SetAbilitySettingPayload(ability, setting.id(), value)));
	}

	private int abilitySettingsLabelY() {
		return SETTINGS_CONTROLS_Y + AbstractSliderButton.DEFAULT_HEIGHT + SETTINGS_SECTION_GAP;
	}

	private int abilitySettingY(int index) {
		return this.abilitySettingsLabelY() + this.font.lineHeight + SETTINGS_SLIDER_GAP
			+ index * (AbstractSliderButton.DEFAULT_HEIGHT + SETTINGS_SLIDER_GAP);
	}

	private ScreenRectangle settingsTab() {
		return SideTabs.settingsTab(this.left, this.top, WIDTH);
	}

	private ScreenRectangle openSettingsArea() {
		int count = this.settings.size();
		int height = (count == 0 ? SETTINGS_CONTROLS_Y + AbstractSliderButton.DEFAULT_HEIGHT : this.abilitySettingY(count - 1) + AbstractSliderButton.DEFAULT_HEIGHT)
			+ PADDING;
		return SideTabs.settingsArea(this.left, this.top, WIDTH, height, this.width, this.height, MARGIN);
	}

	private ScreenRectangle settingsTitle(ScreenRectangle area) {
		return new ScreenRectangle(area.left(), area.top(), area.width(), SETTINGS_CONTROLS_Y - PADDING);
	}

	private Component keyLabel(KeyMapping key) {
		Component bound = key.getTranslatedKeyMessage();
		return key == this.listening
			? Component.literal("> ").append(bound.copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE)).append(" <")
				.withStyle(ChatFormatting.YELLOW)
			: bound;
	}

	private AbilityPresets presets() {
		return this.minecraft.player.getAttachedOrElse(AbilityStorage.PRESETS, AbilityPresets.EMPTY);
	}

	@Override
	public void tick() {
		int active = this.presets().active();
		for (int preset = 0; preset < this.presetButtons.size(); preset++) {
			this.presetButtons.get(preset).active = preset != active;
		}
		ManaData mana = AlayaMana.get(this.minecraft.player);
		this.manaText = mana == null ? null
			: Component.translatable("alayacore.screen.presets.mana", MANA_FORMAT.format(mana.current()), MANA_FORMAT.format(mana.max()));
	}

	@Override
	public void removed() {
		ClientConfig.save();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, this.left, this.top, WIDTH, this.panelHeight());
		if (!this.settingsOpen) {
			SideTabs.extractSettingsTab(graphics, this.settingsTab(), this.minecraft.getWindow().getGuiScale());
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int listX = this.left + PADDING;
		graphics.text(this.font, this.title, listX, this.top + TITLE_Y, TEXT, false);
		graphics.text(this.font, LABEL_ABILITIES, listX, this.top + LIST_LABEL_Y, SUBTEXT, false);

		int hoveredRow = this.rowAt(mouseX, mouseY);
		for (int row = 0; row < VISIBLE_ROWS && this.scroll + row < this.abilities.size(); row++) {
			Identifier id = this.abilities.get(this.scroll + row);
			int y = this.top + LIST_Y + row * ROW_HEIGHT;
			boolean isSelected = id.equals(this.selected);
			if (isSelected) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, listX, y, LIST_WIDTH, ROW_HEIGHT);
			} else if (row == hoveredRow) {
				graphics.fill(listX, y, listX + LIST_WIDTH, y + ROW_HEIGHT, HOVER);
			}
			graphics.text(this.font, Ability.name(id), listX + TEXT_INSET, y + TEXT_INSET, isSelected ? SLOT_TEXT : TEXT, isSelected);
		}

		graphics.text(this.font, LABEL_QUICK_CAST, listX, this.top + QUICK_CAST_LABEL_Y, SUBTEXT, false);
		if (this.selected == null) {
			graphics.text(this.font, LABEL_SELECT_ABILITY, listX, this.top + QUICK_CAST_Y + PROMPT_DROP, TEXT, false);
		} else {
			int modes = Ability.modes(this.selected);
			for (int row = 0; row < VISIBLE_QUICK_CASTS && this.quickCastScroll + row < modes; row++) {
				List<FormattedCharSequence> label = this.quickCastLabels.get(this.quickCastScroll + row);
				int labelY = this.top + QUICK_CAST_Y + row * QUICK_CAST_ROW_HEIGHT + (KEY_BUTTON_HEIGHT - label.size() * this.font.lineHeight) / 2 + 1;
				for (int line = 0; line < label.size(); line++) {
					graphics.text(this.font, label.get(line), listX, labelY + line * this.font.lineHeight, TEXT, false);
				}
			}
		}

		AbilityPresets presets = this.presets();
		for (int slot = 0; slot < AbilityPresets.SLOTS; slot++) {
			int y = this.top + SLOT_Y + slot * SLOT_SPACING;
			if (this.covered(this.left + RIGHT_X, y, WIDTH - PADDING - RIGHT_X, SLOT_BOX_HEIGHT)) {
				continue;
			}
			KeyBox.extract(graphics, this.font, this.left + RIGHT_X, y + (SLOT_BOX_HEIGHT - KeyBox.SIZE) / 2, AbilityKeys.slotKeyName(slot), 1.0F);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, this.left + SLOT_BOX_X, y, SLOT_BOX_WIDTH, SLOT_BOX_HEIGHT);
			Optional<AbilityPresets.Slot> filled = presets.slot(presets.active(), slot);
			int textX = this.left + SLOT_BOX_X + TEXT_INSET;
			if (filled.isPresent()) {
				Identifier ability = filled.get().ability();
				if (Ability.modes(ability) > 1) {
					graphics.text(this.font, Ability.name(ability), textX, y + SLOT_TEXT_TOP, SLOT_TEXT, true);
					graphics.text(this.font, Ability.modeName(ability, filled.get().mode()), textX, y + SLOT_TEXT_TOP + this.font.lineHeight + 1,
						SLOT_SUBTEXT, true);
				} else {
					graphics.text(this.font, Ability.name(ability), textX, KeyBox.centredTextY(this.font, y, SLOT_BOX_HEIGHT), SLOT_TEXT, true);
				}
			} else {
				graphics.text(this.font, PresetHud.EMPTY_SLOT, textX, KeyBox.centredTextY(this.font, y, SLOT_BOX_HEIGHT), SLOT_SUBTEXT, true);
			}
		}

		if (this.manaText != null && !this.covered(this.left + RIGHT_X, this.top + MANA_Y, this.font.width(this.manaText), this.font.lineHeight)) {
			graphics.text(this.font, this.manaText, this.left + RIGHT_X, this.top + MANA_Y, TEXT, false);
		}
		int hintY = this.top + HINT_Y;
		for (FormattedCharSequence line : this.hint) {
			if (!this.covered(this.left + RIGHT_X, hintY, this.font.width(line), this.font.lineHeight)) {
				graphics.text(this.font, line, this.left + RIGHT_X, hintY, TEXT, false);
			}
			hintY += this.font.lineHeight;
		}
		this.extractDescription(graphics);
		ScreenRectangle area = this.settingsArea;
		if (area != null) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, area.left(), area.top(), area.width(), area.height());
		}
		super.extractRenderState(graphics, mouseX, mouseY, a);
		if (area != null) {
			SideTabs.extractSettingsTitle(graphics, this.font, area, PADDING, SETTINGS_TITLE_GAP, LABEL_SETTINGS, TEXT);
			if (!this.settings.isEmpty()) {
				graphics.text(this.font, this.settingsAbilityName, area.left() + PADDING, area.top() + this.abilitySettingsLabelY(), SUBTEXT, false);
			}
		} else if (this.settingsTab().containsPoint(mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(LABEL_SETTINGS, mouseX, mouseY);
		}
	}

	private boolean covered(int x, int y, int width, int height) {
		ScreenRectangle area = this.settingsArea;
		if (area == null) {
			return false;
		}
		return x < area.right() && x + width > area.left() && y < area.bottom() && y + height > area.top();
	}

	private void extractDescription(GuiGraphicsExtractor graphics) {
		int boxX = this.left + PADDING;
		int boxY = this.top + DESCRIPTION_Y;
		int boxHeight = this.descriptionHeight();
		graphics.text(this.font, LABEL_DESCRIPTION, boxX, this.top + DESCRIPTION_LABEL_Y, SUBTEXT, false);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, boxX, boxY, DESCRIPTION_WIDTH, boxHeight);
		for (int row = 0; row < this.descriptionLines && this.descriptionScroll + row < this.description.size(); row++) {
			graphics.text(this.font, this.description.get(this.descriptionScroll + row), boxX + DESCRIPTION_PADDING,
				boxY + DESCRIPTION_PADDING + row * (this.font.lineHeight + DESCRIPTION_LINE_SPACING), SLOT_TEXT, true);
		}
		if (this.maxDescriptionScroll() > 0) {
			ScrollBar.extract(graphics, boxX + DESCRIPTION_WIDTH - DESCRIPTION_SCROLL_MARGIN - ScrollBar.WIDTH, boxY + DESCRIPTION_SCROLL_MARGIN,
				boxHeight - 2 * DESCRIPTION_SCROLL_MARGIN, this.descriptionLines, this.description.size(), this.descriptionScroll);
		}
	}

	private boolean overDescription(double mouseX, double mouseY) {
		double x = mouseX - this.left - PADDING;
		double y = mouseY - this.top - DESCRIPTION_Y;
		return x >= 0 && x < DESCRIPTION_WIDTH && y >= 0 && y < this.descriptionHeight();
	}

	private int rowAt(double mouseX, double mouseY) {
		double x = mouseX - this.left - PADDING;
		double y = mouseY - this.top - LIST_Y;
		if (x < 0 || x >= LIST_WIDTH || y < 0 || y >= VISIBLE_ROWS * ROW_HEIGHT) {
			return -1;
		}
		return (int) (y / ROW_HEIGHT);
	}

	private boolean overQuickCasts(double mouseX, double mouseY) {
		double x = mouseX - this.left - PADDING;
		double y = mouseY - this.top - QUICK_CAST_Y;
		return x >= 0 && x < LIST_WIDTH && y >= 0 && y < VISIBLE_QUICK_CASTS * QUICK_CAST_ROW_HEIGHT;
	}

	private int slotAt(double mouseX, double mouseY) {
		double x = mouseX - this.left - RIGHT_X;
		double y = mouseY - this.top - SLOT_Y;
		if (x < 0 || x >= SLOT_BOX_X - RIGHT_X + SLOT_BOX_WIDTH || y < 0) {
			return -1;
		}
		int slot = (int) (y / SLOT_SPACING);
		return slot < AbilityPresets.SLOTS && y - slot * SLOT_SPACING < SLOT_BOX_HEIGHT ? slot : -1;
	}

	private void bind(KeyMapping listening, InputConstants.Key key) {
		listening.setKey(key);
		this.listening = null;
		KeyMapping.resetMapping();
		this.minecraft.options.save();
		this.rebuildWidgets();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (this.listening != null) {
			this.bind(this.listening, InputConstants.Type.MOUSE.getOrCreate(event.button()));
			return true;
		}
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		ScreenRectangle area = this.settingsArea;
		if ((area != null ? this.settingsTitle(area) : this.settingsTab()).containsPoint((int) event.x(), (int) event.y())) {
			this.settingsOpen = !this.settingsOpen;
			this.rebuildWidgets();
			return true;
		}
		if (area != null && area.containsPoint((int) event.x(), (int) event.y())) {
			return true;
		}
		int row = this.rowAt(event.x(), event.y());
		if (row >= 0 && this.scroll + row < this.abilities.size()) {
			this.selected = this.abilities.get(this.scroll + row);
			this.quickCastScroll = 0;
			this.descriptionScroll = 0;
			this.rebuildWidgets();
			return true;
		}
		int slot = this.slotAt(event.x(), event.y());
		if (slot < 0) {
			return false;
		}
		int active = this.presets().active();
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			ClientPlayNetworking.send(new SetPresetSlotPayload(active, slot, Optional.empty()));
		} else if (this.selected != null) {
			ClientPlayNetworking.send(new SetPresetSlotPayload(active, slot, Optional.of(this.selected)));
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (this.settingsArea != null && this.settingsArea.containsPoint((int) mouseX, (int) mouseY)) {
			return true;
		}
		// Like the hotbar: scrolling up goes back.
		int step = scrollY > 0 ? -1 : 1;
		int slot = this.slotAt(mouseX, mouseY);
		if (slot >= 0) {
			ClientPlayNetworking.send(new ShiftSlotModePayload(this.presets().active(), slot, step));
			return true;
		}
		if (this.rowAt(mouseX, mouseY) >= 0) {
			this.scroll = Math.clamp(this.scroll + step, 0, this.maxScroll());
			return true;
		}
		if (this.overDescription(mouseX, mouseY)) {
			this.descriptionScroll = Math.clamp(this.descriptionScroll + ScrollBar.wheelLines(scrollY), 0, this.maxDescriptionScroll());
			return true;
		}
		if (this.selected != null && this.overQuickCasts(mouseX, mouseY)) {
			this.quickCastScroll += step;
			this.rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.listening != null) {
			this.bind(this.listening, event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
			return true;
		}
		if (AbilityKeys.isPresetScreenKey(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
