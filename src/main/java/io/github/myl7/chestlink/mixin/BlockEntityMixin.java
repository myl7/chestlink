package io.github.myl7.chestlink.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import io.github.myl7.chestlink.LinkManager;

/**
 * Dirty-tracking funnel. Menu slot writes and hopper transfers all end in the
 * container's {@code setChanged}, declared here on {@code BlockEntity}. For
 * linked chests this marks the persistent channel state dirty and refreshes
 * comparators next to the other members.
 */
@Mixin(BlockEntity.class)
public class BlockEntityMixin {
	@Inject(method = "setChanged()V", at = @At("TAIL"))
	private void chestlink$afterSetChanged(CallbackInfo ci) {
		if ((Object) this instanceof ChestBlockEntity chest) {
			LinkManager.onChestChanged(chest);
		}
	}
}
