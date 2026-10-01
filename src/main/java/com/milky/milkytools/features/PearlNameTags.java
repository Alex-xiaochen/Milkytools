package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import com.milky.milkytools.features.ScreenTagRenderer.GuiPainter;
import com.milky.milkytools.features.ScreenTagRenderer.Painter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;*/
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

/**
 * 末影珍珠名牌（PearlNameTags）：把每颗飞行中的末影珍珠投影到屏幕，
 * 在其上方画出投掷者的名字，用来判断这颗珍珠是谁扔的。
 * <p>
 * 投影与 {@link Painter} 层由 {@link ScreenTagRenderer} 提供，与自定义玩家名牌共用，观感一致。
 * <p>
 * 投掷者取自珍珠的 owner：服务端把投掷者的实体 id 放进生成包，客户端据此解析，
 * 因此<b>投掷者不在客户端加载范围内时名字不显示</b>（此时 owner 为 null）。
 * 发射器、指令生成等没有玩家的珍珠同样不画。
 */
public final class PearlNameTags {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    /** 名字画在珍珠包围盒上方再高一点点的位置（格）。 */
    private static final double LABEL_OFFSET = 0.25;

    /** 只显示玩家名，固定用白色。 */
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private PearlNameTags() {
    }

    /** Fabric HUD 回调入口（26.1.2 起）。 */
    //? if >=26.1.2 {
    public static void onHudExtract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!shouldRender() || !ScreenTagRenderer.beginFrame()) {
            return;
        }

        render(new GuiPainter(graphics),
                delta.getGameTimeDeltaPartialTick(false), graphics.guiWidth(), graphics.guiHeight());
    }
    //?} else {
    /*public static void onHudRender(GuiGraphics graphics, DeltaTracker delta) {
        if (!shouldRender() || !ScreenTagRenderer.beginFrame()) {
            return;
        }

        render(new GuiPainter(graphics),
                delta.getGameTimeDeltaPartialTick(false), graphics.guiWidth(), graphics.guiHeight());
    }*/
    //?}

    /** 两个版本共用的每帧流程：遍历珍珠 → 取投掷者 → 投影 → 画名字。 */
    private static void render(Painter painter, float partialTick, int guiWidth, int guiHeight) {
        // 本帧要用的世界与本地玩家各取一次，循环里不再读 CLIENT 的字段。
        ClientLevel level = CLIENT.level;
        Player self = CLIENT.player;
        if (level == null || self == null) {
            return;
        }

        double range = Configs.PEARL_NAMETAGS_RANGE.getDoubleValue();
        float scale = (float) Configs.PEARL_NAMETAGS_SCALE.getDoubleValue();
        boolean showSelf = Configs.PEARL_NAMETAGS_SELF.getBooleanValue();

        // entitiesForRendering() 是当前维度已加载实体的实时视图，不复制，逐帧遍历即可。
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl pearl)) {
                continue;
            }

            // 本帧已被移除（命中/超时/换图）与范围外的直接跳过。
            if (pearl.isRemoved() || pearl.distanceTo(self) > range) {
                continue;
            }

            // 一次性覆盖三种不该画的情况：投掷者已离开客户端加载范围（owner 为 null）、
            // 珍珠由发射器/指令生成（没有 owner）、投掷者不是玩家。
            if (!(pearl.getOwner() instanceof Player owner)) {
                continue;
            }

            if (owner == self && !showSelf) {
                continue;
            }

            // 珍珠很矮，锚点抬到包围盒上方，名字才不会压着珍珠本体。
            Vec3 position = pearl.getPosition(partialTick);
            if (!ScreenTagRenderer.project(position.x, position.y + pearl.getBbHeight() + LABEL_OFFSET,
                    position.z, guiWidth, guiHeight)) {
                continue;
            }

            drawLabel(painter, owner.getName().getString(),
                    ScreenTagRenderer.screenX(), ScreenTagRenderer.screenY(), scale);
        }
    }

    /**
     * 只画一行名字，没有底板也没有边框——珍珠本体会被文字压住，加框反而挡住珍珠。
     * 缩放方式与玩家名牌一致：先把原点整体缩放，再按缩放后的坐标定位。
     * 文字带阴影（{@link Painter#text} 内部固定开启），所以没有背景也看得清。
     */
    private static void drawLabel(Painter painter, String name, float screenX, float screenY, float scale) {
        Font font = CLIENT.font;
        int textWidth = font.width(name);
        int lineHeight = font.lineHeight;

        painter.pushScale(scale);
        painter.text(font, name,
                Math.round(screenX / scale) - textWidth / 2,
                Math.round(screenY / scale) - lineHeight / 2,
                TEXT_COLOR);
        painter.pop();
    }

    /** 两个版本共用的每帧前置检查。 */
    private static boolean shouldRender() {
        if (!Configs.PEARL_NAMETAGS_ENABLED.getBooleanValue()
                || CLIENT.player == null
                || CLIENT.level == null
                || ScreenTagRenderer.hudHidden()) {
            return false;
        }

        // 打开界面时是否隐藏，由“仅无界面时”决定。
        return !Configs.PEARL_NAMETAGS_ONLY_VISIBLE.getBooleanValue()
                || ScreenTagRenderer.currentScreen() == null;
    }
}
