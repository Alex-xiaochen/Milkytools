package com.milky.milkytools.features;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 末影珍珠与箭矢共用的轨迹模拟，逐行对照香草实体运动代码实现。
 * <p>
 * 这段物理在 1.21.11 / 26.1.2 / 26.2 / 26.3 四个受支持版本里完全一致，因此不需要 Stonecutter 条件编译。
 * <p>
 * 旧实现只做「位置 += 速度，速度 -= 重力」的近似，漏掉了香草的随机散布（随机动量）、空气与水的惯性、
 * 投掷者自身的速度，且箭矢的重力与阻力顺序写反，落点会明显偏移，这里全部按香草补齐。
 */
public final class TrajectorySimulation {
    /**
     * 香草 {@code Projectile#getMovementToShoot} 的散布系数：单位方向向量逐轴叠加
     * {@code triangle(0, 0.0172275 * uncertainty)} 之后才乘初速度，且不再重新归一化。
     */
    private static final double UNCERTAINTY_SPREAD = 0.0172275;

    /** 弓、弩、末影珍珠在香草里传给 {@code spawnProjectileFromRotation} 的 uncertainty 都是 1.0。 */
    private static final float PLAYER_UNCERTAINTY = 1.0F;

    /** 末影珍珠初速度，对应 {@code EnderpearlItem#PROJECTILE_SHOOT_POWER}。 */
    public static final double PEARL_POWER = 1.5;

    /**
     * 两种弹射物的积分顺序在香草里并不相同，必须分开建模，否则重力与阻力会互相串味：
     * <ul>
     *   <li>{@link #ARROW}（{@code AbstractArrow#tick}）：用 tick 开始时的速度位移，位移之后再乘空气阻力
     *       0.99、再减重力 0.05；在水中则改成位移前乘 0.6 惯性，位移后不再乘空气阻力（重力照常减）。</li>
     *   <li>{@link #THROWABLE}（{@code ThrowableProjectile#tick}）：先减重力 0.03、再乘阻力
     *       （水中 0.8、空气中 0.99），最后用乘完的速度位移。</li>
     * </ul>
     */
    public enum Kind {
        ARROW,
        THROWABLE
    }

    /** 生成位置：香草用 {@code mob.getX() / getEyeY() - 0.1 / mob.getZ()}，比视线起点低 0.1 格。 */
    private static final double SPAWN_EYE_OFFSET = 0.1;

    private static final double ARROW_GRAVITY = 0.05;
    private static final float ARROW_AIR_DRAG = 0.99F;
    private static final float ARROW_WATER_INERTIA = 0.6F;

    private static final double THROWABLE_GRAVITY = 0.03;
    private static final float THROWABLE_AIR_DRAG = 0.99F;
    private static final float THROWABLE_WATER_INERTIA = 0.8F;

    private static final int MAX_TICKS = 200;
    private static final float CENTER_WIDTH = 3.0F;
    private static final float SPREAD_WIDTH = 1.0F;
    private static final float LANDING_MARKER_SIZE = 0.25F;
    private static final float SPREAD_LANDING_SIZE = 0.12F;
    /** 散布包络的压暗系数，否则 8 条包络线会盖住中心轨迹。 */
    private static final float SPREAD_DIM = 0.55F;

    private TrajectorySimulation() {
    }

