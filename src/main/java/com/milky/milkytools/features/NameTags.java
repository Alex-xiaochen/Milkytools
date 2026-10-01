package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import com.milky.milkytools.features.ScreenTagRenderer.GuiPainter;
import com.milky.milkytools.features.ScreenTagRenderer.Painter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;*/
//?}
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.milky.milkytools.features.ScreenTagRenderer.BORDER_COLOR;
import static com.milky.milkytools.features.ScreenTagRenderer.BOX_COLOR;

/**
 * 自定义玩家名牌（NameTags）：把每个玩家头顶的世界坐标投影到屏幕，
 * 再在 2D 画面上绘制名称、延迟、血量、图腾次数与装备。
 * <p>
 * 投影（世界坐标 → GUI 像素）与 {@link Painter} 层由 {@link ScreenTagRenderer} 提供，
 * 与末影珍珠名牌共用；本类只负责玩家名牌自己的版面：名称、延迟、血量、图腾、装备。
 * <p>
 * 图腾次数由客户端收到的实体事件（eventId 35）统计，不依赖外部事件总线；
 * 名称颜色使用原版队伍颜色，并保留隐身/死亡的状态色。
 */
public final class NameTags {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("0.0");

    /** 图腾使用事件：原版在该事件里播放不死图腾粒子与音效（ClientPacketListener）。 */
    public static final int TOTEM_USE_EVENT_ID = 35;

    // 调色板。底板与边框色由 ScreenTagRenderer 提供，两个名牌功能共用。
    private static final int BAR_BACKGROUND = 0x66000000;
    private static final int ABSORPTION_COLOR = 0xFFFFD24A;
    private static final int PING_COLOR = 0xFFAAAAAA;
    private static final int POP_COLOR = 0xFFFF5555;
    private static final int INVISIBLE_COLOR = 0xFFC8C8C8;
    private static final int DEAD_COLOR = 0xFFFF7777;

    /** 本次进入世界后，每个玩家使用不死图腾的次数。 */
    private static final Map<UUID, Integer> TOTEM_POPS = new ConcurrentHashMap<>();

    /** 上一次统计所在的世界，用于换图/重连时清空图腾次数。 */
    private static Object lastLevel;

    private NameTags() {
    }

    /** 由图腾事件 mixin 调用，记录一次图腾使用。 */
    public static void onTotemUse(Player player) {
        if (!Configs.NAMETAGS_ENABLED.getBooleanValue()) {
            return;
        }
        TOTEM_POPS.merge(player.getUUID(), 1, Integer::sum);
    }

    /** 两个版本共用的每帧前置检查；返回 false 表示这一帧整块名牌都不画。 */
    private static boolean shouldRender() {
        if (!Configs.NAMETAGS_ENABLED.getBooleanValue()
                || CLIENT.player == null
                || CLIENT.level == null
                || ScreenTagRenderer.hudHidden()) {
            return false;
        }

        // 打开界面时是否隐藏，由“仅无界面时”决定。
        if (Configs.NAMETAGS_ONLY_VISIBLE.getBooleanValue() && ScreenTagRenderer.currentScreen() != null) {
            return false;
        }

        // 换图/重连后上一个世界的图腾次数不再有意义。
        if (CLIENT.level != lastLevel) {
            lastLevel = CLIENT.level;
            TOTEM_POPS.clear();
        }

        return true;
    }

