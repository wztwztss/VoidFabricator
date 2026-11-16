package com.wztwzt.voidfabricator.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

/** Localized player-facing documentation for the placed fabricator. */
public class ItemBlockVoidFabricator extends ItemBlock {

    public ItemBlockVoidFabricator(Block block) {
        super(block);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        add(tooltip, "header", EnumChatFormatting.AQUA);
        add(tooltip, "description.1", EnumChatFormatting.GRAY);
        add(tooltip, "description.2", EnumChatFormatting.GRAY);
        add(tooltip, "requirement", EnumChatFormatting.GOLD);
        add(tooltip, "no_private_cpu", EnumChatFormatting.DARK_AQUA);
        add(tooltip, "consumption", EnumChatFormatting.GRAY);
        add(tooltip, "buffer", EnumChatFormatting.BLUE);
        add(tooltip, "speed", EnumChatFormatting.GRAY);
        add(tooltip, "install", EnumChatFormatting.YELLOW);
        add(tooltip, "status", EnumChatFormatting.YELLOW);
        add(tooltip, "protection", EnumChatFormatting.RED);
        if (advanced) {
            add(tooltip, "advanced", EnumChatFormatting.DARK_GRAY);
        }
    }

    private static void add(List<String> tooltip, String suffix, EnumChatFormatting color) {
        tooltip.add(color + StatCollector.translateToLocal("voidfabricator.tooltip.block." + suffix));
    }
}
