package uk.co.hopperelec.mc.stockblockjda;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.Button;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.platform.PlayerAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

final class BungeeHandler implements Listener {
    private final StockBlockJDA plugin;
    private final PlayerAdapter<ProxiedPlayer> luckpermsPlayers;
    private final UserManager userManager;
    final ProxyServer proxyServer = ProxyServer.getInstance();

    BungeeHandler(StockBlockJDA plugin) {
        this.plugin = plugin;
        new BungeeChatListener(plugin.jdaHandler);

        final LuckPerms luckperms = LuckPermsProvider.get();
        luckpermsPlayers = luckperms.getPlayerAdapter(ProxiedPlayer.class);
        userManager = luckperms.getUserManager();
    }

    private void setAuthor(EmbedBuilder embed, CachedMetaData luckpermsData, ProxiedPlayer player) {
        final String prefix = luckpermsData.getPrefix();
        final String suffix = luckpermsData.getSuffix();
        String name = player.getDisplayName();
        if (prefix != null) name = prefix+name;
        if (suffix != null) name = name+suffix;
        embed.setAuthor(name.replace("&[0-f]",""),null,"https://crafatar.com/avatars/"+player.getUniqueId()+"?size=64&overlay");
    }

    private void setAuthor(EmbedBuilder embed, ProxiedPlayer player) {
        setAuthor(embed, luckpermsPlayers.getMetaData(player), player);
    }

    @EventHandler
    public void onPlayerJoinServer(ServerConnectedEvent event) {
        final String serverName = event.getServer().getInfo().getName();
        final EmbedBuilder embed = new EmbedBuilder();
        setAuthor(embed,event.getPlayer());
        embed.setDescription("has joined "+serverName);
        embed.setColor(0x779977);

        final List<Button> buttons = plugin.jdaHandler.getJoinButtons(event.getPlayer().getDisplayName(),serverName);
        if (serverName.equals("dsmp")) plugin.jdaHandler.sendTodSMPGuild(embed).setActionRow(buttons).queue();
        plugin.jdaHandler.sendToStockBlockGuild(embed).setActionRow(buttons).queue();
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent event) {
        userManager.loadUser(event.getPlayer().getUniqueId()).thenAcceptAsync(user -> {
            final EmbedBuilder embed = new EmbedBuilder();
            setAuthor(embed,user.getCachedData().getMetaData(),event.getPlayer());
            embed.setDescription("has left the network");
            embed.setColor(0xcc9999);

            if (event.getPlayer().getServer().getInfo().getName().equals("dsmp")) plugin.jdaHandler.sendTodSMPGuild(embed).queue();
            plugin.jdaHandler.sendToStockBlockGuild(embed).queue();
        });
    }

    @EventHandler
    public void onPlayerSwitch(ServerSwitchEvent event) {
        if (event.getFrom() == null) return;
        if (event.getFrom().getName().equals("dsmp")) {
            final EmbedBuilder embed = new EmbedBuilder();
            setAuthor(embed,event.getPlayer());
            embed.setDescription("has left the server");
            embed.setColor(0xcc9999);
            plugin.jdaHandler.sendTodSMPGuild(embed).queue();
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) throws IOException {
        if (!event.getTag().equals("stockblockjda:node")) return;
        if (!(event.getSender() instanceof Server)) return;
        final String[] msg = new DataInputStream(new ByteArrayInputStream(event.getData())).readUTF().split("\\|");

        final String title;
        switch (msg[0]) {
            case "death" -> title = msg[1];
            case "advancement" -> title = msg[1] + " made advancement " + msg[2];
            case "detectionArea" -> {
                plugin.jdaHandler.getUserFromUUID(msg[2]).openPrivateChannel().flatMap(channel -> channel.sendMessage(msg[3] + " entered your " + msg[1])).queue();
                return;
            }
            default -> title = "Received message on tag StockBlockJDA with unknown prefix '" + msg[0] + "'";
        }

        if (title == null) return;
        final EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        if (((Server) event.getSender()).getInfo().getName().equals("dsmp")) plugin.jdaHandler.sendTodSMPGuild(embed).queue();
        plugin.jdaHandler.sendToStockBlockGuild(embed).queue();
    }
}
