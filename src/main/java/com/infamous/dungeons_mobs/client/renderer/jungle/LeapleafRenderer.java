package com.infamous.dungeons_mobs.client.renderer.jungle;

import com.infamous.dungeons_mobs.DungeonsMobs;
import com.infamous.dungeons_mobs.client.models.jungle.LeapleafModel;
import com.infamous.dungeons_mobs.client.renderer.layer.PulsatingGlowLayer;
import com.infamous.dungeons_mobs.entities.jungle.LeapleafEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class LeapleafRenderer extends GeoEntityRenderer<LeapleafEntity> {
   public LeapleafRenderer(EntityRendererManager renderManager) {
      super(renderManager, new LeapleafModel());
      this.addLayer(new PulsatingGlowLayer<>(this, new ResourceLocation(DungeonsMobs.MODID, "textures/entity/jungle/leapleaf_glow.png"), 0.25F, 1.0F, 0.75F));
   }

   protected void applyRotations(LeapleafEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks,
                                 float rotationYaw, float partialTicks) {
      float scaleFactor = 0.9375F;
      matrixStackIn.scale(scaleFactor, scaleFactor, scaleFactor);
      super.applyRotations(entityLiving, matrixStackIn, ageInTicks, rotationYaw, partialTicks);
   }

   @Override
   public RenderType getRenderType(LeapleafEntity animatable, float partialTicks, MatrixStack stack,
                                   IRenderTypeBuffer renderTypeBuffer, IVertexBuilder vertexBuilder, int packedLightIn,
                                   ResourceLocation textureLocation) {
      return RenderType.entityTranslucent(getTextureLocation(animatable));
   }
}
