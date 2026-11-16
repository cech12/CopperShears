package de.cech12.coppershears.gametest.tests;

import de.cech12.coppershears.gametest.GameTestPlatform;
import de.cech12.coppershears.gametest.TestUtils;
import de.cech12.coppershears.gametest.CopperShearsGameTests;
import de.cech12.coppershears.platform.Services;
import de.cech12.coppershears.platform.services.IConfigHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Config option "durability". Every test runs in a test environment that sets the config value for its batch
 * (see test_environment jsons and DurabilityConfigEnvironment).
 */
public final class DurabilityTests {

    private DurabilityTests() {}

    public static void register(CopperShearsGameTests.TestCollector tests) {
        tests.add("durability/default_value", DurabilityTests::defaultValue);
        tests.add("durability/custom_value", DurabilityTests::customValue);
        tests.add("durability/maximum_value", DurabilityTests::maximumValue);
        tests.add("durability/above_maximum_is_limited", DurabilityTests::aboveMaximumIsLimited);
        tests.add("durability/zero_makes_shears_unbreakable", DurabilityTests::zeroMakesShearsUnbreakable);
        tests.add("durability/below_zero_makes_shears_unbreakable", DurabilityTests::zeroMakesShearsUnbreakable);
        tests.add("durability/shears_break_when_used_up", DurabilityTests::shearsBreakWhenUsedUp);
        tests.add("durability/config_change_applies_to_existing_shears", DurabilityTests::configChangeAppliesToExistingShears);
    }

    /** Default durability as documented (README, TESTING.md, config description). */
    private static final int DOCUMENTED_DEFAULT = 181;

    private static void assertMaxDamage(GameTestHelper helper, ItemStack stack, int expected) {
        helper.assertTrue(stack.isDamageableItem(), "copper shears are not damageable");
        helper.assertValueEqual(stack.getMaxDamage(), expected, "max damage of copper shears");
    }

    private static void assertUnbreakable(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(!stack.isDamageableItem(), "copper shears are damageable although durability is 0");
        helper.assertTrue(!stack.has(DataComponents.MAX_DAMAGE), "copper shears have a max damage although durability is 0");
    }

    /** Environment "default": durability 181 (default config value). */
    public static void defaultValue(GameTestHelper helper) {
        // the documented default is 181; it is read from the config definition of the loader, not from the environment
        helper.assertValueEqual(IConfigHelper.DURABILITY_DEFAULT, DOCUMENTED_DEFAULT, "default durability constant");
        helper.assertValueEqual(GameTestPlatform.INSTANCE.defaultDurability(), DOCUMENTED_DEFAULT, "default durability of the config definition");
        helper.assertValueEqual(Services.CONFIG.getDurability(), DOCUMENTED_DEFAULT, "configured durability");
        assertMaxDamage(helper, TestUtils.copperShears(), DOCUMENTED_DEFAULT);
        helper.succeed();
    }

    /** Environment "durability_200": a custom durability is used as max damage. */
    public static void customValue(GameTestHelper helper) {
        assertMaxDamage(helper, TestUtils.copperShears(), 200);
        helper.succeed();
    }

    /** Environment "durability_10000": the maximal allowed durability. */
    public static void maximumValue(GameTestHelper helper) {
        assertMaxDamage(helper, TestUtils.copperShears(), IConfigHelper.DURABILITY_MAX);
        helper.succeed();
    }

    /** Environment "durability_20000": values above the maximum are limited to 10000. */
    public static void aboveMaximumIsLimited(GameTestHelper helper) {
        helper.assertValueEqual(Services.CONFIG.getDurability(), IConfigHelper.DURABILITY_MAX, "configured durability");
        assertMaxDamage(helper, TestUtils.copperShears(), IConfigHelper.DURABILITY_MAX);
        helper.succeed();
    }

    /**
     * Environments "durability_0" and "durability_negative" (-1): durability 0 (or less) deactivates the durability.
     * The shears are not damaged by usage.
     */
    public static void zeroMakesShearsUnbreakable(GameTestHelper helper) {
        helper.assertValueEqual(Services.CONFIG.getDurability(), 0, "configured durability");
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.copperShears());
        ItemStack stack = player.getMainHandItem();
        assertUnbreakable(helper, stack);
        stack.hurtAndBreak(10, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(!player.getMainHandItem().isEmpty(), "unbreakable copper shears broke");
        TestUtils.assertDamage(helper, player.getMainHandItem(), 0, "after usage");
        helper.succeed();
    }

    /** Environment "default": shears break after 181 uses. */
    public static void shearsBreakWhenUsedUp(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, TestUtils.copperShears());
        int uses = GameTestPlatform.INSTANCE.defaultDurability();
        helper.assertValueEqual(uses, DOCUMENTED_DEFAULT, "default durability of the config definition");
        player.getMainHandItem().hurtAndBreak(uses - 1, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(!player.getMainHandItem().isEmpty(), "copper shears broke too early");
        player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        helper.assertTrue(player.getMainHandItem().isEmpty(), "copper shears did not break after " + uses + " uses");
        helper.succeed();
    }

    /**
     * Environment "durability_dynamic" (own batch, because the test changes the config while running):
     * The durability is not stored in the item stack, so config changes apply to already existing shears.
     */
    public static void configChangeAppliesToExistingShears(GameTestHelper helper) {
        ItemStack stack = TestUtils.copperShears();
        stack.setDamageValue(10);
        GameTestPlatform.INSTANCE.setDurability(100);
        assertMaxDamage(helper, stack, 100);
        TestUtils.assertDamage(helper, stack, 10, "after config change");
        GameTestPlatform.INSTANCE.setDurability(0);
        helper.assertTrue(!stack.isDamageableItem(), "existing copper shears are still damageable after durability was set to 0");
        GameTestPlatform.INSTANCE.setDurability(IConfigHelper.DURABILITY_DEFAULT);
        assertMaxDamage(helper, stack, IConfigHelper.DURABILITY_DEFAULT);
        helper.succeed();
    }

}
