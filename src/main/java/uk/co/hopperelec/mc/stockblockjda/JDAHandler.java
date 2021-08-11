package uk.co.hopperelec.mc.stockblockjda;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

final class JDAHandler extends ListenerAdapter {
    private JDA jda;
    private MessageChannel stockblockGuildChannel;
    private MessageChannel dSMPGuildChannel;
    private final StockBlockJDA plugin;
    private final Map<String, User> uuidToUser = new HashMap<>();
    private final String[] joinButtons = {"Hey","Yoo","Hi","Hello","Welcome"};
    private final String embedFooter = "Made by hopperelec#3060";

    JDAHandler(StockBlockJDA plugin) {
        this.plugin = plugin;
    }

    void shutdown() {
        jda.shutdown();
    }

    void setup(Plugin plugin) throws LoginException {
        final JDABuilder builder;
        try {
            builder = JDABuilder.createDefault(Files.readString(Paths.get(plugin.getDataFolder().toString(),"token")));
        } catch (IOException e) {
            throw new LoginException("Please create a 'token' file in this plugin's directory (StockBlockJDA) containing only the token of the Discord bot");
        }
        builder.setActivity(Activity.playing("on Demonetized SMP"));
        builder.disableCache(CacheFlag.EMOTE, CacheFlag.ONLINE_STATUS, CacheFlag.VOICE_STATE);
        jda = builder.build();
        jda.addEventListener(this);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final Guild stockblockGuild = jda.getGuildById(847254345267281960L);
        if (stockblockGuild != null) stockblockGuildChannel = stockblockGuild.getTextChannelById(868639327884828722L);
        else throw new NullPointerException("Failed to obtain StockBlock guild!");
        final Guild dSMPGuild = jda.getGuildById(782748045200326667L);
        if (dSMPGuild != null) dSMPGuildChannel = dSMPGuild.getTextChannelById(868639078638309417L);
        else throw new NullPointerException("Failed to obtain Demonetized SMP guild!");

        final Map<String, Long> uuidToDiscordID = new HashMap<>();
        uuidToDiscordID.put("4ee1cc2f-f517-4aee-8f74-7f3f36be22d8",348083986989449216L);
        uuidToDiscordID.put("eac2f553-d4ac-412a-a581-0324b57463af",264519033188122625L);
        for (String uuid : uuidToDiscordID.keySet()) {
            final Member member = dSMPGuild.retrieveMemberById(uuidToDiscordID.get(uuid)).complete();
            if (member != null) uuidToUser.put(uuid,member.getUser());
        }

        final EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Network is back online");
        embed.setColor(0x00ff00);
        embed.setFooter("Made by hopperelec#3060");
        stockblockGuildChannel.sendMessage("<@&868872369631019049>").setEmbeds(embed.build()).queue();
        dSMPGuildChannel.sendMessage("<@&868703367688519761>").setEmbeds(embed.build()).queue();
    }

    static String getDiscordName(User user) {
        return user.getName()+"#"+user.getDiscriminator();
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        if (event.getMessage() == null) event.reply("Failed processing button event!").queue();
        else {
            final String[] data = event.getComponentId().split("-");
            event.reply(getDiscordName(event.getUser())+" sent message '"+data[0]+"'").queue();
            plugin.discordToMinecraftMessageSender.sendMinecraftMessage(null, event.getUser(),
                    data[0],event.getMessage().getJumpUrl(),
                    plugin.bungeeHandler.proxyServer.getServers().get(data[1]).getPlayers(),
                    new ArrayList<>()
            );
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        final String rawContent = event.getMessage().getContentRaw();

        if (event.getAuthor().isBot()) return;
        final long id = event.getChannel().getIdLong();
        if (!(id == 868639327884828722L || id == 868639078638309417L)) return;
        final MessageType messageType = event.getMessage().getType();
        if (!(messageType == MessageType.DEFAULT || messageType == MessageType.INLINE_REPLY)) return;

        switch (rawContent) {
            case "!dynmap" -> event.getTextChannel().sendMessage("https://dynmap.hopperelec.co.uk/").queue();
            case "!ip" -> event.getTextChannel().sendMessage("mc.hopperelec.co.uk").queue();
            case "!players" -> {
                final EmbedBuilder embed = new EmbedBuilder();
                embed.setFooter(embedFooter);
                final Map<String, ServerInfo> servers = plugin.bungeeHandler.proxyServer.getServers();
                final CompletableFuture<Void> cf = new CompletableFuture<>();
                final AtomicInteger counter = new AtomicInteger();
                final Object mutex = new Object();

                for (String serverName : servers.keySet()) {
                    servers.get(serverName).ping((res, err) -> {
                        synchronized (mutex) {
                            String players;
                            if (err == null) {
                                players = servers.get(serverName).getPlayers().toString();
                                if (players.length() > 5)
                                    players = players.substring(1, players.length() - 1).replaceAll("_", "\\\\_");
                                else players = "None";
                            } else players = "Offline";

                            embed.addField(serverName, players, false);
                            if (counter.incrementAndGet() >= servers.size()) cf.complete(null);
                        }
                    });
                }

                cf.thenAcceptAsync(unused -> event.getTextChannel().sendMessageEmbeds(embed.build()).queue());
            }
            default -> {
                final Message msg = event.getMessage();
                final Collection<ProxiedPlayer> players;
                if (event.getChannel().getIdLong() == 782748045200326667L) players = plugin.bungeeHandler.proxyServer.getServerInfo("dSMP").getPlayers();
                else players = plugin.bungeeHandler.proxyServer.getPlayers();
                plugin.discordToMinecraftMessageSender.sendMinecraftMessage(msg.getReferencedMessage(), event.getAuthor(), msg.getContentDisplay(), msg.getJumpUrl(), players, event.getMessage().getAttachments());
            }
        }
    }

    MessageAction sendToStockBlockGuild(MessageEmbed embed) {
        return stockblockGuildChannel.sendMessageEmbeds(embed);
    }

    MessageAction sendTodSMPGuild(MessageEmbed embed) {
        return dSMPGuildChannel.sendMessageEmbeds(embed);
    }

    MessageAction sendToStockBlockGuild(EmbedBuilder embed) {
        embed.setFooter(embedFooter);
        return sendToStockBlockGuild(embed.build());
    }

    MessageAction sendTodSMPGuild(EmbedBuilder embed) {
        embed.setFooter(embedFooter);
        return sendTodSMPGuild(embed.build());
    }

    List<Button> getJoinButtons(String playername, String serverName) {
        final List<Button> buttons = new ArrayList<>();
        final String suffix = " "+playername+"!"+"-"+serverName;
        for (String joinButton : joinButtons) buttons.add(Button.secondary(joinButton+suffix,joinButton));
        return buttons;
    }

    User getUserFromUUID(String uuid) {
        return uuidToUser.get(uuid);
    }
}
