package com.milky.milkytools.config;

import com.milky.milkytools.features.QuickFirework;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;

import static com.milky.milkytools.config.Configs.QUICK_FIREWORK;

// 监听 MaLiLib 热键回调。
public class HotkeysCallback implements IHotkeyCallback {
    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        if (key == QUICK_FIREWORK.getKeybind()) {
            return QuickFirework.accelerated();
        }

        return false;
    }

    // 设置反馈到 onKeyAction() 方法的快捷键。
    public static void init() {
        HotkeysCallback hotkeysCallback = new HotkeysCallback();

        for (ConfigHotkey configHotkey : Configs.KEY_LIST) {
            configHotkey.getKeybind().setCallback(hotkeysCallback);
        }
    }
}
