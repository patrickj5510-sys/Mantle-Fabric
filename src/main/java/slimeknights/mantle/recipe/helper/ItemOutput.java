package slimeknights.mantle.recipe.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.LoadableCodec;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class representing an item stack output. Supports both direct stacks and tag output, behaving like an ingredient used for output
 */
public abstract class ItemOutput implements Supplier<ItemStack> {
  /* Codecs - just adding these as needed */
  /** Codec for an output that may not be empty with any size */
  public static Codec<ItemOutput> REQUIRED_STACK_CODEC = new LoadableCodec<>(Loadable.REQUIRED_STACK);

  /** Empty instance */
  public static final ItemOutput EMPTY = new OfStack(ItemStack.EMPTY);

  @Override
  public abstract ItemStack get();

  public abstract JsonElement serialize(boolean writeCount);

  public static ItemOutput fromStack(ItemStack stack) {
    if (stack.isEmpty()) {
      return EMPTY;
    }
    return new OfStack(stack);
  }

  public static ItemOutput fromItem(ItemLike item, int count) {
    return new OfItem(item.asItem(), count);
  }

  public static ItemOutput fromItem(ItemLike item) {
    return fromItem(item, 1);
  }

  public static ItemOutput fromTag(TagKey<Item> tag, int count) {
    return new OfTagPreference(tag, count);
  }

  public static ItemOutput fromTag(TagKey<Item> tag) {
    return fromTag(tag, 1);
  }

  public void write(FriendlyByteBuf buffer) {
    buffer.writeItem(get());
  }

  public static ItemOutput read(FriendlyByteBuf buffer) {
    return fromStack(buffer.readItem());
  }

  @RequiredArgsConstructor
  private static class OfItem extends ItemOutput {
    private final Item item;
    private final int count;
    private ItemStack cachedStack;

    @Override
    public ItemStack get() {
      if (cachedStack == null) {
        cachedStack = new ItemStack(item, count);
      }
      return cachedStack;
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
      JsonElement item = Loadables.ITEM.serialize(this.item);
      if (writeCount && count > 1) {
        JsonObject json = new JsonObject();
        json.add("item", item);
        json.addProperty("count", count);
        return json;
      }
      return item;
    }
  }

  @RequiredArgsConstructor
  private static class OfStack extends ItemOutput {
    private final ItemStack stack;

    @Override
    public ItemStack get() {
      return stack;
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
      if (writeCount) {
        return ItemStackLoadable.OPTIONAL_STACK_NBT.serialize(stack);
      }
      return ItemStackLoadable.OPTIONAL_ITEM_NBT.serialize(stack);
    }
  }

  @RequiredArgsConstructor
  private static class OfTagPreference extends ItemOutput {
    private final TagKey<Item> tag;
    private final int count;
    private ItemStack cachedResult = null;

    @Override
    public ItemStack get() {
      if (cachedResult == null) {
        Optional<Item> preference = TagPreference.getPreference(tag);
        if (preference.isEmpty()) {
          return ItemStack.EMPTY;
        }
        cachedResult = new ItemStack(preference.orElseThrow(), count);
      }
      return cachedResult;
    }

    @Override
    public JsonElement serialize(boolean writeCount) {
      JsonObject json = new JsonObject();
      json.addProperty("tag", tag.location().toString());
      if (writeCount) {
        json.addProperty("count", count);
      }
      return json;
    }
  }

  public enum Loadable implements slimeknights.mantle.data.loadable.Loadable<ItemOutput> {
    OPTIONAL_ITEM(false, false),
    OPTIONAL_STACK(false, true),
    REQUIRED_ITEM(true, false),
    REQUIRED_STACK(true, true);

    private final boolean nonEmpty;
    private final boolean readCount;
    private final RecordLoadable<ItemStack> stack;
    Loadable(boolean nonEmpty, boolean readCount) {
      this.nonEmpty = nonEmpty;
      this.readCount = readCount;
      if (nonEmpty) {
        this.stack = readCount ? ItemStackLoadable.REQUIRED_STACK_NBT : ItemStackLoadable.REQUIRED_ITEM_NBT;
      } else {
        this.stack = readCount ? ItemStackLoadable.OPTIONAL_STACK_NBT : ItemStackLoadable.OPTIONAL_ITEM_NBT;
      }
    }

    @Override
    public ItemOutput convert(JsonElement element, String key) {
      if (element.isJsonPrimitive()) {
        return fromStack(stack.convert(element, key));
      }
      JsonObject json = GsonHelper.convertToJsonObject(element, key);
      if (json.has("tag")) {
        TagKey<Item> tag = Loadables.ITEM_TAG.getIfPresent(json, "tag");
        int count = 1;
        if (readCount) {
          count = IntLoadable.FROM_ONE.getOrDefault(json, "count", 1);
        }
        return fromTag(tag, count);
      }
      return fromStack(stack.deserialize(json));
    }

    @Override
    public JsonElement serialize(ItemOutput output) {
      if (nonEmpty && (output instanceof OfItem || output instanceof OfStack) && output.get().isEmpty()) {
        throw new IllegalArgumentException("ItemOutput cannot be empty for this recipe");
      }
      return output.serialize(readCount);
    }

    @Override
    public ItemOutput decode(FriendlyByteBuf buffer) {
      return fromStack(stack.decode(buffer));
    }

    @Override
    public void encode(FriendlyByteBuf buffer, ItemOutput object) {
      stack.encode(buffer, object.get());
    }

    public ItemOutput getOrEmpty(JsonObject parent, String key) {
      return getOrDefault(parent, key, ItemOutput.EMPTY);
    }

    public <P> LoadableField<ItemOutput,P> emptyField(String key, boolean serializeDefault, Function<P,ItemOutput> getter) {
      return defaultField(key, ItemOutput.EMPTY, serializeDefault, getter);
    }

    public <P> LoadableField<ItemOutput,P> emptyField(String key, Function<P,ItemOutput> getter) {
      return emptyField(key, false, getter);
    }
  }
}
