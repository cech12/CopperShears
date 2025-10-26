package de.cech12.coppershears.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class NeoforgeCopperShearsItem extends CopperShearsItem {

    @Override
    public boolean isDamageable(@NotNull ItemStack stack) {
        return stack.isDamageableItem(); //references to mixed in method
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        return stack.getMaxDamage(); //references to mixed in method
    }

}
