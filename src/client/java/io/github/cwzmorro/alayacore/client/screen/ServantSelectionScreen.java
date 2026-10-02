package io.github.cwzmorro.alayacore.client.screen;

import io.github.cwzmorro.alayacore.ability.Ability;
import io.github.cwzmorro.alayacore.api.AlayaRegistries;
import io.github.cwzmorro.alayacore.network.ChooseServantPayload;
import io.github.cwzmorro.alayacore.network.OpenServantSelectionPayload;
import io.github.cwzmorro.alayacore.servant.Servant;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.servant.ServantRules;
import io.github.cwzmorro.alayacore.servant.ServantStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

/**
 * The servant selection screen (plan 3): one vanilla book, laid out like Mahou Tsukai's guidebook.
 *
 * <p>It opens on the table of contents ("Choose your class"): one line per class, then Random and
 * Human, each with its page number; lines that don't fit continue on the next page. A class line
 * leads to the class's page ("Choose your servant", one line per servant), which is followed by one page
 * per servant. The arrows and the scroll wheel flip through every page in order; "Contents", between the
 * arrows, goes back.
 * One Choose button takes whatever the open page offers. With force random on, only Random and Stay
 * human are in the book.
 *
 * <p>The first-join screen can't be closed; reopened by the Spirit Origin Changer, Esc closes it and
 * nothing changes. The server checks the answer and reopens the screen if it was refused.
 */
public final class ServantSelectionScreen extends Screen {
	// Book layout, the same as vanilla's BookViewScreen.
	private static final int BOOK_SIZE = 192;
	private static final int BOOK_TEXTURE_SIZE = 256;
	private static final int PAGE_BOTTOM = 181;
	private static final int TEXT_X = 36;
	private static final int TEXT_Y = 30;
	private static final int TEXT_WIDTH = 114;
	private static final int TEXT_BOTTOM = 152;
	private static final int INDICATOR_RIGHT = 148;
	private static final int INDICATOR_Y = 16;
	private static final int ARROW_Y = 157;
	private static final int ARROW_BACK_X = 43;
	private static final int ARROW_FORWARD_X = 116;
	/** Width of vanilla's page arrows; the Contents link sits centred between them. */
	private static final int ARROW_WIDTH = 23;
	private static final int ARROW_HEIGHT = 13;

	/** Height of one contents line. */
	private static final int LINK_HEIGHT = 12;
	/** Space between a page's heading and its contents lines or text. */
	private static final int HEADING_GAP = 4;
	private static final int BUTTON_GAP = 4;
	private static final int TEXT_COLOR = 0xFF000000;
	private static final int LINK_COLOR = 0xFF404040;
	private static final int LINK_HOVER_COLOR = 0xFF000000;
	private static final int LEADER_COLOR = 0xFFA0A0A0;
	private static final int SUBTITLE_COLOR = 0xFF555555;
	private static final int STATUS_COLOR = 0xFFAA0000;

	/** A contents line: its text and the page it leads to. */
	private record Link(Component label, int target) {
	}

	/** One page: its heading, contents lines or text, and what Choose does on it (nothing if {@code kind} is null). */
	private record Page(Component title, @Nullable Component subtitle, @Nullable Component status, List<Link> links, Component body,
		ChooseServantPayload.@Nullable Kind kind, @Nullable Identifier servant, boolean choosable) {
		static Page contents(Component title, @Nullable Component subtitle, @Nullable Component status, List<Link> links) {
			return new Page(title, subtitle, status, links, Component.empty(), null, null, false);
		}

		int headingLines() {
			return ServantSelectionScreen.headingLines(this.subtitle, this.status);
		}
	}

	private final OpenServantSelectionPayload options;
	private final List<Page> pages = new ArrayList<>();
	private int page;
	private int left;
	private int top;

	public ServantSelectionScreen(OpenServantSelectionPayload options) {
		super(Component.translatable("alayacore.screen.servant_selection.title"));
		this.options = options;
	}

