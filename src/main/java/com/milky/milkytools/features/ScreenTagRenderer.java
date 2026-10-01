package com.milky.milkytools.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import org.joml.Vector4f;
//?} else {
/*import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;*/
//?}

/**
 * 屏幕投影名牌的公共底座：世界坐标 → GUI 像素的投影，以及 2D 名牌的底板/文字绘制。
 * 由自定义玩家名牌（{@link NameTags}）与末影珍珠名牌（{@link PearlNameTags}）共用，
 * 两者的观感与缩放方式因此保持一致。
 * <p>
 * 投影在两个版本上走不同入口，但都用原版自己的实现，避免自行拼装矩阵导致静默错位：
 * <ul>
 *   <li>26.1.2 起：{@code CameraRenderState} 直接给出 projection 与 viewRotation 两个矩阵；</li>
 *   <li>1.21.11：{@code Camera} 还没有这两个矩阵，改用 {@code GameRenderer.projectPointToScreen}
 *       ——原版航点定位条自己用的投影入口，内部就是
 *       {@code getProjectionMatrix(getFov(...)) * rotation(camera.rotation().conjugate())}，
 *       连私有的 {@code getFov} 都替我们算好了。</li>
 * </ul>
 * 绘制部分两个版本共用同一套排版：{@link Painter} 只把名字不同的几个调用
 * （text/drawString、outline/renderOutline、item/renderItem）包起来。
 * <p>
 * <b>只在渲染线程使用。</b>{@link #PROJECTED}、{@link #VIEW_PROJECTION}、{@link #CLIP}
 * 与相机位置都是复用的静态缓冲，靠“单线程 + 逐个名牌顺序绘制”成立：
 * 一次 {@link #beginFrame()} 之后就连续调用 {@link #project} 并立刻读取 {@link #screenX()}/{@link #screenY()}。
 */
public final class ScreenTagRenderer {
    private static final Minecraft CLIENT = Minecraft.getInstance();

    /** 名牌底板与边框颜色，两个名牌功能共用，保证观感一致。 */
    public static final int BOX_COLOR = 0xAA101015;
    public static final int BORDER_COLOR = 0x661F1F28;

    /** 上一次 {@link #project} 的结果，避免为每个实体分配数组。 */
    private static final float[] PROJECTED = new float[2];

    /** 当前帧的相机位置，由 {@link #beginFrame} 写入。 */
    private static double cameraX;
    private static double cameraY;
    private static double cameraZ;

    //? if >=26.1.2 {
    /** 每帧的 clip = projection * viewRotation，由 {@link #beginFrame} 复用填充。 */
    private static final Matrix4f VIEW_PROJECTION = new Matrix4f();
    private static final Vector4f CLIP = new Vector4f();
    //?} else {
    /*// 1.21.11 走 projectPointToScreen，只需要位置与视线前向量。
    // 前向量与相对位置的点积正好等于投影后的 w（两者都等于 -viewZ），用它判掉相机背后的点。
    private static double forwardX;
    private static double forwardY;
    private static double forwardZ;*/
    //?}

    private ScreenTagRenderer() {
    }

    /** HUD 是否被玩家隐藏（F1）。26.2 起从 {@code Options.hideGui} 搬到了 {@code Gui.hud}。 */
    public static boolean hudHidden() {
        //? if >=26.2 {
        return CLIENT.gui.hud.isHidden();
        //?} else {
        /*return CLIENT.options.hideGui;*/
        //?}
    }

    /** 当前打开的界面，没有则返回 null。26.2 起从 {@code Minecraft.screen} 搬到了 {@code Gui}。 */
    public static Screen currentScreen() {
        //? if >=26.2 {
        return CLIENT.gui.screen();
        //?} else {
        /*return CLIENT.screen;*/
        //?}
    }

    /**
     * 取当前帧的相机并准备好投影；相机还没就绪时返回 false（本帧整块名牌都不画）。
     * 每帧调用一次，之后才可以用 {@link #project}。
     */
    //? if >=26.1.2 {
    public static boolean beginFrame() {
        // 这里用 var 而不是显式类型：CameraRenderState 的包路径在 26.1.2 与 26.2 之间变过，
        // 交给 CameraAccess 去分辨，本类连那个 import 都不需要。
        var camera = CameraAccess.camera();
        if (camera == null || camera.pos == null
                || camera.projectionMatrix == null || camera.viewRotationMatrix == null) {
            return false;
        }

        // 与参照实现一致：clip = projection * viewRotation * (世界坐标 - 相机坐标)。
        VIEW_PROJECTION.set(camera.projectionMatrix).mul(camera.viewRotationMatrix);
        cameraX = camera.pos.x;
        cameraY = camera.pos.y;
        cameraZ = camera.pos.z;
        return true;
    }
    //?} else {
    /*public static boolean beginFrame() {
        Camera camera = CLIENT.gameRenderer.getMainCamera();
        // 相机还没 setup() 时 position() 没有意义，先挡掉。
        if (!camera.isInitialized() || camera.position() == null) {
            return false;
        }

        // 相机在本帧内不再变化，位置与前向量各取一次，省掉每个名牌一次 Vec3 分配。
        // 注意 forwardVector() 返回的是 JOML 的 Vector3fc，不是原版的 Vec3。
        Vec3 position = camera.position();
        Vector3fc forward = camera.forwardVector();
        cameraX = position.x;
        cameraY = position.y;
        cameraZ = position.z;
        forwardX = forward.x();
        forwardY = forward.y();
        forwardZ = forward.z();
        return true;
    }*/
    //?}

