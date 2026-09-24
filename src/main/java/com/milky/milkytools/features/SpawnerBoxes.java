package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 试炼描框（SpawnerBoxes）。
 * 开启后扫描附近已加载区块的方块实体，为以下目标绘制方框：
 *  - 试炼刷怪笼（TrialSpawnerBlockEntity，含不祥变体）；
 *  - 宝库 / 不祥宝库（VaultBlockEntity，按 OMINOUS 状态区分颜色）。
 * 普通刷怪笼（SpawnerBlockEntity）不在描框范围内。
 * 方框可通过配置决定是否填充内部颜色，并可开启透视（始终绘制在最上层）以穿透方块查看。
 * 扫描结果按客户端 tick 缓存，渲染帧只负责把缓存的方框交给 Gizmo 系统绘制。
 */
public final class SpawnerBoxes {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    /** 当前需要描框的目标。每 tick 重建后整体替换，避免渲染时读到半成品集合。 */
    private static volatile List<Target> targets = Collections.emptyList();

    private SpawnerBoxes() {
    }

    /** 由客户端 tick 处理器调用，重建目标列表。 */
    public static void onClientTick() {
        if (!Configs.SPAWNER_BOXES_ENABLED.getBooleanValue()) {
            targets = Collections.emptyList();
            return;
        }

        ClientLevel level = CLIENT.level;
        if (level == null || CLIENT.player == null) {
            targets = Collections.emptyList();
            return;
        }

        double range = Configs.SPAWNER_BOXES_RANGE.getDoubleValue();
        double rangeSq = range * range;
        BlockPos center = CLIENT.player.blockPosition();
        int chunkRadius = ((int) Math.ceil(range) >> 4) + 1;
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;

        List<Target> found = new ArrayList<>();

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    Integer color = colorOf(blockEntity);
                    if (color == null) {
                        continue;
                    }

                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.distSqr(center) > rangeSq) {
                        continue;
                    }

                    found.add(new Target(pos.immutable(), color));
                }
            }
        }

        targets = found;
    }

    /** 由 DebugRenderer 的 Gizmo 渲染钩子调用。 */
    public static void onRenderGizmos() {
        if (!Configs.SPAWNER_BOXES_ENABLED.getBooleanValue()) {
            return;
        }

        List<Target> current = targets;
        if (current.isEmpty()) {
            return;
        }

        boolean fill = Configs.SPAWNER_BOXES_FILL.getBooleanValue();
        float lineWidth = (float) Configs.SPAWNER_BOXES_LINE_WIDTH.getDoubleValue();
        int fillAlpha = (int) Math.round(Configs.SPAWNER_BOXES_FILL_ALPHA.getDoubleValue()) & 0xFF;
        boolean xray = Configs.SPAWNER_BOXES_XRAY.getBooleanValue();

        for (Target target : current) {
            int rgb = target.color() & 0xFFFFFF;
            int stroke = 0xFF000000 | rgb;
            GizmoStyle style = fill
                    ? GizmoStyle.strokeAndFill(stroke, lineWidth, (fillAlpha << 24) | rgb)
                    : GizmoStyle.stroke(stroke, lineWidth);

            GizmoProperties properties = Gizmos.cuboid(target.pos(), style);
            if (xray) {
                properties.setAlwaysOnTop();
            }
        }
    }

    /** 目标方块实体对应的描框颜色；不是目标时返回 null。 */
    private static Integer colorOf(BlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        boolean ominous = ominous(state);

        if (blockEntity instanceof VaultBlockEntity) {
            return ominous ? color(Configs.SPAWNER_BOXES_OMINOUS_COLOR.getColor()) : color(Configs.SPAWNER_BOXES_VAULT_COLOR.getColor());
        }
        if (blockEntity instanceof TrialSpawnerBlockEntity) {
            return ominous ? color(Configs.SPAWNER_BOXES_OMINOUS_COLOR.getColor()) : color(Configs.SPAWNER_BOXES_TRIAL_SPAWNER_COLOR.getColor());
        }
        return null;
    }

    /** 宝库与试炼刷怪笼都有 OMINOUS 状态，用于区分不祥变体。 */
    private static boolean ominous(BlockState state) {
        if (state.hasProperty(VaultBlock.OMINOUS)) {
            return state.getValue(VaultBlock.OMINOUS);
        }
        if (state.hasProperty(TrialSpawnerBlock.OMINOUS)) {
            return state.getValue(TrialSpawnerBlock.OMINOUS);
        }
        return false;
    }

    private static int color(Color4f color) {
        return (color.ri << 16) | (color.gi << 8) | color.bi;
    }

    private record Target(BlockPos pos, int color) {
    }
}
