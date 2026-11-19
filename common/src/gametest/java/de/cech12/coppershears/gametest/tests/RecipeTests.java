package de.cech12.coppershears.gametest.tests;

import de.cech12.coppershears.Constants;
import de.cech12.coppershears.gametest.TestUtils;
import de.cech12.coppershears.gametest.CopperShearsGameTests;
import de.cech12.coppershears.platform.Services;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Crafting recipe, smelting and blasting to copper nuggets, recipe unlock advancements and repairing by combining two
 * copper shears.
 */
public final class RecipeTests {

    private static final ResourceKey<Recipe<?>> RECIPE = recipeKey("copper_shears");
    private static final ResourceKey<Recipe<?>> SMELTING_RECIPE = recipeKey("copper_nugget_from_smelting");
    private static final ResourceKey<Recipe<?>> BLASTING_RECIPE = recipeKey("copper_nugget_from_blasting");
    /** Common tag of copper ingots, used by the recipe. */
    private static final TagKey<Item> COPPER_INGOTS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/copper"));
    /** Remaining durability of the two shears that are combined by the repair tests (before the bonus). */
    private static final int FIRST_REMAINING = 30;
    private static final int SECOND_REMAINING = 40;

    private RecipeTests() {}

    public static void register(CopperShearsGameTests.TestCollector tests) {
        tests.add("recipe/crafted_from_two_diagonal_copper_ingots", RecipeTests::craftedFromTwoDiagonalCopperIngots);
        tests.add("recipe/not_crafted_from_two_copper_ingots_in_a_row", RecipeTests::notCraftedFromTwoCopperIngotsInARow);
        tests.add("recipe/is_in_equipment_category", RecipeTests::isInEquipmentCategory);
        tests.add("recipe/unlocked_by_having_copper_ingots", RecipeTests::unlockedByHavingCopperIngots);
        tests.add("recipe/smelted_to_copper_nugget", RecipeTests::smeltedToCopperNugget);
        tests.add("recipe/furnace_smelts_damaged_shears", RecipeTests::furnaceSmeltsDamagedShears);
        tests.add("recipe/blast_furnace_smelts_damaged_shears", RecipeTests::blastFurnaceSmeltsDamagedShears);
        tests.add("recipe/smelting_unlocked_by_having_copper_shears", RecipeTests::smeltingUnlockedByHavingCopperShears);
        tests.add("repair/combines_two_damaged_shears", RecipeTests::combinesTwoDamagedShears);
        tests.add("repair/grindstone_combines_two_damaged_shears", RecipeTests::grindstoneCombinesTwoDamagedShears);
        tests.add("repair/grindstone_result_follows_config", RecipeTests::grindstoneResultFollowsConfig);
        tests.add("repair/anvil_combines_two_damaged_shears", RecipeTests::anvilCombinesTwoDamagedShears);
        tests.add("repair/fully_repaired_shears_equal_new_shears", RecipeTests::fullyRepairedShearsEqualNewShears);
        tests.add("repair/anvil_fully_repaired_shears_like_vanilla_shears", RecipeTests::anvilFullyRepairedShearsLikeVanillaShears);
        tests.add("repair/grindstone_disenchants_single_shears", RecipeTests::grindstoneDisenchantsSingleShears);
        tests.add("repair/no_repair_material_like_vanilla_shears", RecipeTests::noRepairMaterialLikeVanillaShears);
        tests.add("repair/not_possible_when_unbreakable", RecipeTests::notPossibleWhenUnbreakable);
    }

    private static ResourceKey<Recipe<?>> recipeKey(String name) {
        return ResourceKey.create(Registries.RECIPE, Constants.id(name));
    }

    private static Optional<RecipeHolder<CraftingRecipe>> findRecipe(GameTestHelper helper, CraftingInput input) {
        return helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
    }

