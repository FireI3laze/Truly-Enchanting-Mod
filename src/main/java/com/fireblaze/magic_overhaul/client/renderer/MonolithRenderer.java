package com.fireblaze.magic_overhaul.client.renderer;

import com.fireblaze.magic_overhaul.blockentity.MonolithBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.entity.ItemRenderer;

import java.util.Objects;

public class MonolithRenderer implements BlockEntityRenderer<MonolithBlockEntity> {

    public MonolithRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MonolithBlockEntity entity, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {

        ItemStack stack = entity.getItemInSlot(0);
        if (stack.isEmpty()) return;

        ItemRenderer itemRenderer = net.minecraft.client.Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(stack, entity.getLevel(), null, 0); // 0 ist der seed

        poseStack.pushPose();
        poseStack.translate(0.5, 1.95, 0.5);

        float time = (Objects.requireNonNull(entity.getLevel()).getGameTime() + partialTicks) * 5f;
        poseStack.mulPose(Axis.YP.rotationDegrees(time));
        poseStack.scale(1f, 1f, 1f);

        int light = 0xF000F0;
        itemRenderer.render(stack, ItemDisplayContext.GROUND, false, poseStack, bufferSource, light, combinedOverlay, model);


        poseStack.popPose();
    }
}
