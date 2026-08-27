package slimeknights.mantle.client.book.data.element;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class TextComponentData {
  @Deprecated
  public static final TextComponentData LINEBREAK = new TextComponentData((Component)null).linebreak(true);

  @Nullable
  public Component text;
  public boolean isParagraph = false;
  public boolean linebreak = false;
  public boolean dropShadow = false;
  public float scale = 1F;
  public String action = "";
  @Nullable
  public Component[] tooltips = null;

  public TextComponentData(@Nullable Component text) {
    this.text = text;
  }

  public TextComponentData(String text) {
    this(Component.literal(text));
  }

  public TextComponentData linebreak(boolean linebreak) {
    this.linebreak = linebreak;
    return this;
  }
}
