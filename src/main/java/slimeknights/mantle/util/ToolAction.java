package slimeknights.mantle.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader-neutral identifier for named tool actions.
 *
 * <p>Forge exposed these names through ToolAction/ItemAbility. Fabric has no equivalent global
 * action object, but Mantle data still needs stable identifiers. This class preserves that serialized
 * API without coupling common data code to a loader-specific tool capability system.</p>
 */
public final class ToolAction {
  private static final Map<String, ToolAction> ACTIONS = new ConcurrentHashMap<>();

  private final String name;

  private ToolAction(String name) {
    this.name = name;
  }

  public static ToolAction get(String name) {
    return ACTIONS.computeIfAbsent(name, ToolAction::new);
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