	@Override
	protected void init() {
		this.buildBook();
		this.page = Math.min(this.page, this.pages.size() - 1);
		this.left = (this.width - BOOK_SIZE) / 2;
		this.top = Math.max(BUTTON_GAP, (this.height - PAGE_BOTTOM - BUTTON_GAP - Button.DEFAULT_HEIGHT) / 2);
		Page current = this.pages.get(this.page);

		if (this.page > 0) {
			this.addRenderableWidget(new PageButton(this.left + ARROW_BACK_X, this.top + ARROW_Y, false, b -> this.goTo(this.page - 1), true));
			Component contents = Component.translatable("alayacore.screen.servant_selection.contents");
			int between = (ARROW_BACK_X + ARROW_WIDTH + ARROW_FORWARD_X) / 2;
			this.addRenderableWidget(new LinkButton(this.left + between - this.font.width(contents) / 2,
				this.top + ARROW_Y + (ARROW_HEIGHT - LINK_HEIGHT) / 2, contents, 0, false));
		}
		if (this.page < this.pages.size() - 1) {
			this.addRenderableWidget(new PageButton(this.left + ARROW_FORWARD_X, this.top + ARROW_Y, true, b -> this.goTo(this.page + 1), true));
		}
		int y = this.top + this.headingBottom(current.headingLines());
		for (Link link : current.links()) {
			this.addRenderableWidget(new LinkButton(this.left + TEXT_X, y, link.label(), link.target(), true));
			y += LINK_HEIGHT;
		}
		Button choose = this.addRenderableWidget(Button.builder(Component.translatable("alayacore.screen.servant_selection.choose"), b -> this.choose())
			.bounds((this.width - Button.DEFAULT_WIDTH) / 2, this.top + PAGE_BOTTOM + BUTTON_GAP, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT)
			.build());
		choose.active = current.choosable();
	}

	/** Builds every page in reading order: contents, each class with its servants, Random, Human. */
	private void buildBook() {
		this.pages.clear();
		Map<UUID, Identifier> owners = this.minecraft.level.globalAttachments().getAttachedOrElse(ServantStorage.OWNERS, Map.of());
		List<Identifier> free = ServantRules.free(AlayaRegistries.SERVANT.keySet(), owners, this.minecraft.player.getUUID(), this.options.allowDuplicates());
		List<Identifier> classes = new ArrayList<>();
		if (!this.options.forceRandom()) {
			for (ServantClass servantClass : AlayaRegistries.SERVANT_CLASS) {
				classes.add(AlayaRegistries.SERVANT_CLASS.getKey(servantClass));
			}
		}

		// Page numbers first: each section's first page, so contents lines can point at them.
		Component contentsTitle = Component.translatable("alayacore.screen.servant_selection.choose_class");
		int contentsCount = this.pageCount(headingLines(null, null), classes.size() + 2);
		List<Link> contents = new ArrayList<>();
		int next = contentsCount;
		List<List<Identifier>> servantsByClass = new ArrayList<>();
		for (Identifier classId : classes) {
			List<Identifier> servants = servantsOf(classId);
			servantsByClass.add(servants);
			contents.add(new Link(ServantClass.name(classId), next));
			next += this.classPageCount(servants) + servants.size();
		}
		Component randomTitle = Component.translatable("alayacore.screen.servant_selection.random.title");
		Component humanTitle = Component.translatable("alayacore.screen.servant_selection.human.title");
		contents.add(new Link(randomTitle, next));
		contents.add(new Link(humanTitle, next + 1));

		this.addLinkPages(contentsTitle, null, null, contents);
		for (int i = 0; i < classes.size(); i++) {
			Identifier classId = classes.get(i);
			List<Identifier> servants = servantsByClass.get(i);
			int firstServantPage = this.pages.size() + this.classPageCount(servants);
			List<Link> links = new ArrayList<>();
			for (int s = 0; s < servants.size(); s++) {
				links.add(new Link(Servant.name(servants.get(s)), firstServantPage + s));
			}
			this.addLinkPages(ServantClass.name(classId), chooseServant(), servants.isEmpty() ? comingSoon() : null, links);
			for (Identifier servant : servants) {
				boolean isFree = free.contains(servant);
				this.pages.add(new Page(Servant.name(servant), ServantClass.name(classId),
					isFree ? null : Component.translatable("alayacore.screen.servant_selection.taken"), List.of(),
					servantBody(servant), ChooseServantPayload.Kind.SERVANT, servant, isFree));
			}
		}
		this.pages.add(new Page(randomTitle, null, free.isEmpty() ? Component.translatable("alayacore.screen.servant_selection.all_taken") : null,
			List.of(), Component.translatable(this.options.forceRandom()
				? "alayacore.screen.servant_selection.force_random"
				: "alayacore.screen.servant_selection.random.description"),
			ChooseServantPayload.Kind.RANDOM, null, !free.isEmpty()));
		this.pages.add(new Page(humanTitle, null, null, List.of(), Component.translatable("alayacore.screen.servant_selection.human.description"),
			ChooseServantPayload.Kind.HUMAN, null, true));
	}

