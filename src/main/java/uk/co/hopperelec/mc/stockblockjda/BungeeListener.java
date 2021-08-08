package uk.co.hopperelec.mc.stockblockjda;

import dev.aura.bungeechat.api.filter.FilterManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.platform.PlayerAdapter;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import javax.security.auth.login.LoginException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

public final class BungeeListener extends Plugin implements Listener {
    JDA jda;
    JDAListener jdaListener;
    UserManager userManager;
    PlayerAdapter<ProxiedPlayer> luckpermsPlayers;
    final String[] joinButtons = {"Hey","Yoo","Hi","Hello","Welcome"};
    final Map<String, Long> privateMessageChannels = new HashMap<>();

    @Override
    public void onEnable() {
        final JDABuilder builder;
        try {
            builder = JDABuilder.createDefault(Files.readString(Paths.get(getDataFolder().toString(),"token")));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE,"Please create a 'token' file in this plugin's directory (StockBlockJDA) containing only the token of the Discord bot");
            return;
        }
        builder.setActivity(Activity.playing("on Demonetized SMP"));
        builder.disableCache(CacheFlag.EMOTE, CacheFlag.ONLINE_STATUS, CacheFlag.VOICE_STATE);

        try {
            jda = builder.build();
        } catch (LoginException e) {
            System.out.println("Failed to login to the bot!");
            return;
        }

        DiscordToMinecraftMessageSender discordToMinecraftMessageSender = new DiscordToMinecraftMessageSender();
        getProxy().getPluginManager().registerCommand(this,discordToMinecraftMessageSender);
        jdaListener = new JDAListener(jda,discordToMinecraftMessageSender);
        jda.addEventListener(jdaListener);

        getProxy().getPluginManager().registerListener(this, this);
        privateMessageChannels.put("4ee1cc2f-f517-4aee-8f74-7f3f36be22d8",348083986989449216L);
        privateMessageChannels.put("eac2f553-d4ac-412a-a581-0324b57463af",264519033188122625L);
        getProxy().registerChannel("stockblockjda:node");

        FilterManager.addFilter("DiscordBotListener", new BungeeChatListener(jdaListener));

        final LuckPerms luckperms = LuckPermsProvider.get();
        luckpermsPlayers = luckperms.getPlayerAdapter(ProxiedPlayer.class);
        userManager = luckperms.getUserManager();
    }

    @Override
    public void onDisable() {
        final EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Network is going offline");
        embed.setColor(0xff0000);

        final MessageEmbed embedMsg = embed.build();
        jdaListener.stockblockGuildChannel.sendMessageEmbeds(embedMsg).queue();
        jdaListener.dSMPGuildChannel.sendMessageEmbeds(embedMsg).queue();

        jda.shutdown();
    }

    public void setAuthor(EmbedBuilder embed, CachedMetaData luckpermsData, ProxiedPlayer player) {
        final String prefix = luckpermsData.getPrefix();
        final String suffix = luckpermsData.getSuffix();
        String name = player.getDisplayName();
        if (prefix != null) name = prefix+name;
        if (suffix != null) name = name+suffix;
        embed.setAuthor(name.replace("&[0-f]",""),null,"https://crafatar.com/avatars/"+player.getUniqueId()+"?size=64&overlay");
        embed.setFooter(jdaListener.embedFooter);
    }

    public void setAuthor(EmbedBuilder embed, ProxiedPlayer player) {
        setAuthor(embed, luckpermsPlayers.getMetaData(player), player);
    }

    @EventHandler
    public void onPlayerJoinServer(ServerConnectedEvent event) {
        final String serverName = event.getServer().getInfo().getName();
        final EmbedBuilder embed = new EmbedBuilder();
        setAuthor(embed,event.getPlayer());
        embed.setDescription("has joined "+serverName);
        embed.setColor(0x779977);
        final MessageEmbed embedMsg = embed.build();

        final List<Button> buttons = new ArrayList<>();
        final String suffix = " "+event.getPlayer().getDisplayName()+"!"+"-"+serverName;
        for (String joinButton : joinButtons) buttons.add(Button.secondary(joinButton+suffix,joinButton));

        if (serverName.equals("dsmp")) jdaListener.dSMPGuildChannel.sendMessageEmbeds(embedMsg).setActionRow(buttons).queue();
        jdaListener.stockblockGuildChannel.sendMessageEmbeds(embedMsg).setActionRow(buttons).queue();
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent event) {
        userManager.loadUser(event.getPlayer().getUniqueId()).thenAcceptAsync(user -> {
            final EmbedBuilder embed = new EmbedBuilder();
            setAuthor(embed,user.getCachedData().getMetaData(),event.getPlayer());
            embed.setDescription("has left the network");
            embed.setColor(0xcc9999);

            final MessageEmbed embedMsg = embed.build();
            if (event.getPlayer().getServer().getInfo().getName().equals("dsmp")) jdaListener.dSMPGuildChannel.sendMessageEmbeds(embedMsg).queue();
            jdaListener.stockblockGuildChannel.sendMessageEmbeds(embedMsg).queue();
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
            jdaListener.dSMPGuildChannel.sendMessageEmbeds(embed.build()).queue();
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
                final Member member = jdaListener.dSMPGuild.retrieveMemberById(privateMessageChannels.get(msg[2])).complete();
                if (member != null)
                    member.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage(msg[3] + " entered your " + msg[1])).queue();
                return;
            }
            default -> title = "Received message on tag StockBlockJDA with unknown prefix '" + msg[0] + "'";
        }

        if (title == null) return;
        final EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title);
        final MessageEmbed embedMsg = embed.build();
        if (((Server) event.getSender()).getInfo().getName().equals("dsmp")) jdaListener.dSMPGuildChannel.sendMessageEmbeds(embedMsg).queue();
        jdaListener.stockblockGuildChannel.sendMessageEmbeds(embedMsg).queue();
    }
}
