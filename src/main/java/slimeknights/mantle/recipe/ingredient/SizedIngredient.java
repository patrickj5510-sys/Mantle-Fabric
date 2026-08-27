package slimeknights.mantle.recipe.ingredient;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.common.IngredientLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Extension of the vanilla ingredient to make stack size checks. */
public class SizedIngredient implements Predicate<ItemStack> {
  public static final SizedIngredient EMPTY = of(Ingredient.EMPTY, 0);

  public static final RecordLoadable<SizedIngredient> LOADABLE = RecordLoadable.create(
    IngredientLoadable.DISALLOW_EMPTY.tryDirectField("ingredient", SizedIngredient::getIngredient, "amount_needed"),
    IntLoadable.FROM_ONE.defaultField("amount_needed", 1, SizedIngredient::getAmountNeeded),
    SizedIngredient::new);

  private final Ingredient ingredient;
  private final int amountNeeded;
  private WeakReference<ItemStack[]> lastIngredientMatch;
  private List<ItemStack> matchingStacks;

  public SizedIngredient(Ingredient ingredient, int amountNeeded) {
    this.ingredient = ingredient;
    this.amountNeeded = amountNeeded;
  }

  public Ingredient getIngredient() {
    return ingredient;
  }

  public int getAmountNeeded() {
    return amountNeeded;
  }

  public static SizedIngredient of(Ingredient ingredient, int amountNeeded) {
    return new SizedIngredient(ingredient, amountNeeded);
  }

  public static SizedIngredient of(Ingredient ingredient) {
    return of(ingredient, 1);
  }

  public static SizedIngredient fromItems(int amountNeeded, ItemLike... items) {
    return of(Ingredient.of(items), amountNeeded);
  }

  public static SizedIngredient fromItems(ItemLike... items) {
    return fromItems(1, items);
  }

  public static SizedIngredient fromTag(TagKey<Item> tag, int amountNeeded) {
    return of(Ingredient.of(tag), amountNeeded);
  }

  public static SizedIngredient fromTag(TagKey<Item> tag) {
    return fromTag(tag, 1);
  }

  @Override
  public boolean test(ItemStack stack) {
    return stack.getCount() >= amountNeeded && ingredient.test(stack);
  }

  public boolean isEmpty() {
    return ingredient.isEmpty();
  }

  public List<ItemStack> getMatchingStacks() {
    ItemStack[] ingredientMatch = ingredient.getItems();
    if (matchingStacks == null || lastIngredientMatch == null || lastIngredientMatch.get() != ingredientMatch) {
      matchingStacks = Arrays.stream(ingredientMatch).map(stack -> {
        if (stack.getCount() != amountNeeded) {
          stack = stack.copy();
          stack.setCount(amountNeeded);
        }
        return stack;
      }).collect(Collectors.toList());
      lastIngredientMatch = new WeakReference<>(ingredientMatch);
    }
    return matchingStacks;
  }

  public void write(FriendlyByteBuf buffer) {
    LOADABLE.encode(buffer, this);
  }

  public JsonObject serialize() {
    JsonObject json = new JsonObject();
    LOADABLE.serialize(this, json);
    return json;
  }

  public static SizedIngredient read(FriendlyByteBuf buffer) {
    return LOADABLE.decode(buffer);
  }

  public static SizedIngredient deserialize(JsonObject json) {
    return LOADABLE.deserialize(json);
  }
}
