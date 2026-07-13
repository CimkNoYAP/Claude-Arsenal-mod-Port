package dev.doctor4t.arsenal.client.render.item;

import dev.doctor4t.arsenal.client.ArsenalClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.ModelIdentifier;

public abstract class GUIHeldVaryingItemRenderer extends BuiltinModelItemRenderer {
    protected final Identifier weaponId;
    private BakedModel inventoryWeaponModel;
    private BakedModel worldWeaponModel;

    public GUIHeldVaryingItemRenderer(Identifier weaponId) {
        super(MinecraftClient.getInstance().getBlockEntityRenderDispatcher(),
              MinecraftClient.getInstance().getEntityModelLoader());
        this.weaponId = weaponId;
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (this.inventoryWeaponModel == null || this.worldWeaponModel == null) {
            // Lazy-load models after the model manager is ready
            var manager = MinecraftClient.getInstance().getBakedModelManager();
            this.inventoryWeaponModel = manager.getModel(
                new ModelIdentifier(Identifier.of(weaponId.getNamespace(), weaponId.getPath() + "_inventory"), "inventory"));
            this.worldWeaponModel = manager.getModel(
                new ModelIdentifier(Identifier.of(weaponId.getNamespace(), weaponId.getPath() + "_in_hand"), "inventory"));
        }

        ArsenalClient.currentMode = mode;
        BakedModel model = switch (mode) {
            case GUI, GROUND, FIXED -> this.inventoryWeaponModel;
            default -> this.worldWeaponModel;
        };

        renderModel(stack, mode, matrices, vertexConsumers, light, overlay, model);
        ArsenalClient.currentMode = ModelTransformationMode.NONE;
    }

    protected abstract void renderModel(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                                        VertexConsumerProvider vertexConsumers, int light, int overlay,
                                        BakedModel model);
}
