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
            fakeX = prevFakeX = targetX;
            fakeY = prevFakeY = targetY;
            fakeZ = prevFakeZ = targetZ;
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
    }

    /** 由相机 mixin 在渲染时取出插值后的虚拟相机坐标。 */
    public static Vec3 interpolatedPosition(float partialTicks) {
        double x = lerp(prevFakeX, fakeX, partialTicks);
        double y = lerp(prevFakeY, fakeY, partialTicks);
        double z = lerp(prevFakeZ, fakeZ, partialTicks);
        return new Vec3(x, y, z);
    }

    private static double animate(double current, double target, double speed) {
        return current + (target - current) * speed;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
