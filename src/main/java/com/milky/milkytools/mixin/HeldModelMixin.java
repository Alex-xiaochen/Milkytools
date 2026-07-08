package com.milky.milkytools.mixin;

import com.milky.milkytools.config.Configs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第一人称手持物品模型调整（ViewModel）。
 * 在 ItemInHandRenderer.renderItem 的 PoseStack 上叠加平移/缩放/旋转，仅作用于第一人称手持上下文。
 */
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public class HeldModelMixin {
    @Inject(method = "renderItem", at = @At("HEAD"))
    private void milkytools$adjustHeldModel(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext,
                                            PoseStack poseStack, SubmitNodeCollector bufferSource, int combinedLight,
                                            CallbackInfo ci) {
        if (!Configs.HELD_MODEL_ENABLED.getBooleanValue()) {
            return;
        }
        if (displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                && displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            return;
        }

        double scale = Configs.HELD_MODEL_SCALE.getDoubleValue();
        double px = Configs.HELD_MODEL_POS_X.getDoubleValue();
        double py = Configs.HELD_MODEL_POS_Y.getDoubleValue();
        double pz = Configs.HELD_MODEL_POS_Z.getDoubleValue();
        double rx = Math.toRadians(Configs.HELD_MODEL_ROT_X.getDoubleValue());
        double ry = Math.toRadians(Configs.HELD_MODEL_ROT_Y.getDoubleValue());
        double rz = Math.toRadians(Configs.HELD_MODEL_ROT_Z.getDoubleValue());

        if (scale != 1.0) {
            poseStack.scale((float) scale, (float) scale, (float) scale);
        }
        if (rx != 0.0 || ry != 0.0 || rz != 0.0) {
            Quaternionf quaternion = new Quaternionf();
            if (rx != 0.0) quaternion.rotateX((float) rx);
            if (ry != 0.0) quaternion.rotateY((float) ry);
            if (rz != 0.0) quaternion.rotateZ((float) rz);
            poseStack.mulPose(quaternion);
        }
        if (px != 0.0 || py != 0.0 || pz != 0.0) {
            poseStack.translate(px, py, pz);
        }
    }
}
