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

    /** 画一段轨迹线，长度可忽略时跳过。两条包络线几乎重合，跳过零长度段能省下不少图元。 */
    public static void drawSegment(Vec3 from, Vec3 to, int color, float width) {
        if (from.distanceToSqr(to) < 1.0E-7) {
            return;
        }
        Gizmos.line(from, to, color, width);
    }

    /**
     * 按比例压暗颜色，用来区分散布包络与中心轨迹。
     * <p>
     * 这里压暗的是 RGB 而不是 alpha：Gizmo 的线条在 alpha 不足时会整段看不见，
     * 只有保持 alpha 不透明才稳定可见。
     */
    public static int dim(int argb, float factor) {
        int r = (int) (((argb >> 16) & 0xFF) * factor);
        int g = (int) (((argb >> 8) & 0xFF) * factor);
        int b = (int) ((argb & 0xFF) * factor);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** 读取配置的轨迹颜色；Gizmo 颜色按 ARGB 解释，必须把 alpha 置为不透明，否则线条完全透明不可见。 */
    public static int colorOf() {
        Color4f color = Configs.PEARL_TRAJECTORY_COLOR.getColor();
        int rgb = (color.ri << 16) | (color.gi << 8) | color.bi;
        return 0xFF000000 | rgb;
    }
}
