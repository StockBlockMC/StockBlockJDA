package uk.co.hopperelec.mc.stockblockjda;

import dev.aura.bungeechat.api.account.BungeeChatAccount;
import dev.aura.bungeechat.api.enums.ChannelType;
import dev.aura.bungeechat.api.filter.BungeeChatFilter;
import dev.aura.bungeechat.api.filter.FilterManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BungeeChatListener implements BungeeChatFilter {
    private final JDAHandler jdaHandler;

    BungeeChatListener(JDAHandler jdaHandler) {
        this.jdaHandler = jdaHandler;
        FilterManager.addFilter("DiscordBotListener", this);
    }

    @Override
    public String applyFilter(BungeeChatAccount sender, String message) {
        if (sender.getChannelType() != ChannelType.STAFF && sender.getChannelType() != ChannelType.HELP) {
            final EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(message.replace("&[0-f]", ""));
            embed.setAuthor((sender.getStoredPrefix().orElse("") + sender.getDisplayName() + sender.getStoredSuffix().orElse("")).replace("&[0-f]", ""), null, "https://crafatar.com/avatars/" + sender.getUniqueId() + "?size=64&overlay");
            embed.setColor(0x888888);
            final String serverName = sender.getServerName();

            final List<Button> buttons = new ArrayList<>();
            if (message.toLowerCase(Locale.ROOT).contains("dynmap")) {
                buttons.add(Button.link("https://dynmap.hopperelec.co.uk", "DynMap"));
                buttons.add(Button.secondary("https://dynmap.hopperelec.co.uk-" + serverName, "Send DynMap"));
            }

            if (serverName.equals("dsmp")) {
                MessageAction messageaction = jdaHandler.sendTodSMPGuild(embed);
                if (buttons.size() > 0) messageaction = messageaction.setActionRow(buttons);
                messageaction.queue();
                embed.setDescription("Sent to Demonetized SMP");
            } else embed.setDescription("Sent to " + serverName);
            MessageAction messageaction = jdaHandler.sendToStockBlockGuild(embed);
            if (buttons.size() > 0) messageaction = messageaction.setActionRow(buttons);
            messageaction.queue();
        }

        return message;
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
