package io.github.myl7.chestlink.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import io.github.myl7.chestlink.LinkManager;

/**
 * Block-destruction hook. When a block entity is replaced,
 * {@code LevelChunk#setBlockState} calls {@code BlockEntity#preRemoveSideEffects},
 * which drops container contents. Unlinking first means the vanilla drop logic
 * then reads the chest's own (no longer intercepted) storage: a non-last member
 * is empty and drops nothing, the last member has just received the channel
 * contents and drops everything.
 */
@Mixin(LevelChunk.class)
public class LevelChunkMixin {
	@WrapOperation(method = "setBlockState",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/entity/BlockEntity;preRemoveSideEffects(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
	private void chestlink$unlinkBeforeRemoval(BlockEntity blockEntity, BlockPos pos, BlockState state,
			Operation<Void> original) {
		if (blockEntity instanceof ChestBlockEntity chest) {
			LinkManager.beforeChestRemoved(chest);
		}

		original.call(blockEntity, pos, state);
	}
}
