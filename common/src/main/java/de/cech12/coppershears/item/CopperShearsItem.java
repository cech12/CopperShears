package de.cech12.coppershears.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ShearsItem;

/**
 * Extends the ShearsItem to have a unique class and simplify the construction.
 */
public class CopperShearsItem extends ShearsItem {

    /**
     * Constructs a CopperShearsItem by configure it to stack to one item.
     */
    public CopperShearsItem(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.TOOL, ShearsItem.createToolProperties()));
    }

}
