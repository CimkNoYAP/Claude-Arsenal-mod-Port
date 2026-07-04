package dev.doctor4t.arsenal.client;

import dev.doctor4t.arsenal.Arsenal;
import dev.doctor4t.arsenal.cca.BackWeaponComponent;
import dev.doctor4t.arsenal.client.particle.contract.ColoredParticleInitialData;
import dev.doctor4t.arsenal.client.render.entity.*;
import dev.doctor4t.arsenal.client.render.item.AnchorbladeDynamicItemRenderer;
import dev.doctor4t.arsenal.client.render.item.ScytheDynamicItemRenderer;
import dev.doctor4t.arsenal.client.render.item.TridentDynamicItemRenderer;
import dev.doctor4t.arsenal.index.ArsenalEntities;
import dev.doctor4t.arsenal.index.ArsenalItems;
import dev.doctor4t.arsenal.index.ArsenalParticles;
import dev.doctor4t.arsenal.item.AnchorbladeItem;
import dev.doctor4t.arsenal.network.SweepPayload;
import dev.doctor4t.arsenal.network.SwapWeaponPayload;
import dev.doctor4t.arsenal.util.ArsenalConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

@SuppressWarnings("unused")
public class ArsenalClient implements ClientModInitializer {
    public static ModelTransformationMode currentMode = ModelTransformationMode.NONE;

    static {
        for (var mode : ModelTransformationMode.values()) {
            ModelPredicateProviderRegistry.register(Arsenal.id(mode.name().toLowerCase(Locale.ROOT)),
                (stack, world, entity, seed) -> mode == currentMode ? 1.0F : 0.0F);
        }
        ModelPredicateProviderRegistry.register(
            Identifier.ofVanilla("vanilla"),
            (stack, world, entity, seed) -> ArsenalConfig.CUSTOM_TRIDENT_RENDERING ? 0f : 1f);
    }

    public static KeyBinding weaponKeybind;
    public static KeyBinding swapKeybind;

    @Override
    public void onInitializeClient() {
        // Register integrated resource pack
        FabricLoader.getInstance().getModContainer(Arsenal.MOD_ID).ifPresent(modContainer ->
            ResourceManagerHelper.registerBuiltinResourcePack(
                Arsenal.id("classic"), modContainer, ResourcePackActivationType.NORMAL));

        // Built-in Item Renderers
        BuiltinItemRendererRegistry.INSTANCE.register(ArsenalItems.SCYTHE, new ScytheDynamicItemRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ArsenalItems.ANCHORBLADE, new AnchorbladeDynamicItemRenderer());
        if (ArsenalConfig.CUSTOM_TRIDENT_RENDERING)
            BuiltinItemRendererRegistry.INSTANCE.register(Items.TRIDENT, new TridentDynamicItemRenderer());

        // Force-load weapon models
        ModelLoadingPlugin.register(ctx -> ctx.addModels(ScytheDynamicItemRenderer.MODELS_TO_REGISTER));
        ModelLoadingPlugin.register(ctx -> ctx.addModels(AnchorbladeDynamicItemRenderer.MODELS_TO_REGISTER));
        if (ArsenalConfig.CUSTOM_TRIDENT_RENDERING)
            ModelLoadingPlugin.register(ctx -> ctx.addModels(TridentDynamicItemRenderer.MODELS_TO_REGISTER));
        ModelLoadingPlugin.register(ctx -> ctx.addModels(WeaponRackEntityRenderer.MODEL));

        ModEntityModelLayers.initialize();

        EntityRendererRegistry.register(ArsenalEntities.BLOOD_SCYTHE, BloodScytheEntityRenderer::new);
        EntityRendererRegistry.register(ArsenalEntities.ANCHORBLADE, AnchorbladeEntityRenderer::new);
        if (ArsenalConfig.CUSTOM_TRIDENT_RENDERING)
            EntityRendererRegistry.register(EntityType.TRIDENT, ArsenalTridentEntityRenderer::new);
        EntityRendererRegistry.register(ArsenalEntities.WEAPON_RACK, WeaponRackEntityRenderer::new);

        ArsenalParticles.registerFactories();

        weaponKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.arsenal.select_weapon", GLFW.GLFW_KEY_R, "category.arsenal"));
        swapKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.arsenal.swap_weapon", GLFW.GLFW_KEY_G, "category.arsenal"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (weaponKeybind.wasPressed() && client.player != null) {
                BackWeaponComponent.setHoldingBackWeapon(client.player,
                    !BackWeaponComponent.isHoldingBackWeapon(client.player));
            }
            if (swapKeybind.wasPressed()) {
                ClientPlayNetworking.send(new SwapWeaponPayload());
            }
        });

        for (AnchorbladeItem.Skin value : AnchorbladeItem.Skin.values()) {
            ModelLoadingPlugin.register(context -> context.addModels(value.anchorbladeEntityModel));
        }

        // Sweep particle packet (S2C)
        ClientPlayNetworking.registerGlobalReceiver(SweepPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                var world = context.client().world;
                if (world != null) {
                    world.addParticle(ArsenalParticles.SWEEP_PARTICLE.setData(
                        new ColoredParticleInitialData(payload.color())), payload.x(), payload.y(), payload.z(), 0, 0, 0);
                    world.addParticle(ArsenalParticles.SWEEP_SHADOW_PARTICLE.setData(
                        new ColoredParticleInitialData(payload.shadowColor())), payload.x(), payload.y(), payload.z(), 0, 0, 0);
                }
            });
        });
    }
}
