package slimeknights.mantle.recipe.data;

import io.github.fabricators_of_create.porting_lib.tags.Tags;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.MetalItemObject;
import slimeknights.mantle.registration.object.WallBuildingBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject;

/** Crafting helper for common recipe types, like stairs, slabs, and packing. */
@SuppressWarnings("unused")
public interface ICommonRecipeHelper extends IRecipeHelper {
  /* Metals */

  default void packingRecipe(RecipeOutput output, RecipeCategory category, String largeName, ItemLike large, String smallName, ItemLike small, String folder) {
    ResourceLocation largeId = id(large);
    ShapedRecipeBuilder.shaped(category, large)
      .define('#', small)
      .pattern("###").pattern("###").pattern("###")
      .unlockedBy("has_item", RecipeProvider.has(small))
      .group(largeId.toString())
      .save(output, wrap(largeId, folder, String.format("_from_%ss", smallName)));

    ResourceLocation smallId = id(small);
    ShapelessRecipeBuilder.shapeless(category, small, 9)
      .requires(large)
      .unlockedBy("has_item", RecipeProvider.has(large))
      .group(smallId.toString())
      .save(output, wrap(smallId, folder, String.format("_from_%s", largeName)));
  }

  default void packingRecipe(RecipeOutput output, RecipeCategory category, String largeName, ItemLike largeItem, String smallName, ItemLike smallItem, TagKey<Item> smallTag, String folder) {
    ResourceLocation largeId = id(largeItem);
    ShapedRecipeBuilder.shaped(category, largeItem)
      .define('#', smallTag)
      .define('*', smallItem)
      .pattern("###").pattern("#*#").pattern("###")
      .unlockedBy("has_item", RecipeProvider.has(smallItem))
      .group(largeId.toString())
      .save(output, wrap(largeId, folder, String.format("_from_%ss", smallName)));

    ResourceLocation smallId = id(smallItem);
    ShapelessRecipeBuilder.shapeless(category, smallItem, 9)
      .requires(largeItem)
      .unlockedBy("has_item", RecipeProvider.has(largeItem))
      .group(smallId.toString())
      .save(output, wrap(smallId, folder, String.format("_from_%s", largeName)));
  }

  default void metalCrafting(RecipeOutput output, MetalItemObject metal, String folder) {
    ItemLike ingot = metal.getIngot();
    packingRecipe(output, RecipeCategory.MISC, "block", metal.get(), "ingot", ingot, metal.getIngotTag(), folder);
    packingRecipe(output, RecipeCategory.MISC, "ingot", ingot, "nugget", metal.getNugget(), metal.getNuggetTag(), folder);
  }

  /* Building blocks */

  default void slabStairsCrafting(RecipeOutput output, BuildingBlockObject building, String folder, boolean addStonecutter) {
    Item item = building.asItem();
    ResourceLocation itemId = id(item);
    Criterion<?> hasBlock = RecipeProvider.has(item);

    ItemLike slab = building.getSlab();
    ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, slab, 6)
      .define('B', item).pattern("BBB")
      .unlockedBy("has_item", hasBlock)
      .group(id(slab).toString())
      .save(output, wrap(itemId, folder, "_slab"));

