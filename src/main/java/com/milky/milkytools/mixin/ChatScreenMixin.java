package com.milky.milkytools.mixin;

import com.milky.milkytools.features.CoordinateBeacon;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 点击聊天气泡文本时，若点击的是我们标记过的坐标片段，则登记光柱标记并拦截原版处理。
 * <p>
 * 这是唯一同时能拿到 {@code Style}（进而 {@code Style.getClickEvent()}）又能取消原版处理的点：
 * 返回 {@code true} 会让 {@code mouseClicked} 直接返回，不再落到
 * {@code Screen#defaultHandleGameClickEvent}（那里会把自定义 ClickEvent 打包发给服务器）。
 * 四个版本签名一致，不需要版本条件。
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(
            method = "handleComponentClicked(Lnet/minecraft/network/chat/Style;Z)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void milkytools$beaconClick(Style style, boolean insertionMode, CallbackInfoReturnable<Boolean> cir) {
        if (CoordinateBeacon.handleClick(style)) {
            cir.setReturnValue(true);
        }
    }
}
