package slimeknights.mantle.util;

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

/** Logic to handle offhand having its own cooldown. */
@RequiredArgsConstructor
public class OffhandCooldownTracker implements Component, EntityComponentInitializer {
  public static final ResourceLocation KEY = Mantle.getResource("offhand_cooldown");
  public static final Function<OffhandCooldownTracker,Float> COOLDOWN_TRACKER = OffhandCooldownTracker::getCooldown;
  private static final Function<OffhandCooldownTracker,Boolean> ATTACK_READY = OffhandCooldownTracker::isAttackReady;

  public OffhandCooldownTracker() {
    this.player = null;
  }

  public static final ComponentKey<OffhandCooldownTracker> CAPABILITY = ComponentRegistry.getOrCreate(KEY, OffhandCooldownTracker.class);

  @Override
  public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
    registry.registerForPlayers(CAPABILITY, OffhandCooldownTracker::attachCapability);
  }

  public static void register() {
  }

  private static OffhandCooldownTracker attachCapability(Player player) {
    return new OffhandCooldownTracker(player);
  }

  @Nullable
  private final Player player;
  private int lastCooldown = 0;
  private int attackReady = 0;
  private int enabled = 0;

  private int getTicksExisted() {
    return player == null ? 0 : player.tickCount;
  }

  public boolean isEnabled() {
    return enabled > 0;
  }

  public void setEnabled(boolean enable) {
    if (enable) {
      enabled++;
    } else {
      enabled--;
    }
  }

  public void applyCooldown(int cooldown) {
    this.lastCooldown = cooldown;
    this.attackReady = getTicksExisted() + cooldown;
  }

  public float getCooldown() {
    int ticksExisted = getTicksExisted();
    if (ticksExisted > this.attackReady || this.lastCooldown == 0) {
      return 1.0f;
    }
    return Mth.clamp((this.lastCooldown + ticksExisted - this.attackReady) / (float) this.lastCooldown, 0f, 1f);
  }

  public boolean isAttackReady() {
    return getTicksExisted() + this.lastCooldown > this.attackReady;
  }

  public static float getCooldown(Player player) {
    return CAPABILITY.maybeGet(player).map(COOLDOWN_TRACKER).orElse(1.0f);
  }

  public static void applyCooldown(Player player, int cooldown) {
    CAPABILITY.maybeGet(player).ifPresent(cap -> cap.applyCooldown(cooldown));
  }

  public static boolean isAttackReady(Player player) {
    return CAPABILITY.maybeGet(player).map(ATTACK_READY).orElse(true);
  }

  public static void applyCooldown(Player player, float attackSpeed, int cooldownTime) {
    applyCooldown(player, Math.round(cooldownTime / attackSpeed));
  }

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