	private static List<Identifier> servantsOf(Identifier classId) {
		List<Identifier> servants = new ArrayList<>();
		for (Servant servant : AlayaRegistries.SERVANT) {
			if (servant.servantClass().identifier().equals(classId)) {
				servants.add(AlayaRegistries.SERVANT.getKey(servant));
			}
		}
		return servants;
	}

	private static Component chooseServant() {
		return Component.translatable("alayacore.screen.servant_selection.choose_servant");
	}

	private static Component comingSoon() {
		return Component.translatable("alayacore.screen.servant_selection.coming_soon");
	}

	private int classPageCount(List<Identifier> servants) {
		return this.pageCount(headingLines(chooseServant(), servants.isEmpty() ? comingSoon() : null), servants.size());
	}

	/** Pages needed for {@code links} lines under a heading of this many lines (at least one page). */
	private int pageCount(int headingLines, int links) {
		int perPage = this.linksPerPage(headingLines);
		return Math.max(1, (links + perPage - 1) / perPage);
	}

	/** Adds as many pages as the links need, each with the same heading. */
	private void addLinkPages(Component title, @Nullable Component subtitle, @Nullable Component status, List<Link> links) {
		int perPage = this.linksPerPage(headingLines(subtitle, status));
		int start = 0;
		do {
			this.pages.add(Page.contents(title, subtitle, status, links.subList(start, Math.min(links.size(), start + perPage))));
			start += perPage;
		} while (start < links.size());
	}

	private int linksPerPage(int headingLines) {
		return (TEXT_BOTTOM - this.headingBottom(headingLines)) / LINK_HEIGHT;
	}

	/** A heading's lines: the title, then the subtitle and status if there are any. */
	private static int headingLines(@Nullable Component subtitle, @Nullable Component status) {
		return 1 + (subtitle != null ? 1 : 0) + (status != null ? 1 : 0);
	}

	/** Y (from the book's top) where a page's lines or text start, under a heading of this many lines. */
	private int headingBottom(int headingLines) {
		return TEXT_Y + headingLines * this.font.lineHeight + HEADING_GAP;
	}

	/** The servant's description, then its abilities (plan 3). */
	private static Component servantBody(Identifier id) {
		MutableComponent body = Servant.description(id).copy();
		Servant servant = AlayaRegistries.SERVANT.getValue(id);
		if (servant != null && !servant.abilities().isEmpty()) {
			body.append("\n\n").append(Component.translatable("alayacore.screen.servant_selection.abilities").withStyle(ChatFormatting.BOLD));
			servant.abilities().forEach(ability -> body.append("\n- ").append(Ability.name(ability.identifier())));
		}
		return body;
	}

