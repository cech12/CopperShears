package de.cech12.coppershears.gametest;

import de.cech12.coppershears.platform.Services;

/**
 * Holds the loader specific {@link IGameTestPlatform} implementation.
 */
public final class GameTestPlatform {

    /** loader specific game test platform */
    public static final IGameTestPlatform INSTANCE = Services.load(IGameTestPlatform.class);

    private GameTestPlatform() {}

}
