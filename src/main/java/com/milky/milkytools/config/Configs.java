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

    public static final ConfigBoolean NAMETAGS_ENABLED = new ConfigBoolean(
            "自定义名牌",
            false,
            "开启后，在屏幕上为其他玩家绘制投影名牌：名称、延迟、血量与图腾次数，并可显示护甲/手持物品。\n纯客户端显示，不影响服务端与其他玩家。"
    );

    public static final ConfigBoolean NAMETAGS_PING = new ConfigBoolean(
            "名牌 延迟", true, "在名牌上显示该玩家的延迟（毫秒）。"
    );

    public static final ConfigBoolean NAMETAGS_HEALTH = new ConfigBoolean(
            "名牌 血量", true, "在名牌上显示该玩家的当前血量（含伤害吸收）。"
    );

    public static final ConfigBoolean NAMETAGS_POPS = new ConfigBoolean(
            "名牌 图腾", true, "在名牌上显示本次进入世界后该玩家使用不死图腾的次数。"
    );

    public static final ConfigBoolean NAMETAGS_ARMOR = new ConfigBoolean(
            "名牌 护甲", true, "在名牌上显示该玩家四个护甲槽的物品。"
    );

    public static final ConfigBoolean NAMETAGS_HANDS = new ConfigBoolean(
            "名牌 手持", true, "在名牌上显示该玩家主手与副手的物品。"
    );

    public static final ConfigBoolean NAMETAGS_ONLY_VISIBLE = new ConfigBoolean(
            "名牌 仅无界面时",
            true,
            "开启时，只要打开了任意界面（背包、聊天等）就隐藏名牌；关闭时始终显示。"
    );

    public static final ConfigBoolean NAMETAGS_SELF = new ConfigBoolean(
            "名牌 显示自己", false, "是否也为本地玩家自己绘制名牌。"
    );

    public static final ConfigDouble NAMETAGS_RANGE = new ConfigDouble(
            "名牌 范围", 48.0, 8.0, 128.0, "在此距离（格）内才会绘制名牌，超出范围的名牌不显示。"
    );

    public static final ConfigDouble NAMETAGS_MAX_SCALE = new ConfigDouble(
            "名牌 最大缩放", 1.0, 0.4, 3.0, "近距离时的名牌最大缩放倍数。"
    );

    public static final ConfigDouble NAMETAGS_MIN_SCALE = new ConfigDouble(
            "名牌 最小缩放", 0.55, 0.2, 2.0, "远距离时的名牌最小缩放倍数，会随距离在最大/最小缩放之间过渡。"
    );

    public static final ConfigBoolean SPAWNER_BOXES_ENABLED = new ConfigBoolean(
            "试炼描框",
            false,
            "开启后，为附近的试炼刷怪笼（含不祥变体）与宝库（含不祥宝库）绘制方框，不含普通刷怪笼。\n纯客户端显示，可穿透方块查看（透视）。"
    );

    public static final ConfigBoolean SPAWNER_BOXES_FILL = new ConfigBoolean(
            "描框 填充",
            true,
            "开启时方框内部会用半透明颜色涂色；关闭时只绘制方框的边框。"
    );

    public static final ConfigDouble SPAWNER_BOXES_FILL_ALPHA = new ConfigDouble(
            "描框 填充透明度", 60.0, 0.0, 255.0, "方框内部填充颜色的不透明度（0 完全透明，255 完全不透明）。"
    );

    public static final ConfigBoolean SPAWNER_BOXES_XRAY = new ConfigBoolean(
            "描框 透视",
            true,
            "开启后方框始终绘制在最上层，即使被方块或墙壁挡住也能看到（透视）。"
    );

    public static final ConfigDouble SPAWNER_BOXES_RANGE = new ConfigDouble(
            "描框 范围", 64.0, 8.0, 256.0, "检测并绘制方框的最大距离（格），越远消耗越高。"
    );

    public static final ConfigDouble SPAWNER_BOXES_LINE_WIDTH = new ConfigDouble(
            "描框 线宽", 2.0, 0.5, 8.0, "方框边框的线条宽度。"
    );

    public static final ConfigColor SPAWNER_BOXES_TRIAL_SPAWNER_COLOR = new ConfigColor(
            "描框 试炼刷怪笼颜色", Color4f.fromColor(0xFF5555)
    );

    public static final ConfigColor SPAWNER_BOXES_VAULT_COLOR = new ConfigColor(
            "描框 宝库颜色", Color4f.fromColor(0xFFD24A)
    );

    public static final ConfigColor SPAWNER_BOXES_OMINOUS_COLOR = new ConfigColor(
            "描框 不祥颜色", Color4f.fromColor(0xB06CFF)
    );

    public static final ConfigBoolean AMETHYST_BOXES_ENABLED = new ConfigBoolean(
            "紫水晶母岩描框",
            false,
            "开启后，为附近的紫水晶母岩（Budding Amethyst）绘制方框，用于定位紫水晶洞。\n纯客户端显示，可穿透方块查看（透视）。"
    );

    public static final ConfigBoolean AMETHYST_BOXES_FILL = new ConfigBoolean(
            "母岩描框 填充",
            true,
            "开启时方框内部会用半透明颜色涂色；关闭时只绘制方框的边框。"
    );

    public static final ConfigDouble AMETHYST_BOXES_FILL_ALPHA = new ConfigDouble(
            "母岩描框 填充透明度", 60.0, 0.0, 255.0, "方框内部填充颜色的不透明度（0 完全透明，255 完全不透明）。"
    );

    public static final ConfigBoolean AMETHYST_BOXES_XRAY = new ConfigBoolean(
            "母岩描框 透视",
            true,
            "开启后方框始终绘制在最上层，即使被方块或墙壁挡住也能看到（透视）。"
    );

    public static final ConfigDouble AMETHYST_BOXES_RANGE = new ConfigDouble(
            "母岩描框 范围", 64.0, 8.0, 256.0, "检测并绘制方框的最大距离（格），越远消耗越高。"
    );

    public static final ConfigDouble AMETHYST_BOXES_LINE_WIDTH = new ConfigDouble(
            "母岩描框 线宽", 2.0, 0.5, 8.0, "方框边框的线条宽度。"
    );

    public static final ConfigColor AMETHYST_BOXES_COLOR = new ConfigColor(
            "母岩描框 颜色", Color4f.fromColor(0xC77DFF)
    );

    public static final ConfigBoolean OBSIDIAN_BOXES_ENABLED = new ConfigBoolean(
            "黑曜石描框",
            false,
            "开启后，为附近的黑曜石（Obsidian）与哭泣的黑曜石（Crying Obsidian，按单独颜色区分）绘制方框，用于定位废弃传送门、下界传送门框架等。\n纯客户端显示，可穿透方块查看（透视）。"
    );

    public static final ConfigBoolean OBSIDIAN_BOXES_FILL = new ConfigBoolean(
            "黑曜石描框 填充",
            true,
            "开启时方框内部会用半透明颜色涂色；关闭时只绘制方框的边框。"
    );

    public static final ConfigDouble OBSIDIAN_BOXES_FILL_ALPHA = new ConfigDouble(
            "黑曜石描框 填充透明度", 60.0, 0.0, 255.0, "方框内部填充颜色的不透明度（0 完全透明，255 完全不透明）。"
    );

    public static final ConfigBoolean OBSIDIAN_BOXES_XRAY = new ConfigBoolean(
            "黑曜石描框 透视",
            true,
            "开启后方框始终绘制在最上层，即使被方块或墙壁挡住也能看到（透视）。"
    );

    public static final ConfigDouble OBSIDIAN_BOXES_RANGE = new ConfigDouble(
            "黑曜石描框 范围", 64.0, 8.0, 256.0, "检测并绘制方框的最大距离（格），越远消耗越高。"
    );

    public static final ConfigDouble OBSIDIAN_BOXES_LINE_WIDTH = new ConfigDouble(
            "黑曜石描框 线宽", 2.0, 0.5, 8.0, "方框边框的线条宽度。"
    );

    public static final ConfigColor OBSIDIAN_BOXES_COLOR = new ConfigColor(
            "黑曜石描框 颜色", Color4f.fromColor(0x8B5CF6)
    );

    public static final ConfigColor OBSIDIAN_BOXES_CRYING_COLOR = new ConfigColor(
            "黑曜石描框 哭泣颜色", Color4f.fromColor(0xE066FF)
    );

    public static final ConfigBoolean COORDINATE_BEACON_ENABLED = new ConfigBoolean(
            "坐标光柱",
            true,
            "开启后，聊天栏里以单个空格分隔的坐标（x z 或 x y z）会变为可点击文本。\n点击聊天栏里的坐标后，会在对应 x/z 所在方块渲染一根类似信标光柱的高亮柱体；三组数字时会丢弃 y，只用 x/z。\nx/z 范围 -30000000~30000000，y 范围 -64~320。\n光柱始终绘制在最上层（透视），并可在任意距离（包括未加载区块）看到。"
    );

    public static final ConfigColor COORDINATE_BEACON_COLOR = new ConfigColor(
            "坐标光柱 颜色", Color4f.fromColor(0x00E5FF)
    );

    public static final ConfigHotkey COORDINATE_BEACON_CLEAR = new ConfigHotkey(
            "坐标光柱 清除",
            "",
            PRESS_ALLOWEXTRA,
            "工具功能：清除当前已识别的所有坐标光柱标记。"
    );

    // 按下时激活一次的热键列表。InputHandler 和 HotkeysCallback 都会使用这里。
    public static final ImmutableList<ConfigHotkey> KEY_LIST = ImmutableList.of(
            QUICK_FIREWORK,
            COORDINATE_BEACON_CLEAR
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
            TOTEM_PARTICLE_COLOR2,
            NAMETAGS_ENABLED,
            NAMETAGS_PING,
            NAMETAGS_HEALTH,
            NAMETAGS_POPS,
            NAMETAGS_ARMOR,
            NAMETAGS_HANDS,
            NAMETAGS_ONLY_VISIBLE,
            NAMETAGS_SELF,
            NAMETAGS_RANGE,
            NAMETAGS_MAX_SCALE,
            NAMETAGS_MIN_SCALE,
            SPAWNER_BOXES_ENABLED,
            SPAWNER_BOXES_FILL,
            SPAWNER_BOXES_FILL_ALPHA,
            SPAWNER_BOXES_XRAY,
            SPAWNER_BOXES_RANGE,
            SPAWNER_BOXES_LINE_WIDTH,
            SPAWNER_BOXES_TRIAL_SPAWNER_COLOR,
            SPAWNER_BOXES_VAULT_COLOR,
            SPAWNER_BOXES_OMINOUS_COLOR,
            AMETHYST_BOXES_ENABLED,
            AMETHYST_BOXES_FILL,
            AMETHYST_BOXES_FILL_ALPHA,
            AMETHYST_BOXES_XRAY,
            AMETHYST_BOXES_RANGE,
            AMETHYST_BOXES_LINE_WIDTH,
            AMETHYST_BOXES_COLOR,
            OBSIDIAN_BOXES_ENABLED,
            OBSIDIAN_BOXES_FILL,
            OBSIDIAN_BOXES_FILL_ALPHA,
            OBSIDIAN_BOXES_XRAY,
            OBSIDIAN_BOXES_RANGE,
            OBSIDIAN_BOXES_LINE_WIDTH,
            OBSIDIAN_BOXES_COLOR,
            OBSIDIAN_BOXES_CRYING_COLOR,
            COORDINATE_BEACON_ENABLED,
            COORDINATE_BEACON_COLOR
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
