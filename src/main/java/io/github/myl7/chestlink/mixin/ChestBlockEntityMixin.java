package io.github.myl7.chestlink.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import io.github.myl7.chestlink.ChestLinkChest;
import io.github.myl7.chestlink.LinkManager;

/**
 * Core read/write interception. Every inventory access on a chest —
 * {@code getItem}/{@code setItem}/{@code removeItem} via
 * {@code BaseContainerBlockEntity}, hopper/dropper transfers, menu slots, and
 * comparator output — funnels through {@code getItems}, so returning the
 * channel's shared list here makes every linked chest literally the same
 * inventory. Save/load ({@code saveAdditional}/{@code loadAdditional}) access
 * the backing field directly and are deliberately not intercepted.
 *
 * <p>Also applies to {@code TrappedChestBlockEntity}, which inherits this class.
 */
@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin implements ChestLinkChest {
	@Shadow
	protected abstract void setItems(NonNullList<ItemStack> items);

	@Inject(method = "getItems", at = @At("HEAD"), cancellable = true)
	private void chestlink$returnSharedItems(CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
		BlockEntity self = (BlockEntity) (Object) this;
		Level level = self.getLevel();
		// level == null covers chunk deserialization; client side keeps vanilla behavior
		// (the LinkManager index only exists on the server main thread).
		if (level == null || level.isClientSide()) {
			return;
		}

		NonNullList<ItemStack> shared = LinkManager.sharedItems(level, self.getBlockPos());
		if (shared != null) {
			cir.setReturnValue(shared);
		}
	}

	@Override
	public void chestlink$setOwnItems(NonNullList<ItemStack> items) {
		this.setItems(items);
	}
}
