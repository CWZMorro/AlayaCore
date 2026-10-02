package io.github.cwzmorro.alayacore.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Abilities;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.ability.AbilityPresets;
import io.github.cwzmorro.alayacore.ability.AbilityStorage;
import io.github.cwzmorro.alayacore.client.input.AbilityKeys;
import io.github.cwzmorro.alayacore.network.SelectPresetPayload;
import io.github.cwzmorro.alayacore.network.SetPresetSlotPayload;
import io.github.cwzmorro.alayacore.network.ShiftSlotModePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
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

/**
 * Editing the ability presets (plan 8), a vanilla-grey panel like Tensura's. Left: the player's abilities,
 * and under them the selected ability's quick-cast keys, one per mode (click, then press a key; Esc
 * unbinds), the same keys as in Controls. Right: the 5 preset buttons and the active preset's 3 slots.
 * Click an ability, then a slot, to place it; right-click a slot to empty it; scroll over a slot to change
 * its mode. A preset button shows that preset and makes it the active one. The server checks every
 * preset change; the screen redraws from the synced presets.
 */
public final class PresetScreen extends Screen {
	private static final Identifier PANEL = AlayaCore.id("panel");
	private static final Identifier INSET = AlayaCore.id("inset");
	private static final Identifier KEY_SLOT = Identifier.withDefaultNamespace("container/slot");

	private static final int WIDTH = 276;
	private static final int HEIGHT = 196;
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

	private static final int RIGHT_X = 156;
	private static final int PRESET_BUTTON_SIZE = 20;
	private static final int PRESET_BUTTON_GAP = 2;
	private static final int PRESET_Y = 18;
	private static final int SLOT_Y = 44;
	private static final int SLOT_SPACING = 25;
	private static final int KEY_SLOT_SIZE = 18;
	private static final int SLOT_BOX_X = RIGHT_X + KEY_SLOT_SIZE + 4;
	private static final int SLOT_BOX_WIDTH = WIDTH - PADDING - SLOT_BOX_X;
	private static final int SLOT_BOX_HEIGHT = 22;
	private static final int HINT_Y = SLOT_Y + AbilityPresets.SLOTS * SLOT_SPACING + 1;

	private static final int TEXT = 0xFF404040;
	private static final int SUBTEXT = 0xFF707070;
	private static final int SLOT_TEXT = 0xFFFFFFFF;
	private static final int SLOT_SUBTEXT = 0xFFD0D0D0;
	private static final int HOVER = 0x30FFFFFF;

	private final List<Button> presetButtons = new ArrayList<>();
	private List<Identifier> abilities = List.of();
	private @Nullable Identifier selected;
	/** The quick-cast key waiting for its new key, if any. */
	private @Nullable KeyMapping listening;
	private int scroll;
	private int quickCastScroll;
	private int left;
	private int top;

	public PresetScreen() {
		super(Component.translatable("alayacore.screen.presets.title"));
	}

