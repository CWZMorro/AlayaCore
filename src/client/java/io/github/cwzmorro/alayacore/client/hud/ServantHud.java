package io.github.cwzmorro.alayacore.client.hud;

import io.github.cwzmorro.alayacore.AlayaCore;
import io.github.cwzmorro.alayacore.api.AlayaMana;
import io.github.cwzmorro.alayacore.api.AlayaServants;
import io.github.cwzmorro.alayacore.mana.ManaData;
import io.github.cwzmorro.alayacore.mana.ManaRules;
import io.github.cwzmorro.alayacore.progression.ClassGrade;
import io.github.cwzmorro.alayacore.progression.ClassGrades;
import io.github.cwzmorro.alayacore.servant.ServantClass;
import io.github.cwzmorro.alayacore.util.Percent;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * The player HUD (plan 14), top-left, in the style of the old health-indicator mods: a framed
 * portrait with the live player model and the class badge on its bottom-left corner; touching it, and
 * each other, the name plate (as wide as the name), the HP bar ("current | max") and the shorter mana
 * bar (percent). Vanilla's hearts are hidden.
 *
 * <p>Like Tensura's HUD: shown in every game mode, only the portrait in spectator, and hidden while
 * the GUI is hidden (F1) or the debug screen (F3) is open.
 *
 * <p>Drawn from the {@code hud/} GUI sprites. Sizes below are in texels, about two per
 * GUI pixel so the frames' 1-texel lines are thin; each texel covers a whole number of screen pixels, so
 * lines stay straight at every GUI scale.
 */
public final class ServantHud {
	private static final int TEXELS_PER_PIXEL = 2;
	private static final Identifier PORTRAIT = AlayaCore.id("hud/portrait");
	private static final Identifier NAME_PLATE = AlayaCore.id("hud/name_plate");
	private static final Identifier HEALTH_BAR = AlayaCore.id("hud/health_bar");
	private static final Identifier HEALTH_BAR_FILL = AlayaCore.id("hud/health_bar_fill");
	private static final Identifier MANA_BAR = AlayaCore.id("hud/mana_bar");
	private static final Identifier MANA_BAR_FILL = AlayaCore.id("hud/mana_bar_fill");

	private static final int MARGIN = 8;
	private static final int FRAME = 4;
	/** The frame's gold line, counted from its outer edge; the badge is centred on its corner. */
	private static final int GOLD_LINE = 2;

	private static final int PORTRAIT_SIZE = 96;
	/** Part of the portrait's height a standing player fills, leaving room around it like Tensura's portrait. */
	private static final float PORTRAIT_FILL = 0.8F;
	/** How far the body turns from facing the viewer, in degrees: a three-quarter view like Tensura's. */
	private static final float PORTRAIT_BODY_TURN = 20.0F;
	/** Width of a badge's diamond (Black to Gold); Grand's star is wider. */
	private static final int BADGE_DIAMOND = 32;
	/** Drawn size of a badge: every grade sits on the same canvas, its diamond {@link #BADGE_DIAMOND} wide. */
	private static final int BADGE_SIZE = 38;
	/**
	 * A badge texture holds one copy per screen pixels per texel (1 to this), side by side, so the badge is
	 * drawn pixel for pixel at every GUI scale up to 8 ({@code art/textures.py}). Past that, the largest is stretched.
	 */
	private static final int BADGE_COPIES = 4;
	private static final int BADGE_TEXTURE_WIDTH = BADGE_SIZE * BADGE_COPIES * (BADGE_COPIES + 1) / 2;
	private static final int BADGE_TEXTURE_HEIGHT = BADGE_SIZE * BADGE_COPIES;
	/** The badge sticks out past the portrait's left edge, so the portrait starts that much further in. */
	private static final int PORTRAIT_X = MARGIN + BADGE_DIAMOND / 2 - GOLD_LINE;
	private static final int PORTRAIT_Y = MARGIN;

