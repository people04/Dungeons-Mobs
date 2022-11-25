package com.infamous.dungeons_mobs.client.renderer.util;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.blaze3d.vertex.VertexBuilderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.LightType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import software.bernie.geckolib3.compat.PatchouliCompat;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimatableModel;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class ModGeoReplacedEntityRenderer<T extends IAnimatable> extends EntityRenderer implements IGeoRenderer {
	final AnimatedGeoModel<IAnimatable> modelProvider;
	protected final T animatable;
	protected IRenderTypeBuffer rtb;

	protected ItemStack mainHand;
	protected ItemStack offHand;
	protected ItemStack helmet;
	protected ItemStack chestplate;
	protected ItemStack leggings;
	protected ItemStack boots;
	protected ResourceLocation whTexture;
	protected final List<GeoLayerRenderer> layerRenderers = Lists.newArrayList();
	protected IAnimatable currentAnimatable;
	protected LivingEntity currentEntity;
	protected static final Map<Class<? extends IAnimatable>, ModGeoReplacedEntityRenderer> renderers = new ConcurrentHashMap<>();

	static {
		AnimationController.addModelFetcher((IAnimatable object) -> {
			ModGeoReplacedEntityRenderer renderer = renderers.get(object.getClass());
			return renderer == null ? null : renderer.getGeoModelProvider();
		});
	}

	public ModGeoReplacedEntityRenderer(EntityRendererManager renderManager,
										AnimatedGeoModel<IAnimatable> modelProvider, T animatable) {
		super(renderManager);
		this.modelProvider = modelProvider;
		this.animatable = animatable;
		if (!renderers.containsKey(animatable.getClass())) {
			renderers.put(animatable.getClass(), this);
		}
	}

	public static ModGeoReplacedEntityRenderer getRenderer(Class<? extends IAnimatable> item) {
		return renderers.get(item);
	}

	@Override
	public void render(Entity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
		this.render(entityIn, this.animatable, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
	}

	@Override
	public void renderEarly(Object animatable, MatrixStack stackIn, float partialTicks, @Nullable IRenderTypeBuffer renderTypeBuffer, @Nullable IVertexBuilder vertexBuilder, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
		this.rtb = renderTypeBuffer;
		Matrix4f renderEarlyMat = stackIn.last().pose().copy();
		IGeoRenderer.super.renderEarly(animatable, stackIn, partialTicks, renderTypeBuffer, vertexBuilder, packedLightIn, packedOverlayIn, red, green, blue, alpha);
	}

	@SuppressWarnings("resource")
	public void render(Entity entity, IAnimatable animatable, float entityYaw, float partialTicks, MatrixStack stack,
					   IRenderTypeBuffer bufferIn, int packedLightIn) {
		this.currentAnimatable = animatable;
		LivingEntity entityLiving;
		IAnimatable geo = animatable;
		if (entity instanceof LivingEntity) {
			entityLiving = (LivingEntity) entity;
			this.mainHand =   ((LivingEntity)entity).getItemBySlot(EquipmentSlotType.MAINHAND);
			this.offHand =    ((LivingEntity)entity).getItemBySlot(EquipmentSlotType.OFFHAND);
			this.helmet =     ((LivingEntity)entity).getItemBySlot(EquipmentSlotType.HEAD);
			this.chestplate = ((LivingEntity)entity).getItemBySlot(EquipmentSlotType.CHEST);
			this.leggings =   ((LivingEntity)entity).getItemBySlot(EquipmentSlotType.LEGS);
			this.boots =      ((LivingEntity)entity).getItemBySlot(EquipmentSlotType.FEET);
			this.whTexture = this.getTextureLocation(animatable);
			this.currentEntity = entityLiving;
		} else {
			throw (new RuntimeException("Replaced renderer was not an instanceof LivingEntity"));
		}

		if (animatable instanceof IGeoReplacedEntity) {
			IGeoReplacedEntity z = (IGeoReplacedEntity) geo;
			if (entity instanceof MobEntity) {
				z.setMobEntity((MobEntity) entity);
			}
		}

		stack.pushPose();
		if (entity instanceof MobEntity) {
			Entity leashHolder = ((MobEntity) entity).getLeashHolder();
			if (leashHolder != null) {
				this.renderLeash(((MobEntity) entity), partialTicks, stack, bufferIn, leashHolder);
			}
		}
		boolean shouldSit = entity.isPassenger()
				&& (entity.getVehicle() != null && entity.getVehicle().shouldRiderSit());
		EntityModelData entityModelData = new EntityModelData();
		entityModelData.isSitting = shouldSit;
		entityModelData.isChild = entityLiving.isBaby();

		float f = MathHelper.rotLerp(partialTicks, entityLiving.yBodyRotO, entityLiving.yBodyRot);
		float f1 = MathHelper.rotLerp(partialTicks, entityLiving.yHeadRotO, entityLiving.yHeadRot);
		float f2 = f1 - f;
		if (shouldSit && entity.getVehicle() instanceof LivingEntity) {
			LivingEntity livingentity = (LivingEntity) entity.getVehicle();
			f = MathHelper.rotLerp(partialTicks, livingentity.yBodyRotO, livingentity.yBodyRot);
			f2 = f1 - f;
			float f3 = MathHelper.wrapDegrees(f2);
			if (f3 < -85.0F) {
				f3 = -85.0F;
			}

			if (f3 >= 85.0F) {
				f3 = 85.0F;
			}

			f = f1 - f3;
			if (f3 * f3 > 2500.0F) {
				f += f3 * 0.2F;
			}

			f2 = f1 - f;
		}

		float f6 = MathHelper.lerp(partialTicks, entity.xRot, entity.xRot);
		if (entity.getPose() == Pose.SLEEPING) {
			Direction direction = entityLiving.getBedOrientation();
			if (direction != null) {
				float f4 = entity.getEyeHeight(Pose.STANDING) - 0.1F;
				stack.translate((float) (-direction.getStepX()) * f4, 0.0D, (float) (-direction.getStepZ()) * f4);
			}
		}
		float f7 = this.handleRotationFloat(entityLiving, partialTicks);
		this.applyRotations(entityLiving, stack, f7, f, partialTicks);
		this.preRenderCallback(entityLiving, stack, partialTicks);

		float limbSwingAmount = 0.0F;
		float limbSwing = 0.0F;
		if (!shouldSit && entity.isAlive()) {
			limbSwingAmount = MathHelper.lerp(partialTicks, entityLiving.animationSpeedOld, entityLiving.animationSpeed);
			limbSwing = entityLiving.animationPosition - entityLiving.animationSpeed * (1.0F - partialTicks);
			if (entityLiving.isBaby()) {
				limbSwing *= 3.0F;
			}

			if (limbSwingAmount > 1.0F) {
				limbSwingAmount = 1.0F;
			}
		}

		entityModelData.headPitch = -f6;
		entityModelData.netHeadYaw = -f2;

		GeoModel model = modelProvider.getModel(modelProvider.getModelLocation(animatable));
		AnimationEvent predicate = new AnimationEvent(animatable, limbSwing, limbSwingAmount, partialTicks,
				!(limbSwingAmount > -0.15F && limbSwingAmount < 0.15F), Collections.singletonList(entityModelData));
		((IAnimatableModel) modelProvider).setLivingAnimations(animatable, this.getUniqueID(entity), predicate);

		stack.translate(0, 0.01f, 0);
		Minecraft.getInstance().getTextureManager().bind(getTextureLocation(entity));
		Color renderColor = getRenderColor(animatable, partialTicks, stack, bufferIn, null, packedLightIn);
		RenderType renderType = getRenderType(entity, partialTicks, stack, bufferIn, null, packedLightIn,
				getTextureLocation(entity));
		if (!entity.isInvisibleTo(Minecraft.getInstance().player)) {
			IVertexBuilder glintBuffer = bufferIn.getBuffer(RenderType.entityGlintDirect());
			IVertexBuilder translucentBuffer = bufferIn
					.getBuffer(RenderType.entityTranslucentCull(getTextureLocation(entity)));
			render(model, entity, partialTicks, renderType, stack, bufferIn,
					glintBuffer != translucentBuffer ? VertexBuilderUtils.create(glintBuffer, translucentBuffer)
							: null,
					packedLightIn, getPackedOverlay(entityLiving, this.getOverlayProgress(entityLiving, partialTicks)),
					(float) renderColor.getRed() / 255f, (float) renderColor.getGreen() / 255f,
					(float) renderColor.getBlue() / 255f, (float) renderColor.getAlpha() / 255);
		}

		if (!entity.isSpectator()) {
			for (GeoLayerRenderer layerRenderer : this.layerRenderers) {
				layerRenderer.render(stack, bufferIn, packedLightIn, entity, limbSwing, limbSwingAmount, partialTicks,
						f7, f2, f6);
			}
		}
		if (ModList.get().isLoaded("patchouli")) {
			PatchouliCompat.patchouliLoaded(stack);
		}
		stack.popPose();
		super.render(entity, entityYaw, partialTicks, stack, bufferIn, packedLightIn);
	}

	protected float getOverlayProgress(LivingEntity livingEntityIn, float partialTicks) {
		return 0.0F;
	}

	protected void preRenderCallback(LivingEntity entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
	}

	@Override
	public ResourceLocation getTextureLocation(Entity entity) {
		return getTextureLocation(currentAnimatable);
	}

	@Override
	public AnimatedGeoModel getGeoModelProvider() {
		return this.modelProvider;
	}

	public static int getPackedOverlay(LivingEntity livingEntityIn, float uIn) {
		return OverlayTexture.pack(OverlayTexture.u(uIn),
				OverlayTexture.v(livingEntityIn.hurtTime > 0 || livingEntityIn.deathTime > 0));
	}

	protected void applyRotations(LivingEntity entityLiving, MatrixStack matrixStackIn, float ageInTicks,
			float rotationYaw, float partialTicks) {
		Pose pose = entityLiving.getPose();
		if (pose != Pose.SLEEPING) {
			matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180.0F - rotationYaw));
		}

		if (entityLiving.deathTime > 0) {
			float f = ((float) entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
			f = MathHelper.sqrt(f);
			if (f > 1.0F) {
				f = 1.0F;
			}

			matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(f * this.getDeathMaxRotation(entityLiving)));
		} else if (entityLiving.isAutoSpinAttack()) {
			matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-90.0F - entityLiving.xRot));
			matrixStackIn
					.mulPose(Vector3f.YP.rotationDegrees(((float) entityLiving.tickCount + partialTicks) * -75.0F));
		} else if (pose == Pose.SLEEPING) {
			Direction direction = entityLiving.getBedOrientation();
			float f1 = direction != null ? getFacingAngle(direction) : rotationYaw;
			matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(f1));
			matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(this.getDeathMaxRotation(entityLiving)));
			matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(270.0F));
		} else if (entityLiving.hasCustomName() || entityLiving instanceof PlayerEntity) {
			String s = TextFormatting.stripFormatting(entityLiving.getName().getString());
			if (("Dinnerbone".equals(s) || "Grumm".equals(s)) && (!(entityLiving instanceof PlayerEntity)
					|| ((PlayerEntity) entityLiving).isModelPartShown(PlayerModelPart.CAPE))) {
				matrixStackIn.translate(0.0D, entityLiving.getBbHeight() + 0.1F, 0.0D);
				matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
			}
		}

	}

	protected boolean isVisible(LivingEntity livingEntityIn) {
		return !livingEntityIn.isInvisible();
	}

	private static float getFacingAngle(Direction facingIn) {
		switch (facingIn) {
		case SOUTH:
			return 90.0F;
		case WEST:
			return 0.0F;
		case NORTH:
			return 270.0F;
		case EAST:
			return 180.0F;
		default:
			return 0.0F;
		}
	}

	protected float getDeathMaxRotation(LivingEntity entityLivingBaseIn) {
		return 90.0F;
	}

	@Override
	public boolean shouldShowName(Entity entity) {
		double d0 = this.entityRenderDispatcher.distanceToSqr(entity);
		float f = entity.isDiscrete() ? 32.0F : 64.0F;
		if (d0 >= (double) (f * f)) {
			return false;
		} else {
			return entity == this.entityRenderDispatcher.crosshairPickEntity && entity.hasCustomName();
		}
	}

	/**
	 * Returns where in the swing animation the living entity is (from 0 to 1). Args
	 * : entity, partialTickTime
	 */
	protected float getSwingProgress(LivingEntity livingBase, float partialTickTime) {
		return livingBase.getAttackAnim(partialTickTime);
	}

	/**
	 * Defines what float the third param in setRotationAngles of ModelBase is
	 */
	protected float handleRotationFloat(LivingEntity livingBase, float partialTicks) {
		return (float) livingBase.tickCount + partialTicks;
	}

	@Override
	public ResourceLocation getTextureLocation(Object instance) {
		return this.modelProvider.getTextureLocation((IAnimatable) instance);
	}
	
	public <E extends Entity> void renderLeash(MobEntity entity, float partialTicks, MatrixStack poseStack,
			IRenderTypeBuffer buffer, E leashHolder) {
		int u;
		poseStack.pushPose();
		Vector3d vec3d = leashHolder.getRopeHoldPosition(partialTicks);
		double d = (double) (MathHelper.lerp(partialTicks, entity.yBodyRot, entity.yBodyRotO) * ((float) Math.PI / 180))
				+ 1.5707963267948966;
		Vector3d vec3d2 = ((Entity) entity).getLeashOffset();
		double e = Math.cos(d) * vec3d2.z + Math.sin(d) * vec3d2.x;
		double f = Math.sin(d) * vec3d2.z - Math.cos(d) * vec3d2.x;
		double g = MathHelper.lerp(partialTicks, ((MobEntity) entity).xo, ((MobEntity) entity).getX()) + e;
		double h = MathHelper.lerp(partialTicks, ((MobEntity) entity).yo, ((MobEntity) entity).getY()) + vec3d2.y;
		double i = MathHelper.lerp(partialTicks, ((MobEntity) entity).zo, ((MobEntity) entity).getZ()) + f;
		poseStack.translate(e, vec3d2.y, f);
		float j = (float) (vec3d.x - g);
		float k = (float) (vec3d.y - h);
		float l = (float) (vec3d.z - i);
		IVertexBuilder vertexConsumer = buffer.getBuffer(RenderType.leash());
		Matrix4f matrix4f = poseStack.last().pose();
		float n = MathHelper.fastInvSqrt(j * j + l * l) * 0.025f / 2.0f;
		float o = l * n;
		float p = j * n;
		BlockPos blockPos = new BlockPos(((MobEntity) entity).getEyePosition(partialTicks));
		BlockPos blockPos2 = new BlockPos(leashHolder.getEyePosition(partialTicks));
		int q = this.getBlockLightLevel(entity, blockPos);
		int r = leashHolder.isOnFire() ? 15 : leashHolder.level.getBrightness(LightType.BLOCK, blockPos2);
		int s = entity.level.getBrightness(LightType.SKY, blockPos);
		int t = entity.level.getBrightness(LightType.SKY, blockPos2);
		for (u = 0; u <= 24; ++u) {
			ModGeoReplacedEntityRenderer.renderLeashPiece(vertexConsumer, matrix4f, j, k, l, q, r, s, t, 0.025f, 0.025f, o,
					p, u, false);
		}
		for (u = 24; u >= 0; --u) {
			ModGeoReplacedEntityRenderer.renderLeashPiece(vertexConsumer, matrix4f, j, k, l, q, r, s, t, 0.025f, 0.0f, o,
					p, u, true);
		}
		poseStack.popPose();
	}

	private static void renderLeashPiece(IVertexBuilder vertexConsumer, Matrix4f positionMatrix, float f, float g,
			float h, int leashedEntityBlockLight, int holdingEntityBlockLight, int leashedEntitySkyLight,
			int holdingEntitySkyLight, float i, float j, float k, float l, int pieceIndex, boolean isLeashKnot) {
		float m = (float) pieceIndex / 24.0f;
		int n = (int) MathHelper.lerp(m, leashedEntityBlockLight, holdingEntityBlockLight);
		int o = (int) MathHelper.lerp(m, leashedEntitySkyLight, holdingEntitySkyLight);
		int p = LightTexture.pack(n, o);
		float q = pieceIndex % 2 == (isLeashKnot ? 1 : 0) ? 0.7f : 1.0f;
		float r = 0.5f * q;
		float s = 0.4f * q;
		float t = 0.3f * q;
		float u = f * m;
		float v = g > 0.0f ? g * m * m : g - g * (1.0f - m) * (1.0f - m);
		float w = h * m;
		vertexConsumer.vertex(positionMatrix, u - k, v + j, w + l).color(r, s, t, 1.0f).uv2(p).endVertex();
		vertexConsumer.vertex(positionMatrix, u + k, v + i - j, w - l).color(r, s, t, 1.0f).uv2(p).endVertex();
	}

	@Override
	public IRenderTypeBuffer getCurrentRTB() {
		return this.rtb;
	}

	@Override
	public void setCurrentRTB(IRenderTypeBuffer rtb) {
		this.rtb = rtb;
	}
}
