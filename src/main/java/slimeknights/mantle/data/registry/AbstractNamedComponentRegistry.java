package slimeknights.mantle.data.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;

/** Shared logic for registries that map a resource location to an object. */
public abstract class AbstractNamedComponentRegistry<T> implements ResourceLocationLoadable<T> {
  protected final String errorText;

  public AbstractNamedComponentRegistry(String errorText) {
    this.errorText = errorText + " ";
  }

  @Nullable
  public abstract T getValue(ResourceLocation name);

  public abstract Collection<ResourceLocation> getKeys();

  public abstract Collection<T> getValues();

  @Override
  public T fromKey(ResourceLocation name, String key) {
    T value = getValue(name);
    if (value != null) {
      return value;
    }
    throw new JsonSyntaxException(errorText + name + " at '" + key + '\'');
  }

  @Override
  public void encode(FriendlyByteBuf buffer, T value) {
    buffer.writeResourceLocation(getKey(value));
  }

  public void encodeOptional(FriendlyByteBuf buffer, @Nullable T value) {
    if (value != null) {
      buffer.writeUtf(getKey(value).toString());
    } else {
      buffer.writeUtf("");
    }
  }

  private T decodeInternal(ResourceLocation name) {
    T value = getValue(name);
    if (value == null) {
      throw new DecoderException(errorText + name);
    }
    return value;
  }

  @Override
  public T decode(FriendlyByteBuf buffer) {
    return decodeInternal(buffer.readResourceLocation());
  }

  @Nullable
  public T decodeOptional(FriendlyByteBuf buffer) {
    String key = buffer.readUtf(Short.MAX_VALUE);
    if (key.isEmpty()) {
      return null;
    }
    return decodeInternal(ResourceLocation.parse(key));
  }

  @Override
  public <P> LoadableField<T,P> nullableField(String key, Function<P,T> getter) {
    return new NullableField<>(this, key, getter);
  }

  private record NullableField<T,P>(AbstractNamedComponentRegistry<T> registry, String key, Function<P,T> getter) implements LoadableField<T,P> {
    @Nullable
    @Override
    public T get(JsonObject json) {
      return registry.getOrDefault(json, key, null);
    }

    @Override
    public void serialize(P parent, JsonObject json) {
      T object = getter.apply(parent);
      if (object != null) {
        json.add(key, registry.serialize(object));
      }
    }

    @Nullable
    @Override
    public T decode(FriendlyByteBuf buffer) {
      return registry.decodeOptional(buffer);
    }

    @Override
    public void encode(FriendlyByteBuf buffer, P parent) {
      registry.encodeOptional(buffer, getter.apply(parent));
    }
  }
}
