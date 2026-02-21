package com.projectecho.mod.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.projectecho.mod.ProjectEcho;
import com.projectecho.mod.entity.custom.NullPointerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.BipedRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public class NullPointerRenderer extends BipedRenderer<NullPointerEntity, PlayerModel<NullPointerEntity>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ProjectEcho.MOD_ID, "textures/entity/null_pointer.png");

    public NullPointerRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0, false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(NullPointerEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(NullPointerEntity entity, float yaw, float partialTicks,
                       MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {

        // Дрожание при стадии утечки
        if (entity.getStage() >= NullPointerEntity.STAGE_LEAK) {
            float shake = MathHelper.sin(entity.tickCount * 0.5F) * 0.05F;
            matrixStack.translate(shake, 0, shake);
        }

        super.render(entity, yaw, partialTicks, matrixStack, buffer, packedLight);
    }

    @Override
    protected int getOverlayCoords(NullPointerEntity entity, float partialTick) {
        // Всегда максимальная яркость — "светится"
        return 0xF000F0;
    }
}
