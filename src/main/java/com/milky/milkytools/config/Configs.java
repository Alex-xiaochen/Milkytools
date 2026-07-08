package com.milky.milkytools.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.util.data.Color4f;
import fi.dy.masa.malilib.util.data.json.JsonUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.milky.milkytools.MilkytoolsClient.MOD_ID;
import static fi.dy.masa.malilib.hotkeys.KeybindSettings.PRESS_ALLOWEXTRA;

public class Configs implements IConfigHandler {
    public static final Configs INSTANCE = new Configs();

    private static final Path CONFIG_DIR = Paths.get("./config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve(MOD_ID + ".json");

    public static final ConfigHotkey QUICK_FIREWORK = new ConfigHotkey(
            "快捷烟花",
            "",
            PRESS_ALLOWEXTRA,
            "工具功能：鞘翅飞行时自动使用背包里的安全烟花，并在使用后尝试换回原物品。\n未处于飞行状态时不会拦截原版右键；不会使用带爆炸效果的烟花。"
    );

    public static final ConfigBoolean QUICK_SHULKER = new ConfigBoolean(
            "快捷盒子支持",
            false,
            "兼容功能：Litematica 投影中键取方块时，如果背包内潜影盒包含目标物品，则通过 quickshulker 自动打开潜影盒并把物品换到可选取热栏槽。\n需要客户端和服务端安装 quickshulker；建议同时安装 Litematica。默认关闭。"
    );

    public static final ConfigBoolean PEARL_TRAJECTORY = new ConfigBoolean(
            "珍珠轨迹",
            false,
            "渲染末影珍珠投掷轨迹。手持末影珍珠时显示预测落点连线与落点标记。\n轨迹颜色与箭矢轨迹共用“轨迹颜色”配置。"
    );

    public static final ConfigBoolean ARROW_TRAJECTORY = new ConfigBoolean(
            "箭矢轨迹",
            false,
            "渲染箭矢投掷轨迹。手持弓（按当前拉弓力度）或已上弦的弩时显示预测落点连线与落点标记。\n轨迹颜色与珍珠轨迹共用“轨迹颜色”配置。"
    );

    public static final ConfigColor PEARL_TRAJECTORY_COLOR = new ConfigColor(
            "轨迹颜色",
            Color4f.fromColor(0x33DDFF)
    );

    public static final ConfigBoolean HELD_MODEL_ENABLED = new ConfigBoolean(
            "手持模型调整",
            false,
            "开启后，按下方“手持模型 *”配置调整第一人称手持物品的模型位置/缩放/旋转，以及手臂挥动速度。\n仅影响显示，不影响交互与攻击判定。"
    );

    public static final ConfigDouble HELD_MODEL_POS_X = new ConfigDouble(
            "手持模型 偏移X", 0.0, -2.0, 2.0, "手持物品相对默认位置的 X 偏移（单位：方块，向右为正）。"
    );

    public static final ConfigDouble HELD_MODEL_POS_Y = new ConfigDouble(
            "手持模型 偏移Y", 0.0, -2.0, 2.0, "手持物品相对默认位置的 Y 偏移（向上为正）。"
    );

    public static final ConfigDouble HELD_MODEL_POS_Z = new ConfigDouble(
            "手持模型 偏移Z", 0.0, -2.0, 2.0, "手持物品相对默认位置的 Z 偏移（向屏幕外/靠近玩家为正）。"
    );

    public static final ConfigDouble HELD_MODEL_SCALE = new ConfigDouble(
            "手持模型 缩放", 1.0, 0.1, 3.0, "手持物品整体缩放倍数（1 为原版大小）。"
    );

    public static final ConfigDouble HELD_MODEL_ROT_X = new ConfigDouble(
            "手持模型 旋转X", 0.0, -180.0, 180.0, "手持物品绕 X 轴旋转（度）。"
    );

    public static final ConfigDouble HELD_MODEL_ROT_Y = new ConfigDouble(
            "手持模型 旋转Y", 0.0, -180.0, 180.0, "手持物品绕 Y 轴旋转（度）。"
    );

    public static final ConfigDouble HELD_MODEL_ROT_Z = new ConfigDouble(
            "手持模型 旋转Z", 0.0, -180.0, 180.0, "手持物品绕 Z 轴旋转（度）。"
    );

    public static final ConfigDouble HELD_MODEL_SWING_SPEED = new ConfigDouble(
            "手持模型 挥动速度", 1.0, 0.1, 4.0, "第一人称手臂挥动动画速度倍数（1 为原版，>1 更快，<1 更慢）。"
    );

    public static final ConfigBoolean MOTION_CAMERA_ENABLED = new ConfigBoolean(
            "运动相机",
            false,
            "开启后，渲染用的相机位置会平滑地（带延迟地）跟随真实相机位置，产生“运动相机”的拖尾/缓动效果。\n纯视觉效果，不影响交互与命中判定。"
    );

    public static final ConfigBoolean MOTION_CAMERA_NO_FIRST_PERSON = new ConfigBoolean(
            "运动相机 第一人称除外",
            true,
            "开启时，处于第一人称视角下不启用运动相机（仅第三人称生效）。关闭时第一人称也启用。"
    );

    public static final ConfigDouble MOTION_CAMERA_FIRST_PERSON_SPEED = new ConfigDouble(
            "运动相机 第一人称速度", 0.6, 0.0, 1.0, "第一人称下相机平滑跟随的速度（0 完全静止，1 完全贴合真实位置）。"
    );

    public static final ConfigDouble MOTION_CAMERA_SPEED = new ConfigDouble(
            "运动相机 速度", 0.3, 0.0, 1.0, "非第一人称（第三人称）下相机平滑跟随的速度（0 完全静止，1 完全贴合真实位置）。"
    );

    public static final ConfigBoolean TOTEM_PARTICLE_ENABLED = new ConfigBoolean(
            "图腾粒子自定义",
            false,
            "开启后，用自定义颜色与速度替换不死图腾激活时默认的金色粒子。仅影响显示，不影响图腾的保命效果与音效。"
    );

    public static final ConfigDouble TOTEM_PARTICLE_VELOCITY_XZ = new ConfigDouble(
            "图腾粒子 水平速度", 100.0, 0.0, 500.0, "水平方向（X/Z）粒子速度缩放百分比（100 为原版速度）。"
    );

    public static final ConfigDouble TOTEM_PARTICLE_VELOCITY_Y = new ConfigDouble(
            "图腾粒子 垂直速度", 100.0, 0.0, 500.0, "垂直方向（Y）粒子速度缩放百分比（100 为原版速度）。"
    );

    public static final ConfigColor TOTEM_PARTICLE_COLOR = new ConfigColor(
            "图腾粒子 颜色1", Color4f.fromColor(0xFFFFFF)
    );

    public static final ConfigColor TOTEM_PARTICLE_COLOR2 = new ConfigColor(
            "图腾粒子 颜色2", Color4f.fromColor(0x000000)
    );

    // 按下时激活一次的热键列表。InputHandler 和 HotkeysCallback 都会使用这里。
    public static final ImmutableList<ConfigHotkey> KEY_LIST = ImmutableList.of(
            QUICK_FIREWORK
    );

    public static final ImmutableList<IConfigBase> ALL_CONFIGS = ImmutableList.of(
            QUICK_FIREWORK,
            QUICK_SHULKER,
            PEARL_TRAJECTORY,
            ARROW_TRAJECTORY,
            PEARL_TRAJECTORY_COLOR,
            HELD_MODEL_ENABLED,
            HELD_MODEL_POS_X,
            HELD_MODEL_POS_Y,
            HELD_MODEL_POS_Z,
            HELD_MODEL_SCALE,
            HELD_MODEL_ROT_X,
            HELD_MODEL_ROT_Y,
            HELD_MODEL_ROT_Z,
            HELD_MODEL_SWING_SPEED,
            MOTION_CAMERA_ENABLED,
            MOTION_CAMERA_NO_FIRST_PERSON,
            MOTION_CAMERA_FIRST_PERSON_SPEED,
            MOTION_CAMERA_SPEED,
            TOTEM_PARTICLE_ENABLED,
            TOTEM_PARTICLE_VELOCITY_XZ,
            TOTEM_PARTICLE_VELOCITY_Y,
            TOTEM_PARTICLE_COLOR,
            TOTEM_PARTICLE_COLOR2
    );

    @Override
    public void load() {
        if (CONFIG_FILE.toFile().isFile() && CONFIG_FILE.toFile().exists()) {
            JsonElement jsonElement = JsonUtils.parseJsonFile(CONFIG_FILE);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                ConfigUtils.readConfigBase(obj, MOD_ID, ALL_CONFIGS);
            }
        }
    }

    @Override
    public void save() {
        if ((CONFIG_DIR.toFile().exists() && CONFIG_DIR.toFile().isDirectory()) || CONFIG_DIR.toFile().mkdirs()) {
            JsonObject configRoot = new JsonObject();
            ConfigUtils.writeConfigBase(configRoot, MOD_ID, ALL_CONFIGS);
            JsonUtils.writeJsonToFile(configRoot, CONFIG_FILE);
        }
    }
}
