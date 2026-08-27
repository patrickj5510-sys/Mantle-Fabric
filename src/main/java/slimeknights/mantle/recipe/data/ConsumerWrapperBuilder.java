package slimeknights.mantle.recipe.data;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Builds a recipe output wrapper which adds Fabric resource conditions to generated recipes. */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class ConsumerWrapperBuilder {
  private final List<ResourceCondition> conditions = new ArrayList<>();

  private ConsumerWrapperBuilder() {}

  /** Creates a wrapper builder. */
  public static ConsumerWrapperBuilder wrap() {
    return new ConsumerWrapperBuilder();
  }

  /** Adds a resource condition to generated recipes. */
  public ConsumerWrapperBuilder addCondition(ResourceCondition condition) {
    conditions.add(condition);
    return this;
  }

  /** Builds a RecipeOutput that associates the configured Fabric conditions with every generated recipe. */
  public RecipeOutput build(RecipeOutput output) {
    if (conditions.isEmpty()) {
      return output;
    }
    ResourceCondition[] applied = conditions.toArray(ResourceCondition[]::new);
    return new RecipeOutput() {
      @Override
      public void accept(ResourceLocation id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
        FabricDataGenHelper.addConditions(recipe, applied);
        output.accept(id, recipe, advancement);
      }

      @Override
      public Advancement.Builder advancement() {
        return output.advancement();
      }
    };
  }
}
