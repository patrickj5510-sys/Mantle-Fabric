package slimeknights.mantle.client.book.data.element;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class TextData {
  @Deprecated(forRemoval = true)
  public static final TextData LINEBREAK = new TextData().linebreak(true);

  @Nullable
  public String text = "";
  public String color = "black";
  public int rgbColor = 0;
  public boolean useOldColor = true;
  public boolean bold = false;
  public boolean italic = false;
  public boolean underlined = false;
  public boolean strikethrough = false;
  public boolean obfuscated = false;
  public boolean paragraph = false;
  public boolean linebreak = false;
  public boolean dropshadow = false;
  public float scale = 1F;
  public String action = "";
  @Nullable
  public Component[] tooltip = null;

  public TextData() {
    this("");
  }

  public TextData(String text) {
    this.text = text;
  }

  public TextData linebreak(boolean linebreak) {
    this.linebreak = linebreak;
    return this;
  }

  public String getText() {
    return text == null ? "" : text;
  }
}