    private static ItemStack craft(GameTestHelper helper, CraftingInput input) {
        return findRecipe(helper, input).map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    /** The recipe with the given key, with a clear message if it is missing. */
    private static Recipe<?> recipe(GameTestHelper helper, ResourceKey<Recipe<?>> key) {
        var recipe = helper.getLevel().recipeAccess().byKey(key);
        helper.assertTrue(recipe.isPresent(), "recipe " + key.identifier() + " is missing");
        return recipe.get().value();
    }

    /**
     * Two copper ingots (every item of #c:ingots/copper) placed diagonally (in both directions) craft one pair of
     * copper shears.
     */
    public static void craftedFromTwoDiagonalCopperIngots(GameTestHelper helper) {
        var ingots = BuiltInRegistries.ITEM.getTagOrEmpty(COPPER_INGOTS);
        int count = 0;
        for (var holder : ingots) {
            ItemStack ingot = new ItemStack(holder);
            for (List<ItemStack> grid : List.of(
                    List.of(ItemStack.EMPTY, ingot, ingot, ItemStack.EMPTY),
                    List.of(ingot, ItemStack.EMPTY, ItemStack.EMPTY, ingot))) {
                ItemStack result = craft(helper, CraftingInput.of(2, 2, grid));
                helper.assertTrue(result.is(TestUtils.copperShearsItem()) && result.getCount() == 1,
                        "two diagonal " + holder.getRegisteredName() + " do not craft copper shears but " + result);
            }
            count++;
        }
        helper.assertTrue(new ItemStack(Items.COPPER_INGOT).is(COPPER_INGOTS), "minecraft:copper_ingot is not in #c:ingots/copper");
        helper.assertTrue(count > 0, "no copper ingots found in #c:ingots/copper");
        helper.succeed();
    }

    /**
     * Negative cases: two copper ingots next to or above each other, and two diagonal copper nuggets or iron ingots do
     * not match the copper shears recipe. Checks the recipe itself, because another recipe could win the recipe lookup.
     */
    public static void notCraftedFromTwoCopperIngotsInARow(GameTestHelper helper) {
        Recipe<?> recipe = recipe(helper, RECIPE);
        helper.assertTrue(recipe instanceof CraftingRecipe, "recipe " + RECIPE.identifier() + " is no crafting recipe");
        @SuppressWarnings("unchecked")
        Recipe<CraftingInput> craftingRecipe = (Recipe<CraftingInput>) recipe;
        ItemStack ingot = new ItemStack(Items.COPPER_INGOT);
        for (CraftingInput input : List.of(CraftingInput.of(2, 1, List.of(ingot, ingot)), CraftingInput.of(1, 2, List.of(ingot, ingot)))) {
            helper.assertTrue(!craftingRecipe.matches(input, helper.getLevel()),
                    "two copper ingots in a " + input.width() + "x" + input.height() + " grid match the copper shears recipe");
        }
        for (ItemStack other : List.of(new ItemStack(Items.COPPER_NUGGET), new ItemStack(Items.IRON_INGOT))) {
            CraftingInput input = CraftingInput.of(2, 2, List.of(ItemStack.EMPTY, other, other, ItemStack.EMPTY));
            helper.assertTrue(!craftingRecipe.matches(input, helper.getLevel()), "two diagonal " + other + " match the copper shears recipe");
        }
        helper.succeed();
    }

    /** WoodenShears#27: the recipe is shown in the equipment category of the recipe book. */
    public static void isInEquipmentCategory(GameTestHelper helper) {
        Recipe<?> recipe = recipe(helper, RECIPE);
        helper.assertTrue(recipe instanceof CraftingRecipe, "recipe is no crafting recipe");
        helper.assertValueEqual(((CraftingRecipe) recipe).category(), CraftingBookCategory.EQUIPMENT, "recipe category");
        helper.succeed();
    }

    /**
     * Checks a recipe advancement: it unlocks the recipe as soon as the player has the given item (inventory_changed
     * criterion) or already knows the recipe (has_the_recipe criterion).
     */
    private static void assertRecipeAdvancement(GameTestHelper helper, String name, ResourceKey<Recipe<?>> recipe, String criterionName, Item item) {
        AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(Constants.id(name));
        helper.assertTrue(advancement != null, "recipe advancement " + name + " is missing");
        helper.assertTrue(advancement.value().rewards().recipes().contains(recipe), name + " does not unlock the recipe " + recipe.identifier());
        var hasItem = advancement.value().criteria().get(criterionName);
        helper.assertTrue(hasItem != null, name + " has no " + criterionName + " criterion");
        helper.assertTrue(hasItem.trigger() == CriteriaTriggers.INVENTORY_CHANGED, criterionName + " of " + name + " is no inventory_changed trigger");
        helper.assertTrue(hasItem.triggerInstance() instanceof InventoryChangeTrigger.TriggerInstance inventory
                        && inventory.items().size() == 1 && inventory.items().getFirst().test(new ItemStack(item))
                        && !inventory.items().getFirst().test(new ItemStack(Items.COPPER_NUGGET)),
                criterionName + " of " + name + " does not check for " + BuiltInRegistries.ITEM.getKey(item));
        var hasTheRecipe = advancement.value().criteria().get("has_the_recipe");
        helper.assertTrue(hasTheRecipe != null, name + " has no has_the_recipe criterion");
        var ownRecipe = helper.getLevel().recipeAccess().byKey(recipe);
        helper.assertTrue(ownRecipe.isPresent(), "recipe " + recipe.identifier() + " is missing");
        helper.assertTrue(hasTheRecipe.triggerInstance() instanceof RecipeUnlockedTrigger.TriggerInstance unlocked && unlocked.matches(ownRecipe.get()),
                "has_the_recipe of " + name + " does not check for its own recipe " + recipe.identifier());
        // either criterion alone completes the advancement (OR), so having the item is enough
        var requirements = advancement.value().requirements();
        helper.assertTrue(requirements.test(criterionName::equals), name + " is not completed by " + criterionName + " alone: " + requirements);
        helper.assertTrue(requirements.test("has_the_recipe"::equals), name + " is not completed by has_the_recipe alone: " + requirements);
        helper.assertTrue(!requirements.test(criterion -> false), name + " is completed without any criterion: " + requirements);
    }

    /** The recipe is unlocked by an advancement as soon as a player has copper ingots. */
    public static void unlockedByHavingCopperIngots(GameTestHelper helper) {
        assertRecipeAdvancement(helper, "recipes/tools/copper_shears", RECIPE, "has_copper_ingot", Items.COPPER_INGOT);
        helper.succeed();
    }

    /**
     * Like vanilla copper tools, copper shears can be smelted in a furnace (200 ticks) and in a blast furnace (100
     * ticks) to one copper nugget with 0.1 experience.
     */
    public static void smeltedToCopperNugget(GameTestHelper helper) {
        SingleRecipeInput input = new SingleRecipeInput(TestUtils.copperShears());
        for (var entry : List.of(
                Map.entry(RecipeType.SMELTING, Map.entry(SMELTING_RECIPE, 200)),
                Map.entry(RecipeType.BLASTING, Map.entry(BLASTING_RECIPE, 100)))) {
            @SuppressWarnings("unchecked")
            RecipeType<AbstractCookingRecipe> type = (RecipeType<AbstractCookingRecipe>) (RecipeType<?>) entry.getKey();
            var recipe = helper.getLevel().recipeAccess().getRecipeFor(type, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent(), "copper shears cannot be cooked with " + type);
            helper.assertValueEqual(recipe.get().id(), entry.getValue().getKey(), "recipe of " + type);
            AbstractCookingRecipe cooking = recipe.get().value();
            ItemStack result = cooking.assemble(input);
            helper.assertTrue(result.is(Items.COPPER_NUGGET) && result.getCount() == 1, type + " of copper shears results in " + result);
            helper.assertValueEqual(cooking.cookingTime(), entry.getValue().getValue(), "cooking time of " + type);
            helper.assertValueEqual(cooking.experience(), 0.1F, "experience of " + type);
            helper.assertValueEqual(cooking.category(), CookingBookCategory.MISC, "category of " + type);
        }
        helper.succeed();
    }

    /** Puts damaged and enchanted copper shears with coal into a furnace block and waits for the copper nugget. */
    private static void smeltsDamagedShears(GameTestHelper helper, Block furnaceBlock) {
        BlockPos pos = TestUtils.CENTER;
        helper.setBlock(pos, furnaceBlock);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos, AbstractFurnaceBlockEntity.class);
        ItemStack shears = TestUtils.copperShears();
        shears.setDamageValue(shears.getMaxDamage() / 2);
        shears.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        furnace.setItem(0, shears);
        furnace.setItem(1, new ItemStack(Items.COAL));
        helper.succeedWhen(() -> {
            ItemStack output = furnace.getItem(2);
            helper.assertTrue(output.is(Items.COPPER_NUGGET) && output.getCount() == 1, "output of the " + furnaceBlock + " is " + output);
            helper.assertTrue(furnace.getItem(0).isEmpty(), "the copper shears were not consumed");
        });
    }

