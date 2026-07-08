package com.milky.milkytools.mixin;

import com.milky.milkytools.features.QuickShulkerSupport;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * quickshulker 打开潜影盒后，服务端会同步容器内容。
 * 在内容包处理完成后，从打开的潜影盒中取出 Litematica 需要的物品。
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleContainerContent", at = @At("TAIL"), require = 0)
    private void milkytools$handleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (QuickShulkerSupport.isSwitchingItem()) {
            QuickShulkerSupport.switchFromOpenedShulker();
        }
    }
}
