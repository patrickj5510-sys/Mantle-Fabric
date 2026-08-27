package slimeknights.mantle.data.registry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.gson.GenericRegisteredSerializer;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;

import java.util.function.Function;

/** Generic registry for an object that can both be sent over a friendly byte buffer and serialized into JSON. */
@SuppressWarnings("unused")
public class GenericLoaderRegistry<T extends IHaveLoader> implements Loadable<T> {
  protected static final JsonObject EMPTY_OBJECT = new JsonObject();

  @Getter
  private final String name;
  protected final NamedComponentRegistry<IGenericLoader<? extends T>> loaders;
  protected final boolean compact;

  public GenericLoaderRegistry(String name, boolean compact) {
    this.name = name;
    this.compact = compact;
    this.loaders = new NamedComponentRegistry<>("Unknown " + name + " loader");
  }

  public void register(ResourceLocation name, IGenericLoader<? extends T> loader) {
    loaders.register(name, loader);
  }

  @Override
  public T convert(JsonElement element, String key) {
    if (element.isJsonObject()) {
      JsonObject object = element.getAsJsonObject();
      return loaders.getIfPresent(object, "type").deserialize(object);
    }
    if (compact && element.isJsonPrimitive()) {
      EMPTY_OBJECT.entrySet().clear();
      return loaders.convert(element, "type").deserialize(EMPTY_OBJECT);
    }
    throw new JsonSyntaxException("Invalid " + name + " JSON at " + key + ", must be a JSON object" + (compact ? " or a string" : ""));
  }

  public T deserialize(JsonElement element) {
    return convert(element, "[unknown]");
  }

  @SuppressWarnings("unchecked")
  private <L extends IHaveLoader> JsonElement serialize(IGenericLoader<L> loader, T src) {
    JsonObject json = new JsonObject();
    JsonElement type = new JsonPrimitive(loaders.getKey((IGenericLoader<? extends T>)loader).toString());
    json.add("type", type);
    loader.serialize((L)src, json);
    if (json.get("type") != type) {
      throw new IllegalStateException(name + " serializer " + type.getAsString() + " modified the type key, this is not allowed as it breaks deserialization");
    }
    if (compact && json.entrySet().size() == 1) {
      return type;
    }
    return json;
  }

  @Override
  public JsonElement serialize(T src) {
    return serialize(src.getLoader(), src);
  }

  @SuppressWarnings("unchecked")
  protected <L extends IHaveLoader> void toNetwork(IGenericLoader<L> loader, T src, FriendlyByteBuf buffer) {
    loader.toNetwork((L)src, buffer);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void encode(FriendlyByteBuf buffer, T src) {
    loaders.encode(buffer, (IGenericLoader<? extends T>)src.getLoader());
    toNetwork(src.getLoader(), src, buffer);
  }

  @Override
  public T decode(FriendlyByteBuf buffer) {
    return loaders.decode(buffer).fromNetwork(buffer);
  }

  @Deprecated(forRemoval = true)
  public void toNetwork(T src, FriendlyByteBuf buffer) {
    encode(buffer, src);
  }

  @Deprecated(forRemoval = true)
  public T fromNetwork(FriendlyByteBuf buffer) {
    return decode(buffer);
  }

  public <P> LoadableField<T,P> directField(Function<P,T> getter) {
    return new DirectRegistryField<>(this, getter);
  }

  public <P> LoadableField<T,P> directField(String typeKey, Function<P,T> getter) {
    return new MergingRegistryField<>(this, typeKey, getter);
  }

  @Override
  public String toString() {
    return getClass().getName() + "('" + name + "')";
  }

  @Deprecated
  public interface IGenericLoader<T> {
    T deserialize(JsonObject json);
    T fromNetwork(FriendlyByteBuf buffer);
    void serialize(T object, JsonObject json);
    void toNetwork(T object, FriendlyByteBuf buffer);
  }

  public interface IHaveLoader {
    IGenericLoader<? extends IHaveLoader> getLoader();
  }

  /** Loader instance for an object with only a single implementation. */
  public static class SingletonLoader<T> implements IGenericLoader<T> {
    private final T instance;

    public SingletonLoader(T instance) {
      this.instance = instance;
    }

    public SingletonLoader(Function<IGenericLoader<T>,T> creator) {
      this.instance = creator.apply(this);
    }

    public T getInstance() {
      return instance;
    }

    @Override
    public T deserialize(JsonObject json) {
      return instance;
    }

    @Override
    public T fromNetwork(FriendlyByteBuf buffer) {
      return instance;
    }

    @Override
    public void serialize(T object, JsonObject json) {}

    @Override
    public void toNetwork(T object, FriendlyByteBuf buffer) {}

    public static <T> T singleton(Function<IGenericLoader<T>,T> instance) {
      return new SingletonLoader<>(instance).getInstance();
    }
  }
}