	private static final int NAME_HEIGHT = 30;
	/** Space between the name and the plate's frame, on each side. */
	private static final int NAME_PADDING = 10;
	private static final int HEALTH_WIDTH = 200;
	private static final int HEALTH_HEIGHT = 32;
	private static final int MANA_WIDTH = 152;
	private static final int MANA_HEIGHT = 28;

	/** Texels per font pixel: vanilla text size. */
	private static final int TEXT_SCALE = 2;
	/** Height of capitals and digits in font pixels, for centring text in a box. */
	private static final int TEXT_CAP_HEIGHT = 7;
	private static final int TEXT = 0xFFFFFFFF;

	private ServantHud() {
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, vanilla -> (graphics, deltaTracker) -> {
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, AlayaCore.id("servant_hud"), ServantHud::extract);
	}

	/** The portrait's box on the screen at {@code guiScale}, in screen pixels: x, y, width and height. */
	public static int[] portraitOnScreen(int guiScale) {
		int pixelsPerTexel = pixelsPerTexel(guiScale);
		return new int[] {PORTRAIT_X * pixelsPerTexel, PORTRAIT_Y * pixelsPerTexel, PORTRAIT_SIZE * pixelsPerTexel, PORTRAIT_SIZE * pixelsPerTexel};
	}

	/** Whole screen pixels per HUD texel at {@code guiScale}. */
	private static int pixelsPerTexel(int guiScale) {
		return Math.max(1, Math.round(guiScale / (float) TEXELS_PER_PIXEL));
	}

