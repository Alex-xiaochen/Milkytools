package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义玩家名牌（NameTags）。
 * 仿照 1.21.11 的 NameTags 模块思路：把每个玩家头顶的世界坐标投影到屏幕，
 * 然后在 2D 画面上绘制一个包含名称、延迟、血量、图腾次数与装备的自定义名牌。
 * 与参照实现相比，这里做了如下本地化适配：
 *  - 使用 Fabric 的 HudElement / GuiGraphicsExtractor 渲染 2D，而不是事件总线的 Render2DEvent；
 *  - 投影矩阵直接取自 26.x 的 CameraRenderState（projection * viewRotation），无需自行拼装；
 *  - 图腾次数由客户端收到的实体事件（eventId 35）统计，不依赖外部事件总线；
 *  - 名称颜色使用原版队伍颜色，并保留隐身/死亡的状态色。
 */
public final class NameTags {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    private static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("0.0");

    /** 图腾使用事件：原版在该事件里播放不死图腾粒子与音效（ClientPacketListener）。 */
    public static final int TOTEM_USE_EVENT_ID = 35;

    // 调色板。
    private static final int BOX_COLOR = 0xAA101015;
    private static final int BORDER_COLOR = 0x661F1F28;
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

    /** 复用的投影结果，避免每帧为每个玩家分配数组。 */
    private static final float[] PROJECTED = new float[2];

    private NameTags() {
    }

    /** 由图腾事件 mixin 调用，记录一次图腾使用。 */
    public static void onTotemUse(Player player) {
        if (!Configs.NAMETAGS_ENABLED.getBooleanValue()) {
            return;
        }
        TOTEM_POPS.merge(player.getUUID(), 1, Integer::sum);
    }

    /** Fabric HUD 回调入口。 */
    public static void onHudExtract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!Configs.NAMETAGS_ENABLED.getBooleanValue()
                || CLIENT.player == null
                || CLIENT.level == null
                || CLIENT.options.hideGui) {
            return;
        }

        // 打开界面时是否隐藏，由“仅无界面时”决定。
        if (Configs.NAMETAGS_ONLY_VISIBLE.getBooleanValue() && CLIENT.screen != null) {
            return;
        }

        if (CLIENT.level != lastLevel) {
            lastLevel = CLIENT.level;
            TOTEM_POPS.clear();
        }

        CameraRenderState camera = CLIENT.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        if (camera == null || camera.pos == null || camera.projectionMatrix == null || camera.viewRotationMatrix == null) {
            return;
        }

        // 与参照实现一致：clip = projection * viewRotation * (世界坐标 - 相机坐标)。
        Matrix4f viewProjection = new Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix);
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

            if (!project(camera, viewProjection, player, partialTick, guiWidth, guiHeight)) {
                continue;
            }

            drawTag(graphics, player, PROJECTED[0], PROJECTED[1], scaleForDistance(distance));
        }
    }

    /**
     * 把玩家头顶坐标投影到 GUI 坐标，成功时写入 {@link #PROJECTED}。
     * 屏幕外的点（NDC 越界）与相机背后的点会被丢弃。
     */
    private static boolean project(CameraRenderState camera, Matrix4f viewProjection, Player player,
                                   float partialTick, int guiWidth, int guiHeight) {
        Vec3 position = player.getPosition(partialTick);
        Vector4f clip = new Vector4f(
                (float) (position.x - camera.pos.x),
                (float) (position.y + player.getBbHeight() + 0.45 - camera.pos.y),
                (float) (position.z - camera.pos.z),
                1.0F
        );
        clip.mul(viewProjection);

        if (clip.w() <= 0.05F) {
            return false;
        }

        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        if (Math.abs(ndcX) > 1.2F || Math.abs(ndcY) > 1.2F) {
            return false;
        }

        PROJECTED[0] = (ndcX * 0.5F + 0.5F) * guiWidth;
        PROJECTED[1] = (1.0F - (ndcY * 0.5F + 0.5F)) * guiHeight;
        return true;
    }

    /** 距离越远名牌越小，并在最大/最小缩放之间做过渡。 */
    private static float scaleForDistance(double distance) {
        float max = (float) Configs.NAMETAGS_MAX_SCALE.getDoubleValue();
        float min = (float) Math.min(Configs.NAMETAGS_MIN_SCALE.getDoubleValue(), max);
        float scale = max / (1.0F + (float) Math.max(0.0, distance - 4.0) * 0.025F);
        return Math.max(min, Math.min(max, scale));
    }

    private static void drawTag(GuiGraphicsExtractor graphics, Player player, float screenX, float screenY, float scale) {
        // 先在原始 GUI 坐标里定位，再以原点为基准缩放，等价于参照实现的矩阵缩放。
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale);
        drawTagAt(graphics, player, Math.round(screenX / scale), Math.round(screenY / scale));
        graphics.pose().popMatrix();
    }

    private static void drawTagAt(GuiGraphicsExtractor graphics, Player player, int centerX, int centerY) {
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

        List<ItemStack> equipment = equipmentOf(player);
        if (!equipment.isEmpty()) {
            drawEquipment(graphics, font, equipment, centerX, top - 19);
        }

        graphics.fill(centerX - halfWidth, top, centerX + halfWidth, bottom, BOX_COLOR);
        graphics.outline(centerX - halfWidth, top, halfWidth * 2, bottom - top, BORDER_COLOR);

        drawHealthBar(graphics, player, centerX, halfWidth, bottom);

        int textX = centerX - textWidth / 2;
        int textY = centerY - lineHeight / 2;
        for (Segment segment : segments) {
            graphics.text(font, segment.text(), textX, textY, segment.color(), true);
            textX += font.width(segment.text());
        }
    }

    private static void drawHealthBar(GuiGraphicsExtractor graphics, Player player, int centerX, int halfWidth, int bottom) {
        int barTop = bottom - 2;
        int barWidth = halfWidth * 2;
        int left = centerX - halfWidth;

        float fraction = healthFraction(player);
        graphics.fill(left, barTop, centerX + halfWidth, bottom, BAR_BACKGROUND);
        graphics.fill(left, barTop, left + Math.round(barWidth * fraction), bottom, healthColor(fraction));

        float absorption = player.getAbsorptionAmount();
        if (absorption > 0.0F) {
            float maxHealth = Math.max(1.0F, player.getMaxHealth());
            int absorptionWidth = Math.min(barWidth, Math.round(barWidth * (absorption / maxHealth)));
            graphics.fill(left, barTop - 2, left + absorptionWidth, barTop, ABSORPTION_COLOR);
        }
    }

    private static void drawEquipment(GuiGraphicsExtractor graphics, Font font, List<ItemStack> equipment, int centerX, int y) {
        int x = centerX - equipment.size() * 9;
        for (ItemStack stack : equipment) {
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
            x += 18;
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
