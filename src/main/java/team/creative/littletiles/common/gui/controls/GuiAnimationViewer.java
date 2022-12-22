package team.creative.littletiles.common.gui.controls;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.creativecore.common.gui.GuiChildControl;
import team.creative.creativecore.common.gui.GuiControl;
import team.creative.creativecore.common.gui.style.ControlFormatting;
import team.creative.creativecore.common.util.math.geo.Rect;
import team.creative.creativecore.common.util.math.vec.SmoothValue;
import team.creative.littletiles.common.animation.preview.AnimationPreview;

public class GuiAnimationViewer extends GuiControl implements IAnimationControl {
    
    public AnimationPreview preview;
    
    public SmoothValue rotX = new SmoothValue(200);
    public SmoothValue rotY = new SmoothValue(200);
    public SmoothValue rotZ = new SmoothValue(200);
    public SmoothValue distance = new SmoothValue(200);
    
    public boolean grabbed = false;
    public double grabX;
    public double grabY;
    
    public GuiAnimationViewer(String name) {
        super(name);
    }
    
    @Override
    public ControlFormatting getControlFormatting() {
        return ControlFormatting.NESTED_NO_PADDING;
    }
    
    @Override
    public void mouseMoved(Rect rect, double x, double y) {
        super.mouseMoved(rect, x, y);
        if (grabbed) {
            rotY.set(rotY.aimed() + x - grabX);
            rotX.set(rotX.aimed() + y - grabY);
            grabX = x;
            grabY = y;
        }
    }
    
    @Override
    public boolean mouseClicked(Rect rect, double x, double y, int button) {
        if (button == 0) {
            grabbed = true;
            grabX = x;
            grabY = y;
            return true;
        }
        return false;
    }
    
    @Override
    public void mouseReleased(Rect rect, double x, double y, int button) {
        if (button == 0)
            grabbed = false;
    }
    
    @Override
    public boolean mouseScrolled(Rect rect, double x, double y, double delta) {
        distance.set(Math.max(distance.aimed() + delta * -(Screen.hasControlDown() ? 5 : 1), 0));
        return true;
    }
    
    public static void makeLightBright() {
        /*try {
            LightTexture texture = Minecraft.getInstance().gameRenderer.lightTexture();
            for (int i = 0; i < 16; ++i) {
                for (int j = 0; j < 16; ++j) {
                    texture.lightPixels.setPixelRGBA(j, i, -1);
                }
            }
            
            texture.lightTexture.upload();
            
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }*/
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    protected void renderContent(PoseStack pose, GuiChildControl control, Rect rect, int mouseX, int mouseY) {
        if (preview == null)
            return;
        
        Minecraft mc = Minecraft.getInstance();
        makeLightBright();
        
        rotX.tick();
        rotY.tick();
        rotZ.tick();
        distance.tick();
        /*
        RenderSystem.cullFace(CullFace.BACK);
        
        pose.pushPose();
        
        //mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        //mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        GlStateManager.alphaFunc(516, 0.1F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        RenderSystem.viewport((int) rect.minX, (int) rect.minY, (int) rect.maxX, (int) rect.maxY);
        GlStateManager.matrixMode(5889);
        GlStateManager.loadIdentity();
        Project.gluPerspective(90, (float) width / (float) height, 0.05F, 16 * 16);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        //GlStateManager.matrixMode(5890);
        pose.translate(0, 0, -distance.current());
        RenderSystem.enableDepthTest();
        
        Vec3d rotationCenter = preview.animation.getCenter().rotationCenter;
        
        pose.mulPose(Vector3f.XP.rotation((float) rotX.current()));
        pose.mulPose(Vector3f.YP.rotation((float) rotY.current()));
        pose.mulPose(Vector3f.ZP.rotation((float) rotZ.current()));
        
        pose.translate(-preview.box.minX, -preview.box.minY, -preview.box.minZ);
        
        pose.translate(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z);
        
        GlStateManager.translate(TileEntityRendererDispatcher.staticPlayerX, TileEntityRendererDispatcher.staticPlayerY, TileEntityRendererDispatcher.staticPlayerZ);
        GlStateManager.translate(0, -75, 0);
        
        LittleAnimationHandlerClient.render.doRender(animation, 0, 0, 0, 0, TickUtils.getPartialTickTime());
        
        this.renderChunkLayer(RenderType.solid(), p_109600_, d0, d1, d2, p_109607_);
        this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels().get() > 0); // FORGE: fix flickering leaves when mods mess up the blurMipmap settings
        this.renderChunkLayer(RenderType.cutoutMipped(), p_109600_, d0, d1, d2, p_109607_);
        this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).restoreLastBlurMipmap();
        this.renderChunkLayer(RenderType.cutout(), p_109600_, d0, d1, d2, p_109607_);
        
        pose.popPose();
        
        GlStateManager.matrixMode(5888);
        
        RenderSystem.disableLighting();
        GlStateManager.cullFace(GlStateManager.CullFace.BACK);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        
        RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        mc.entityRenderer.setupOverlayRendering();
        RenderSystem.disableDepthTest();*/
        
    }
    
    @Override
    public void onLoaded(AnimationPreview preview) {
        this.preview = preview;
        this.distance.setStart(preview.grid.toVanillaGrid(preview.entireBox.getLongestSide()) / 2D + 2);
    }
    
    @Override
    public void closed() {}
    
    @Override
    public void init() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void tick() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void flowX(int width, int preferred) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void flowY(int width, int height, int preferred) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected int preferredWidth() {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    protected int preferredHeight(int width) {
        // TODO Auto-generated method stub
        return 0;
    }
}