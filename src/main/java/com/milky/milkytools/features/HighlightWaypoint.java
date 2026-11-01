package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import fi.dy.masa.malilib.util.data.Color4f;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
//?} else {
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;*/
//?}
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 高亮坐标点（移植自 oh-my-minecraft-client 的 HighlightWaypoint）。
 * <p>
 * 与坐标光柱（{@link CoordinateBeacon}）是两套独立功能：
 * <ul>
 *   <li>坐标光柱识别的是「100 64 200」这类裸空格坐标；本功能识别
 *       「[x:100, y:64, z:200]」「(100, 64, 200)」这类带括号/键名的坐标。</li>
 *   <li>本功能通过客户端命令 {@code /highlightWaypoint x y z} 或点击聊天坐标设置航点；
 *       对同一坐标再执行一次会额外显示 10 秒信标光柱。</li>
 *   <li>航点始终显示坐标与距离标签，并随视角方向透视显示；
 *       进出下界时坐标自动按 1/8、8 倍换算。</li>
 * </ul>
 * 标签使用 Gizmo 的 billboard 文字绘制：原实现的贴图图标依赖世界空间贴图渲染，
 * 现版本没有对应的 Gizmo 接口，因此只保留文字标签与光柱。
 */
public final class HighlightWaypoint {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    private static final String COMMAND_NAME = "highlightWaypoint";
    private static final String CLICK_TOOLTIP = "点击以高亮坐标点";
    private static final long BEAM_MILLIS = 10_000L;

    /** 光柱拉近后使用的相机远裁剪面比例，避免几何体被裁掉。 */
    private static final double FAR_PLANE_USAGE = 0.6;

