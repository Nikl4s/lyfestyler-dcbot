package de.nikl4s;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.io.InputStream;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

/**
 * Listener für Nachrichtenereignisse.
 */
public class GymTracker extends ListenerAdapter {
    private final PointsManager pointsManager;

    public GymTracker(PointsManager pointsManager) {
        this.pointsManager = pointsManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent Nachricht) {
        if (!Nachricht.isFromGuild() || Nachricht.getMember() == null) {
            return;
        }

        String msgContent = Nachricht.getMessage().getContentStripped();
        var member = Nachricht.getMember();
        if (member == null) {
            return;
        }
        String displayName = member.getEffectiveName();
        var ch = Nachricht.getChannel();
        if (!(ch instanceof GuildMessageChannel)) {
            return;
        }
        String channelName = ((GuildMessageChannel) ch).getName();

        if ("!gym".equalsIgnoreCase(msgContent)) {
            if (!"╠►pumper".equalsIgnoreCase(channelName)) {
                return;
            }
            Message message = Nachricht.getMessage();
            List<Attachment> attachments = message.getAttachments();
            Optional<Attachment> firstImage = attachments.stream().filter(Attachment::isImage).findFirst();
            if (firstImage.isEmpty()) {
                Nachricht.getChannel().sendMessage(displayName + ", bitte schicke ein Bild mit deinem !gym Kommando.").queue();
                return;
            }

            Attachment image = firstImage.get();
            boolean isCheat = false;
            try {
                try (InputStream in = image.getProxy().download().join()) {
                    Metadata metadata = ImageMetadataReader.readMetadata(in);
                    ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                    if (exif != null && exif.getDateOriginal() != null) {
                        LocalDate shotDate = exif.getDateOriginal().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        if (shotDate.isBefore(LocalDate.now().minusDays(1))) {
                            isCheat = true;
                        }
                    }
                }
            } catch (Exception ignore) {
            }

            String userId = Nachricht.getAuthor().getId();
            if (isCheat) {
                pointsManager.adjustPoints(userId, displayName, -5);
                Nachricht.getChannel().sendMessage("<@" + userId + "> hat ein altes Bild verwendet. -5 Punkte!").queue();
                return;
            }

            var res = pointsManager.handleGym(userId, displayName, LocalDate.now());
            if (res.accepted) {
                Nachricht.getChannel().sendMessage(displayName + " ist am Pumpen! (+" + res.pointsAdded + " Punkte, Streak: " + res.currentStreak + ")").queue();
            } else {
                Nachricht.getChannel().sendMessage(displayName + ", du hast heute schon eingecheckt. (Punkte: " + res.totalPoints + ")").queue();
            }
            return;
        }

        if ("!rank".equalsIgnoreCase(msgContent)) {
            Nachricht.getChannel().sendMessage(pointsManager.buildRankMessage()).queue();
            Nachricht.getChannel().sendMessage(pointsManager.buildYearRankMessage()).queue();
        }
    }
}

 