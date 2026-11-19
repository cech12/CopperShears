package de.cech12.coppershears.gametest;

import de.cech12.coppershears.platform.FabricConfigHelper;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Thin Fabric adapter for the common game tests. Only part of the gametest source set (separate test mod).
 */
public final class FabricGameTestAdapter implements ModInitializer, IGameTestPlatform {

    @Override
    public void onInitialize() {
        CopperShearsGameTests.registerTestFunctions((id, test) -> Registry.register(BuiltInRegistries.TEST_FUNCTION, id, test));
        CopperShearsGameTests.registerEnvironmentTypes((id, codec) -> Registry.register(BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE, id, codec));
    }

    @Override
    public int setDurability(int durability) {
        FabricConfigHelper config = AutoConfig.getConfigHolder(FabricConfigHelper.class).getConfig();
        int previous = config.DURABILITY;
        config.DURABILITY = durability;
        return previous;
    }

    @Override
    public int defaultDurability() {
        return new FabricConfigHelper().DURABILITY;
    }

}
