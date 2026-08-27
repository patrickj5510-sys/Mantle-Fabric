package slimeknights.mantle.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.Mantle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Command to inspect the vanilla harvest tier ordering used by Minecraft 1.21. */
public class HarvestTiersCommand {
  protected static final ResourceLocation HARVEST_TIERS = Mantle.getResource("item_tier_ordering.json");
  private static final String HARVEST_TIER_PATH = HARVEST_TIERS.getNamespace() + "/" + HARVEST_TIERS.getPath();

  private static final Component SUCCESS_LOG = Component.translatable("command.mantle.harvest_tiers.success_log");
  private static final Component EMPTY = Component.translatable("command.mantle.tag.empty");

  /** 1.21 no longer exposes Forge's global TierSortingRegistry, so keep the vanilla ordering for this debug command. */
  private static final List<TierEntry> TIERS = List.of(
    new TierEntry(ResourceLocation.withDefaultNamespace("wood"), Tiers.WOOD),
    new TierEntry(ResourceLocation.withDefaultNamespace("gold"), Tiers.GOLD),
    new TierEntry(ResourceLocation.withDefaultNamespace("stone"), Tiers.STONE),
    new TierEntry(ResourceLocation.withDefaultNamespace("iron"), Tiers.IRON),
    new TierEntry(ResourceLocation.withDefaultNamespace("diamond"), Tiers.DIAMOND),
    new TierEntry(ResourceLocation.withDefaultNamespace("netherite"), Tiers.NETHERITE)
  );

  private record TierEntry(ResourceLocation id, Tier tier) {}

  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand) {
    subCommand.requires(sender -> sender.hasPermission(MantleCommand.PERMISSION_EDIT_SPAWN))
      .then(Commands.literal("save").executes(source -> run(source, true)))
      .then(Commands.literal("log").executes(source -> run(source, false)))
      .then(Commands.literal("list").executes(HarvestTiersCommand::list));
  }

  private static Component getTagComponent(TagKey<Block> tag) {
    ResourceLocation id = tag.location();
    return Component.literal(id.toString()).withStyle(style -> style.withUnderlined(true).withClickEvent(
      new ClickEvent(Action.SUGGEST_COMMAND, "/mantle dump_tag " + Registries.BLOCK.location() + " " + id + " save")));
  }

  private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    MutableComponent output = Component.translatable("command.mantle.harvest_tiers.success_list");
    if (TIERS.isEmpty()) {
      output.append("\n* ").append(EMPTY);
    } else {
      for (TierEntry entry : TIERS) {
        output.append("\n* ");
        TagKey<Block> tag = entry.tier().getIncorrectBlocksForDrops();
        if (tag != null) {
          output.append(Component.translatable("command.mantle.harvest_tiers.tag", entry.id(), getTagComponent(tag)));
        } else {
          output.append(Component.translatable("command.mantle.harvest_tiers.no_tag", entry.id()));
        }
      }
    }
    context.getSource().sendSuccess(() -> output, true);
    return TIERS.size();
  }

  private static int run(CommandContext<CommandSourceStack> context, boolean saveFile) throws CommandSyntaxException {
    JsonArray entries = new JsonArray();
    for (TierEntry entry : TIERS) {
      entries.add(entry.id().toString());
    }
    JsonObject json = new JsonObject();
    json.add("order", entries);

    if (saveFile) {
      File output = new File(DumpAllTagsCommand.getOutputFile(context), HARVEST_TIER_PATH);
      Path path = output.toPath();
      try {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
          writer.write(DumpTagCommand.GSON.toJson(json));
        }
      } catch (IOException ex) {
        Mantle.logger.error("Couldn't save harvest tiers to {}", path, ex);
      }
      context.getSource().sendSuccess(() -> Component.translatable("command.mantle.harvest_tiers.success_save", DumpAllTagsCommand.getOutputComponent(output)), true);
    } else {
      context.getSource().sendSuccess(() -> SUCCESS_LOG, true);
      Mantle.logger.info("Dump of harvest tiers:\n{}", DumpTagCommand.GSON.toJson(json));
    }
    return TIERS.size();
  }
}
