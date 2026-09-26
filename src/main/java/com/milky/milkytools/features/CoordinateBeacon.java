package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 坐标光柱（CoordinateBeacon）。
 * 聊天栏里的坐标（两组的 x z，或三组的 x y z，数字之间用单个空格分隔）会被标记为可点击：
 *  - 三组数字会丢弃中间的 y，仅保留 x、z；
 *  - x、z 必须落在 -30000000~30000000，三组形式的 y 必须落在 -64~320；
 *  - 点击聊天栏里的坐标后，才会在 x、z 确定的方块上渲染一根类似信标光柱的高亮柱体。
 * 柱体始终绘制在最上层（透视），并且会被“拉近”到相机远裁剪面之内绘制，
 * 因此即使目标在数千万格外、所在区块未加载，也能在正确方向上被看到。
 * 同一时间只保留一根光柱，点击新坐标会替换旧光柱；切换世界/维度时清空。
 */
public final class CoordinateBeacon {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    /** 点击坐标时使用的自定义点击事件 ID。 */
    private static final Identifier CLICK_ID = Identifier.fromNamespaceAndPath("milkytools", "coordinate_beacon");

    private static final int MIN_COORD = -30000000;
    private static final int MAX_COORD = 30000000;
    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    /** 光柱基础宽度（格）。 */
    private static final double BEAM_WIDTH = 1.0;
    /** 光柱最大高度（格）。 */
    private static final double MAX_BEAM_HEIGHT = 256.0;
    /** 拉近后光柱的最小角宽度换算系数：宽度 = 距离 * 该系数，保证远距离仍可见。 */
    private static final double MIN_ANGULAR_WIDTH = 0.006;
    /** 相机远裁剪面使用比例，避免几何体被远裁剪面裁掉。 */
    private static final double FAR_PLANE_USAGE = 0.6;

    /**
     * 信标光柱分层：{宽度倍率, 填充透明度}，由外到内绘制。
     * 只保留最内侧的两层：外层宽而淡、内层窄而亮，形成中间亮、边缘淡的发光柱体。
     */
    private static final double[][] BEAM_LAYERS = {
            {1.0, 92.0},
            {0.5, 168.0},
    };

    /** 内层（核心）向白色提亮的比例，模拟信标光柱的高亮核心。 */
    private static final double CORE_WHITEN = 0.55;

    /** key 为打包后的 x/z，value 为标记；同一时间最多一根。 */
    private static final Map<Long, Marker> MARKERS = new LinkedHashMap<>();

    /** 上一次统计所在的世界，用于换图/切换维度时清空标记。 */
    private static Object lastLevel;

    private CoordinateBeacon() {
    }

    /**
     * 由聊天消息 mixin 调用：把消息里合法的坐标片段替换为带自定义点击事件的文本。
     * 消息里没有坐标时原样返回。
     */
    public static Component makeClickable(Component component) {
        if (component == null || !Configs.COORDINATE_BEACON_ENABLED.getBooleanValue()) {
            return component;
        }

        List<Component> segments = component.toFlatList();
        boolean hasCoordinate = false;
        for (Component segment : segments) {
            if (findCoordinates(segment.getString()).length > 0) {
                hasCoordinate = true;
                break;
            }
        }
        if (!hasCoordinate) {
            return component;
        }

        MutableComponent rebuilt = Component.empty();
        for (Component segment : segments) {
            appendWithCoordinates(rebuilt, segment.getString(), segment.getStyle());
        }
        return rebuilt;
    }

    /** 由 ChatScreen 点击 mixin 调用：命中的是我们添加的坐标点击事件时记录标记并返回 true。 */
    public static boolean handleClick(Style style) {
        if (style == null || !Configs.COORDINATE_BEACON_ENABLED.getBooleanValue()) {
            return false;
        }
        ClickEvent event = style.getClickEvent();
        if (!(event instanceof ClickEvent.Custom custom) || !CLICK_ID.equals(custom.id())) {
            return false;
        }

        custom.payload().flatMap(Tag::asString).ifPresent(payload -> {
            String[] parts = payload.split(" ");
            if (parts.length == 2) {
                Integer x = parse(parts[0]);
                Integer z = parse(parts[1]);
                if (validXZ(x, z)) {
                    // 每次点击新坐标都替换掉上一根光柱。
                    MARKERS.clear();
                    addIfValid(x, z);
                    playClickSound();
                }
            }
        });
        return true;
    }

