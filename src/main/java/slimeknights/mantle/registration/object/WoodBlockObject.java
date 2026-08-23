package slimeknights.mantle.registration.object;

import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static slimeknights.mantle.registration.RegistrationHelper.getCastedHolder;
import static slimeknights.mantle.util.RegistryHelper.getHolder;

/** Extension of the fence object with all other wood blocks. */
public class WoodBlockObject extends FenceBuildingBlockObject {
  @Getter
  private final WoodType woodType;
  private final Supplier<? extends Block> log;
  private final Supplier<? extends Block> strippedLog;
  private final Supplier<? extends Block> wood;
  private final Supplier<? extends Block> strippedWood;
  private final Supplier<? extends FenceGateBlock> fenceGate;
  private final Supplier<? extends DoorBlock> door;
  private final Supplier<? extends TrapDoorBlock> trapdoor;
  private final Supplier<? extends PressurePlateBlock> pressurePlate;
  private final Supplier<? extends ButtonBlock> button;
  private final Supplier<? extends StandingSignBlock> sign;
  private final Supplier<? extends WallSignBlock> wallSign;
  @Getter
  private final TagKey<Block> logBlockTag;
  @Getter
  private final TagKey<Item> logItemTag;

  public WoodBlockObject(ResourceLocation name, WoodType woodType, BuildingBlockObject planks,
                         Supplier<? extends Block> log, Supplier<? extends Block> strippedLog, Supplier<? extends Block> wood, Supplier<? extends Block> strippedWood,
                         Supplier<? extends FenceBlock> fence, Supplier<? extends FenceGateBlock> fenceGate, Supplier<? extends DoorBlock> door, Supplier<? extends TrapDoorBlock> trapdoor,
                         Supplier<? extends PressurePlateBlock> pressurePlate, Supplier<? extends ButtonBlock> button,
                         Supplier<? extends StandingSignBlock> sign, Supplier<? extends WallSignBlock> wallSign) {
    super(planks, fence);
    this.woodType = woodType;
    this.log = log;
    this.strippedLog = strippedLog;
    this.wood = wood;
    this.strippedWood = strippedWood;
    this.fenceGate = fenceGate;
    this.door = door;
    this.trapdoor = trapdoor;
    this.pressurePlate = pressurePlate;
    this.button = button;
    this.sign = sign;
    this.wallSign = wallSign;
    ResourceLocation tagName = ResourceLocation.fromNamespaceAndPath(name.getNamespace(), name.getPath() + "_logs");
    this.logBlockTag = TagKey.create(Registries.BLOCK, tagName);
    this.logItemTag = TagKey.create(Registries.ITEM, tagName);
  }

  public WoodBlockObject(ResourceLocation name, WoodType woodType, BuildingBlockObject planks,
                         Block log, Block strippedLog, Block wood, Block strippedWood,
                         Block fence, Block fenceGate, Block door, Block trapdoor,
                         Block pressurePlate, Block button, Block sign, Block wallSign) {
    super(planks, () -> (FenceBlock) fence);
    this.woodType = woodType;
    this.log = getHolder(BuiltInRegistries.BLOCK, log);
    this.strippedLog = getHolder(BuiltInRegistries.BLOCK, strippedLog);
    this.wood = getHolder(BuiltInRegistries.BLOCK, wood);
    this.strippedWood = getHolder(BuiltInRegistries.BLOCK, strippedWood);
    this.fenceGate = getCastedHolder(BuiltInRegistries.BLOCK, fenceGate);
    this.door = getCastedHolder(BuiltInRegistries.BLOCK, door);
    this.trapdoor = getCastedHolder(BuiltInRegistries.BLOCK, trapdoor);
    this.pressurePlate = getCastedHolder(BuiltInRegistries.BLOCK, pressurePlate);
    this.button = getCastedHolder(BuiltInRegistries.BLOCK, button);
    this.sign = getCastedHolder(BuiltInRegistries.BLOCK, sign);
    this.wallSign = getCastedHolder(BuiltInRegistries.BLOCK, wallSign);
    ResourceLocation tagName = ResourceLocation.fromNamespaceAndPath(name.getNamespace(), name.getPath() + "_logs");
    this.logBlockTag = TagKey.create(Registries.BLOCK, tagName);
    this.logItemTag = TagKey.create(Registries.ITEM, tagName);
  }

  public Block getLog() { return log.get(); }
  public Block getStrippedLog() { return strippedLog.get(); }
  public Block getWood() { return wood.get(); }
  public Block getStrippedWood() { return strippedWood.get(); }
  public FenceGateBlock getFenceGate() { return fenceGate.get(); }
  public DoorBlock getDoor() { return door.get(); }
  public TrapDoorBlock getTrapdoor() { return trapdoor.get(); }
  public PressurePlateBlock getPressurePlate() { return pressurePlate.get(); }
  public ButtonBlock getButton() { return button.get(); }
  public StandingSignBlock getSign() { return sign.get(); }
  public WallSignBlock getWallSign() { return wallSign.get(); }

  @Override
  public List<Block> values() {
    return Arrays.asList(
      get(), getSlab(), getStairs(), getFence(),
      getLog(), getStrippedLog(), getWood(), getStrippedWood(),
      getFenceGate(), getDoor(), getTrapdoor(),
      getPressurePlate(), getButton(), getSign(), getWallSign());
  }

  public enum WoodVariant { LOG, WOOD, PLANKS }
}