	private void goTo(int page) {
		this.page = Math.clamp(page, 0, this.pages.size() - 1);
		this.rebuildWidgets();
	}

	private void choose() {
		Page current = this.pages.get(this.page);
		if (current.kind() != null) {
			ClientPlayNetworking.send(new ChooseServantPayload(current.kind(), Optional.ofNullable(current.servant())));
			this.minecraft.setScreen(null);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		graphics.blit(RenderPipelines.GUI_TEXTURED, BookViewScreen.BOOK_LOCATION, this.left, this.top, 0.0F, 0.0F,
			BOOK_SIZE, BOOK_SIZE, BOOK_TEXTURE_SIZE, BOOK_TEXTURE_SIZE);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		Page current = this.pages.get(this.page);
		int x = this.left + TEXT_X;
		int y = this.top + TEXT_Y;
		int lineHeight = this.font.lineHeight;

		Component indicator = Component.translatable("book.pageIndicator", this.page + 1, this.pages.size());
		graphics.text(this.font, indicator, this.left + INDICATOR_RIGHT - this.font.width(indicator), this.top + INDICATOR_Y, TEXT_COLOR, false);

		graphics.text(this.font, current.title().copy().withStyle(ChatFormatting.BOLD), x, y, TEXT_COLOR, false);
		y += lineHeight;
		if (current.subtitle() != null) {
			graphics.text(this.font, current.subtitle(), x, y, SUBTITLE_COLOR, false);
			y += lineHeight;
		}
		if (current.status() != null) {
			graphics.text(this.font, current.status(), x, y, STATUS_COLOR, false);
			y += lineHeight;
		}
		y += HEADING_GAP;
		for (FormattedCharSequence line : this.font.split(current.body(), TEXT_WIDTH)) {
			if (y + lineHeight > this.top + TEXT_BOTTOM) {
				break;
			}
			graphics.text(this.font, line, x, y, TEXT_COLOR, false);
			y += lineHeight;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		// Like Mahou Tsukai's book: scrolling turns the pages.
		if (scrollY != 0.0) {
			this.goTo(this.page + (scrollY < 0 ? 1 : -1));
		}
		return true;
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

	/**
	 * A contents line: its text, then dot leaders and the page number it leads to (when {@code numbered}).
	 * Grey, black under the mouse, like Mahou Tsukai's contents.
	 */
	private final class LinkButton extends AbstractButton {
		private final int target;
		private final boolean numbered;

		private LinkButton(int x, int y, Component label, int target, boolean numbered) {
			super(x, y, numbered ? TEXT_WIDTH : ServantSelectionScreen.this.font.width(label), LINK_HEIGHT, label);
			this.target = target;
			this.numbered = numbered;
		}

		@Override
		public void onPress(InputWithModifiers input) {
			ServantSelectionScreen.this.goTo(this.target);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
			Font font = ServantSelectionScreen.this.font;
			int color = this.isHoveredOrFocused() ? LINK_HOVER_COLOR : LINK_COLOR;
			int textY = this.getY() + (this.height - font.lineHeight) / 2 + 1;
			Component label = this.isHoveredOrFocused() ? this.getMessage().copy().withStyle(ChatFormatting.UNDERLINE) : this.getMessage();
			graphics.text(font, label, this.getX(), textY, color, false);
			if (!this.numbered) {
				return;
			}
			String number = String.valueOf(this.target + 1);
			int numberX = this.getX() + this.width - font.width(number);
			graphics.text(font, number, numberX, textY, color, false);
			int dotWidth = font.width(".");
			int dotsStart = this.getX() + font.width(this.getMessage()) + dotWidth;
			int dots = Math.max(0, (numberX - dotWidth - dotsStart) / dotWidth);
			graphics.text(font, ".".repeat(dots), numberX - dotWidth - dots * dotWidth, textY, LEADER_COLOR, false);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			this.defaultButtonNarrationText(output);
		}
	}
}
