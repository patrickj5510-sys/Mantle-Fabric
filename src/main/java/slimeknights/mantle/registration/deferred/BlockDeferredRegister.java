package slimeknights.mantle.registration.deferred;

import io.github.fabricators_of_create.porting_lib.registry.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
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
import net.minecraft.world.level.block.state.properties.WoodType;
import slimeknights.mantle.block.MantleStandingSignBlock;
import slimeknights.mantle.block.MantleWallSignBlock;
import slimeknights.mantle.block.StrippableLogBlock;
import slimeknights.mantle.block.WoodenDoorBlock;
import slimeknights.mantle.block.entity.MantleSignBlockEntity;
import slimeknights.mantle.item.BurnableBlockItem;
import slimeknights.mantle.item.BurnableSignItem;
import slimeknights.mantle.item.BurnableTallBlockItem;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.registration.object.BuildingBlockObject;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.FenceBuildingBlockObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.registration.object.MetalItemObject;
import slimeknights.mantle.registration.object.WallBuildingBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject.WoodVariant;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/** Deferred register to handle registering blocks with possible item forms. */
@SuppressWarnings({"WeakerAccess", "unused"})
public class BlockDeferredRegister extends DeferredRegisterWrapper<Block> {
  protected final SynchronizedDeferredRegister<Item> itemRegister;

  public BlockDeferredRegister(String modID) {
    super(Registries.BLOCK, modID);
    this.itemRegister = SynchronizedDeferredRegister.create(BuiltInRegistries.ITEM, modID);
  }

  @Override
  public void register() {
    super.register();
    itemRegister.register();
  }

  /* Blocks with no items */

  public <B extends Block> DeferredHolder<Block, B> registerNoItem(String name, Supplier<? extends B> block) {
    return register.register(name, block);
  }

  public DeferredHolder<Block, Block> registerNoItem(String name, BlockBehaviour.Properties props) {
    return registerNoItem(name, () -> new Block(props));
  }

  /* Block item pairs */

  public <B extends Block> ItemObject<B> register(String name, Supplier<? extends B> block, Function<? super B, ? extends BlockItem> item) {
    DeferredHolder<Block, B> blockObj = registerNoItem(name, block);
    itemRegister.register(name, () -> item.apply(blockObj.get()));
    return new ItemObject<>(blockObj);
  }

  public ItemObject<Block> register(String name, BlockBehaviour.Properties blockProps, Function<? super Block, ? extends BlockItem> item) {
    return register(name, () -> new Block(blockProps), item);
  }

  /* Building */

  public BuildingBlockObject registerBuilding(String name, Supplier<? extends Block> block, Function<? super Block, ? extends BlockItem> item) {
    ItemObject<Block> blockObj = register(name, block, item);
    return new BuildingBlockObject(
      blockObj,
      register(name + "_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(blockObj.get())), item),
      register(name + "_stairs", () -> new StairBlock(blockObj.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(blockObj.get())), item));
  }

  public BuildingBlockObject registerBuilding(String name, BlockBehaviour.Properties props, Function<? super Block, ? extends BlockItem> item) {
    ItemObject<Block> blockObj = register(name, props, item);
    return new BuildingBlockObject(
      blockObj,
      register(name + "_slab", () -> new SlabBlock(props), item),
      register(name + "_stairs", () -> new StairBlock(blockObj.get().defaultBlockState(), props), item));
  }

  public WallBuildingBlockObject registerWallBuilding(String name, Supplier<? extends Block> block, Function<? super Block, ? extends BlockItem> item) {
    BuildingBlockObject obj = registerBuilding(name, block, item);
    return new WallBuildingBlockObject(obj, register(name + "_wall", () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(obj.get())), item));
  }

  public WallBuildingBlockObject registerWallBuilding(String name, BlockBehaviour.Properties props, Function<? super Block, ? extends BlockItem> item) {
    return new WallBuildingBlockObject(
      registerBuilding(name, props, item),
      register(name + "_wall", () -> new WallBlock(props), item));
  }

