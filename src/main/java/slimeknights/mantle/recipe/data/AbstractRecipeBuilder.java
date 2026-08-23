package slimeknights.mantle.recipe.data;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/** Common logic to create a recipe builder class. */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class AbstractRecipeBuilder<T extends AbstractRecipeBuilder<T>> {
  /** Criteria for this recipe's advancement. */
  protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
  /** Group for this recipe. */
  @Nonnull
  protected String group = "";

  /** Adds a criterion to the recipe. */
  @SuppressWarnings("unchecked")
  public T unlockedBy(String name, Criterion<?> criterion) {
    this.criteria.put(name, criterion);
    return (T) this;
  }

  /** Sets the group for this recipe. */
  @SuppressWarnings("unchecked")
  public T group(String group) {
    this.group = group;
    return (T) this;
  }

  /** Sets the group for this recipe from a resource location. */
  public T group(ResourceLocation group) {
    if ("minecraft".equals(group.getNamespace())) {
      return group(group.getPath());
    }
    return group(group.toString());
  }

  /** Builds the recipe with its default recipe ID. */
  public abstract void save(RecipeOutput recipeOutput);

  /** Builds the recipe with the supplied ID. */
  public abstract void save(RecipeOutput recipeOutput, ResourceLocation id);

  /** Builds the recipe advancement. */
  private AdvancementHolder buildAdvancementInternal(RecipeOutput recipeOutput, ResourceLocation id, String folder) {
    Advancement.Builder builder = recipeOutput.advancement()
      .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
      .rewards(AdvancementRewards.Builder.recipe(id))
      .requirements(AdvancementRequirements.Strategy.OR);
    criteria.forEach(builder::addCriterion);
    return builder.build(id.withPrefix("recipes/" + folder + "/"));
  }

  /** Builds and validates the advancement for this recipe. */
  protected AdvancementHolder buildAdvancement(RecipeOutput recipeOutput, ResourceLocation id, String folder) {
    if (criteria.isEmpty()) {
      throw new IllegalStateException("No way of obtaining recipe " + id);
    }
    return buildAdvancementInternal(recipeOutput, id, folder);
  }

  /** Builds an advancement only when criteria were supplied. */
  @Nullable
  protected AdvancementHolder buildOptionalAdvancement(RecipeOutput recipeOutput, ResourceLocation id, String folder) {
    if (criteria.isEmpty()) {
      return null;
    }
    return buildAdvancementInternal(recipeOutput, id, folder);
  }

  /** Saves a recipe directly through the 1.21 recipe output. */
  protected void saveRecipe(RecipeOutput recipeOutput, ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
    recipeOutput.accept(id, recipe, advancement);
  }
}
