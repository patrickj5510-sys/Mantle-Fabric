package slimeknights.mantle.registration.deferred;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.types.Type;
import io.github.fabricators_of_create.porting_lib.registry.DeferredHolder;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import slimeknights.mantle.registration.object.EnumObject;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Deferred register to register block entity type instances. */
@SuppressWarnings("unused")
public class BlockEntityTypeDeferredRegister extends DeferredRegisterWrapper<BlockEntityType<?>> {
  public BlockEntityTypeDeferredRegister(String modID) {
    super(Registries.BLOCK_ENTITY_TYPE, modID);
  }

  @Nullable
  private Type<?> getType(String name) {
    return Util.fetchChoiceType(References.BLOCK_ENTITY, resourceName(name));
  }

  public <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String name, BlockEntitySupplier<? extends T> factory, Supplier<? extends Block> block) {
    return register.register(name, () -> BlockEntityType.Builder.<T>of(factory, block.get()).build(getType(name)));
  }

  public <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String name, BlockEntitySupplier<? extends T> factory, EnumObject<?, ? extends Block> blocks) {
    return register.register(name, () -> new BlockEntityType<>(factory, ImmutableSet.copyOf(blocks.values()), getType(name)));
  }

  public <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(String name, BlockEntitySupplier<? extends T> factory, Consumer<ImmutableSet.Builder<Block>> blockCollector) {
    return register.register(name, () -> {
      ImmutableSet.Builder<Block> blocks = ImmutableSet.builder();
      blockCollector.accept(blocks);
      return new BlockEntityType<>(factory, blocks.build(), getType(name));
    });
  }
}