  public FenceBuildingBlockObject registerFenceBuilding(String name, Supplier<? extends Block> block, Function<? super Block, ? extends BlockItem> item) {
    BuildingBlockObject obj = registerBuilding(name, block, item);
    return new FenceBuildingBlockObject(obj, register(name + "_fence", () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(obj.get())), item));
  }

  public FenceBuildingBlockObject registerFenceBuilding(String name, BlockBehaviour.Properties props, Function<? super Block, ? extends BlockItem> item) {
    return new FenceBuildingBlockObject(
      registerBuilding(name, props, item),
      register(name + "_fence", () -> new FenceBlock(props), item));
  }

  /* Wood */

  public WoodBlockObject registerWood(String name, Function<WoodVariant, BlockBehaviour.Properties> behaviorCreator, boolean flammable) {
    BlockSetType setType = new BlockSetType(resourceName(name));
    BlockSetType.register(setType);
    WoodType woodType = new WoodType(resourceName(name), setType);
    RegistrationHelper.registerWoodType(woodType);
    Item.Properties itemProps = new Item.Properties();

    Function<Integer, Function<? super Block, ? extends BlockItem>> burnableItem;
    Function<? super Block, ? extends BlockItem> burnableTallItem;
    BiFunction<? super Block, ? super Block, ? extends BlockItem> burnableSignItem;
    Item.Properties signProps = new Item.Properties().stacksTo(16);
    if (flammable) {
      burnableItem = burnTime -> block -> new BurnableBlockItem(block, itemProps, burnTime);
      burnableTallItem = block -> new BurnableTallBlockItem(block, itemProps, 200);
      burnableSignItem = (standing, wall) -> new BurnableSignItem(signProps, standing, wall, 200);
    } else {
      Function<? super Block, ? extends BlockItem> defaultItemBlock = block -> new BlockItem(block, itemProps);
      burnableItem = burnTime -> defaultItemBlock;
      burnableTallItem = block -> new DoubleHighBlockItem(block, itemProps);
      burnableSignItem = (standing, wall) -> new SignItem(signProps, standing, wall);
    }

    Function<? super Block, ? extends BlockItem> burnable300 = burnableItem.apply(300);
    BlockBehaviour.Properties planksProps = behaviorCreator.apply(WoodVariant.PLANKS).strength(2.0f, 3.0f);
    BuildingBlockObject planks = registerBuilding(name + "_planks", planksProps, block -> burnableItem.apply(block instanceof SlabBlock ? 150 : 300).apply(block));
    ItemObject<FenceBlock> fence = register(name + "_fence", () -> new FenceBlock(Properties.ofFullCopy(planks.get())), burnable300);

    Supplier<? extends RotatedPillarBlock> stripped = () -> new RotatedPillarBlock(behaviorCreator.apply(WoodVariant.PLANKS).strength(2.0f));
    ItemObject<RotatedPillarBlock> strippedLog = register("stripped_" + name + "_log", stripped, burnable300);
    ItemObject<RotatedPillarBlock> strippedWood = register("stripped_" + name + "_wood", stripped, burnable300);
    ItemObject<RotatedPillarBlock> log = register(name + "_log", () -> new StrippableLogBlock(strippedLog, behaviorCreator.apply(WoodVariant.LOG).strength(2.0f)), burnable300);
    ItemObject<RotatedPillarBlock> wood = register(name + "_wood", () -> new StrippableLogBlock(strippedWood, behaviorCreator.apply(WoodVariant.WOOD).strength(2.0f)), burnable300);

    ItemObject<DoorBlock> door = register(name + "_door", () -> new WoodenDoorBlock(behaviorCreator.apply(WoodVariant.PLANKS).strength(3.0F).noOcclusion(), setType), burnableTallItem);
    ItemObject<TrapDoorBlock> trapdoor = register(name + "_trapdoor", () -> new TrapDoorBlock(setType, behaviorCreator.apply(WoodVariant.PLANKS).strength(3.0F).noOcclusion().isValidSpawn(Blocks::never)), burnable300);
    ItemObject<FenceGateBlock> fenceGate = register(name + "_fence_gate", () -> new FenceGateBlock(woodType, Properties.ofFullCopy(planks.get())), burnable300);

    BlockBehaviour.Properties redstoneProps = behaviorCreator.apply(WoodVariant.PLANKS).noCollission().strength(0.5F);
    ItemObject<PressurePlateBlock> pressurePlate = register(name + "_pressure_plate", () -> new PressurePlateBlock(setType, redstoneProps), burnable300);
    ItemObject<ButtonBlock> button = register(name + "_button", () -> new ButtonBlock(setType, 30, redstoneProps), burnableItem.apply(100));

    DeferredHolder<Block, StandingSignBlock> standingSign = registerNoItem(name + "_sign", () -> new MantleStandingSignBlock(behaviorCreator.apply(WoodVariant.PLANKS).noCollission().strength(1.0F), woodType));
    DeferredHolder<Block, WallSignBlock> wallSign = registerNoItem(name + "_wall_sign", () -> new MantleWallSignBlock(behaviorCreator.apply(WoodVariant.PLANKS).noCollission().strength(1.0F).lootFrom(standingSign), woodType));
    MantleSignBlockEntity.registerSignBlock(standingSign);
    MantleSignBlockEntity.registerSignBlock(wallSign);
    itemRegister.register(name + "_sign", () -> burnableSignItem.apply(standingSign.get(), wallSign.get()));

    return new WoodBlockObject(resource(name), woodType, planks, log, strippedLog, wood, strippedWood, fence, fenceGate, door, trapdoor, pressurePlate, button, standingSign, wallSign);
  }

  /* Enum */

  public <T extends Enum<T> & StringRepresentable, B extends Block> EnumObject<T,B> registerEnum(
    T[] values, String name, Function<T,? extends B> mapper, Function<? super B, ? extends BlockItem> item) {
    return registerEnum(values, name, (fullName, value) -> register(fullName, () -> mapper.apply(value), item));
  }

  public <T extends Enum<T> & StringRepresentable, B extends Block> EnumObject<T,B> registerEnum(
    String name, T[] values, Function<T,? extends B> mapper, Function<? super B, ? extends BlockItem> item) {
    return registerEnum(name, values, (fullName, value) -> register(fullName, () -> mapper.apply(value), item));
  }

  public <T extends Enum<T> & StringRepresentable, B extends Block> EnumObject<T, B> registerEnumNoItem(T[] values, String name, Function<T, ? extends B> mapper) {
    return registerEnum(values, name, (fullName, value) -> registerNoItem(fullName, () -> mapper.apply(value)));
  }

  /* Metal */

  public MetalItemObject registerMetal(String name, String tagName, Supplier<Block> blockSupplier, Function<Block,? extends BlockItem> blockItem, Item.Properties itemProps) {
    ItemObject<Block> block = register(name + "_block", blockSupplier, blockItem);
    Supplier<Item> itemSupplier = () -> new Item(itemProps);
    DeferredHolder<Item, Item> ingot = itemRegister.register(name + "_ingot", itemSupplier);
    DeferredHolder<Item, Item> nugget = itemRegister.register(name + "_nugget", itemSupplier);
    return new MetalItemObject(tagName, block, ingot, nugget);
  }

  public MetalItemObject registerMetal(String name, Supplier<Block> blockSupplier, Function<Block,? extends BlockItem> blockItem, Item.Properties itemProps) {
    return registerMetal(name, name, blockSupplier, blockItem, itemProps);
  }

  public MetalItemObject registerMetal(String name, String tagName, BlockBehaviour.Properties blockProps, Function<Block,? extends BlockItem> blockItem, Item.Properties itemProps) {
    return registerMetal(name, tagName, () -> new Block(blockProps), blockItem, itemProps);
  }

  public MetalItemObject registerMetal(String name, BlockBehaviour.Properties blockProps, Function<Block,? extends BlockItem> blockItem, Item.Properties itemProps) {
    return registerMetal(name, name, blockProps, blockItem, itemProps);
  }
}
