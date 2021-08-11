package uk.co.hopperelec.mc.stockblockjda;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.*;
import java.util.regex.Pattern;

public final class DiscordToMinecraftMessageSender extends Command {
    private final List<ProxiedPlayer> blacklistedReceivingPlayers = new ArrayList<>();
    private final Pattern removeResetPattern;
    private final LinkedHashMap<Pattern,String> discordToMinecraftPatterns = new LinkedHashMap<>();

    public DiscordToMinecraftMessageSender() {
        super("togglediscord");

        discordToMinecraftPatterns.put(Pattern.compile("\\*\\*(.*?)\\*\\*"), "l");
        discordToMinecraftPatterns.put(Pattern.compile("\\*(.*?)\\*"), "o");
        discordToMinecraftPatterns.put(Pattern.compile("__(.*?)__"), "n");
        discordToMinecraftPatterns.put(Pattern.compile("~~(.*?)~~"), "m");
        discordToMinecraftPatterns.put(Pattern.compile("`(.*?)`"), "7");
        removeResetPattern = Pattern.compile("(§r)+");
    }

    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer player) {
            String change = "w";
            if (!blacklistedReceivingPlayers.remove(player)) {
                change = " longer";
                blacklistedReceivingPlayers.add(player);
            }
            player.sendMessage(new TextComponent("You will no"+change+" receive Discord messages"));
        }
    }

    private String discordToMinecraftFormat(String discordMsg) {
        for (Map.Entry<Pattern,String> entry : discordToMinecraftPatterns.entrySet())
            discordMsg = entry.getKey().matcher(discordMsg).replaceAll("§"+entry.getValue()+"$1§r");
        return removeResetPattern.matcher(discordMsg).replaceAll("§r");
    }

    private void applyReplyEvents(ComponentBuilder text, Message reply) {
        final String replyTitle;
        String replyAuthor = JDAHandler.getDiscordName(reply.getAuthor());
        if (replyAuthor.equals("StockBlock#3858")) {
            final MessageEmbed embed = reply.getEmbeds().get(0);
            final MessageEmbed.AuthorInfo authorInfo = embed.getAuthor();
            if (authorInfo == null) replyAuthor = "StockBlock bot";
            else replyAuthor = embed.getAuthor().getName();
            replyTitle = embed.getTitle();
        } else replyTitle = discordToMinecraftFormat(reply.getContentDisplay());

        text.append(" (replying to " + replyAuthor + ")").color(ChatColor.GRAY)
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(replyTitle)))
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, reply.getJumpUrl()));
    }

    private ComponentBuilder applySpoilerEvents(String msg, ComponentBuilder text) {
        final String[] msgSpoilerSplits = msg.split("\\|\\|");
        if (msgSpoilerSplits.length > 1) {
            ComponentBuilder msgText = new ComponentBuilder(msgSpoilerSplits[0]);
            for (int i = 1; i < msgSpoilerSplits.length; i++) {
                if (i % 2 == 0) msgText.append(msgSpoilerSplits[i]).color(ChatColor.WHITE);
                else msgText.append("[SPOILER]").color(ChatColor.DARK_GRAY)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(msgSpoilerSplits[i])));
            }
            return text.append(msgText.create());
        } else return text.append(msg);
    }

    private void applyAttachments(List<Message.Attachment> attachments, ComponentBuilder text) {
        String mediaType;
        for (Message.Attachment attachment : attachments) {
            if (attachment.isImage()) mediaType = "Image";
            else mediaType = "Video";
            text.append(" ").append(mediaType).color(ChatColor.BLUE).underlined(true)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(attachment.getUrl())))
                    .event(new ClickEvent(ClickEvent.Action.OPEN_URL, attachment.getUrl()));
        }
    }

    void sendMinecraftMessage(Message reply, User author, String message, String jumpUrl, Collection<ProxiedPlayer> players, List<Message.Attachment> attachments) {
        final ComponentBuilder text = new ComponentBuilder("Discord ").color(ChatColor.DARK_AQUA)
                .append(JDAHandler.getDiscordName(author)).color(ChatColor.GREEN);

        if (reply != null) applyReplyEvents(text,reply);
        text.append(": ").color(ChatColor.WHITE);
        final String msg = discordToMinecraftFormat(message);
        applySpoilerEvents(msg,text)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, jumpUrl));
        applyAttachments(attachments,text);

        BaseComponent[] textToSend = text.create();
        for (ProxiedPlayer player : players) {
            if (!blacklistedReceivingPlayers.contains(player)) player.sendMessage(textToSend);
        }
    }
}
