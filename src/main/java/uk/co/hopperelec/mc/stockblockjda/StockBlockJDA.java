package uk.co.hopperelec.mc.stockblockjda;

import net.dv8tion.jda.api.EmbedBuilder;
import net.md_5.bungee.api.plugin.Plugin;

import javax.security.auth.login.LoginException;

public final class StockBlockJDA extends Plugin {
    final JDAHandler jdaHandler = new JDAHandler(this);
    final BungeeHandler bungeeHandler = new BungeeHandler(this);
    final DiscordToMinecraftMessageSender discordToMinecraftMessageSender = new DiscordToMinecraftMessageSender();

    @Override
    public void onEnable() {
        try {
            jdaHandler.setup(this);
        } catch (LoginException e) {
            getLogger().severe(e.getMessage());
            return;
        }
        getProxy().getPluginManager().registerCommand(this,discordToMinecraftMessageSender);
        getProxy().getPluginManager().registerListener(this, bungeeHandler);
        getProxy().registerChannel("stockblockjda:node");
    }

    @Override
    public void onDisable() {
        final EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Network is going offline");
        embed.setColor(0xff0000);
        jdaHandler.sendToStockBlockGuild(embed).queue();
        jdaHandler.sendTodSMPGuild(embed).queue();
        jdaHandler.shutdown();
    }
}
