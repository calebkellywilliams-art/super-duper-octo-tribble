package com.example.xclient.mixin;

import com.example.xclient.Modules;
import com.example.xclient.XClient;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * X-ray for Sodium. Sodium replaces vanilla's face culling, so BlockMixin never runs when it is installed.
 * @Pseudo + require = 0 means nothing breaks if Sodium is missing or its internals differ.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public class SodiumBlockOcclusionCacheMixin {
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void xclient$xray(BlockState state, BlockView world, BlockPos pos, Direction facing,
                              CallbackInfoReturnable<Boolean> cir) {
        if (Modules.XRAY.enabled) {
            cir.setReturnValue(XClient.XRAY_BLOCKS.contains(state.getBlock()));
        }
    }
}