    /** 由客户端 tick 处理器调用，切换世界/维度时清空标记。 */
    public static void onClientTick() {
        ClientLevel level = CLIENT.level;
        if (level != lastLevel) {
            lastLevel = level;
            MARKERS.clear();
        }
    }

    /** 清除所有坐标光柱标记。 */
    public static void clear() {
        MARKERS.clear();
    }

    /** 点击坐标登记成功后的音效反馈。 */
    private static void playClickSound() {
        if (CLIENT.getSoundManager() != null) {
            CLIENT.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.2F));
        }
    }

    /** 由 DebugRenderer 的 Gizmo 渲染钩子调用。 */
    public static void onRenderGizmos() {
        if (!Configs.COORDINATE_BEACON_ENABLED.getBooleanValue() || MARKERS.isEmpty()) {
            return;
        }

        CameraRenderState camera = CLIENT.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState;
        if (camera == null || camera.pos == null) {
            return;
        }

        double camX = camera.pos.x;
        double camY = camera.pos.y;
        double camZ = camera.pos.z;
        // 远裁剪面随渲染距离变化，取一部分作为“拉近”距离，保证几何体一定在裁剪面内。
        double maxDistance = clamp(camera.depthFar * FAR_PLANE_USAGE, 48.0, 2000.0);
        double beamHeight = Math.min(MAX_BEAM_HEIGHT, maxDistance * 1.2);

        int rgb = color(Configs.COORDINATE_BEACON_COLOR.getColor()) & 0xFFFFFF;
        int coreRgb = brighten(rgb, CORE_WHITEN);

        for (Marker marker : MARKERS.values().toArray(new Marker[0])) {
            double centerX = marker.x() + 0.5;
            double centerZ = marker.z() + 0.5;
            double dx = centerX - camX;
            double dz = centerZ - camZ;
            double distance = Math.sqrt(dx * dx + dz * dz);

            // 超过可视距离时，把水平偏移缩放到 maxDistance，保留方向但落到远裁剪面内。
            double renderX;
            double renderZ;
            if (distance > maxDistance && distance > 1.0E-4) {
                double scale = maxDistance / distance;
                renderX = camX + dx * scale;
                renderZ = camZ + dz * scale;
            } else {
                renderX = centerX;
                renderZ = centerZ;
            }

            // 越远光柱越粗，保证在极远距离下仍有至少约 0.34° 的角宽度。
            double effectiveDistance = Math.min(distance, maxDistance);
            double baseWidth = Math.max(BEAM_WIDTH, effectiveDistance * MIN_ANGULAR_WIDTH);

            // 以相机高度为中心向上下延伸，保证摄像机下方也能看到光柱。
            double baseY = camY - beamHeight * 0.5;
            double topY = camY + beamHeight * 0.5;

            // 由外到内叠加多层，形成中间亮、边缘淡的发光光柱。
            for (int layer = 0; layer < BEAM_LAYERS.length; layer++) {
                double layerWidth = baseWidth * BEAM_LAYERS[layer][0];
                int alpha = (int) Math.round(BEAM_LAYERS[layer][1]) & 0xFF;
                int layerRgb = layer >= BEAM_LAYERS.length - 2 ? coreRgb : rgb;
                double radius = layerWidth * 0.5;

                AABB box = new AABB(
                        renderX - radius, baseY, renderZ - radius,
                        renderX + radius, topY, renderZ + radius
                );

                GizmoProperties properties = Gizmos.cuboid(box, GizmoStyle.fill((alpha << 24) | layerRgb));
                properties.setAlwaysOnTop();
            }
        }
    }

    /** 把一段文本按坐标片段切分，坐标片段附加自定义点击事件后追加到 root。 */
    private static void appendWithCoordinates(MutableComponent root, String text, Style style) {
        CoordinateRun[] runs = findCoordinates(text);
        if (runs.length == 0) {
            root.append(Component.literal(text).withStyle(style));
            return;
        }

        int cursor = 0;
        for (CoordinateRun run : runs) {
            if (run.start() > cursor) {
                root.append(Component.literal(text.substring(cursor, run.start())).withStyle(style));
            }
            Style clickable = style.withClickEvent(new ClickEvent.Custom(
                    CLICK_ID,
                    Optional.of(StringTag.valueOf(run.x() + " " + run.z()))
            ));
            root.append(Component.literal(text.substring(run.start(), run.end())).withStyle(clickable));
            cursor = run.end();
        }
        if (cursor < text.length()) {
            root.append(Component.literal(text.substring(cursor)).withStyle(style));
        }
    }

    /** 在文本里找出所有合法的坐标片段（2 或 3 个以单个空格分隔的整数）。 */
    private static CoordinateRun[] findCoordinates(String text) {
        List<CoordinateRun> runs = new ArrayList<>();
        int length = text.length();
        int index = 0;

        while (index < length) {
            while (index < length && text.charAt(index) == ' ') {
                index++;
            }
            if (index >= length) {
                break;
            }

            int tokenStart = index;
            while (index < length && text.charAt(index) != ' ') {
                index++;
            }
            int tokenEnd = index;
            if (!isInteger(text.substring(tokenStart, tokenEnd))) {
                continue;
            }

            // 收集连续的整数字标记，要求它们之间恰好只有一个空格。
            List<int[]> tokens = new ArrayList<>();
            tokens.add(new int[]{tokenStart, tokenEnd});
            int scan = tokenEnd;
            while (scan < length && text.charAt(scan) == ' ') {
                int nextStart = scan + 1;
                if (nextStart >= length || text.charAt(nextStart) == ' ') {
                    break;
                }
                int nextEnd = nextStart;
                while (nextEnd < length && text.charAt(nextEnd) != ' ') {
                    nextEnd++;
                }
                if (!isInteger(text.substring(nextStart, nextEnd))) {
                    break;
                }
                tokens.add(new int[]{nextStart, nextEnd});
                scan = nextEnd;
            }
            index = scan;

            int count = tokens.size();
            if (count == 2) {
                Integer x = parse(text.substring(tokens.get(0)[0], tokens.get(0)[1]));
                Integer z = parse(text.substring(tokens.get(1)[0], tokens.get(1)[1]));
                if (validXZ(x, z)) {
                    runs.add(new CoordinateRun(tokens.get(0)[0], tokens.get(1)[1], x, z));
                }
            } else if (count == 3) {
                Integer x = parse(text.substring(tokens.get(0)[0], tokens.get(0)[1]));
                Integer y = parse(text.substring(tokens.get(1)[0], tokens.get(1)[1]));
                Integer z = parse(text.substring(tokens.get(2)[0], tokens.get(2)[1]));
                if (validY(y) && validXZ(x, z)) {
                    runs.add(new CoordinateRun(tokens.get(0)[0], tokens.get(2)[1], x, z));
                }
            }
        }

        return runs.toArray(new CoordinateRun[0]);
    }

    private static boolean validXZ(Integer x, Integer z) {
        return x != null && z != null
                && x >= MIN_COORD && x <= MAX_COORD
                && z >= MIN_COORD && z <= MAX_COORD;
    }

    private static boolean validY(Integer y) {
        return y != null && y >= MIN_Y && y <= MAX_Y;
    }

    /** 校验 x、z 范围后记录标记；同一 x/z 覆盖旧标记。 */
    private static void addIfValid(int x, int z) {
        if (x < MIN_COORD || x > MAX_COORD || z < MIN_COORD || z > MAX_COORD) {
            return;
        }
        MARKERS.put(key(x, z), new Marker(x, z));
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static boolean isInteger(String token) {
        return parse(token) != null;
    }

    private static Integer parse(String token) {
        if (token.isEmpty() || token.length() > 11) {
            return null;
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int color(Color4f color) {
        return (color.ri << 16) | (color.gi << 8) | color.bi;
    }

    /** 将颜色向白色提亮，用于光柱的高亮核心。 */
    private static int brighten(int rgb, double amount) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        red = (int) Math.round(red + (255 - red) * amount);
        green = (int) Math.round(green + (255 - green) * amount);
        blue = (int) Math.round(blue + (255 - blue) * amount);
        return (red << 16) | (green << 8) | blue;
    }

    private record Marker(int x, int z) {
    }

    private record CoordinateRun(int start, int end, int x, int z) {
    }
}