    /** Fabric HUD 回调入口（26.1.2 起）。 */
    //? if >=26.1.2 {
    public static void onHudExtract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!shouldRender() || !ScreenTagRenderer.beginFrame()) {
            return;
        }

        Painter painter = new GuiPainter(graphics);
        float partialTick = delta.getGameTimeDeltaPartialTick(false);
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        double range = Configs.NAMETAGS_RANGE.getDoubleValue();

        for (AbstractClientPlayer player : CLIENT.level.players()) {
            if (player == CLIENT.player && !Configs.NAMETAGS_SELF.getBooleanValue()) {
                continue;
            }

            double distance = player.distanceTo(CLIENT.player);
            if (distance > range) {
                continue;
            }

            if (!project(player, partialTick, guiWidth, guiHeight)) {
                continue;
            }

            drawTag(painter, player, ScreenTagRenderer.screenX(), ScreenTagRenderer.screenY(),
                    scaleForDistance(distance));
        }
    }
    //?} else {
    /*public static void onHudRender(GuiGraphics graphics, DeltaTracker delta) {
        if (!shouldRender() || !ScreenTagRenderer.beginFrame()) {
            return;
        }

        Painter painter = new GuiPainter(graphics);
        float partialTick = delta.getGameTimeDeltaPartialTick(false);
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        double range = Configs.NAMETAGS_RANGE.getDoubleValue();

        for (AbstractClientPlayer player : CLIENT.level.players()) {
            if (player == CLIENT.player && !Configs.NAMETAGS_SELF.getBooleanValue()) {
                continue;
            }

            double distance = player.distanceTo(CLIENT.player);
            if (distance > range) {
                continue;
            }

            if (!project(player, partialTick, guiWidth, guiHeight)) {
                continue;
            }

            drawTag(painter, player, ScreenTagRenderer.screenX(), ScreenTagRenderer.screenY(),
                    scaleForDistance(distance));
        }
    }*/
    //?}

    /**
     * 把玩家头顶坐标投影到 GUI 坐标，成功时结果可由 {@link ScreenTagRenderer#screenX()} 读出。
     * 相机背后的点与 NDC 越界的点会被丢弃；调用前必须先 {@link ScreenTagRenderer#beginFrame()} 成功。
     */
    private static boolean project(Player player, float partialTick, int guiWidth, int guiHeight) {
        Vec3 position = player.getPosition(partialTick);
        return ScreenTagRenderer.project(position.x,
                position.y + player.getBbHeight() + 0.45, position.z, guiWidth, guiHeight);
    }

    /** 距离越远名牌越小，并在最大/最小缩放之间做过渡。 */
    private static float scaleForDistance(double distance) {
        return ScreenTagRenderer.scaleForDistance(distance,
                (float) Configs.NAMETAGS_MAX_SCALE.getDoubleValue(),
                (float) Configs.NAMETAGS_MIN_SCALE.getDoubleValue());
    }

    /** 先在原始 GUI 坐标里定位，再以原点为基准缩放，等价于参照实现的矩阵缩放。 */
    private static void drawTag(Painter painter, Player player, float screenX, float screenY, float scale) {
        painter.pushScale(scale);
        drawTagAt(painter, player, Math.round(screenX / scale), Math.round(screenY / scale));
        painter.pop();
    }

    private static void drawTagAt(Painter painter, Player player, int centerX, int centerY) {
        Font font = CLIENT.font;
        TagLayout layout = TagLayout.of(player, centerY);
        int top = layout.top();
        int bottom = layout.bottom();
        int halfWidth = layout.halfWidth();

        if (!layout.equipment().isEmpty()) {
            drawEquipment(painter, font, layout.equipment(), centerX, top - 19);
        }

        painter.fill(centerX - halfWidth, top, centerX + halfWidth, bottom, BOX_COLOR);
        painter.outline(centerX - halfWidth, top, halfWidth * 2, bottom - top, BORDER_COLOR);

        drawHealthBar(painter, player, centerX, halfWidth, bottom);

        int textX = centerX - layout.textWidth() / 2;
        int textY = centerY - layout.lineHeight() / 2;
        for (Segment segment : layout.segments()) {
            painter.text(font, segment.text(), textX, textY, segment.color());
            textX += font.width(segment.text());
        }
    }

    private static void drawHealthBar(Painter painter, Player player, int centerX, int halfWidth, int bottom) {
        int barTop = bottom - 2;
        int barWidth = halfWidth * 2;
        int left = centerX - halfWidth;

        float fraction = healthFraction(player);
        painter.fill(left, barTop, centerX + halfWidth, bottom, BAR_BACKGROUND);
        painter.fill(left, barTop, left + Math.round(barWidth * fraction), bottom, healthColor(fraction));

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            float maxHealth = Math.max(1.0F, player.getMaxHealth());
            int absorptionWidth = Math.min(barWidth, Math.round(barWidth * (absorption / maxHealth)));
            painter.fill(left, barTop - 2, left + absorptionWidth, barTop, ABSORPTION_COLOR);
        }
    }

    private static void drawEquipment(Painter painter, Font font, List<ItemStack> equipment, int centerX, int y) {
        int x = centerX - equipment.size() * 9;
        for (ItemStack stack : equipment) {
            painter.item(stack, x, y);
            painter.itemDecorations(font, stack, x, y);
            x += 18;
        }
    }

    /**
     * 名牌的版面几何。两个版本共用同一份排版计算，避免一套改了另一套没跟上。
     */
    private record TagLayout(List<Segment> segments, List<ItemStack> equipment,
                             int textWidth, int lineHeight, int halfWidth, int top, int bottom) {

        static TagLayout of(Player player, int centerY) {
            Font font = CLIENT.font;
            List<Segment> segments = buildSegments(player);

            int textWidth = 0;
            for (Segment segment : segments) {
                textWidth += font.width(segment.text());
            }

            int lineHeight = font.lineHeight;
            int halfWidth = Math.max(24, textWidth / 2 + 5);
            int halfHeight = lineHeight / 2 + 4;
            int top = centerY - halfHeight;
            int bottom = centerY + halfHeight + 2;
            return new TagLayout(segments, equipmentOf(player), textWidth, lineHeight, halfWidth, top, bottom);
        }
    }

    private static List<Segment> buildSegments(Player player) {
        List<Segment> segments = new ArrayList<>();
        segments.add(new Segment(player.getName().getString(), nameColor(player)));

        if (Configs.NAMETAGS_PING.getBooleanValue()) {
            int ping = pingOf(player);
            segments.add(new Segment(ping < 0 ? " --" : " " + ping + "ms", PING_COLOR));
        }

        if (Configs.NAMETAGS_HEALTH.getBooleanValue()) {
            float health = player.getHealth() + player.getAbsorptionAmount();
            segments.add(new Segment(" " + HEALTH_FORMAT.format(health), healthColor(healthFraction(player))));
        }

        if (Configs.NAMETAGS_POPS.getBooleanValue()) {
            int pops = TOTEM_POPS.getOrDefault(player.getUUID(), 0);
            if (pops > 0) {
                segments.add(new Segment(" -" + pops, POP_COLOR));
            }
        }

        return segments;
    }

    private static List<ItemStack> equipmentOf(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        if (Configs.NAMETAGS_HANDS.getBooleanValue()) {
            stacks.add(player.getOffhandItem());
        }
        if (Configs.NAMETAGS_ARMOR.getBooleanValue()) {
            stacks.add(player.getItemBySlot(EquipmentSlot.HEAD));
            stacks.add(player.getItemBySlot(EquipmentSlot.CHEST));
            stacks.add(player.getItemBySlot(EquipmentSlot.LEGS));
            stacks.add(player.getItemBySlot(EquipmentSlot.FEET));
        }
        if (Configs.NAMETAGS_HANDS.getBooleanValue()) {
            stacks.add(player.getMainHandItem());
        }
        stacks.removeIf(ItemStack::isEmpty);
        return stacks;
    }

    private static int pingOf(Player player) {
        if (CLIENT.getConnection() == null) {
            return -1;
        }
        PlayerInfo info = CLIENT.getConnection().getPlayerInfo(player.getUUID());
        return info == null ? -1 : info.getLatency();
    }

    private static int nameColor(Player player) {
        if (player.isInvisible()) {
            return INVISIBLE_COLOR;
        }
        if (!player.isAlive()) {
            return DEAD_COLOR;
        }
        // 原版队伍颜色；无队伍时 getTeamColor 返回白色。
        return 0xFF000000 | (player.getTeamColor() & 0xFFFFFF);
    }

    private static float healthFraction(Player player) {
        float maxHealth = Math.max(1.0F, player.getMaxHealth());
        return Math.max(0.0F, Math.min(1.0F, player.getHealth() / maxHealth));
    }

    /** 低血量偏红、高血量偏绿。 */
    private static int healthColor(float fraction) {
        int red = (int) ((1.0F - fraction) * 255.0F);
        int green = (int) (fraction * 255.0F);
        return 0xFF000000 | (red << 16) | (green << 8);
    }

    private record Segment(String text, int color) {
    }
}
