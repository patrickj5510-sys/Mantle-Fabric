package slimeknights.mantle.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fabricators_of_create.porting_lib.event.client.OverlayRenderCallback;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.GameType;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.book.BookLoader;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.mantle.client.model.FallbackModelLoader;
import slimeknights.mantle.client.model.NBTKeyModel;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.connected.ConnectedModel;
import slimeknights.mantle.client.model.fluid.FluidsModel;
import slimeknights.mantle.client.model.inventory.InventoryModel;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.render.MantleShaders;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.mantle.fluid.tooltip.FluidTooltipHandler;
import slimeknights.mantle.network.MantleNetwork;
import slimeknights.mantle.registration.MantleRegistrations;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.util.OffhandCooldownTracker;

import java.util.Map;
import java.util.function.Function;

import static net.minecraft.client.renderer.Sheets.SIGN_SHEET;

@SuppressWarnings("unused")
public class ClientEvents implements ClientModInitializer {
  private static final Function<OffhandCooldownTracker,Float> COOLDOWN_TRACKER = OffhandCooldownTracker::getCooldown;

  public static void onConstruct() {
    FluidTextureManager.init();
  }

  static void registerEntityRenderers() {
    BlockEntityRenderers.register(MantleRegistrations.SIGN, SignRenderer::new);
  }

  static void registerListeners() {
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(ModelHelper.LISTENER);
    ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new BookLoader());
    ResourceColorManager.init();
    FluidTooltipHandler.init();
  }

  @Override
  public void onInitializeClient() {
    RegistrationHelper.forEachWoodType(woodType -> {
      ResourceLocation location = ResourceLocation.parse(woodType.name());
      Sheets.SIGN_MATERIALS.put(woodType, new Material(SIGN_SHEET,
        ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "entity/signs/" + location.getPath())));
    });

    BookLoader.registerBook(Mantle.getResource("test"), new FileRepository(Mantle.getResource("books/test")));
    registerEntityRenderers();
    registerListeners();
    CoreShaderRegistrationCallback.EVENT.register(MantleShaders::registerShaders);
    RegisterGeometryLoadersCallback.EVENT.register(ClientEvents::registerModelLoaders);
    commonSetup();
    MantleNetwork.INSTANCE.network.initClientListener();
  }

  static void registerModelLoaders(Map<ResourceLocation,IGeometryLoader<?>> loaders) {
    loaders.put(Mantle.getResource("connected"), ConnectedModel.LOADER);
    loaders.put(Mantle.getResource("item_layer"), MantleItemLayerModel.LOADER);
    loaders.put(Mantle.getResource("colored_block"), ColoredBlockModel.LOADER);
    loaders.put(Mantle.getResource("fallback"), FallbackModelLoader.INSTANCE);
    loaders.put(Mantle.getResource("nbt_key"), NBTKeyModel.LOADER);
    loaders.put(Mantle.getResource("retextured"), RetexturedModel.LOADER);
    loaders.put(Mantle.getResource("inventory"), InventoryModel.LOADER);
    loaders.put(Mantle.getResource("fluids"), FluidsModel.LOADER);
  }

  static void commonSetup() {
    OverlayRenderCallback.EVENT.register(new ExtraHeartRenderHandler()::renderHealthbar);
    OverlayRenderCallback.EVENT.register(ClientEvents::renderOffhandAttackIndicator);
  }

  /**
   * Renders Mantle's offhand cooldown indicator in the crosshair overlay.
   * Porting Lib 1.21 no longer exposes a separate hotbar overlay callback, so the hotbar-specific
   * placement will be reattached through Fabric's HUD layer API in the client-polish pass.
   */
  private static boolean renderOffhandAttackIndicator(GuiGraphics guiGraphics, float partialTicks, Window window, OverlayRenderCallback.Types overlay) {
    if (overlay != OverlayRenderCallback.Types.CROSSHAIRS) {
      return false;
    }

    Minecraft minecraft = Minecraft.getInstance();
    Options settings = minecraft.options;
    AttackIndicatorStatus indicator = settings.attackIndicator().get();
    if (minecraft.player == null || minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR || indicator != AttackIndicatorStatus.CROSSHAIR) {
      return false;
    }

    float cooldown = OffhandCooldownTracker.CAPABILITY.maybeGet(minecraft.player)
      .filter(OffhandCooldownTracker::isEnabled)
      .map(COOLDOWN_TRACKER)
      .orElse(1.0f);
    if (cooldown >= 1.0f || !minecraft.options.getCameraType().isFirstPerson()) {
      return false;
    }

    if (!settings.renderDebug || settings.hideGui || minecraft.player.isReducedDebugInfo() || settings.reducedDebugInfo().get()) {
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
        GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
        GlStateManager.SourceFactor.ONE,
        GlStateManager.DestFactor.ZERO);
      int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
      int y = (scaledHeight / 2) - 14 + (2 * (scaledHeight % 2));
      int x = minecraft.getWindow().getGuiScaledWidth() / 2 - 8;
      int width = (int)(cooldown * 17.0F);
      guiGraphics.blit(Gui.GUI_ICONS_LOCATION, x, y, 36, 94, 16, 4);
      guiGraphics.blit(Gui.GUI_ICONS_LOCATION, x, y, 52, 94, width, 4);
    }
    return false;
  }
}
