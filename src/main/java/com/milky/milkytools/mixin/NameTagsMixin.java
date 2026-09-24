package com.milky.milkytools.mixin;

import com.milky.milkytools.features.NameTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 捕捉服务端下发的“使用不死图腾”实体事件（eventId 35），
 * 为对应玩家累加图腾次数，供自定义名牌显示。
 */
@Mixin(ClientPacketListener.class)
public class NameTagsMixin {

    @Inject(
            method = "handleEntityEvent(Lnet/minecraft/network/protocol/game/ClientboundEntityEventPacket;)V",
            at = @At("HEAD"),
            require = 0
    )
    private void milkytools$trackTotemUse(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() != NameTags.TOTEM_USE_EVENT_ID) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Entity entity = packet.getEntity(minecraft.level);
        if (entity instanceof Player player) {
            NameTags.onTotemUse(player);
        }
    }
}
