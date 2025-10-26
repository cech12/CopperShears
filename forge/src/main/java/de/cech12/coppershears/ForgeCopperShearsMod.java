package de.cech12.coppershears;

import de.cech12.coppershears.item.CopperShearsItem;
import net.minecraft.core.dispenser.ShearsDispenseItemBehavior;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Mod class for the Forge loader.
 */
@SuppressWarnings("unused")
@Mod(Constants.MOD_ID)
@Mod.EventBusSubscriber(modid= Constants.MOD_ID, bus= Mod.EventBusSubscriber.Bus.MOD)
public class ForgeCopperShearsMod {

    /** mod specific item registry */
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    static {
        Constants.COPPER_SHEARS = ITEMS.register("copper_shears", CopperShearsItem::new);
    }

    /**
     * Constructor of a mod instance.
     */
    public ForgeCopperShearsMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        CommonLoader.init();
    }

    /**
     * Registers the dispense behaviour of the shears.
     * @param event FMLCommonSetupEvent
     */
    @SubscribeEvent
    public static void registerDispenseBehavior(FMLCommonSetupEvent event) {
        DispenserBlock.registerBehavior(Constants.COPPER_SHEARS.get(), new ShearsDispenseItemBehavior());
    }

    /**
     * Registers the creative tabs for all items.
     * @param event BuildCreativeModeTabContentsEvent
     */
    @SubscribeEvent
    public static void addItemsToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(Constants.COPPER_SHEARS);
        }
    }

}
