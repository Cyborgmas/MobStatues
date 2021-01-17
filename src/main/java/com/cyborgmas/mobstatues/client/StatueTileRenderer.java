package com.cyborgmas.mobstatues.client;

import com.cyborgmas.mobstatues.objects.StatueTileEntity;
import com.cyborgmas.mobstatues.util.StatueCreationHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.WeakHashMap;

public class StatueTileRenderer extends TileEntityRenderer<StatueTileEntity> {
    public StatueTileRenderer(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(StatueTileEntity statue, float partialTicks, MatrixStack stack, IRenderTypeBuffer buffer, int combinedLight, int combinedOverlay) {
        statue.renderEntity(stack, partialTicks, buffer, combinedLight);
    }

    public static ItemStackTileEntityRenderer getStatueItemRenderer() {
        return new ItemStackTileEntityRenderer() {
            private final Map<ItemStack, Entity> dynamicModelMap = new WeakHashMap<>();

            @Override
            public void func_239207_a_(ItemStack itemStack, ItemCameraTransforms.TransformType transformType, MatrixStack stack, IRenderTypeBuffer buffer, int light, int overlay) {
                Entity statue = dynamicModelMap.computeIfAbsent(itemStack, s ->
                        StatueCreationHelper.getEntity(s.getOrCreateTag()).withWorld(Minecraft.getInstance().world));

                if (statue == null)
                    return;

                stack.push();

                Minecraft.getInstance().getRenderManager().getRenderer(statue).render(statue, 0, 0, stack, buffer, light);

                stack.pop();
            }
        };
    }
}
