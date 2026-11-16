package de.cech12.coppershears.mixin;

import de.cech12.coppershears.item.CopperShearsItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Copper shears have no default damage component (the durability comes from the config, see
 * {@link DataComponentHolderMixin}), so setting the damage to 0 (anvil, mending) would store a damage component that
 * new shears do not have. Like vanilla shears, fully repaired copper shears must equal new ones.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(at = @At("RETURN"), method = "setDamageValue")
    private void coppershears$removeZeroDamage(int damage, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        if (damage <= 0 && stack.getItem() instanceof CopperShearsItem) {
            stack.remove(DataComponents.DAMAGE);
        }
    }

}