    ItemLike stairs = building.getStairs();
    ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, stairs, 4)
      .define('B', item).pattern("B  ").pattern("BB ").pattern("BBB")
      .unlockedBy("has_item", hasBlock)
      .group(id(stairs).toString())
      .save(output, wrap(itemId, folder, "_stairs"));

    if (addStonecutter) {
      Ingredient ingredient = Ingredient.of(item);
      SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, slab, 2)
        .unlockedBy("has_item", hasBlock)
        .save(output, wrap(itemId, folder, "_slab_stonecutter"));
      SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, stairs)
        .unlockedBy("has_item", hasBlock)
        .save(output, wrap(itemId, folder, "_stairs_stonecutter"));
    }
  }

  default void stairSlabWallCrafting(RecipeOutput output, WallBuildingBlockObject building, String folder, boolean addStonecutter) {
    slabStairsCrafting(output, building, folder, addStonecutter);
    Item item = building.asItem();
    ResourceLocation itemId = id(item);
    Criterion<?> hasBlock = RecipeProvider.has(item);
    ItemLike wall = building.getWall();
    ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, wall, 6)
      .define('B', item).pattern("BBB").pattern("BBB")
      .unlockedBy("has_item", hasBlock)
      .group(id(wall).toString())
      .save(output, wrap(itemId, folder, "_wall"));
    if (addStonecutter) {
      Ingredient ingredient = Ingredient.of(item);
      SingleItemRecipeBuilder.stonecutting(ingredient, RecipeCategory.BUILDING_BLOCKS, wall)
        .unlockedBy("has_item", hasBlock)
        .save(output, wrap(itemId, folder, "_wall_stonecutter"));
    }
  }

  default void woodCrafting(RecipeOutput output, WoodBlockObject wood, String folder) {
    Criterion<?> hasPlanks = RecipeProvider.has(wood);

    ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, wood, 4)
      .requires(wood.getLogItemTag())
      .group("planks")
      .unlockedBy("has_log", RecipeProvider.inventoryTrigger(ItemPredicate.Builder.item().of(wood.getLogItemTag()).build()))
      .save(output, location(folder + "planks"));

    ItemLike slab = wood.getSlab();
    ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, slab, 6)
      .define('#', wood).pattern("###")
      .unlockedBy("has_planks", hasPlanks).group("wooden_slab")
      .save(output, location(folder + "slab"));

    ItemLike stairs = wood.getStairs();
    ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, stairs, 4)
      .define('#', wood).pattern("#  ").pattern("## ").pattern("###")
      .unlockedBy("has_planks", hasPlanks).group("wooden_stairs")
      .save(output, location(folder + "stairs"));

    ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, wood.getWood(), 3)
      .define('#', wood.getLog()).pattern("##").pattern("##")
      .group("bark").unlockedBy("has_log", RecipeProvider.has(wood.getLog()))
      .save(output, location(folder + "log_to_wood"));
    ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, wood.getStrippedWood(), 3)
      .define('#', wood.getStrippedLog()).pattern("##").pattern("##")
      .group("bark").unlockedBy("has_log", RecipeProvider.has(wood.getStrippedLog()))
      .save(output, location(folder + "stripped_log_to_wood"));

    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, wood.getFence(), 3)
      .define('#', Tags.Items.RODS_WOODEN).define('W', wood)
      .pattern("W#W").pattern("W#W")
      .group("wooden_fence").unlockedBy("has_planks", hasPlanks)
      .save(output, location(folder + "fence"));
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, wood.getFenceGate())
      .define('#', Items.STICK).define('W', wood)
      .pattern("#W#").pattern("#W#")
      .group("wooden_fence_gate").unlockedBy("has_planks", hasPlanks)
      .save(output, location(folder + "fence_gate"));
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, wood.getDoor(), 3)
      .define('#', wood).pattern("##").pattern("##").pattern("##")
      .group("wooden_door").unlockedBy("has_planks", hasPlanks)
      .save(output, location(folder + "door"));
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, wood.getTrapdoor(), 2)
      .define('#', wood).pattern("###").pattern("###")
      .group("wooden_trapdoor").unlockedBy("has_planks", hasPlanks)
      .save(output, location(folder + "trapdoor"));

    ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, wood.getButton())
      .requires(wood).group("wooden_button")
      .unlockedBy("has_planks", hasPlanks)
      .save(output, location(folder + "button"));
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, wood.getPressurePlate())
      .define('#', wood).pattern("##")
      .group("wooden_pressure_plate").unlockedBy("has_planks", hasPlanks)
      .save(output, location(folder + "pressure_plate"));

    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, wood.getSign(), 3)
      .group("sign").define('#', wood).define('X', Tags.Items.RODS_WOODEN)
      .pattern("###").pattern("###").pattern(" X ")
      .unlockedBy("has_planks", RecipeProvider.has(wood))
      .save(output, location(folder + "sign"));
    ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, wood.getHangingSign(), 6)
      .group("hanging_sign").define('#', wood.getStrippedLog()).define('X', Items.CHAIN)
      .pattern("X X").pattern("###").pattern("###")
      .unlockedBy("has_stripped_logs", RecipeProvider.has(wood.getStrippedLog()))
      .save(output, location(folder + "hanging_sign"));
  }
}
