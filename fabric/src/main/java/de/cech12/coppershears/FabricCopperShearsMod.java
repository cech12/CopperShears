package de.cech12.coppershears;

import de.cech12.coppershears.item.CopperShearsItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;

import java.util.function.Function;

/**
 * Mod class for the Fabric loader.
 */
@SuppressWarnings("unused")
public class FabricCopperShearsMod implements ModInitializer {

    /** copper shears item registry object */

    public static final Item COPPER_SHEARS = registerItem("copper_shears", CopperShearsItem::new);

    static {
        Constants.COPPER_SHEARS = () -> COPPER_SHEARS;
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> itemConstructor) {
        ResourceKey<Item> resourceKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), Constants.id(name));
        return Registry.register(BuiltInRegistries.ITEM, resourceKey, itemConstructor.apply(new Item.Properties().setId(resourceKey)));
    }

    /**
     * Initialization of a mod instance.
     */
    @Override
    public void onInitialize() {
        CommonLoader.init();
        //Register item in the creative tab.
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(COPPER_SHEARS);
        });
        //register dispense behavior
        DispenserBlock.registerBehavior(COPPER_SHEARS, new ShearsDispenseItemBehavior());
    }

}
