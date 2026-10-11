package com.example.xclient.mixin;

import com.example.xclient.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    // ESP / ItemESP: give entities the glow outline (visible through walls)
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void xclient$esp(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == MinecraftClient.getInstance().player) return;
        if (Modules.ESP.enabled && entity instanceof LivingEntity) cir.setReturnValue(true);
        else if (Modules.ITEMESP.enabled && entity instanceof ItemEntity) cir.setReturnValue(true);
    }
}
