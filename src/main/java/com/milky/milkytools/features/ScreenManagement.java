package com.milky.milkytools.features;

import net.minecraft.client.gui.screens.Screen;

/**
 * 用于隐藏 quickshulker 自动打开的容器屏幕。
 */
public class ScreenManagement {
    /**
     * 大于 0 时，Minecraft#setScreen 打开容器界面会被取消一次。
     */
    public static int closeScreen = 0;

    public static Screen screen = null;
}
