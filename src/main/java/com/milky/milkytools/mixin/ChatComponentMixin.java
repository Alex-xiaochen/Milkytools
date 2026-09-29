package com.milky.milkytools.mixin;

import com.milky.milkytools.features.CoordinateBeacon;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 拦截聊天栏消息，把其中合法的坐标片段替换为带自定义点击事件的文本，供玩家点击后再渲染光柱。
 * <p>
 * 26.x 把 {@code addMessage} 拆成了 addClientSystemMessage / addServerSystemMessage /
 * addPlayerMessage 三个公共入口，但它们全部委托给同一个私有 {@code addMessage} 漏斗，
 * 只是参数表多了一个 {@code GuiMessageSource}。注入这个漏斗即可覆盖所有消息来源。
 * <p>
 * 必须写显式描述符：1.21.11 有两个 {@code addMessage} 重载，裸方法名会产生歧义；
 * 且不能静默失败 —— 26.x 上未被拦截的 {@code ClickEvent.Custom} 会被打包成
 * ServerboundCustomClickActionPacket 发往服务器，属于行为回归。
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    //? if >=26.1 {
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;"
                    + "Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true, require = 1)
    //?} else {
    /*@ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;"
                    + "Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true, require = 1)
    *///?}
    private Component milkytools$clickableCoordinates(Component message) {
        return CoordinateBeacon.makeClickable(message);
    }
}