	@Override
	protected void init() {
		this.left = (this.width - WIDTH) / 2;
		this.top = (this.height - HEIGHT) / 2;
		this.abilities = Abilities.owned(this.minecraft.player).stream().map(ResourceKey::identifier).toList();
		this.scroll = Math.clamp(this.scroll, 0, Math.max(0, this.abilities.size() - VISIBLE_ROWS));

		this.presetButtons.clear();
		for (int preset = 0; preset < AbilityPresets.PRESETS; preset++) {
			int index = preset;
			this.presetButtons.add(this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(preset + 1)),
					button -> ClientPlayNetworking.send(new SelectPresetPayload(index)))
				.bounds(this.left + RIGHT_X + preset * (PRESET_BUTTON_SIZE + PRESET_BUTTON_GAP), this.top + PRESET_Y,
					PRESET_BUTTON_SIZE, PRESET_BUTTON_SIZE)
				.build()));
		}

		if (this.selected != null) {
			Identifier ability = this.selected;
			int modes = Ability.modes(ability);
			this.quickCastScroll = Math.clamp(this.quickCastScroll, 0, Math.max(0, modes - VISIBLE_QUICK_CASTS));
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
		this.tick();
	}

	/** The key's name, or "> name <" in yellow while it waits for a new key, like Controls. */
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
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, this.left, this.top, WIDTH, HEIGHT);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		int listX = this.left + PADDING;
		graphics.text(this.font, this.title, listX, this.top + TITLE_Y, TEXT, false);
		graphics.text(this.font, Component.translatable("alayacore.screen.presets.abilities"), listX, this.top + LIST_LABEL_Y, SUBTEXT, false);

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
			graphics.text(this.font, Ability.name(id), listX + 3, y + 3, isSelected ? SLOT_TEXT : TEXT, isSelected);
		}

		graphics.text(this.font, Component.translatable("alayacore.screen.presets.quick_cast"), listX, this.top + QUICK_CAST_LABEL_Y, SUBTEXT, false);
		if (this.selected == null) {
			graphics.text(this.font, Component.translatable("alayacore.screen.presets.select_ability"), listX, this.top + QUICK_CAST_Y + 5, TEXT, false);
		} else {
			int modes = Ability.modes(this.selected);
			int nameWidth = LIST_WIDTH - KEY_BUTTON_WIDTH - 4;
			for (int row = 0; row < VISIBLE_QUICK_CASTS && this.quickCastScroll + row < modes; row++) {
				Component name = modes > 1 ? Ability.modeName(this.selected, this.quickCastScroll + row) : Ability.name(this.selected);
				graphics.text(this.font, this.font.substrByWidth(name, nameWidth).getString(), listX,
					this.top + QUICK_CAST_Y + row * QUICK_CAST_ROW_HEIGHT + 5, TEXT, false);
			}
		}

		AbilityPresets presets = this.presets();
		for (int slot = 0; slot < AbilityPresets.SLOTS; slot++) {
			int y = this.top + SLOT_Y + slot * SLOT_SPACING;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, KEY_SLOT, this.left + RIGHT_X, y + (SLOT_BOX_HEIGHT - KEY_SLOT_SIZE) / 2,
				KEY_SLOT_SIZE, KEY_SLOT_SIZE);
			graphics.centeredText(this.font, AbilityKeys.slotKeyName(slot), this.left + RIGHT_X + KEY_SLOT_SIZE / 2,
				y + (SLOT_BOX_HEIGHT - this.font.lineHeight) / 2 + 1, SLOT_TEXT);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, this.left + SLOT_BOX_X, y, SLOT_BOX_WIDTH, SLOT_BOX_HEIGHT);
			Optional<AbilityPresets.Slot> filled = presets.slot(presets.active(), slot);
			int textX = this.left + SLOT_BOX_X + 3;
			if (filled.isPresent()) {
				Identifier ability = filled.get().ability();
				if (Ability.modes(ability) > 1) {
					graphics.text(this.font, Ability.name(ability), textX, y + 2, SLOT_TEXT, true);
					graphics.text(this.font, Ability.modeName(ability, filled.get().mode()), textX, y + 2 + this.font.lineHeight + 1,
						SLOT_SUBTEXT, true);
				} else {
					graphics.text(this.font, Ability.name(ability), textX, this.centredTextY(y), SLOT_TEXT, true);
				}
			} else {
				graphics.text(this.font, Component.translatable("alayacore.hud.empty_slot"), textX, this.centredTextY(y), SLOT_SUBTEXT, true);
			}
		}

		int hintY = this.top + HINT_Y;
		for (FormattedCharSequence line : this.font.split(Component.translatable("alayacore.screen.presets.hint"), WIDTH - PADDING - RIGHT_X)) {
			graphics.text(this.font, line, this.left + RIGHT_X, hintY, TEXT, false);
			hintY += this.font.lineHeight;
		}
	}

	/** Text y for one line centred in a slot box starting at {@code boxY}. */
	private int centredTextY(int boxY) {
		return boxY + (SLOT_BOX_HEIGHT - this.font.lineHeight) / 2 + 1;
	}

	/** The ability list row under the mouse, or -1. */
	private int rowAt(double mouseX, double mouseY) {
		double x = mouseX - this.left - PADDING;
		double y = mouseY - this.top - LIST_Y;
		if (x < 0 || x >= LIST_WIDTH || y < 0 || y >= VISIBLE_ROWS * ROW_HEIGHT) {
			return -1;
		}
		return (int) (y / ROW_HEIGHT);
	}

	/** Whether the mouse is over the quick-cast rows. */
	private boolean overQuickCasts(double mouseX, double mouseY) {
		double x = mouseX - this.left - PADDING;
		double y = mouseY - this.top - QUICK_CAST_Y;
		return x >= 0 && x < LIST_WIDTH && y >= 0 && y < VISIBLE_QUICK_CASTS * QUICK_CAST_ROW_HEIGHT;
	}

	/** The preset slot under the mouse (its key or its box), or -1. */
	private int slotAt(double mouseX, double mouseY) {
		double x = mouseX - this.left - RIGHT_X;
		double y = mouseY - this.top - SLOT_Y;
		if (x < 0 || x >= SLOT_BOX_X - RIGHT_X + SLOT_BOX_WIDTH || y < 0) {
			return -1;
		}
		int slot = (int) (y / SLOT_SPACING);
		return slot < AbilityPresets.SLOTS && y - slot * SLOT_SPACING < SLOT_BOX_HEIGHT ? slot : -1;
	}

	/** Gives the waiting quick-cast key its new key, saves the options and redraws. */
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
			// Like Controls: a mouse button can be a key too.
			this.bind(this.listening, InputConstants.Type.MOUSE.getOrCreate(event.button()));
			return true;
		}
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		int row = this.rowAt(event.x(), event.y());
		if (row >= 0 && this.scroll + row < this.abilities.size()) {
			this.selected = this.abilities.get(this.scroll + row);
			this.quickCastScroll = 0;
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
		// Like the hotbar: scrolling up goes back.
		int step = scrollY > 0 ? -1 : 1;
		int slot = this.slotAt(mouseX, mouseY);
		if (slot >= 0) {
			ClientPlayNetworking.send(new ShiftSlotModePayload(this.presets().active(), slot, step));
			return true;
		}
		if (this.rowAt(mouseX, mouseY) >= 0) {
			this.scroll = Math.clamp(this.scroll + step, 0, Math.max(0, this.abilities.size() - VISIBLE_ROWS));
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
			// Like Controls: Esc unbinds.
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
