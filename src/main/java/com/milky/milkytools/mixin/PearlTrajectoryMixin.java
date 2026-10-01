package com.milky.milkytools.mixin;

import com.milky.milkytools.config.Configs;
import com.milky.milkytools.features.TrajectoryGizmos;
import com.milky.milkytools.features.TrajectorySimulation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 手持末影珍珠时，按投掷物理预测其轨迹，并用 Gizmo 绘制连线与落点标记。
 * 物理（含随机散布与水中阻力）见 {@link TrajectorySimulation}；颜色取自配置项“珍珠轨迹颜色”。
 */
@Mixin(DebugRenderer.class)
public class PearlTrajectoryMixin {
    @Inject(method = "emitGizmos", at = @At("TAIL"))
    private void milkytools$drawPearlTrajectory(Frustum frustum, double camX, double camY, double camZ, float partialTick, CallbackInfo ci) {
        if (!Configs.PEARL_TRAJECTORY.getBooleanValue()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        if (!(mainHand.getItem() instanceof EnderpearlItem) && !(offHand.getItem() instanceof EnderpearlItem)) {
            return;
        }

        TrajectorySimulation.render(player, TrajectorySimulation.PEARL_POWER,
                TrajectorySimulation.Kind.THROWABLE, TrajectoryGizmos.colorOf());
    }
}
