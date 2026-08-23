package slimeknights.mantle.recipe;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** All recipe serializers registered under Mantle's namespace. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MantleRecipeSerializers {
  /** Assigned during Mantle initialization after registration. */
  public static RecipeSerializer<?> CRAFTING_SHAPED_FALLBACK;
  /** Assigned during Mantle initialization after registration. */
  public static RecipeSerializer<?> CRAFTING_SHAPED_RETEXTURED;
}