    /**
     * 把一个世界坐标点投影到 GUI 坐标；点在相机前方且在屏幕范围内时返回 true，
     * 结果由 {@link #screenX()}/{@link #screenY()} 读取。调用前必须先 {@link #beginFrame()} 成功。
     */
    public static boolean project(double x, double y, double z, int guiWidth, int guiHeight) {
        //? if >=26.1.2 {
        CLIP.set((float) (x - cameraX), (float) (y - cameraY), (float) (z - cameraZ), 1.0F);
        CLIP.mul(VIEW_PROJECTION);

        if (CLIP.w() <= 0.05F) {
            return false;
        }

        return toScreen(CLIP.x() / CLIP.w(), CLIP.y() / CLIP.w(), guiWidth, guiHeight);
        //?} else {
        /*// 投影交给原版 GameRenderer.projectPointToScreen：它内部就是
        // getProjectionMatrix(getFov(camera, 0, true)) * rotation(camera.rotation().conjugate())。
        // getFov 是私有的，自己重算既要复刻一整套 FOV 修正（疾跑、拉弓、水下、反胃……），
        // 也容易和原版对不上，不如直接用原版这个公开入口。
        //
        // 唯一要自己补的是相机背后的判定：transformProject 在 w<0 时会把点镜像到屏幕另一侧，
        // 返回的 NDC 本身看不出这一点。
        double relativeX = x - cameraX;
        double relativeY = y - cameraY;
        double relativeZ = z - cameraZ;
        if (relativeX * forwardX + relativeY * forwardY + relativeZ * forwardZ <= 0.05) {
            return false;
        }

        Vec3 ndc = CLIENT.gameRenderer.projectPointToScreen(new Vec3(x, y, z));
        return toScreen((float) ndc.x, (float) ndc.y, guiWidth, guiHeight);*/
        //?}
    }

    /** 上一次 {@link #project} 得到的屏幕 X（GUI 像素）。 */
    public static float screenX() {
        return PROJECTED[0];
    }

    /** 上一次 {@link #project} 得到的屏幕 Y（GUI 像素）。 */
    public static float screenY() {
        return PROJECTED[1];
    }

    /** NDC 换算成 GUI 像素，越界（屏幕外）返回 false。两个版本的投影入口共用这段换算。 */
    private static boolean toScreen(float ndcX, float ndcY, int guiWidth, int guiHeight) {
        if (Math.abs(ndcX) > 1.2F || Math.abs(ndcY) > 1.2F) {
            return false;
        }

        PROJECTED[0] = (ndcX * 0.5F + 0.5F) * guiWidth;
        PROJECTED[1] = (1.0F - (ndcY * 0.5F + 0.5F)) * guiHeight;
        return true;
    }

    /** 距离越远名牌越小，并在最大/最小缩放之间做过渡；两个界限由调用方从各自配置传入。 */
    public static float scaleForDistance(double distance, float max, float min) {
        min = Math.min(min, max);
        float scale = max / (1.0F + (float) Math.max(0.0, distance - 4.0) * 0.025F);
        return Math.max(min, Math.min(max, scale));
    }

    /**
     * 两个版本 HUD 绘制 API 的最小交集。
     * <p>
     * 26.1.2 起是 {@code GuiGraphicsExtractor}（text / outline / item），1.21.11 是 {@code GuiGraphics}
     * （drawString / renderOutline / renderItem）；只有这几个名字不同，其余（fill、itemDecorations、pose）
     * 两边签名一致。包一层就够，不必把同一套排版写两遍——那种重复迟早会有一边忘了改。
     */
    public interface Painter {
        void fill(int x1, int y1, int x2, int y2, int color);

        void outline(int x, int y, int width, int height, int color);

        void text(Font font, String text, int x, int y, int color);

        void item(ItemStack stack, int x, int y);

        void itemDecorations(Font font, ItemStack stack, int x, int y);

        /** 以原点为基准整体缩放，配合 {@link #pop()} 还原。 */
        void pushScale(float scale);

        void pop();
    }

    //? if >=26.1.2 {
    public record GuiPainter(GuiGraphicsExtractor graphics) implements Painter {
        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
            graphics.fill(x1, y1, x2, y2, color);
        }

        @Override
        public void outline(int x, int y, int width, int height, int color) {
            graphics.outline(x, y, width, height, color);
        }

        @Override
        public void text(Font font, String text, int x, int y, int color) {
            graphics.text(font, text, x, y, color, true);
        }

        @Override
        public void item(ItemStack stack, int x, int y) {
            graphics.item(stack, x, y);
        }

        @Override
        public void itemDecorations(Font font, ItemStack stack, int x, int y) {
            graphics.itemDecorations(font, stack, x, y);
        }

        @Override
        public void pushScale(float scale) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale);
        }

        @Override
        public void pop() {
            graphics.pose().popMatrix();
        }
    }
    //?} else {
    /*public record GuiPainter(GuiGraphics graphics) implements Painter {
        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
            graphics.fill(x1, y1, x2, y2, color);
        }

        @Override
        public void outline(int x, int y, int width, int height, int color) {
            graphics.renderOutline(x, y, width, height, color);
        }

        @Override
        public void text(Font font, String text, int x, int y, int color) {
            graphics.drawString(font, text, x, y, color, true);
        }

        @Override
        public void item(ItemStack stack, int x, int y) {
            graphics.renderItem(stack, x, y);
        }

        @Override
        public void itemDecorations(Font font, ItemStack stack, int x, int y) {
            graphics.renderItemDecorations(font, stack, x, y);
        }

        @Override
        public void pushScale(float scale) {
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale);
        }

        @Override
        public void pop() {
            graphics.pose().popMatrix();
        }
    }*/
    //?}
}
