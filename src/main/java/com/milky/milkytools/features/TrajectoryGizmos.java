package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;

/**
 * 轨迹类功能（末影珍珠、箭矢）共用的 Gizmo 绘制辅助。
 * <p>
 * 两个轨迹 mixin 都注入 {@code DebugRenderer}，若各自定义同名私有方法会被 Mixin
 * 判定为方法覆写冲突（日志实测 "Method overwrite conflict ... Skipping method"，
 * 后注入的那份实现被静默丢弃），因此统一放在这里。
 */
public final class TrajectoryGizmos {
    private TrajectoryGizmos() {
    }

    /** 画一段轨迹线，长度可忽略时跳过。 */
    public static void drawSegment(Vec3 from, Vec3 to, int color) {
        if (from.distanceToSqr(to) < 1.0E-7) {
            return;
        }
        Gizmos.line(from, to, color);
    }

    /** 读取配置的轨迹颜色；Gizmo 颜色按 ARGB 解释，必须把 alpha 置为不透明，否则线条完全透明不可见。 */
    public static int colorOf() {
        Color4f color = Configs.PEARL_TRAJECTORY_COLOR.getColor();
        int rgb = (color.ri << 16) | (color.gi << 8) | color.bi;
        return 0xFF000000 | rgb;
    }
}
