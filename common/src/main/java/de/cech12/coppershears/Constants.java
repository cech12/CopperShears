package de.cech12.coppershears;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Class that contains all common constants.
 */
public class Constants {

    /** mod id */
    public static final String MOD_ID = "coppershears";
    /** mod name*/
    public static final String MOD_NAME = "Copper Shears";
    /** Logger instance */
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    /** Supplier of registered copper shears item */
    public static Supplier<Item> COPPER_SHEARS;

    private Constants() {}

    /**
     * Static method to create a ResourceLocation for this mod.
     * @param name name/path of the ResourceLocation
     * @return ResourceLocation object
     */
    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
    }

}