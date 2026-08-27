package slimeknights.mantle.client.screen;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@SuppressWarnings("unused")
public class TabsWidget extends Widget {
  private static final ResourceLocation creativeInventoryTabs = ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tabs.png");

  private final ElementScreen[] tabActive = new ElementScreen[3];
  private final ElementScreen[] tab = new ElementScreen[3];
  public ResourceLocation tabsResource = creativeInventoryTabs;
  public int yOffset = 4;
  public int spacing = 2;
  public int selected;
  public int highlighted;
  protected List<ItemStack> icons = Lists.newArrayList();
  private final MultiModuleScreen<?> parent;
  private boolean clicked = false;
  private boolean leftMouseDown = false;

  public TabsWidget(MultiModuleScreen<?> parent, ElementScreen tabLeft, ElementScreen tabCenter, ElementScreen tabRight, ElementScreen activeLeft, ElementScreen activeCenter, ElementScreen activeRight) {
    this.parent = parent;
    this.tab[0] = tabLeft;
    this.tab[1] = tabCenter;
    this.tab[2] = tabRight;
    this.tabActive[0] = activeLeft;
    this.tabActive[1] = activeCenter;
    this.tabActive[2] = activeRight;
    this.selected = 0;
  }

  public void addTab(ItemStack icon) {
    this.icons.add(icon);
  }

  public void clear() {
    this.selected = 0;
    this.icons.clear();
  }

  @Override
  public void handleMouseClicked(int mouseX, int mouseY, int mouseButton) {
    if (mouseButton == 0) {
      this.leftMouseDown = true;
    }
  }

  @Override
  public void handleMouseReleased() {
    this.leftMouseDown = false;
  }

  public void update(int mouseX, int mouseY) {
    mouseX -= this.xPos;
    mouseY -= this.yPos;
    this.highlighted = -1;
    if (mouseY >= 0 && mouseY <= this.tab[1].h) {
      int x = 0;
      for (int i = 0; i < this.icons.size(); i++) {
        if (mouseX >= x && mouseX < x + this.tab[1].w) {
          this.highlighted = i;
          break;
        }
        x += this.tab[1].w + this.spacing;
      }
    }
    if (this.clicked) {
      if (!this.leftMouseDown) {
        this.clicked = false;
      }
      return;
    } else if (this.leftMouseDown) {
      this.clicked = true;
    } else {
      return;
    }
    if (this.highlighted > -1) {
      this.selected = this.highlighted;
    }
  }

  @Override
  public void draw(GuiGraphics guiGraphics, ResourceLocation texture) {
    int y = this.yPos + this.yOffset;
    for (int i = 0; i < this.icons.size(); i++) {
      int x = this.xPos + i * this.tab[0].w;
      if (i > 0) {
        x += i * this.spacing;
      }
      ElementScreen[] toDraw = i == this.selected ? this.tabActive : this.tab;
      ElementScreen actualTab;
      if (i == 0 && x == this.parent.cornerX) {
        actualTab = toDraw[0];
      } else if (x == this.parent.cornerX + this.parent.width) {
        actualTab = toDraw[2];
      } else {
        actualTab = toDraw[1];
      }
      actualTab.draw(guiGraphics, this.tabsResource, x, y);
      ItemStack icon = this.icons.get(i);
      if (icon != null) {
        this.drawItemStack(guiGraphics, icon, x + (actualTab.w - 16) / 2, y + (actualTab.h - 16) / 2);
      }
    }
  }

  private void drawItemStack(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate(0, 0, 232f);
    guiGraphics.renderItem(stack, x, y);
    guiGraphics.pose().popPose();
  }
}