    /**
     * 按香草物理模拟并绘制一条轨迹：一条不加散布的中心轨迹，外加 8 条最坏情况散布包络。
     * <p>
     * 起点、视线、投掷者动量都取自 {@code shooter}，对应香草
     * {@code Projectile#spawnProjectileFromRotation} 的调用方式。
     *
     * @param shooter 投掷者（客户端玩家）
     * @param power   初速度大小：弓为 {@code 拉弓力度 * 3.0}，弩 3.15，珍珠 1.5
     * @param kind    积分顺序，见 {@link Kind}
     * @param argb    轨迹颜色（不透明）
     */
    public static void render(Entity shooter, double power, Kind kind, int argb) {
        Level level = shooter.level();
        Vec3 start = shooter.getEyePosition().subtract(0.0, SPAWN_EYE_OFFSET, 0.0);
        Vec3 look = shooter.getLookAngle();

        // 香草 Projectile#shootFromRotation 会把投掷者自身的速度叠加上去；
        // 竖直分量只在离地时叠加，站在地面上不会把自身重力带进初速度。
        Vec3 sourceMovement = shooter.getDeltaMovement();
        Vec3 carry = new Vec3(sourceMovement.x, shooter.onGround() ? 0.0 : sourceMovement.y, sourceMovement.z);

        trace(level, shooter, start, look.scale(power).add(carry), kind, argb, CENTER_WIDTH, LANDING_MARKER_SIZE);

        // 最坏情况包络：三个轴的散布互相独立且落在 [-s, s] 内，因此方向扰动立方体的 8 个角
        // 就是全部可能的极值，包围它们即可覆盖所有随机结果。
        double spread = UNCERTAINTY_SPREAD * PLAYER_UNCERTAINTY;
        int spreadColor = TrajectoryGizmos.dim(argb, SPREAD_DIM);
        for (int corner = 0; corner < 8; corner++) {
            double dx = (corner & 1) == 0 ? -spread : spread;
            double dy = (corner & 2) == 0 ? -spread : spread;
            double dz = (corner & 4) == 0 ? -spread : spread;
            Vec3 velocity = look.add(dx, dy, dz).scale(power).add(carry);
            trace(level, shooter, start, velocity, kind, spreadColor, SPREAD_WIDTH, SPREAD_LANDING_SIZE);
        }
    }

    /** 模拟一条轨迹并逐段连线；撞到方块时补上落点标记并结束。 */
    private static void trace(Level level, Entity source, Vec3 start, Vec3 initialVelocity,
                              Kind kind, int argb, float width, float landingSize) {
        Vec3 position = start;
        Vec3 previous = start;
        Vec3 velocity = initialVelocity;

        for (int tick = 0; tick < MAX_TICKS; tick++) {
            // 位移前的位置，香草在这里判定一次是否在水里。
            boolean inWater = isInWater(level, position);

            // 本 tick 的位移量。箭矢用 tick 开始时的速度位移（水里被 0.6 缩放的只是存起来的速度，
            // 位移本身仍走旧值，这是 AbstractArrow 的实际行为）；投掷物则是先算完重力与阻力再位移。
            Vec3 step;
            if (kind == Kind.ARROW) {
                step = velocity;
                if (inWater) {
                    velocity = velocity.scale(ARROW_WATER_INERTIA);
                }
            } else {
                velocity = velocity.subtract(0.0, THROWABLE_GRAVITY, 0.0)
                        .scale(inWater ? THROWABLE_WATER_INERTIA : THROWABLE_AIR_DRAG);
                step = velocity;
            }

            Vec3 next = position.add(step);
            // 与香草一致：只做方块碰撞（流体不阻挡弹射物），形状取 COLLIDER。
            HitResult hit = level.clipIncludingBorder(
                    new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                TrajectoryGizmos.drawSegment(previous, hit.getLocation(), argb, width);
                Gizmos.point(hit.getLocation(), argb, landingSize);
                return;
            }

            TrajectoryGizmos.drawSegment(previous, next, argb, width);
            previous = next;
            position = next;

            // 箭矢的阻力与重力在位移之后结算，顺序不能和上面水中那步互换。
            // 这里香草是在「移动之后」的位置再判定一次是否在水里，所以刚入水的那一 tick
            // 会跳过空气阻力，必须用新位置重算，不能复用位移前的结果。
            if (kind == Kind.ARROW) {
                if (!isInWater(level, position)) {
                    velocity = velocity.scale(ARROW_AIR_DRAG);
                }
                velocity = velocity.subtract(0.0, ARROW_GRAVITY, 0.0);
            }
        }
    }

    /**
     * 弹射物是否泡在水里。香草判定的是包围盒与流体高度求交后的 wasTouchingWater，
     * 这里等价地用弹射物所在方块的流体高度是否没过它的位置来判断，足够预测使用。
     */
    private static boolean isInWater(Level level, Vec3 position) {
        BlockPos blockPos = BlockPos.containing(position);
        FluidState fluid = level.getFluidState(blockPos);
        return fluid.is(FluidTags.WATER) && blockPos.getY() + fluid.getHeight(level, blockPos) > position.y;
    }
}
