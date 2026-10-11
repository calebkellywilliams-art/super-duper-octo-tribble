package com.example.xclient.mixin;

import com.example.xclient.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    // SafeWalk: behave as if sneaking at ledges (never walk off an edge)
    @Inject(method = "clipAtLedge", at = @At("HEAD"), cancellable = true, require = 0)
    private void xclient$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (!Modules.SAFEWALK.enabled) return;
        var me = MinecraftClient.getInstance().player;
        if (me != null && me.getUuid().equals(((PlayerEntity) (Object) this).getUuid())) cir.setReturnValue(true);
    }
}
