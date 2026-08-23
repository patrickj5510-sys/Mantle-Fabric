package slimeknights.mantle.client.book.data;

import lombok.Getter;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;

import javax.annotation.Nullable;

/**
 * Small book wrapper around Fabric's 1.21 resource-condition API.
 *
 * <p>Fabric 1.21 removed {@code ConditionJsonProvider}; book conditions now keep the decoded
 * {@link ResourceCondition} directly and evaluate it client-side. Conditions which need a
 * registry lookup will receive {@code null}, matching Fabric's supported client-resource path.</p>
 */
public class JsonCondition {
  @Getter
  @Nullable
  private final ResourceCondition condition;

  public JsonCondition(@Nullable ResourceCondition condition) {
    this.condition = condition;
  }

  /** Creates an invalid/unsatisfied condition, used when condition JSON cannot supply a condition. */
  public JsonCondition() {
    this(null);
  }

  public boolean test() {
    return condition != null && condition.test(null);
  }
}
