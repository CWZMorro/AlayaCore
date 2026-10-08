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
	private static final int GOLD_LINE = 2;

	private static final int PORTRAIT_SIZE = 96;
	private static final float PORTRAIT_FILL = 0.8F;
	// Degrees the body turns from facing the viewer: a three-quarter view.
	private static final float PORTRAIT_BODY_TURN = 20.0F;
	private static final int BADGE_DIAMOND = 32;
	// Every grade sits on the same 38 texel canvas, its diamond BADGE_DIAMOND wide.
	private static final StripTexture BADGE = new StripTexture(38, 4);
	private static final int PORTRAIT_X = MARGIN + BADGE_DIAMOND / 2 - GOLD_LINE;
	private static final int PORTRAIT_Y = MARGIN;

	private static final int NAME_HEIGHT = 30;
	private static final int NAME_PADDING = 10;
	private static final int HEALTH_WIDTH = 200;
	private static final int HEALTH_HEIGHT = 32;
	private static final int MANA_WIDTH = 152;
	private static final int MANA_HEIGHT = 28;

	private static final int TEXT_SCALE = 2;
	private static final int TEXT_CAP_HEIGHT = 7;
	private static final int TEXT = 0xFFFFFFFF;

	private ServantHud() {
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, vanilla -> (graphics, deltaTracker) -> {
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, AlayaCore.id("servant_hud"), ServantHud::extract);
	}

	public static int[] portraitOnScreen(int guiScale) {
		int pixelsPerTexel = pixelsPerTexel(guiScale);
		return new int[] {PORTRAIT_X * pixelsPerTexel, PORTRAIT_Y * pixelsPerTexel, PORTRAIT_SIZE * pixelsPerTexel, PORTRAIT_SIZE * pixelsPerTexel};
	}

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
			BADGE.blit(graphics, badge, cornerX - BADGE.size() / 2, cornerY - BADGE.size() / 2, pixelsPerTexel);
		}
		if (!player.isSpectator()) {
			extractStatus(graphics, minecraft.font, player, classId);
		}
		pose.popMatrix();
	}

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
			living.bodyRot = 180.0F + PORTRAIT_BODY_TURN;
			living.boundingBoxWidth /= living.scale;
			living.boundingBoxHeight /= living.scale;
			living.scale = 1.0F;
		}
		float size = (y1 - y0) * PORTRAIT_FILL / EntityType.PLAYER.getDimensions().height();
		graphics.entity(state, size, new Vector3f(0.0F, state.boundingBoxHeight / 2.0F, 0.0F), new Quaternionf().rotateZ((float) Math.PI), null,
			x0, y0, x1, y1);
	}

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

	private static void bar(GuiGraphicsExtractor graphics, Identifier background, Identifier fill, int x, int y, int width, int height, double ratio) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, background, x, y, width, height);
		int innerWidth = width - 2 * FRAME;
		int innerHeight = height - 2 * FRAME;
		int filled = (int) Math.round(innerWidth * Math.clamp(ratio, 0.0, 1.0));
		if (filled > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill, innerWidth, innerHeight, 0, 0, x + FRAME, y + FRAME, filled, innerHeight);
		}
	}

	private static void centered(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int height) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x + (width - font.width(text) * TEXT_SCALE) / 2, y + (height - TEXT_CAP_HEIGHT * TEXT_SCALE) / 2);
		pose.scale(TEXT_SCALE);
		graphics.text(font, text, 0, 0, TEXT, true);
		pose.popMatrix();
	}
}
