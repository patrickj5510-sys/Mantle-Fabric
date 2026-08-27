package slimeknights.mantle.registration.adapter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.PushReaction;
import slimeknights.mantle.block.MantleStandingSignBlock;
import slimeknights.mantle.block.MantleWallSignBlock;
import slimeknights.mantle.block.StrippableLogBlock;
import slimeknights.mantle.block.WoodenDoorBlock;
import slimeknights.mantle.block.entity.MantleSignBlockEntity;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.WallBuildingBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject.WoodVariant;

import java.util.function.Function;
import java.util.function.Supplier;

import static slimeknights.mantle.util.RegistryHelper.getHolder;

/**
 * Provides utility registration methods when registering blocks.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BlockRegistryAdapter extends EnumRegistryAdapter<Block> {

  /** @inheritDoc */
  public BlockRegistryAdapter() {
    super(BuiltInRegistries.BLOCK);
  }

  /** @inheritDoc */
  public BlockRegistryAdapter(String modid) {
    super(BuiltInRegistries.BLOCK, modid);
  }

  /** Registers a block override based on the given block. */
  public <T extends Block> T registerOverride(Function<Properties, T> constructor, Block base) {
    return register(constructor.apply(BlockBehaviour.Properties.ofFullCopy(base)), base);
  }

  public BuildingBlockObject registerBuilding(Block block, String name) {
    return new BuildingBlockObject(
      this.register(block, name),
      this.register(new SlabBlock(BlockBehaviour.Properties.ofFullCopy(block)), name + "_slab"),
      this.register(new StairBlock(block.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(block)), name + "_stairs")
    );
  }

  public WallBuildingBlockObject registerWallBuilding(Block block, String name) {
    return new WallBuildingBlockObject(
      registerBuilding(block, name),
      this.register(new WallBlock(BlockBehaviour.Properties.ofFullCopy(block)), name + "_wall")
    );
  }

  public FenceBuildingBlockObject registerFenceBuilding(Block block, String name) {
    return new FenceBuildingBlockObject(
      registerBuilding(block, name),
      this.register(new FenceBlock(BlockBehaviour.Properties.ofFullCopy(block)), name + "_fence")
    );
  }

  /** Registers a new wood object. */
  public WoodBlockObject registerWood(String name, Function<WoodVariant,BlockBehaviour.Properties> behaviorCreator) {
    BlockSetType setType = BlockSetType.register(new BlockSetType(resourceName(name)));
    WoodType woodType = WoodType.register(new WoodType(resourceName(name), setType));
    RegistrationHelper.registerWoodType(woodType);

    BlockBehaviour.Properties planksProps = behaviorCreator.apply(WoodVariant.PLANKS).instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f);
    BuildingBlockObject planks = registerBuilding(new Block(planksProps), name + "_planks");
    FenceBlock fence = register(new FenceBlock(BlockBehaviour.Properties.ofFullCopy(planks.get()).forceSolidOn()), name + "_fence");

    Supplier<? extends RotatedPillarBlock> stripped = () -> new RotatedPillarBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(NoteBlockInstrument.BASS).strength(2.0f));
    RotatedPillarBlock strippedLog = register(stripped.get(), "stripped_" + name + "_log");
    RotatedPillarBlock strippedWood = register(stripped.get(), "stripped_" + name + "_wood");
    RotatedPillarBlock log = register(new StrippableLogBlock(getHolder(BuiltInRegistries.BLOCK, strippedLog), behaviorCreator.apply(WoodVariant.LOG).instrument(NoteBlockInstrument.BASS).strength(2.0f)), name + "_log");
    RotatedPillarBlock wood = register(new StrippableLogBlock(getHolder(BuiltInRegistries.BLOCK, strippedWood), behaviorCreator.apply(WoodVariant.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.0f)), name + "_wood");

    DoorBlock door = register(new WoodenDoorBlock(behaviorCreator.apply(WoodVariant.PLANKS).instrument(NoteBlockInstrument.BASS).strength(3.0F).noOcclusion().pushReaction(PushReaction.DESTROY), setType), name + "_door");
    TrapDoorBlock trapdoor = register(new TrapDoorBlock(setType, behaviorCreator.apply(WoodVariant.PLANKS).instrument(NoteBlockInstrument.BASS).strength(3.0F).noOcclusion().isValidSpawn(Blocks::never)), name + "_trapdoor");
    FenceGateBlock fenceGate = register(new FenceGateBlock(woodType, BlockBehaviour.Properties.ofFullCopy(fence)), name + "_fence_gate");

    BlockBehaviour.Properties redstoneProps = behaviorCreator.apply(WoodVariant.PLANKS).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().pushReaction(PushReaction.DESTROY).strength(0.5F);
    PressurePlateBlock pressurePlate = register(new PressurePlateBlock(setType, redstoneProps), name + "_pressure_plate");
    ButtonBlock button = register(new ButtonBlock(setType, 30, redstoneProps), name + "_button");

    StandingSignBlock standingSign = register(new MantleStandingSignBlock(behaviorCreator.apply(WoodVariant.PLANKS).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(1.0F), woodType), name + "_sign");
    WallSignBlock wallSign = register(new MantleWallSignBlock(behaviorCreator.apply(WoodVariant.PLANKS).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(1.0F).lootFrom(() -> standingSign), woodType), name + "_wall_sign");
    MantleSignBlockEntity.registerSignBlock(() -> standingSign);
    MantleSignBlockEntity.registerSignBlock(() -> wallSign);

    return new WoodBlockObject(getResource(name), woodType, planks, log, strippedLog, wood, strippedWood, fence, fenceGate, door, trapdoor, pressurePlate, button, standingSign, wallSign);
  }

  /* Fluid registration is handled by the deferred fluid register on Fabric 1.21. */
}
