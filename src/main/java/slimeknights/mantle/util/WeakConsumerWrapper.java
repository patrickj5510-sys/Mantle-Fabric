package slimeknights.mantle.util;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * Consumer that weakly references a parent object so listeners do not keep it alive.
 * @param <TE> Parent object type, typically a block entity
 * @param <C> Consumer value
 */
public class WeakConsumerWrapper<TE,C> implements Consumer<C> {
  private final WeakReference<TE> te;
  private final NonnullBiConsumer<TE,C> consumer;

  public WeakConsumerWrapper(TE te, NonnullBiConsumer<TE,C> consumer) {
    this.te = new WeakReference<>(te);
    this.consumer = consumer;
  }

  @Override
  public void accept(C c) {
    TE te = this.te.get();
    if (te != null) {
      consumer.accept(te, c);
    }
  }
}
