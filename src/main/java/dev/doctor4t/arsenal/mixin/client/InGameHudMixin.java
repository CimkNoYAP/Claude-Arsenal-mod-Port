package dev.doctor4t.arsenal.mixin.client;

import dev.doctor4t.arsenal.cca.BackWeaponComponent;
import dev.doctor4t.arsenal.client.ArsenalClient;
import dev.doctor4t.arsenal.index.ArsenalItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow private MinecraftClient client;

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    private void arsenal$renderBackWeapon(DrawContext context, CallbackInfo ci) {
        PlayerEntity player = this.client.player;
        if (player == null) return;

        ItemStack backStack = BackWeaponComponent.getBackWeapon(player);
        if (backStack.isEmpty()) return;

        int width = this.client.getWindow().getScaledWidth();
        int height = this.client.getWindow().getScaledHeight();
        int i = width / 2;

        boolean holdingBack = BackWeaponComponent.isHoldingBackWeapon(player);
        boolean isArsenalWeapon = backStack.isOf(ArsenalItems.SCYTHE) || backStack.isOf(ArsenalItems.ANCHORBLADE);

        if (holdingBack || isArsenalWeapon) {
            // Draw the back-slot icon above the hotbar
            context.drawItem(backStack, i - 12, height - 23 - 24);
            context.drawItemInSlot(this.client.textRenderer, backStack, i - 12, height - 23 - 24);
        }

        if (ArsenalClient.swapKeybind != null) {
            // Draw weapon slot indicators on each side of the hotbar
            context.drawItem(backStack, i - 91 - 29 + 6, height - 23 + 4);
            context.drawItem(backStack, i + 91 + 6, height - 23 + 4);
        }
    }
}
