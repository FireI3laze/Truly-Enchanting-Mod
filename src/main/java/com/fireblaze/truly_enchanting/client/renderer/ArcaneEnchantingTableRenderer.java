package com.fireblaze.truly_enchanting.client.renderer;

import com.fireblaze.truly_enchanting.blockentity.EnchantingTable.ArcaneEnchantingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Objects;

public class ArcaneEnchantingTableRenderer implements BlockEntityRenderer<ArcaneEnchantingTableBlockEntity> {

    public ArcaneEnchantingTableRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ArcaneEnchantingTableBlockEntity entity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

        ItemStack stack = entity.getItemInSlot0();
        if (stack.isEmpty()) return;

        ItemRenderer itemRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, entity.getLevel(), null, 0);

        poseStack.pushPose();

        // Position über dem Tisch
        poseStack.translate(0.5, 1.25, 0.5);

        // Leichtes Auf- und Abwippen
        float hover = (float) Math.sin((entity.getLevel().getGameTime() + partialTicks) / 10.0) * 0.1f;
        poseStack.translate(0, hover, 0);

        // Rotation
        float time = (Objects.requireNonNull(entity.getLevel()).getGameTime() + partialTicks) * 5f;
        poseStack.mulPose(Axis.YP.rotationDegrees(time));

        // Skalierung
        poseStack.scale(0.5f, 0.5f, 0.5f);

        // Rendern
        int light = 0xF000F0;
        itemRenderer.render(stack, ItemDisplayContext.FIXED, false, poseStack, bufferSource, light, combinedOverlay, model);

        poseStack.popPose();
    }
}
