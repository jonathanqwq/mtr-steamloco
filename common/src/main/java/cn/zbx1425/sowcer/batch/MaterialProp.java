package cn.zbx1425.sowcer.batch;

import cn.zbx1425.mtrsteamloco.mixin.RenderTypeAccessor;
import cn.zbx1425.sowcer.vertex.VertAttrState;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33;

import java.lang.reflect.Field;
import java.util.Objects;

/** Properties regarding material. Set during model loading. Affects batching. */
public class MaterialProp {

    /** Name of the shader program. Must be loaded in ShaderManager. */
    public String shaderName;
    /** The texture to use. Null disables texture. */
    public ResourceLocation texture;

    /** The vertex attribute values to use for those specified with VertAttrSrc MATERIAL. */
    public VertAttrState attrState = new VertAttrState();

    /** If blending should be set up. True for entity_translucent_* and beacon_beam when translucent is true. */
    public boolean translucent = false;
    /** If depth buffer should be written to. False for beacon_beam when translucent is true, true for everything else. */
    public boolean writeDepthBuf = true;
    /** If face culling is enabled. False makes everything effectively double-sided. */
    public boolean cull = true;
    /** If the renderer should remove rotation components from model and view matrices.
     *  Results in faces on the XY plane always facing the camera. */
    public boolean billboard = false;

    public boolean cutoutHack = false;

    public MaterialProp(String shaderName, ResourceLocation texture) {
        this.shaderName = shaderName;
        this.texture = texture;
    }

    private static final ResourceLocation WHITE_TEXTURE_LOCATION = new ResourceLocation("minecraft:textures/misc/white.png");

    public void setupCompositeState() {
        RenderSystem.enableTexture();
        if (texture != null) {
            // TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            // textureManager.getTexture(texture).setFilter(false, false);
            RenderSystem.setShaderTexture(0, texture);
        } else {
            RenderSystem.setShaderTexture(0, WHITE_TEXTURE_LOCATION);
        }

        // HACK: To make cutout transparency on beacon_beam work
        if (translucent || cutoutHack) {
            RenderSystem.enableBlend(); // TransparentState
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        } else {
            RenderSystem.disableBlend();
        }
        RenderSystem.enableDepthTest(); // DepthTestState
        RenderSystem.depthFunc(GL33.GL_LEQUAL);
        if (cull) {
            RenderSystem.enableCull(); // CullState
        } else {
            RenderSystem.disableCull();
        }
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer(); // LightmapState
        Minecraft.getInstance().gameRenderer.overlayTexture().teardownOverlayColor(); // OverlayState
        RenderSystem.depthMask(writeDepthBuf); // WriteMaskState
    }

    private static RenderType.CompositeState getCompositeState(RenderType renderType) {
        RenderType.CompositeRenderType compositeRenderType = (RenderType.CompositeRenderType) renderType;
        try {
            Field privateField = RenderType.CompositeRenderType.class.getDeclaredField("state");
            privateField.setAccessible(true);
            RenderType.CompositeState state = (RenderType.CompositeState)privateField.get(compositeRenderType);
            return state;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public RenderType getBlazeRenderType() {
        RenderType result;
        ResourceLocation textureToUse = texture == null ? WHITE_TEXTURE_LOCATION : texture;
        switch (shaderName) {
            case "rendertype_entity_cutout":
                result = RenderType.entityCutout(textureToUse);
                break;
            case "rendertype_entity_translucent_cull":
                result = RenderType.entityTranslucentCull(textureToUse);
                break;
            case "rendertype_beacon_beam":
                result = RenderType.beaconBeam(textureToUse, translucent);
                break;
            default:
                result = RenderType.entityCutout(textureToUse);
                break;
        }
        ((RenderTypeAccessor)result).setMode(VertexFormat.Mode.TRIANGLES);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaterialProp that = (MaterialProp) o;
        return translucent == that.translucent && writeDepthBuf == that.writeDepthBuf && cull == that.cull && billboard == that.billboard && Objects.equals(shaderName, that.shaderName) && Objects.equals(texture, that.texture) && Objects.equals(attrState, that.attrState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shaderName, texture, attrState, translucent, writeDepthBuf, cull, billboard);
    }

}
