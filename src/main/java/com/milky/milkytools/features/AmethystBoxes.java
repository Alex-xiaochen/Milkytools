package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 紫水晶母岩描框（AmethystBoxes）。
 * 开启后扫描附近已加载区块的方块，为紫水晶母岩（Blocks.BUDDING_AMETHYST）绘制方框。
 * 母岩被晶芽与方解石包裹、无法直接看到，因此本功能主要用于配合透视定位紫水晶洞。
 * 通过区块分段的调色板 {@code maybeHas} 先做粗筛，只有可能含母岩的分段才逐方块检查。
 * 扫描结果按客户端 tick 缓存，渲染帧只负责把缓存的方框交给 Gizmo 系统绘制。
 */
public final class AmethystBoxes {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    /** 当前需要描框的母岩位置。每 tick 重建后整体替换，避免渲染时读到半成品集合。 */
    private static volatile List<BlockPos> targets = Collections.emptyList();

    private AmethystBoxes() {
    }

    /** 由客户端 tick 处理器调用，重建目标列表。 */
    public static void onClientTick() {
        if (!Configs.AMETHYST_BOXES_ENABLED.getBooleanValue()) {
            targets = Collections.emptyList();
            return;
        }

        ClientLevel level = CLIENT.level;
        if (level == null || CLIENT.player == null) {
            targets = Collections.emptyList();
            return;
        }

        double range = Configs.AMETHYST_BOXES_RANGE.getDoubleValue();
        double rangeSq = range * range;
        BlockPos center = CLIENT.player.blockPosition();
        int chunkRadius = ((int) Math.ceil(range) >> 4) + 1;
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        List<BlockPos> found = new ArrayList<>();

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                scanChunk(chunk, center, range, rangeSq, found);
            }
        }

        targets = found;
    }

    /** 扫描单个区块内范围内的所有分段，收集母岩位置。 */
    private static void scanChunk(LevelChunk chunk, BlockPos center, double range, double rangeSq, List<BlockPos> found) {
        LevelChunkSection[] sections = chunk.getSections();
        int originX = chunk.getPos().getMinBlockX();
        int originZ = chunk.getPos().getMinBlockZ();
        int minY = center.getY() - (int) Math.ceil(range);
        int maxY = center.getY() + (int) Math.ceil(range);

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }

            int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
            int baseY = sectionY << 4;
            if (baseY + 15 < minY || baseY > maxY) {
                continue;
            }

            if (!section.maybeHas(AmethystBoxes::isBuddingAmethyst)) {
                continue;
            }

            for (int y = 0; y < 16; y++) {
                int worldY = baseY + y;
                if (worldY < minY || worldY > maxY) {
                    continue;
                }

                for (int x = 0; x < 16; x++) {
                    int worldX = originX + x;
                    int dx = worldX - center.getX();
                    if ((double) dx * dx > rangeSq) {
                        continue;
                    }

                    for (int z = 0; z < 16; z++) {
                        if (!isBuddingAmethyst(section.getBlockState(x, y, z))) {
                            continue;
                        }

                        int worldZ = originZ + z;
                        int dz = worldZ - center.getZ();
                        int dy = worldY - center.getY();
                        if ((double) dx * dx + (double) dy * dy + (double) dz * dz > rangeSq) {
                            continue;
                        }

                        found.add(new BlockPos(worldX, worldY, worldZ));
                    }
                }
            }
        }
    }

    private static boolean isBuddingAmethyst(BlockState state) {
        return state.is(Blocks.BUDDING_AMETHYST);
    }

    /** 由 DebugRenderer 的 Gizmo 渲染钩子调用。 */
    public static void onRenderGizmos() {
        if (!Configs.AMETHYST_BOXES_ENABLED.getBooleanValue()) {
            return;
        }

        List<BlockPos> current = targets;
        if (current.isEmpty()) {
            return;
        }

        int color = color(Configs.AMETHYST_BOXES_COLOR.getColor());
        boolean fill = Configs.AMETHYST_BOXES_FILL.getBooleanValue();
        float lineWidth = (float) Configs.AMETHYST_BOXES_LINE_WIDTH.getDoubleValue();
        int fillAlpha = (int) Math.round(Configs.AMETHYST_BOXES_FILL_ALPHA.getDoubleValue()) & 0xFF;
        boolean xray = Configs.AMETHYST_BOXES_XRAY.getBooleanValue();

        int rgb = color & 0xFFFFFF;
        int stroke = 0xFF000000 | rgb;
        GizmoStyle style = fill
                ? GizmoStyle.strokeAndFill(stroke, lineWidth, (fillAlpha << 24) | rgb)
                : GizmoStyle.stroke(stroke, lineWidth);

        for (BlockPos pos : current) {
            GizmoProperties properties = Gizmos.cuboid(pos, style);
            if (xray) {
                properties.setAlwaysOnTop();
            }
        }
    }

    private static int color(Color4f color) {
        return (color.ri << 16) | (color.gi << 8) | color.bi;
    }
}
