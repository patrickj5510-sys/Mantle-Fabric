package slimeknights.mantle.client.model.util;

import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.resources.model.BakedModel;

/**
 * Base wrapper for Mantle models which supply dynamic Fabric Renderer API quad emission.
 * @param <T> baked model parent type
 */
@SuppressWarnings("WeakerAccess")
public abstract class DynamicBakedWrapper<T extends BakedModel> extends ForwardingBakedModel {
  protected DynamicBakedWrapper(T originalModel) {
    wrapped = originalModel;
  }

  @Override
  public boolean isVanillaAdapter() {
    return false;
  }
}
