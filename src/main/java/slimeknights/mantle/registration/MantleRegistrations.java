package slimeknights.mantle.registration;

import net.minecraft.world.level.block.entity.BlockEntityType;
import slimeknights.mantle.block.entity.MantleSignBlockEntity;

/** Various objects registered under Mantle. */
public class MantleRegistrations {
  private MantleRegistrations() {}

  /** Assigned during Mantle initialization after registration. */
  public static BlockEntityType<MantleSignBlockEntity> SIGN;
}