    /** A real furnace smelts damaged and enchanted copper shears to a copper nugget. */
    public static void furnaceSmeltsDamagedShears(GameTestHelper helper) {
        smeltsDamagedShears(helper, Blocks.FURNACE);
    }

    /** A real blast furnace smelts damaged and enchanted copper shears to a copper nugget. */
    public static void blastFurnaceSmeltsDamagedShears(GameTestHelper helper) {
        smeltsDamagedShears(helper, Blocks.BLAST_FURNACE);
    }

    /** The smelting and blasting recipes are unlocked as soon as a player has copper shears. */
    public static void smeltingUnlockedByHavingCopperShears(GameTestHelper helper) {
        assertRecipeAdvancement(helper, "recipes/misc/copper_nugget_from_smelting", SMELTING_RECIPE, "has_copper_shears", TestUtils.copperShearsItem());
        assertRecipeAdvancement(helper, "recipes/misc/copper_nugget_from_blasting", BLASTING_RECIPE, "has_copper_shears", TestUtils.copperShearsItem());
        helper.succeed();
    }

    /**
     * WoodenShears#38: two damaged copper shears can be combined in the crafting grid. The result has the remaining durability of
     * both plus a 5% bonus. The max damage is not stored in the result, so it still follows the config.
     */
    public static void combinesTwoDamagedShears(GameTestHelper helper) {
        int max = Services.CONFIG.getDurability(); // set by the test environment "default"
        ItemStack first = TestUtils.copperShears();
        first.setDamageValue(max - FIRST_REMAINING);
        ItemStack second = TestUtils.copperShears();
        second.setDamageValue(max - SECOND_REMAINING);
        ItemStack result = craft(helper, CraftingInput.of(2, 1, List.of(first, second)));
        helper.assertTrue(result.is(TestUtils.copperShearsItem()), "two damaged copper shears cannot be combined: " + result);
        int remaining = FIRST_REMAINING + SECOND_REMAINING + max * 5 / 100;
        TestUtils.assertDamage(helper, result, max - remaining, "after repair");
        helper.assertTrue(result.getComponentsPatch().forget(type -> type != DataComponents.MAX_DAMAGE).isEmpty(), "repaired copper shears store a fixed max damage");
        helper.succeed();
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** Puts two items into the menu inputs (slots 0 and 1) and returns the output (slot 2). */
    private static ItemStack combine(AbstractContainerMenu menu, ItemStack first, ItemStack second) {
        menu.getSlot(0).set(first);
        menu.getSlot(1).set(second);
        if (menu instanceof AnvilMenu anvil) {
            anvil.createResult();
        }
        return menu.getSlot(2).getItem();
    }

    /**
     * Like vanilla shears, two damaged copper shears can be combined in a grindstone: the remaining durability of both
     * plus a 5% bonus, enchantments (except curses) are removed.
     */
    public static void grindstoneCombinesTwoDamagedShears(GameTestHelper helper) {
        int max = Services.CONFIG.getDurability(); // set by the test environment "default"
        ItemStack result = combineInGrindstone(helper);
        helper.assertTrue(result.is(TestUtils.copperShearsItem()), "two damaged copper shears cannot be combined in a grindstone: " + result);
        int remaining = FIRST_REMAINING + SECOND_REMAINING + max * 5 / 100;
        TestUtils.assertDamage(helper, result, Math.max(0, max - remaining), "after combining in a grindstone");
        helper.assertTrue(result.getEnchantments().isEmpty(), "grindstone did not remove the enchantments: " + result.getEnchantments());
        helper.succeed();
    }

    /**
     * Shears combined in a grindstone do not store a fixed max damage, so config changes still apply to them (like the
     * crafting repair, WoodenShears#38). Fixed WoodenShears#51.
     */
    public static void grindstoneResultFollowsConfig(GameTestHelper helper) {
        ItemStack result = combineInGrindstone(helper);
        helper.assertTrue(result.is(TestUtils.copperShearsItem()), "two damaged copper shears cannot be combined in a grindstone: " + result);
        helper.assertTrue(result.getComponentsPatch().forget(type -> type != DataComponents.MAX_DAMAGE).isEmpty(),
                "copper shears combined in a grindstone store a fixed max damage: " + result.getComponentsPatch());
        helper.assertValueEqual(result.getMaxDamage(), Services.CONFIG.getDurability(), "max damage of the combined shears");
        helper.succeed();
    }

    /** Combines two damaged copper shears (30 and 40 remaining durability, the first with Efficiency I) in a grindstone. */
    private static ItemStack combineInGrindstone(GameTestHelper helper) {
        int max = Services.CONFIG.getDurability(); // set by the test environment "default"
        ItemStack first = TestUtils.copperShears();
        first.setDamageValue(max - FIRST_REMAINING);
        first.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        ItemStack second = TestUtils.copperShears();
        second.setDamageValue(max - SECOND_REMAINING);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
        return combine(menu, first, second);
    }

    /**
     * Like vanilla shears, two damaged copper shears can be combined in an anvil: the remaining durability of both plus
     * a 12% bonus, the enchantments of both are kept. The max damage still follows the config.
     */
    public static void anvilCombinesTwoDamagedShears(GameTestHelper helper) {
        int max = Services.CONFIG.getDurability(); // set by the test environment "default"
        ItemStack first = TestUtils.copperShears();
        first.setDamageValue(max - FIRST_REMAINING);
        first.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        ItemStack second = TestUtils.copperShears();
        second.setDamageValue(max - SECOND_REMAINING);
        second.enchant(enchantment(helper, Enchantments.UNBREAKING), 1);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
        ItemStack result = combine(menu, first, second);
        helper.assertTrue(result.is(TestUtils.copperShearsItem()), "two damaged copper shears cannot be combined in an anvil: " + result);
        int remaining = FIRST_REMAINING + SECOND_REMAINING + max * 12 / 100;
        TestUtils.assertDamage(helper, result, Math.max(0, max - remaining), "after combining in an anvil");
        helper.assertTrue(result.getEnchantments().getLevel(enchantment(helper, Enchantments.EFFICIENCY)) == 1
                && result.getEnchantments().getLevel(enchantment(helper, Enchantments.UNBREAKING)) == 1,
                "anvil did not keep the enchantments of both shears: " + result.getEnchantments());
        helper.assertTrue(menu.getCost() > 0, "combining in an anvil costs no experience");
        helper.assertTrue(result.getComponentsPatch().forget(type -> type != DataComponents.MAX_DAMAGE).isEmpty(), "copper shears combined in an anvil store a fixed max damage");
        helper.succeed();
    }

    /** WoodenShears#38: shears repaired to full durability are identical to new shears (no leftover damage component). */
    public static void fullyRepairedShearsEqualNewShears(GameTestHelper helper) {
        ItemStack first = TestUtils.copperShears();
        first.setDamageValue(1);
        ItemStack second = TestUtils.copperShears();
        second.setDamageValue(1);
        ItemStack result = craft(helper, CraftingInput.of(2, 1, List.of(first, second)));
        helper.assertTrue(result.is(TestUtils.copperShearsItem()), "two damaged copper shears cannot be combined: " + result);
        helper.assertTrue(ItemStack.isSameItemSameComponents(result, TestUtils.copperShears()),
                "fully repaired copper shears differ from new ones: " + result.getComponentsPatch());
        helper.succeed();
    }

    private static AnvilMenu anvil(GameTestHelper helper) {
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        return new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
    }

    /**
     * Shears combined to full durability in an anvil have the same stored components as vanilla shears combined the
     * same way (the repair cost, but no leftover damage or max damage).
     */
    public static void anvilFullyRepairedShearsLikeVanillaShears(GameTestHelper helper) {
        List<ItemStack> results = new java.util.ArrayList<>();
        for (ItemStack prototype : List.of(TestUtils.copperShears(), new ItemStack(Items.SHEARS))) {
            ItemStack first = prototype.copy();
            first.setDamageValue(1);
            ItemStack second = prototype.copy();
            second.setDamageValue(1);
            ItemStack result = combine(anvil(helper), first, second);
            helper.assertTrue(result.is(prototype.getItem()) && result.getDamageValue() == 0, "two slightly damaged " + prototype + " are not fully repaired in an anvil: " + result);
            results.add(result);
        }
        var mod = TestUtils.storedComponentTypes(results.get(0));
        var vanilla = TestUtils.storedComponentTypes(results.get(1));
        helper.assertValueEqual(mod, vanilla, "stored components of fully repaired copper shears compared to vanilla shears");
        helper.succeed();
    }

    /**
     * Like vanilla shears, a single enchanted and damaged pair of shears is disenchanted in a grindstone: the damage is
     * kept, no fixed max damage is stored.
     */
    public static void grindstoneDisenchantsSingleShears(GameTestHelper helper) {
        ItemStack shears = TestUtils.copperShears();
        shears.setDamageValue(10);
        shears.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
        ItemStack result = combine(menu, shears, ItemStack.EMPTY);
        helper.assertTrue(result.is(TestUtils.copperShearsItem()), "a single copper shears cannot be disenchanted in a grindstone: " + result);
        helper.assertTrue(result.getEnchantments().isEmpty(), "grindstone did not remove the enchantments: " + result.getEnchantments());
        TestUtils.assertDamage(helper, result, 10, "after disenchanting in a grindstone");
        helper.assertTrue(result.getComponentsPatch().forget(type -> type != DataComponents.MAX_DAMAGE).isEmpty(),
                "disenchanted copper shears store a fixed max damage: " + result.getComponentsPatch());
        helper.succeed();
    }

    /**
     * Like vanilla shears, copper shears have no repair material: an anvil does not repair them with any item (only by
     * combining two shears).
     */
    public static void noRepairMaterialLikeVanillaShears(GameTestHelper helper) {
        helper.assertTrue(java.util.Objects.equals(TestUtils.copperShears().get(DataComponents.REPAIRABLE), new ItemStack(Items.SHEARS).get(DataComponents.REPAIRABLE)),
                "repair material differs from vanilla shears: " + TestUtils.copperShears().get(DataComponents.REPAIRABLE));
        for (var material : List.of(Items.COPPER_INGOT, Items.COPPER_NUGGET, Items.IRON_INGOT)) {
            ItemStack shears = TestUtils.copperShears();
            shears.setDamageValue(20);
            ItemStack result = combine(anvil(helper), shears, new ItemStack(material, 4));
            helper.assertTrue(result.isEmpty(), "copper shears are repaired with " + material + " in an anvil: " + result);
        }
        helper.succeed();
    }

    /** Environment "durability_0": unbreakable copper shears cannot be damaged, so the repair recipe does not apply. */
    public static void notPossibleWhenUnbreakable(GameTestHelper helper) {
        ItemStack result = craft(helper, CraftingInput.of(2, 1, List.of(TestUtils.copperShears(), TestUtils.copperShears())));
        helper.assertTrue(result.isEmpty(), "unbreakable copper shears can be combined: " + result);
        ServerPlayer player = TestUtils.player(helper, GameType.SURVIVAL, ItemStack.EMPTY);
        GrindstoneMenu grindstone = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(TestUtils.CENTER)));
        ItemStack grindstoneResult = combine(grindstone, TestUtils.copperShears(), TestUtils.copperShears());
        helper.assertTrue(grindstoneResult.isEmpty(), "unbreakable copper shears can be combined in a grindstone: " + grindstoneResult);
        // enchanted, so that the anvil would combine them if they were damageable (enchantments of both are merged)
        ItemStack first = TestUtils.copperShears();
        first.enchant(enchantment(helper, Enchantments.EFFICIENCY), 1);
        ItemStack second = TestUtils.copperShears();
        second.enchant(enchantment(helper, Enchantments.UNBREAKING), 1);
        ItemStack anvilResult = combine(anvil(helper), first, second);
        helper.assertTrue(anvilResult.isEmpty(), "unbreakable copper shears can be combined in an anvil: " + anvilResult);
        helper.succeed();
    }

}
