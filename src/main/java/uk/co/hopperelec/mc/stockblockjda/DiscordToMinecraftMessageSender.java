package uk.co.hopperelec.mc.stockblockjda;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.List;

public final class DiscordToMinecraftMessageSender extends Command {
    private final List<ProxiedPlayer> blacklistedReceivingPlayers = new ArrayList<>();

    public DiscordToMinecraftMessageSender() {
        super("togglediscord");
    }

    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer player) {
            if (blacklistedReceivingPlayers.contains(player)) {
                blacklistedReceivingPlayers.remove(player);
                player.sendMessage(new TextComponent("You will now receive Discord messages"));
            } else {
                blacklistedReceivingPlayers.add(player);
                player.sendMessage(new TextComponent("You will no longer receive Discord messages"));
            }
        }
    }

    public boolean isBlacklisted(ProxiedPlayer player) {
        return blacklistedReceivingPlayers.contains(player);
    }
}
