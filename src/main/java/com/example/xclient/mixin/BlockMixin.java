package com.example.xclient.mixin;

import com.example.xclient.XClient;
import com.example.xclient.Modules;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    // X-ray: only draw faces of ore blocks, hide everything else
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void xclient$xray(BlockState state, BlockState otherState, Direction side,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (Modules.XRAY.enabled) {
            cir.setReturnValue(XClient.XRAY_BLOCKS.contains(state.getBlock()));
        }
    }
}
