package slimeknights.mantle.recipe.crafting;

import lombok.RequiredArgsConstructor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
@RequiredArgsConstructor(staticName = "fromShaped")
public class ShapedRetexturedRecipeBuilder {
  private final ShapedRecipeBuilder parent;
  private Ingredient texture;
  private boolean matchAll;

  /** Sets the texture source to the given ingredient. */
  public ShapedRetexturedRecipeBuilder setSource(Ingredient texture) {
    this.texture = texture;
    return this;
  }

  /** Sets the texture source to the given tag. */
  public ShapedRetexturedRecipeBuilder setSource(TagKey<Item> tag) {
    this.texture = Ingredient.of(tag);
    return this;
  }

  /**
   * Sets the match-all property on the recipe. If set, all items matching the texture ingredient
   * must resolve to the same texture or no texture is applied.
   */
  public ShapedRetexturedRecipeBuilder setMatchAll() {
    this.matchAll = true;
    return this;
  }

  /** Builds the recipe with the default name. */
  public void build(RecipeOutput output) {
    validate();
    parent.save(wrapOutput(output));
  }

  /** Builds the recipe using the given location. */
  public void build(RecipeOutput output, ResourceLocation location) {
    validate();
    parent.save(wrapOutput(output), location);
  }

  private void validate() {
    if (texture == null) {
      throw new IllegalStateException("No texture defined for texture recipe");
    }
  }

  /** Creates an output wrapper that converts the generated shaped recipe into Mantle's retextured recipe. */
  private RecipeOutput wrapOutput(RecipeOutput original) {
    Ingredient source = this.texture;
    boolean all = this.matchAll;
    return new RecipeOutput() {
      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
        original.accept(id, new ShapedRetexturedRecipe((ShapedRecipe) recipe, source, all), advancement);
      }

      @Override
      public Advancement.Builder advancement() {
        return original.advancement();
      }
    };
  }
}
