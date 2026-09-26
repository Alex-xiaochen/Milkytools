package com.milky.milkytools.mixin;

import com.milky.milkytools.features.CoordinateBeacon;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 拦截聊天栏消息（玩家聊天、服务端/客户端系统消息都会经过 addMessage），
 * 把其中合法的坐标片段替换为带自定义点击事件的文本，供玩家点击后再渲染光柱。
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, require = 0)
    private Component milkytools$clickableCoordinates(Component message) {
        return CoordinateBeacon.makeClickable(message);
    }
}