    // 与原实现相同的四种坐标写法：[key:value...] / (key:value...) / [x, y, z] / (x, y, z)
    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("\\[(\\w+\\s*:\\s*[^\\[\\]]+)(,\\s*\\w+\\s*:\\s*[^\\[\\]]+)+]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\((\\w+\\s*:\\s*[^()]+)(,\\s*\\w+\\s*:\\s*[^()]+)+\\)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[(-?\\d+)(,\\s*-?\\d+)(,\\s*-?\\d+)]"),
            Pattern.compile("\\((-?\\d+)(,\\s*-?\\d+)(,\\s*-?\\d+)\\)")
    );

    private static BlockPos highlightPos;
    private static long lastBeamTime;
    private static ResourceKey<Level> currentWorld;

    /** 上一次 tick 所在的世界，用于检测维度切换。 */
    private static ClientLevel lastLevel;

    private HighlightWaypoint() {
    }

    public static boolean isEnabled() {
        return Configs.HIGHLIGHT_WAYPOINT_ENABLED.getBooleanValue();
    }

    // 26.x 的 Fabric API 把 ClientCommandManager 改名成了 ClientCommands。
    private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        //? if >=26.1 {
        return ClientCommands.literal(name);
        //?} else {
        /*return ClientCommandManager.literal(name);*/
        //?}
    }

    private static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        //? if >=26.1 {
        return ClientCommands.argument(name, type);
        //?} else {
        /*return ClientCommandManager.argument(name, type);*/
        //?}
    }

    /** 由客户端命令注册回调调用。 */
    public static void registerCommand(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal(COMMAND_NAME)
                .then(argument("x", IntegerArgumentType.integer())
                        .then(argument("y", IntegerArgumentType.integer())
                                .then(argument("z", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            if (!isEnabled()) {
                                                return 0;
                                            }
                                            setWaypoint(
                                                    IntegerArgumentType.getInteger(context, "x"),
                                                    IntegerArgumentType.getInteger(context, "y"),
                                                    IntegerArgumentType.getInteger(context, "z")
                                            );
                                            return 1;
                                        })))));
    }

    /** 设置航点；对同一坐标重复设置时点亮 10 秒光柱（与原实现一致）。 */
    private static void setWaypoint(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (pos.equals(highlightPos)) {
            lastBeamTime = System.currentTimeMillis() + BEAM_MILLIS;
        } else {
            highlightPos = pos;
            lastBeamTime = 0L;
        }
    }

    /** 由客户端 tick 处理器调用：检测维度切换并换算坐标。 */
    public static void onClientTick() {
        ClientLevel level = CLIENT.level;

        if (level == null) {
            lastLevel = null;
            currentWorld = null;
            highlightPos = null;
            lastBeamTime = 0L;
            return;
        }

        if (level != lastLevel) {
            ResourceKey<Level> newWorld = level.dimension();

            if (highlightPos != null && currentWorld != null && !currentWorld.equals(newWorld)) {
                if (currentWorld.equals(Level.OVERWORLD) && newWorld.equals(Level.NETHER)) {
                    highlightPos = new BlockPos(highlightPos.getX() / 8, highlightPos.getY(), highlightPos.getZ() / 8);
                } else if (currentWorld.equals(Level.NETHER) && newWorld.equals(Level.OVERWORLD)) {
                    highlightPos = new BlockPos(highlightPos.getX() * 8, highlightPos.getY(), highlightPos.getZ() * 8);
                } else {
                    highlightPos = null;
                    lastBeamTime = 0L;
                }
            }

            lastLevel = level;
            currentWorld = newWorld;
        }
    }

    /** 由聊天消息 mixin 调用：把坐标片段替换为可点击的命令文本。 */
    public static Component makeClickable(Component component) {
        if (component == null || !isEnabled()) {
            return component;
        }

        List<Component> segments = component.toFlatList();
        boolean found = false;
        for (Component segment : segments) {
            if (!findWaypoints(segment.getString()).isEmpty()) {
                found = true;
                break;
            }
        }
        if (!found) {
            return component;
        }

        MutableComponent rebuilt = Component.empty();
        for (Component segment : segments) {
            appendWithWaypoints(rebuilt, segment.getString(), segment.getStyle());
        }
        return rebuilt;
    }

    private static void appendWithWaypoints(MutableComponent root, String text, Style style) {
        List<WaypointRun> runs = findWaypoints(text);
        if (runs.isEmpty()) {
            root.append(Component.literal(text).withStyle(style));
            return;
        }

        int cursor = 0;
        for (WaypointRun run : runs) {
            if (run.start() < cursor) {
                continue;
            }
            if (run.start() > cursor) {
                root.append(Component.literal(text.substring(cursor, run.start())).withStyle(style));
            }

            Style clickable = style;
            if (clickable.getClickEvent() == null) {
                BlockPos pos = run.pos();
                clickable = clickable.withClickEvent(new ClickEvent.RunCommand(
                        String.format("/%s %d %d %d", COMMAND_NAME, pos.getX(), pos.getY(), pos.getZ())));
            }
            if (clickable.getColor() == null) {
                clickable = clickable.withColor(ChatFormatting.GREEN);
            }
            clickable = clickable.withHoverEvent(new HoverEvent.ShowText(Component.literal(CLICK_TOOLTIP)));

            root.append(Component.literal(text.substring(run.start(), run.end())).withStyle(clickable));
            cursor = run.end();
        }
        if (cursor < text.length()) {
            root.append(Component.literal(text.substring(cursor)).withStyle(style));
        }
    }

    private static List<WaypointRun> findWaypoints(String text) {
        List<WaypointRun> runs = new ArrayList<>();
        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String match = matcher.group();
                BlockPos pos = parseWaypoint(match.substring(1, match.length() - 1));
                if (pos != null) {
                    runs.add(new WaypointRun(matcher.start(), matcher.end(), pos));
                }
            }
        }
        runs.sort(Comparator.comparingInt(WaypointRun::start));
        return runs;
    }

    /** 解析 x/z（必需）与 y（缺省 64）；支持 key:value 与纯数字两种写法。 */
    private static BlockPos parseWaypoint(String details) {
        String[] pairs = details.split(",");
        Integer x = null;
        Integer z = null;
        int y = 64;

        try {
            for (int i = 0; i < pairs.length; i++) {
                int splitIndex = pairs[i].indexOf(':');
                String key;
                String value;

                if (splitIndex == -1 && pairs.length == 3) {
                    key = i == 0 ? "x" : i == 1 ? "y" : "z";
                    value = pairs[i];
                } else if (splitIndex != -1) {
                    key = pairs[i].substring(0, splitIndex).toLowerCase().trim();
                    value = pairs[i].substring(splitIndex + 1).trim();
                } else {
                    continue;
                }

                switch (key) {
                    case "x" -> x = Integer.parseInt(value.replace(" ", ""));
                    case "y" -> y = Integer.parseInt(value.replace(" ", ""));
                    case "z" -> z = Integer.parseInt(value.replace(" ", ""));
                    default -> {
                    }
                }
            }
        } catch (NumberFormatException ignored) {
        }

        return x == null || z == null ? null : new BlockPos(x, y, z);
    }

    /** 由世界渲染阶段的 Gizmo 钩子调用：绘制标签，光柱激活时额外绘制光柱。 */
    public static void onRenderGizmos() {
        if (!isEnabled()) {
            return;
        }

        BlockPos pos = highlightPos;
        if (pos == null) {
            return;
        }

        var camera = CameraAccess.camera();
        if (camera == null || camera.pos == null) {
            return;
        }

        double camX = camera.pos.x;
        double camY = camera.pos.y;
        double camZ = camera.pos.z;

        // 远裁剪面随渲染距离变化，取一部分作为“拉近”距离，保证几何体一定在裁剪面内。
        //? if >=26.1.2 {
        double depthFar = camera.depthFar;
        //?} else {
        /*double depthFar = CLIENT.gameRenderer.getDepthFar();*/
        //?}
        double maxDistance = clamp(depthFar * FAR_PLANE_USAGE, 48.0, 2000.0);

        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double dx = centerX - camX;
        double dy = centerY - camY;
        double dz = centerZ - camZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 超过可视距离时，把相对偏移缩放到 maxDistance，保留方向但落到远裁剪面内。
        double renderX = centerX;
        double renderY = centerY;
        double renderZ = centerZ;
        if (distance > maxDistance && distance > 1.0E-4) {
            double scale = maxDistance / distance;
            renderX = camX + dx * scale;
            renderY = camY + dy * scale;
            renderZ = camZ + dz * scale;
        }

        int color = argb(Configs.HIGHLIGHT_WAYPOINT_COLOR.getColor());
        String label = String.format("x:%d, y:%d, z:%d (%dm)",
                pos.getX(), pos.getY(), pos.getZ(), (int) distance);

        // 标签抬到方块上方，与 billboardTextOverBlock 的 1.3 格偏移一致。
        Gizmos.billboardText(label, new Vec3(renderX, renderY + 0.8, renderZ),
                        TextGizmo.Style.forColorAndCentered(color).withScale(0.32F))
                .setAlwaysOnTop();

        if (System.currentTimeMillis() < lastBeamTime) {
            double halfWidth = 0.25;
            double halfHeight = 128.0;
            AABB beam = new AABB(
                    renderX - halfWidth, renderY - halfHeight, renderZ - halfWidth,
                    renderX + halfWidth, renderY + halfHeight, renderZ + halfWidth
            );
            Gizmos.cuboid(beam, GizmoStyle.fill((0xCC << 24) | (color & 0xFFFFFF)))
                    .setAlwaysOnTop();
        }
    }

    private static int argb(Color4f color) {
        return 0xFF000000 | ((color.ri & 0xFF) << 16) | ((color.gi & 0xFF) << 8) | (color.bi & 0xFF);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record WaypointRun(int start, int end, BlockPos pos) {
    }
}
