package uk.co.hopperelec.mc.stockblockjda;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public final class JDAListener extends ListenerAdapter {
    JDA jda;
    Guild stockblockGuild;
    Guild dSMPGuild;
    public MessageChannel stockblockGuildChannel;
    public MessageChannel dSMPGuildChannel;
    ServerInfo dSMPServer;
    ProxyServer proxyServer;
    String embedFooter = "Made by hopperelec#3060";
    LinkedHashMap<Pattern,String> discordToMinecraftPatterns = new LinkedHashMap<>();
    Pattern removeResetPattern;

    public JDAListener(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        stockblockGuild = jda.getGuildById(847254345267281960L);
        if (stockblockGuild != null) stockblockGuildChannel = stockblockGuild.getTextChannelById(868639327884828722L);
        else System.out.println("Failed to obtain StockBlock guild!");
        dSMPGuild = jda.getGuildById(782748045200326667L);
        if (dSMPGuild != null) dSMPGuildChannel = dSMPGuild.getTextChannelById(868639078638309417L);
        else System.out.println("Failed to obtain Demonetized SMP guild!");

        proxyServer = ProxyServer.getInstance();
        dSMPServer = proxyServer.getServers().get("dSMP");

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Network is back online");
        embed.setColor(0x00ff00);
        embed.setFooter("Made by hopperelec#3060");
        stockblockGuildChannel.sendMessage("<@&868872369631019049>").setEmbeds(embed.build()).queue();
        dSMPGuildChannel.sendMessage("<@&868703367688519761>").setEmbeds(embed.build()).queue();

        discordToMinecraftPatterns.put(Pattern.compile("\\*\\*(.*?)\\*\\*"), "l");
        discordToMinecraftPatterns.put(Pattern.compile("\\*(.*?)\\*"), "o");
        discordToMinecraftPatterns.put(Pattern.compile("__(.*?)__"), "n");
        discordToMinecraftPatterns.put(Pattern.compile("~~(.*?)~~"), "m");
        discordToMinecraftPatterns.put(Pattern.compile("`(.*?)`"), "7");
        removeResetPattern = Pattern.compile("(§r)+");
    }

    public String discordToMinecraftFormat(String discordMsg) {
        for (Map.Entry<Pattern,String> entry : discordToMinecraftPatterns.entrySet()) discordMsg = entry.getKey().matcher(discordMsg).replaceAll(entry.getValue());
        return discordMsg;
    }

    public String getDiscordName(User user) {
        return user.getName()+"#"+user.getDiscriminator();
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        if (event.getMessage() == null) event.reply("Failed processing button event!").queue();
        else {
            String[] data = event.getComponentId().split("-");
            event.reply(getDiscordName(event.getUser())+" sent message '"+data[0]+"'").queue();
            sendMinecraftMessage(null,event.getUser(),data[0],event.getMessage().getJumpUrl(),proxyServer.getServers().get(data[1]).getPlayers(),new ArrayList<>());
        }
    }

    public void sendMinecraftMessage(Message reply, User author, String message, String jumpUrl, Collection<ProxiedPlayer> players, List<Message.Attachment> attachments) {
        ComponentBuilder text = new ComponentBuilder("Discord ").color(ChatColor.DARK_AQUA)
                .append(getDiscordName(author)).color(ChatColor.GREEN);

        if (reply != null) {
            String replyTitle;
            String replyAuthor = getDiscordName(reply.getAuthor());
            if (replyAuthor.equals("StockBlock#3858")) {
                MessageEmbed embed = reply.getEmbeds().get(0);
                MessageEmbed.AuthorInfo authorInfo = embed.getAuthor();
                if (authorInfo == null) replyAuthor = "StockBlock bot";
                else replyAuthor = embed.getAuthor().getName();
                replyTitle = embed.getTitle();
            } else replyTitle = discordToMinecraftFormat(reply.getContentDisplay());

            text.append(" (replying to " + replyAuthor + ")").color(ChatColor.GRAY)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(replyTitle)))
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, reply.getJumpUrl()));
        }
        text.append(": ").color(ChatColor.WHITE);

        String msg = discordToMinecraftFormat(message);
        String[] msgSpoilerSplits = msg.split("\\|\\|");
        if (msgSpoilerSplits.length > 1) {
            ComponentBuilder msgText = new ComponentBuilder(msgSpoilerSplits[0]);
            for (int i = 1; i < msgSpoilerSplits.length; i++) {
                if (i % 2 == 0) msgText.append(msgSpoilerSplits[i]).color(ChatColor.WHITE);
                else msgText.append("[SPOILER]").color(ChatColor.DARK_GRAY)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(msgSpoilerSplits[i])));
            }
            text.append(msgText.create())
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, jumpUrl));
        } else text.append(msg);

        String mediaType;
        for (Message.Attachment attachment : attachments) {
            if (attachment.isImage()) mediaType = "Image";
            else mediaType = "Video";
            text.append(" ").append(mediaType).color(ChatColor.BLUE).underlined(true)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(attachment.getUrl())))
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
        }

        for (ProxiedPlayer player : players) player.sendMessage(text.create());
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
            case "!dynmap":
                event.getTextChannel().sendMessage("https://dynmap.hopperelec.co.uk/").queue();
                break;

            case "!ip":
                event.getTextChannel().sendMessage("mc.hopperelec.co.uk").queue();
                break;

            case "!players": {
                final EmbedBuilder embed = new EmbedBuilder();
                embed.setFooter(embedFooter);
                final Map<String, ServerInfo> servers = proxyServer.getServers();
                final CompletableFuture<Void> cf = new CompletableFuture<>();
                final AtomicInteger counter = new AtomicInteger();
                final Object mutex = new Object();

                for (String serverName : servers.keySet()) {
                    servers.get(serverName).ping((res, err) -> {
                        synchronized (mutex) {
                            String players;
                            if (err == null) {
                                players = servers.get(serverName).getPlayers().toString();
                                if (players.length() > 5) players = players.substring(1, players.length() - 1).replaceAll("_", "\\\\_");
                                else players = "None";
                            } else players = "Offline";

                            embed.addField(serverName, players, false);
                            if (counter.incrementAndGet() >= servers.size()) cf.complete(null);
                        }
                    });
                }

                cf.thenAcceptAsync(unused -> event.getTextChannel().sendMessageEmbeds(embed.build()).queue());
                break;
            }

            default: {
                final Message msg = event.getMessage();
                final Collection<ProxiedPlayer> players;
                if (event.getChannel().getIdLong() == 782748045200326667L) players = dSMPServer.getPlayers();
                else players = proxyServer.getPlayers();
                sendMinecraftMessage(msg.getReferencedMessage(),event.getAuthor(),msg.getContentDisplay(),msg.getJumpUrl(),players,event.getMessage().getAttachments());
            }
        }

    }
}
