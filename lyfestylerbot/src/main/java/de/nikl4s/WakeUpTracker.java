package de.nikl4s;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class WakeUpTracker extends ListenerAdapter {
    private final PointsManager pointsManager;

    public WakeUpTracker(PointsManager pointsManager) {
        this.pointsManager = pointsManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        MessageChannelUnion channel = event.getChannel();
        String channelName = channel.getName();
        if (channelName == null || !channelName.equalsIgnoreCase("╠►frühe-vögel")) return;

        Message msg = event.getMessage();
        String content = msg.getContentRaw();
        if (!content.toLowerCase().startsWith("!awake")) return;

        List<Attachment> atts = msg.getAttachments();
        boolean hasImage = atts.stream().anyMatch(Attachment::isImage);
        if (!hasImage) {
            channel.sendMessage("Bild fehlt für !awake").queue();
            return;
        }

        String userId = event.getAuthor().getId();
        String displayName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        PointsManager.WakeResult res = pointsManager.handleAwake(userId, displayName, today, now);
        if (!res.accepted) return;

        if (res.isFirst) {
            channel.sendMessage("<@" + userId + "> ist der Frühste Vogel und hat den Wurm :worm: gefangen!").queue();
            // Zweite Nachricht: Tagge noch schlafende Spieler, falls konfiguriert
            var players = pointsManager.getWakePlayers();
            if (!players.isEmpty()) {
                StringBuilder sleepers = new StringBuilder();
                for (String pid : players) {
                    if (!pid.equals(userId)) {
                        sleepers.append("<@").append(pid).append("> ");
                    }
                }
                if (sleepers.length() > 0) {
                    channel.sendMessage(sleepers.toString().trim() + "\nRaus aus den Federn, ihr Lappen!").queue();
                }
            }
        } else {
            // Nicht erster
            var players = pointsManager.getWakePlayers();
            int configuredPlayers = players.isEmpty() ? pointsManager.getPlayerCount() : players.size();
            if (res.isLast && configuredPlayers > 0 && res.position >= configuredPlayers) {
                channel.sendMessage("<@" + userId + "> hat es auch endlich geschafft, du siehst ziemlich beschissen aus dafür, dass du solange gepennt hast. Jetzt sind endlich alle wach!").queue();
                channel.sendMessage(pointsManager.buildWakeOrderMessage()).queue();
            } else {
                channel.sendMessage("<@" + userId + "> hat es auch endlich geschafft. Heute mal ausgeschlafen wa?").queue();
            }
        }
    }
}


