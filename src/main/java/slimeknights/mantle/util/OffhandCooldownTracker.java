package slimeknights.mantle.util;

import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.network.MantleNetwork;
import slimeknights.mantle.network.packet.SwingArmPacket;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Logic to handle offhand having its own cooldown
 */
@RequiredArgsConstructor
public class OffhandCooldownTracker implements Component, EntityComponentInitializer {
  public static final ResourceLocation KEY = Mantle.getResource("offhand_cooldown");
  public static final Function<OffhandCooldownTracker,Float> COOLDOWN_TRACKER = OffhandCooldownTracker::getCooldown;
  private static final Function<OffhandCooldownTracker,Boolean> ATTACK_READY = OffhandCooldownTracker::isAttackReady;

  public OffhandCooldownTracker() {
    this.player = null;
  }

  /**
   * Component instance for offhand cooldown
   */
  public static final ComponentKey<OffhandCooldownTracker> CAPABILITY = ComponentRegistry.getOrCreate(KEY, OffhandCooldownTracker.class);

  /** Registers the component for all players. */
  @Override
  public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
    registry.registerForPlayers(CAPABILITY, OffhandCooldownTracker::attachCapability);
  }

  /** Registers additional listeners. */
  public static void register() {
  }

  /**
   * Called to add the component handler to all players
   * @param player  Player
   */
  private static OffhandCooldownTracker attachCapability(Player player) {
    return new OffhandCooldownTracker(player);
  }

  /** Lazy optional of self for capability requirements */
  private final LazyOptional<OffhandCooldownTracker> capabilityInstance = LazyOptional.of(() -> this);
  /** Player receiving cooldowns */
  @Nullable
  private final Player player;
  /** Scale of the last cooldown */
  private int lastCooldown = 0;
  /** Time in ticks when the player can next attack for full power */
  private int attackReady = 0;

  /** Enables the cooldown tracker if above 0. Intended to be set in equipment change events, not serialized */
  private int enabled = 0;

  /** Null safe way to get the player's ticks existed */
  private int getTicksExisted() {
    if (player == null) {
      return 0;
    }
    return player.tickCount;
  }

  /** If true, the tracker is enabled despite a cooldown item not being held */
  public boolean isEnabled() {
    return enabled > 0;
  }

  /**
   * Call this method when your item causing offhand cooldown to be needed is enabled and disabled. If multiple places call this, the tracker will automatically keep enabled until all places disable
   * @param enable  If true, enable. If false, disable
   */
  public void setEnabled(boolean enable) {
    if (enable) {
      enabled++;
    } else {
      enabled--;
    }
  }

  /**
   * Applies the given amount of cooldown
   * @param cooldown  Cooldown amount
   */
  public void applyCooldown(int cooldown) {
    this.lastCooldown = cooldown;
    this.attackReady = getTicksExisted() + cooldown;
  }

  /**
   * Returns a number from 0 to 1 denoting the current cooldown amount, akin to {@link Player#getAttackStrengthScale(float)}
   * @return number from 0 to 1, with 1 being no cooldown
   */
  public float getCooldown() {
    int ticksExisted = getTicksExisted();
    if (ticksExisted > this.attackReady || this.lastCooldown == 0) {
      return 1.0f;
    }
    return Mth.clamp((this.lastCooldown + ticksExisted - this.attackReady) / (float) this.lastCooldown, 0f, 1f);
  }

  /**
   * Checks if we can perform another attack yet.
   * This counteracts rapid attacks via click macros, in a similar way to vanilla by limiting to once every 10 ticks
   */
  public boolean isAttackReady() {
    return getTicksExisted() + this.lastCooldown > this.attackReady;
  }

  /** Gets the offhand cooldown for the given player. */
  public static float getCooldown(Player player) {
    return CAPABILITY.maybeGet(player).map(COOLDOWN_TRACKER).orElse(1.0f);
  }

  /** Applies cooldown to the given player. */
  public static void applyCooldown(Player player, int cooldown) {
    CAPABILITY.maybeGet(player).ifPresent(cap -> cap.applyCooldown(cooldown));
  }

  /** Checks if the player's offhand attack is ready. */
  public static boolean isAttackReady(Player player) {
    return CAPABILITY.maybeGet(player).map(ATTACK_READY).orElse(true);
  }

  /** Applies cooldown using attack speed. */
  public static void applyCooldown(Player player, float attackSpeed, int cooldownTime) {
    applyCooldown(player, Math.round(cooldownTime / attackSpeed));
  }

  /** Swings the entities hand without resetting cooldown */
  public static void swingHand(LivingEntity entity, InteractionHand hand, boolean updateSelf) {
    if (!entity.swinging || entity.swingTime >= entity.getCurrentSwingDuration() / 2 || entity.swingTime < 0) {
      entity.swingTime = -1;
      entity.swinging = true;
      entity.swingingArm = hand;
      if (!entity.level().isClientSide) {
        SwingArmPacket packet = new SwingArmPacket(entity, hand);
        if (updateSelf) {
          MantleNetwork.INSTANCE.sendToTrackingAndSelf(packet, entity);
        } else {
          MantleNetwork.INSTANCE.sendToTracking(packet, entity);
        }
      }
    }
  }

  @Override
  public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    this.attackReady = tag.getInt("attackReady");
    this.lastCooldown = tag.getInt("lastCooldown");
    this.enabled = tag.getInt("enabled");
  }

  @Override
  public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    tag.putInt("attackReady", this.attackReady);
    tag.putInt("lastCooldown", this.lastCooldown);
    tag.putInt("enabled", this.enabled);
  }
}
