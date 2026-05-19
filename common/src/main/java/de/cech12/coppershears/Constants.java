package de.cech12.coppershears;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private Constants() {}

    /**
     * Static method to create an Identifier for this mod.
     * @param name name/path of the Identifier
     * @return Identifier object
     */
    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

}