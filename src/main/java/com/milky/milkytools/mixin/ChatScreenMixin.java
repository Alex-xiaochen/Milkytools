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
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void milkytools$beaconClick(Style style, boolean insertionMode, CallbackInfoReturnable<Boolean> cir) {
        if (CoordinateBeacon.handleClick(style)) {
            cir.setReturnValue(true);
        }
    }
}
