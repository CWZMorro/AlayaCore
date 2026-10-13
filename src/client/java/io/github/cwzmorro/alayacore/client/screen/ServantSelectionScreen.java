package io.github.cwzmorro.alayacore.client.screen;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.client.hud.StripTexture;
import io.github.cwzmorro.alayacore.network.ChooseServantPayload;
import io.github.cwzmorro.alayacore.network.OpenServantSelectionPayload;
import io.github.cwzmorro.alayacore.rank.Rank;
import io.github.cwzmorro.alayacore.servant.Servant;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.servant.ServantRules;
import io.github.cwzmorro.alayacore.servant.ServantStats;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class ServantSelectionScreen extends Screen {
	private static final Identifier HEADER = AlayaCore.id("servant_select/header");
	private static final Identifier BADGE = AlayaCore.id("servant_select/badge");
	private static final Identifier TEXT_PANEL = AlayaCore.id("servant_select/text_panel");
	private static final Identifier ARROW_LEFT = AlayaCore.id("servant_select/arrow_left");
	private static final Identifier ARROW_LEFT_HIGHLIGHTED = AlayaCore.id("servant_select/arrow_left_highlighted");
	private static final Identifier ARROW_RIGHT = AlayaCore.id("servant_select/arrow_right");
	private static final Identifier ARROW_RIGHT_HIGHLIGHTED = AlayaCore.id("servant_select/arrow_right_highlighted");
	private static final Identifier UNKNOWN_ICON = ServantClass.icon(AlayaCore.id("unknown"));
	private static final Identifier ALL_ICON = ServantClass.icon(AlayaCore.id("all"));
	private static final String KEY_PREFIX = "alayacore.screen.servant_selection.";

	private static final int WINDOW_WIDTH = 176;
	public static final int WINDOW_PADDING = 6;
	private static final int TITLE_GAP = 6;
	public static final int BUTTON_GAP = 6;
	private static final int CONTENTS_BUTTON_WIDTH = 60;
	private static final int BUTTONS_SPACING = 4;
	private static final int SCREEN_MARGIN = 8;
	public static final int LINE_SPACING = 1;
	public static final int HEADER_HEIGHT = 30;
	public static final int HEADER_GAP = 4;
	private static final int HEADER_BORDER = 3;
	private static final int BADGE_SIZE = 24;
	private static final StripTexture ICON = new StripTexture(16);
	private static final int ICON_OFFSET = (BADGE_SIZE - ICON.size()) / 2;
	private static final int HEADER_TEXT_GAP = 6;
	private static final int HEADER_MAX_LINES = 2;
	private static final int MIN_VISIBLE_LINES = 6;
	public static final int PANEL_BORDER = 1;
	public static final int TEXT_PADDING = 4;
	private static final int SCROLL_MARGIN = 2;
	private static final int SCROLL_GAP = 3;
	private static final int VALUE_GAP = 6;
	private static final int ARROW_WIDTH = 16;
	public static final int ARROW_HEIGHT = 28;
	private static final int ARROW_GAP = 6;
	private static final int TITLE_COLOR = 0xFFFFFFFF;
	private static final int HEADER_COLOR = 0xFF2E1806;
	private static final int CLASS_COLOR = 0xFFFFE996;
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	private static final int HEADING_COLOR = 0xFFDEA42C;
	private static final int LABEL_COLOR = 0xFFB4B4C0;
	private static final int VALUE_COLOR = 0xFFFFFFFF;
	private static final int WARNING_COLOR = 0xFFFF6B6B;
	private static final int HOVER_COLOR = 0x30FFFFFF;
	private static final int HOVER_MARGIN = 2;
	private static final int NO_TARGET = -1;
	private static final int FIRST_ENTRY = 0;
	private static final int HUMAN_ENTRY = 0;
	private static final int RANDOM_ENTRY = 1;
	private static final int CHOICES = 2;

	private record Stat(String key, ToDoubleFunction<ServantStats> value, @Nullable String format) {
	}

	private static final List<Stat> STATS = List.of(
		new Stat("max_hp", ServantStats::maxHp, null),
		new Stat("attack", ServantStats::attack, "bonus"),
		new Stat("armor", ServantStats::armor, null),
		new Stat("toughness", ServantStats::toughness, null),
		new Stat("speed", ServantStats::speedPercent, "bonus_percent"),
		new Stat("knockback_resistance", ServantStats::knockbackResistance, null));

	private record Block(Component label, @Nullable Component value, int color, int target) {
		static Block text(Component text, int color) {
			return new Block(text, null, color, NO_TARGET);
		}

		static Block row(Component label, @Nullable Component value, int color) {
			return new Block(label, value, color, NO_TARGET);
		}

		static Block heading(String key) {
			return text(Component.translatable(KEY_PREFIX + key).withStyle(ChatFormatting.BOLD), HEADING_COLOR);
		}

		static Block blank() {
			return text(Component.empty(), TEXT_COLOR);
		}
	}

	private record Line(FormattedCharSequence label, @Nullable Component value, int color, int target) {
	}

	private record Entry(Component name, @Nullable Identifier icon, ItemStack item, List<Block> blocks, ChooseServantPayload.@Nullable Kind kind,
		@Nullable Identifier servant, boolean choosable) {
	}

	private record ClassLine(Identifier classId, int firstServant) {
	}

	private final OpenServantSelectionPayload options;
	private final List<Entry> entries = new ArrayList<>();
	private List<Line> lines = List.of();
	private List<FormattedCharSequence> headerName = List.of();
	private int index;
	private int scroll;
	private boolean draggingThumb;
	private int left;
	private int top;
	private int visibleLines;
	private int contentsPages;
	private Button choose;
	private Button contents;

	public ServantSelectionScreen(OpenServantSelectionPayload options) {
		super(Component.translatable(KEY_PREFIX + "title"));
		this.options = options;
	}

	@Override
	protected void init() {
		this.left = (this.width - WINDOW_WIDTH) / 2;
		boolean first = this.entries.isEmpty();
		int pagesBefore = this.contentsPages;
		this.buildEntries();
		// Resizing can change the page count; preserve the selected servant.
		this.index = first ? FIRST_ENTRY
			: this.index >= pagesBefore ? this.index - pagesBefore + this.contentsPages : Math.min(this.index, this.contentsPages - 1);
		int arrowY = this.top + (this.windowHeight() - ARROW_HEIGHT) / 2;
		this.addRenderableWidget(new ArrowButton(this.left - ARROW_GAP - ARROW_WIDTH, arrowY, -1, ARROW_LEFT, ARROW_LEFT_HIGHLIGHTED));
		this.addRenderableWidget(new ArrowButton(this.left + WINDOW_WIDTH + ARROW_GAP, arrowY, 1, ARROW_RIGHT, ARROW_RIGHT_HIGHLIGHTED));
		int buttonsY = this.top + this.windowHeight() + BUTTON_GAP;
		int chooseX = this.left + CONTENTS_BUTTON_WIDTH + BUTTONS_SPACING;
		this.contents = this.addRenderableWidget(Button.builder(Component.translatable(KEY_PREFIX + "contents"), b -> this.show(FIRST_ENTRY, 0))
			.bounds(this.left, buttonsY, CONTENTS_BUTTON_WIDTH, Button.DEFAULT_HEIGHT)
			.build());
		this.choose = this.addRenderableWidget(Button.builder(Component.translatable(KEY_PREFIX + "choose"), b -> this.choose())
			.bounds(chooseX, buttonsY, this.left + WINDOW_WIDTH - chooseX, Button.DEFAULT_HEIGHT)
			.build());
		this.show(this.index, this.scroll);
	}

	private void buildEntries() {
		Map<UUID, Identifier> owners = this.minecraft.level.globalAttachments().getAttachedOrElse(ServantStorage.OWNERS, Map.of());
		List<Identifier> free = ServantRules.free(AlayaRegistries.SERVANT.keySet(), owners, this.minecraft.player.getUUID(), this.options.allowDuplicates());
		List<Entry> body = new ArrayList<>();
		body.add(new Entry(Component.translatable(KEY_PREFIX + "human.title").withStyle(ChatFormatting.BOLD), null, new ItemStack(Items.PLAYER_HEAD),
			List.of(Block.text(Component.translatable(KEY_PREFIX + "human.description"), TEXT_COLOR)), ChooseServantPayload.Kind.HUMAN, null, true));
		List<Block> random = new ArrayList<>();
		random.add(Block.text(Component.translatable(KEY_PREFIX + (this.options.forceRandom() ? "force_random" : "random.description")), TEXT_COLOR));
		if (free.isEmpty()) {
			random.add(Block.blank());
			random.add(Block.text(Component.translatable(KEY_PREFIX + "all_taken"), WARNING_COLOR));
		}
		body.add(new Entry(Component.translatable(KEY_PREFIX + "random.title").withStyle(ChatFormatting.BOLD), UNKNOWN_ICON, ItemStack.EMPTY, random,
			ChooseServantPayload.Kind.RANDOM, null, !free.isEmpty()));
		List<ClassLine> classes = new ArrayList<>();
		if (!this.options.forceRandom()) {
			for (ServantClass servantClass : AlayaRegistries.SERVANT_CLASS) {
				Identifier classId = AlayaRegistries.SERVANT_CLASS.getKey(servantClass);
				int first = NO_TARGET;
				for (Servant servant : AlayaRegistries.SERVANT) {
					if (servant.servantClass().identifier().equals(classId)) {
						first = first == NO_TARGET ? body.size() : first;
						body.add(this.servantEntry(AlayaRegistries.SERVANT.getKey(servant), servant, classId, free));
					}
				}
				classes.add(new ClassLine(classId, first));
			}
		}
		// The window is as tall as the tallest page, but the contents are paged to fit it, so they come after it is sized.
		this.sizeWindow(body);
		int lineCount = CHOICES + classes.size();
		this.contentsPages = Math.ceilDiv(lineCount, this.visibleLines);
		List<Block> lines = new ArrayList<>();
		lines.add(this.contentsLink(Component.translatable(KEY_PREFIX + "human.title"), this.contentsPages + HUMAN_ENTRY));
		lines.add(this.contentsLink(Component.translatable(KEY_PREFIX + "random.title"), this.contentsPages + RANDOM_ENTRY));
		for (ClassLine line : classes) {
			Component name = ServantClass.name(line.classId());
			lines.add(line.firstServant() == NO_TARGET ? Block.row(name, Component.translatable(KEY_PREFIX + "coming_soon").withColor(LABEL_COLOR), LABEL_COLOR)
				: this.contentsLink(name, this.contentsPages + line.firstServant()));
		}
		this.entries.clear();
		for (int page = 0; page < this.contentsPages; page++) {
			List<Block> pageLines = lines.subList(page * this.visibleLines, Math.min(lineCount, (page + 1) * this.visibleLines));
			this.entries.add(new Entry(this.contentsName(page), ALL_ICON, ItemStack.EMPTY, pageLines, null, null, false));
		}
		this.entries.addAll(body);
	}

	private Block contentsLink(Component label, int target) {
		return new Block(Component.literal(this.font.substrByWidth(label, textWidth()).getString()), null, TEXT_COLOR, target);
	}

	private Component contentsName(int page) {
		MutableComponent name = Component.translatable(KEY_PREFIX + "contents.title").withStyle(ChatFormatting.BOLD);
		if (this.contentsPages > 1) {
			name.append(Component.literal("\n").append(Component.translatable(KEY_PREFIX + "contents.page", page + 1, this.contentsPages))
				.withStyle(Style.EMPTY.withBold(false)));
		}
		return name;
	}

	private Entry servantEntry(Identifier id, Servant servant, Identifier classId, List<Identifier> free) {
		boolean isFree = free.contains(id);
		MutableComponent name = Servant.name(id).copy().withStyle(ChatFormatting.BOLD);
		if (!isFree) {
			name.append(Component.literal(" ").append(Component.translatable(KEY_PREFIX + "taken"))
				.withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(false)));
		}
		List<Block> blocks = new ArrayList<>();
		blocks.add(Block.text(ServantClass.name(classId), CLASS_COLOR));
		Component title = Servant.title(id);
		if (!title.getString().isEmpty()) {
			blocks.add(Block.text(title, TEXT_COLOR));
		}
		ServantStats real = this.options.realStats().get(classId);
		if (real != null) {
			blocks.add(Block.blank());
			blocks.add(Block.heading("stats"));
			for (Stat stat : STATS) {
				blocks.add(Block.row(statLabel(stat), this.statValue(stat, real), LABEL_COLOR));
			}
		}
		if (!servant.abilities().isEmpty()) {
			blocks.add(Block.blank());
			blocks.add(Block.heading("abilities"));
			for (ResourceKey<Ability> key : servant.abilities()) {
				Ability ability = AlayaRegistries.ABILITY.getValue(key);
				String rank = ability == null ? null : ability.rankLabel();
				blocks.add(Block.row(Ability.name(key.identifier()), rank == null ? null : Component.literal(rank), LABEL_COLOR));
			}
		}
		Identifier classIcon = ServantClass.icon(classId);
		boolean hasIcon = this.minecraft.getResourceManager().getResource(classIcon).isPresent();
		return new Entry(name, hasIcon ? classIcon : UNKNOWN_ICON, ItemStack.EMPTY, blocks, ChooseServantPayload.Kind.SERVANT, id, isFree);
	}

	private Component statValue(Stat stat, ServantStats real) {
		String demi = number(stat.value().applyAsDouble(this.options.demi()));
		String end = number(stat.value().applyAsDouble(real));
		String range = demi.equals(end) ? end : demi + Rank.RANGE_DASH + end;
		Component value = formattedStat(stat, range);
		return this.font.width(statLabel(stat)) + VALUE_GAP + this.font.width(value) <= textWidth() ? value : formattedStat(stat, end);
	}

	private static Component statLabel(Stat stat) {
		return Component.translatable(KEY_PREFIX + "stat." + stat.key());
	}

	private static Component formattedStat(Stat stat, String value) {
		return stat.format() == null ? Component.literal(value) : Component.translatable(KEY_PREFIX + stat.format(), value);
	}

	private static String number(double value) {
		return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
	}

	private void sizeWindow(List<Entry> sized) {
		int tallest = MIN_VISIBLE_LINES;
		for (Entry entry : sized) {
			tallest = Math.max(tallest, this.layout(entry.blocks()).size());
		}
		int fixedHeight = 2 * SCREEN_MARGIN + this.font.lineHeight + TITLE_GAP + 2 * WINDOW_PADDING + HEADER_HEIGHT + HEADER_GAP
			+ 2 * (PANEL_BORDER + TEXT_PADDING) + BUTTON_GAP + Button.DEFAULT_HEIGHT;
		int availableLines = Math.max(1, (this.height - fixedHeight) / this.lineStep());
		this.visibleLines = Math.min(availableLines, tallest);
		int total = this.font.lineHeight + TITLE_GAP + this.windowHeight() + BUTTON_GAP + Button.DEFAULT_HEIGHT;
		this.top = Math.max(SCREEN_MARGIN, (this.height - total) / 2) + this.font.lineHeight + TITLE_GAP;
	}

	private void show(int index, int scroll) {
		this.index = index;
		Entry entry = this.entries.get(index);
		this.lines = this.layout(entry.blocks());
		this.headerName = this.font.split(entry.name(), this.panelX() + panelWidth() - HEADER_BORDER - HEADER_TEXT_GAP - this.headerNameX());
		this.draggingThumb = false;
		this.scroll = Math.clamp(scroll, 0, this.maxScroll());
		this.choose.active = entry.choosable();
		this.contents.active = index != FIRST_ENTRY;
	}

	private List<Line> layout(List<Block> blocks) {
		List<Line> result = new ArrayList<>();
		for (Block block : blocks) {
			if (block.value() != null) {
				int labelWidth = Math.max(VALUE_GAP, textWidth() - this.font.width(block.value()) - VALUE_GAP);
				List<FormattedCharSequence> label = this.font.split(block.label(), labelWidth);
				for (int i = 0; i < label.size(); i++) {
					result.add(new Line(label.get(i), i == label.size() - 1 ? block.value() : null, block.color(), block.target()));
				}
				continue;
			}
			List<FormattedCharSequence> wrapped = this.font.split(block.label(), textWidth());
			if (wrapped.isEmpty()) {
				result.add(new Line(FormattedCharSequence.EMPTY, null, block.color(), block.target()));
			}
			for (FormattedCharSequence part : wrapped) {
				result.add(new Line(part, null, block.color(), block.target()));
			}
		}
		return result;
	}

	private @Nullable Line lineAt(double x, double y) {
		if (!this.overPanel(x, y)) {
			return null;
		}
		int row = (int) ((y - this.textY()) / this.lineStep());
		int line = this.scroll + row;
		return y >= this.textY() && row < this.visibleLines && line < this.lines.size() ? this.lines.get(line) : null;
	}

	private int maxScroll() {
		return Math.max(0, this.lines.size() - this.visibleLines);
	}

	private void go(int step) {
		this.show(Math.floorMod(this.index + step, this.entries.size()), 0);
	}

	private void choose() {
		Entry entry = this.entries.get(this.index);
		ClientPlayNetworking.send(new ChooseServantPayload(entry.kind(), Optional.ofNullable(entry.servant())));
		this.minecraft.setScreen(null);
	}

	private int windowHeight() {
		return WINDOW_PADDING + HEADER_HEIGHT + HEADER_GAP + this.panelHeight() + WINDOW_PADDING;
	}

	private static int panelWidth() {
		return WINDOW_WIDTH - 2 * WINDOW_PADDING;
	}

	private int panelHeight() {
		return 2 * (PANEL_BORDER + TEXT_PADDING) + this.visibleLines * this.lineStep();
	}

	private int lineStep() {
		return this.font.lineHeight + LINE_SPACING;
	}

	private int panelX() {
		return this.left + WINDOW_PADDING;
	}

	private int panelY() {
		return this.top + WINDOW_PADDING + HEADER_HEIGHT + HEADER_GAP;
	}

	private int textX() {
		return this.panelX() + PANEL_BORDER + TEXT_PADDING;
	}

	private int textY() {
		return this.panelY() + PANEL_BORDER + TEXT_PADDING;
	}

	private static int textWidth() {
		return scrollbarOffset() - SCROLL_GAP - PANEL_BORDER - TEXT_PADDING;
	}

	private static int scrollbarOffset() {
		return panelWidth() - PANEL_BORDER - SCROLL_MARGIN - ScrollBar.WIDTH;
	}

	private int scrollbarX() {
		return this.panelX() + scrollbarOffset();
	}

	private int scrollbarY() {
		return this.panelY() + PANEL_BORDER + SCROLL_MARGIN;
	}

	private int scrollbarHeight() {
		return this.panelHeight() - 2 * (PANEL_BORDER + SCROLL_MARGIN);
	}

	private boolean overPanel(double x, double y) {
		return x >= this.panelX() && x < this.panelX() + panelWidth() && y >= this.panelY() && y < this.panelY() + this.panelHeight();
	}

	private boolean overScrollbar(double x, double y) {
		return x >= this.scrollbarX() && x < this.scrollbarX() + ScrollBar.WIDTH && y >= this.scrollbarY() && y < this.scrollbarY() + this.scrollbarHeight();
	}

	private void dragThumbTo(double y) {
		this.scroll = ScrollBar.scrollAt(y, this.scrollbarY(), this.scrollbarHeight(), this.visibleLines, this.lines.size());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, GuiSprites.PANEL, this.left, this.top, WINDOW_WIDTH, this.windowHeight());
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEADER, this.panelX(), this.top + WINDOW_PADDING, panelWidth(), HEADER_HEIGHT);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BADGE, this.badgeX(), this.badgeY(), BADGE_SIZE, BADGE_SIZE);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TEXT_PANEL, this.panelX(), this.panelY(), panelWidth(), this.panelHeight());
		if (this.maxScroll() > 0) {
			ScrollBar.extract(graphics, this.scrollbarX(), this.scrollbarY(), this.scrollbarHeight(), this.visibleLines, this.lines.size(), this.scroll);
		}
	}

	private int badgeX() {
		return this.panelX() + HEADER_BORDER;
	}

	private int badgeY() {
		return this.top + WINDOW_PADDING + HEADER_BORDER;
	}

	private int headerNameX() {
		return this.badgeX() + BADGE_SIZE + HEADER_TEXT_GAP;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		Entry entry = this.entries.get(this.index);
		graphics.centeredText(this.font, this.title, this.width / 2, this.top - TITLE_GAP - this.font.lineHeight, TITLE_COLOR);

		int iconX = this.badgeX() + ICON_OFFSET;
		int iconY = this.badgeY() + ICON_OFFSET;
		if (entry.icon() != null) {
			ICON.blit(graphics, entry.icon(), iconX, iconY, this.minecraft.getWindow().getGuiScale());
		} else {
			graphics.item(entry.item(), iconX, iconY);
		}

		int nameLines = Math.min(HEADER_MAX_LINES, this.headerName.size());
		int nameY = this.top + WINDOW_PADDING + (HEADER_HEIGHT - nameLines * this.font.lineHeight) / 2;
		for (int i = 0; i < nameLines; i++) {
			graphics.text(this.font, this.headerName.get(i), this.headerNameX(), nameY + i * this.font.lineHeight, HEADER_COLOR, false);
		}

		int textRight = this.textX() + textWidth();
		Line hovered = this.lineAt(mouseX, mouseY);
		for (int i = 0; i < this.visibleLines && this.scroll + i < this.lines.size(); i++) {
			Line line = this.lines.get(this.scroll + i);
			int y = this.textY() + i * this.lineStep();
			if (line == hovered && line.target() != NO_TARGET) {
				graphics.fill(this.textX() - HOVER_MARGIN, y - LINE_SPACING, textRight + HOVER_MARGIN, y + this.font.lineHeight, HOVER_COLOR);
			}
			graphics.text(this.font, line.label(), this.textX(), y, line.color(), false);
			if (line.value() != null) {
				graphics.text(this.font, line.value(), textRight - this.font.width(line.value()), y, VALUE_COLOR, false);
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.maxScroll() > 0 && this.overScrollbar(event.x(), event.y())) {
			this.draggingThumb = true;
			this.dragThumbTo(event.y());
			return true;
		}
		Line clicked = event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT ? this.lineAt(event.x(), event.y()) : null;
		if (clicked != null && clicked.target() != NO_TARGET) {
			this.show(clicked.target(), 0);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.draggingThumb) {
			this.dragThumbTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.draggingThumb = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0.0) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		if (this.overPanel(mouseX, mouseY)) {
			this.scroll = Math.clamp(this.scroll + ScrollBar.wheelLines(scrollY), 0, this.maxScroll());
		} else {
			this.go(scrollY < 0 ? 1 : -1);
		}
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT) {
			this.go(event.key() == GLFW.GLFW_KEY_LEFT ? -1 : 1);
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return this.options.closable();
	}

	@Override
	public void onClose() {
		ClientPlayNetworking.send(new ChooseServantPayload(ChooseServantPayload.Kind.CANCEL, Optional.empty()));
		super.onClose();
	}

	private final class ArrowButton extends AbstractButton {
		private final int step;
		private final Identifier sprite;
		private final Identifier highlighted;

		private ArrowButton(int x, int y, int step, Identifier sprite, Identifier highlighted) {
			super(x, y, ARROW_WIDTH, ARROW_HEIGHT, Component.translatable(KEY_PREFIX + (step < 0 ? "previous" : "next")));
			this.step = step;
			this.sprite = sprite;
			this.highlighted = highlighted;
		}

		@Override
		public void onPress(InputWithModifiers input) {
			ServantSelectionScreen.this.go(this.step);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.isHoveredOrFocused() ? this.highlighted : this.sprite,
				this.getX(), this.getY(), this.width, this.height);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}
	}
}