	private static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}
		@Nullable Identifier classId = AlayaServants.classId(player).orElse(null);
		Matrix3x2fStack pose = graphics.pose();
		// GUI pixels per texel: whole screen pixels per texel, about half a GUI pixel.
		int guiScale = minecraft.getWindow().getGuiScale();
		int pixelsPerTexel = pixelsPerTexel(guiScale);
		float texel = pixelsPerTexel / (float) guiScale;

		pose.pushMatrix();
		pose.scale(texel);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PORTRAIT, PORTRAIT_X, PORTRAIT_Y, PORTRAIT_SIZE, PORTRAIT_SIZE);
		pose.popMatrix();
		portrait(graphics, player, deltaTracker.getGameTimeDeltaPartialTick(false), texel);

		pose.pushMatrix();
		pose.scale(texel);
		if (classId != null) {
			// Centred on the corner of the portrait's gold lines: the top tip on the left line, the side tips on the bottom line.
			int cornerX = PORTRAIT_X + GOLD_LINE;
			int cornerY = PORTRAIT_Y + PORTRAIT_SIZE - 1 - GOLD_LINE;
			Identifier badge = ServantClass.badge(classId, player.getAttachedOrElse(ClassGrades.GRADE, ClassGrade.BLACK));
			int copy = Math.min(pixelsPerTexel, BADGE_COPIES);
			int copySize = BADGE_SIZE * copy;
			graphics.blit(RenderPipelines.GUI_TEXTURED, badge, cornerX - BADGE_SIZE / 2, cornerY - BADGE_SIZE / 2,
				BADGE_SIZE * copy * (copy - 1) / 2.0F, 0.0F, BADGE_SIZE, BADGE_SIZE, copySize, copySize, BADGE_TEXTURE_WIDTH, BADGE_TEXTURE_HEIGHT);
		}
		if (!player.isSpectator()) {
			extractStatus(graphics, minecraft.font, player, classId);
		}
		pose.popMatrix();
	}

	/**
	 * The live player inside the portrait's frame, turned three-quarters with room around it, showing
	 * every animation: its own head turn and up/down look, arms, legs, sneaking, swimming, flying; but not
	 * the arms' idle sway, so standing still it holds still.
	 */
	private static void portrait(GuiGraphicsExtractor graphics, LocalPlayer player, float partialTick, float texel) {
		// The model renderer works in GUI pixels and clips to this box, kept inside the frame.
		int x0 = Mth.ceil((PORTRAIT_X + FRAME) * texel);
		int y0 = Mth.ceil((PORTRAIT_Y + FRAME) * texel);
		int x1 = Mth.floor((PORTRAIT_X + PORTRAIT_SIZE - FRAME) * texel);
		int y1 = Mth.floor((PORTRAIT_Y + PORTRAIT_SIZE - FRAME) * texel);
		EntityRenderState state = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player).createRenderState(player, partialTick);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		state.nameTag = null;
		state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
		// The arms' idle sway runs on the entity's age; held at one age, a player standing still shows the same pixels.
		state.ageInTicks = 0.0F;
		if (state instanceof LivingEntityRenderState living) {
			// Only the body is turned to face the viewer; the head keeps its own turn and look.
			living.bodyRot = 180.0F + PORTRAIT_BODY_TURN;
			living.boundingBoxWidth /= living.scale;
			living.boundingBoxHeight /= living.scale;
			living.scale = 1.0F;
		}
		float size = (y1 - y0) * PORTRAIT_FILL / EntityType.PLAYER.getDimensions().height();
		graphics.entity(state, size, new Vector3f(0.0F, state.boundingBoxHeight / 2.0F, 0.0F), new Quaternionf().rotateZ((float) Math.PI), null,
			x0, y0, x1, y1);
	}

	/** Name plate, HP bar and mana bar, stacked to the right of the portrait. */
	private static void extractStatus(GuiGraphicsExtractor graphics, Font font, LocalPlayer player, @Nullable Identifier classId) {
		int x = PORTRAIT_X + PORTRAIT_SIZE;
		int y = PORTRAIT_Y;
		Component name = classId != null ? ServantClass.name(classId) : player.getName();
		int nameWidth = font.width(name) * TEXT_SCALE + 2 * (FRAME + NAME_PADDING);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NAME_PLATE, x, y, nameWidth, NAME_HEIGHT);
		centered(graphics, font, name, x, y, nameWidth, NAME_HEIGHT);

		y += NAME_HEIGHT;
		float health = player.getHealth();
		float maxHealth = player.getMaxHealth();
		bar(graphics, HEALTH_BAR, HEALTH_BAR_FILL, x, y, HEALTH_WIDTH, HEALTH_HEIGHT, maxHealth > 0 ? health / maxHealth : 0);
		centered(graphics, font, Component.literal(String.format(Locale.ROOT, "%.1f | %.1f", health, maxHealth)), x, y, HEALTH_WIDTH, HEALTH_HEIGHT);

		ManaData mana = AlayaMana.get(player);
		if (mana != null) {
			y += HEALTH_HEIGHT;
			double fraction = ManaRules.fraction(mana);
			bar(graphics, MANA_BAR, MANA_BAR_FILL, x, y, MANA_WIDTH, MANA_HEIGHT, fraction);
			centered(graphics, font, Component.literal(Math.round(Percent.fromFraction(fraction)) + "%"), x, y, MANA_WIDTH, MANA_HEIGHT);
		}
	}

	/** A framed bar with its fill cut to {@code ratio} (0–1) of the width inside the frame. */
	private static void bar(GuiGraphicsExtractor graphics, Identifier background, Identifier fill, int x, int y, int width, int height, double ratio) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, background, x, y, width, height);
		int innerWidth = width - 2 * FRAME;
		int innerHeight = height - 2 * FRAME;
		int filled = (int) Math.round(innerWidth * Math.clamp(ratio, 0.0, 1.0));
		if (filled > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill, innerWidth, innerHeight, 0, 0, x + FRAME, y + FRAME, filled, innerHeight);
		}
	}

	/** Text centred inside a box's frame, with vanilla's shadow. */
	private static void centered(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int height) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x + (width - font.width(text) * TEXT_SCALE) / 2, y + (height - TEXT_CAP_HEIGHT * TEXT_SCALE) / 2);
		pose.scale(TEXT_SCALE);
		graphics.text(font, text, 0, 0, TEXT, true);
		pose.popMatrix();
	}
}
