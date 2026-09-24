package com.milky.milkytools.features;

import com.milky.milkytools.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * 运动相机（MotionCamera）。
 * 维护一个平滑跟随“真实相机位置”的虚拟相机坐标，渲染时把相机原点替换成插值后的虚拟坐标，
 * 从而产生相机拖尾/缓动的视觉效果。逻辑仿照 1.12.1 的 MotionCamera 模块，但适配 MC 26.1.2：
 *  - 跟随目标取真实相机位置（已包含第三人称的后拉偏移），因此对第一/第三人称都适用；
 *  - 每个客户端 tick 用指数平滑把 fake 朝 target 推进，渲染帧再用 partialTicks 在 prevFake→fake 间插值。
 */
public class MotionCamera {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    /**
     * 渲染相机相对真实相机允许的最大偏移（格）。
     * 原版 WeatherEffectRenderer 用一张 32×32（长度 1024）的查找表，按
     * (雨柱坐标 - 相机坐标 + 16) 索引；雨柱是按真实相机位置生成的，而渲染用的是这里的虚拟相机位置。
     * 一旦两者相差过大（传送/切换维度/高速飞行时虚拟坐标远远落后），索引就会越界导致崩溃。
     * 因此把偏移限制在安全范围内：默认 weatherRadius=10 时上限约为 5 格，取 4 格留出余量，
     * 同时保证正常行走/冲刺时仍能保留拖尾观感。
     */
    private static final double MAX_OFFSET = 4.0;

    private static boolean initialized = false;
    private static boolean wasActive = false;

    private static double fakeX;
    private static double fakeY;
    private static double fakeZ;
    private static double prevFakeX;
    private static double prevFakeY;
    private static double prevFakeZ;

    private static double targetX;
    private static double targetY;
    private static double targetZ;

    /** 当前是否应当启用运动相机（受总开关与“第一人称除外”约束）。 */
    public static boolean isActive() {
        if (!Configs.MOTION_CAMERA_ENABLED.getBooleanValue()) {
            return false;
        }
        if (Configs.MOTION_CAMERA_NO_FIRST_PERSON.getBooleanValue()
                && CLIENT.options.getCameraType().isFirstPerson()) {
            return false;
        }
        return true;
    }

    /** 由相机 mixin 每帧传入真实相机位置，作为平滑跟随的目标。 */
    public static void captureTarget(double x, double y, double z) {
        targetX = x;
        targetY = y;
        targetZ = z;

        // 尚未初始化、或目标发生大跨度跳变（传送/切换维度/进出世界）时立即贴合，
        // 避免虚拟坐标与真实相机相差过大。
        if (!initialized
                || Math.abs(fakeX - x) > MAX_OFFSET
                || Math.abs(fakeY - y) > MAX_OFFSET
                || Math.abs(fakeZ - z) > MAX_OFFSET) {
            snapToTarget();
            initialized = true;
        }
    }

    /** 由客户端 tick 处理器每 tick 调用一次，推进虚拟相机坐标。 */
    public static void onTick() {
        boolean active = isActive();
        if (!active) {
            wasActive = false;
            initialized = false;
            return;
        }

        // 刚启用（或刚从失效态恢复）时，直接把虚拟坐标贴合到目标，避免初始跳变。
        if (!initialized || !wasActive) {
            snapToTarget();
            initialized = true;
            wasActive = true;
            return;
        }

        double speed = CLIENT.options.getCameraType().isFirstPerson()
                ? Configs.MOTION_CAMERA_FIRST_PERSON_SPEED.getDoubleValue()
                : Configs.MOTION_CAMERA_SPEED.getDoubleValue();

        prevFakeX = fakeX;
        prevFakeY = fakeY;
        prevFakeZ = fakeZ;
        fakeX = animate(fakeX, targetX, speed);
        fakeY = animate(fakeY, targetY, speed);
        fakeZ = animate(fakeZ, targetZ, speed);

        // 跟随目标跳变过大时直接贴合，保证渲染偏移始终有界（防止天气渲染器越界崩溃）。
        if (Math.abs(fakeX - targetX) > MAX_OFFSET
                || Math.abs(fakeY - targetY) > MAX_OFFSET
                || Math.abs(fakeZ - targetZ) > MAX_OFFSET) {
            snapToTarget();
        }
    }

    /** 由相机 mixin 在渲染时取出插值后的虚拟相机坐标。 */
    public static Vec3 interpolatedPosition(float partialTicks) {
        double x = clampOffset(lerp(prevFakeX, fakeX, partialTicks), targetX);
        double y = clampOffset(lerp(prevFakeY, fakeY, partialTicks), targetY);
        double z = clampOffset(lerp(prevFakeZ, fakeZ, partialTicks), targetZ);
        return new Vec3(x, y, z);
    }

    private static void snapToTarget() {
        fakeX = prevFakeX = targetX;
        fakeY = prevFakeY = targetY;
        fakeZ = prevFakeZ = targetZ;
    }

    /** 把虚拟坐标限制在真实目标附近 MAX_OFFSET 格内，作为渲染时的最后一道保险。 */
    private static double clampOffset(double value, double target) {
        if (value > target + MAX_OFFSET) {
            return target + MAX_OFFSET;
        }
        if (value < target - MAX_OFFSET) {
            return target - MAX_OFFSET;
        }
        return value;
    }

    private static double animate(double current, double target, double speed) {
        return current + (target - current) * speed;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
