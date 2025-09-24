package de.nikl4s;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.entities.Member;

    /**
     * Handler für Slash-Commands: /setpoints, /setstreak, /setstake, /setplayer, /setwakeplayers und /knecht
     */
public class SlashCommandHandler extends ListenerAdapter {
    private static final String OWNER_ID = "1076562395595538572";
    private final PointsManager pointsManager;

    public SlashCommandHandler(PointsManager pointsManager) {
        this.pointsManager = pointsManager;
    }

    /**
     * Definitionen der Commands zur Registrierung.
     */
    public static CommandData[] commandDefinitions() {
        CommandData setPoints = Commands.slash("setpoints", "Setzt den Punktestand eines Members")
            .addOptions(
                new OptionData(OptionType.USER, "member", "Member, dessen Punkte gesetzt werden", true),
                new OptionData(OptionType.INTEGER, "punkte", "Neuer Punktestand", true)
            );

        CommandData setStreak = Commands.slash("setstreak", "Setzt den Streak eines Members")
            .addOptions(
                new OptionData(OptionType.USER, "member", "Member, dessen Streak gesetzt wird", true),
                new OptionData(OptionType.INTEGER, "streak", "Neuer Streak-Wert", true)
            );

        CommandData knecht = Commands.slash("knecht", "Bezeichne einen Member als Knecht")
            .addOptions(
                new OptionData(OptionType.USER, "member", "Zielmember", true)
            );

        CommandData setStake = Commands.slash("setstake", "Setzt den Einsatz pro Spieler (Euro, z.B. 10.5)")
            .addOptions(
                new OptionData(OptionType.NUMBER, "euro", "Einsatz pro Spieler in Euro", true)
            );

        CommandData setPlayer = Commands.slash("setplayer", "Setzt die Spieleranzahl für den Einsatz-Topf")
            .addOptions(
                new OptionData(OptionType.INTEGER, "anzahl", "Spieleranzahl", true)
            );

        CommandData setWakePlayers = Commands.slash("setwakeplayers", "Setzt die Liste der WakeUp-Spieler (Mentions)")
            .addOptions(
                new OptionData(OptionType.STRING, "liste", "Liste von User-Mentions, getrennt mit Leerzeichen", true)
            );

        return new CommandData[] { setPoints, setStreak, knecht, setStake, setPlayer, setWakePlayers };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        if ("setpoints".equals(name)) {
            handleSetPoints(event);
        } else if ("setstreak".equals(name)) {
            handleSetStreak(event);
        } else if ("setstake".equals(name)) {
            handleSetStake(event);
        } else if ("setplayer".equals(name)) {
            handleSetPlayer(event);
        } else if ("setwakeplayers".equals(name)) {
            handleSetWakePlayers(event);
        } else if ("knecht".equals(name)) {
            handleKnecht(event);
        }
    }

    private void handleSetPoints(SlashCommandInteractionEvent event) {
        String invokerId = event.getUser().getId();
        if (!OWNER_ID.equals(invokerId)) {
            event.reply("Nur Niklas darf diesen Befehl verwenden. Er wird ihn nicht abusen. Vallah sogar.").setEphemeral(true).queue();
            return;
        }

        var memberOpt = event.getOption("member");
        var punkteOpt = event.getOption("punkte");
        if (memberOpt == null || punkteOpt == null) {
            event.reply("Fehlende Optionen.").setEphemeral(true).queue();
            return;
        }
        Member target = memberOpt.getAsMember();
        int newPoints = punkteOpt.getAsInt();
        if (target == null) {
            event.reply("Ungültiger Member.").setEphemeral(true).queue();
            return;
        }
        String displayName = target.getEffectiveName();
        int total = pointsManager.setPoints(target.getId(), displayName, newPoints);
        event.reply("Punkte von " + displayName + " auf " + total + " gesetzt.").queue();
    }

    private void handleSetStreak(SlashCommandInteractionEvent event) {
        String invokerId = event.getUser().getId();
        if (!OWNER_ID.equals(invokerId)) {
            event.reply("Nur Niklas darf diesen Befehl verwenden. Er wird ihn nicht abusen. Vallah sogar.").setEphemeral(true).queue();
            return;
        }

        var memberOpt = event.getOption("member");
        var streakOpt = event.getOption("streak");
        if (memberOpt == null || streakOpt == null) {
            event.reply("Fehlende Optionen.").setEphemeral(true).queue();
            return;
        }
        Member target = memberOpt.getAsMember();
        int newStreak = streakOpt.getAsInt();
        if (target == null) {
            event.reply("Ungültiger Member.").setEphemeral(true).queue();
            return;
        }
        String displayName = target.getEffectiveName();
        int resultStreak = pointsManager.setStreak(target.getId(), displayName, newStreak);
        event.reply("Streak von " + displayName + " auf " + resultStreak + " gesetzt.").queue();
    }

    private void handleSetStake(SlashCommandInteractionEvent event) {
        String invokerId = event.getUser().getId();
        if (!OWNER_ID.equals(invokerId)) {
            event.reply("Nur Niklas darf diesen Befehl verwenden. Er wird ihn nicht abusen. Vallah sogar.").setEphemeral(true).queue();
            return;
        }
        var euroOpt = event.getOption("euro");
        if (euroOpt == null) {
            event.reply("Fehlende Option: euro").setEphemeral(true).queue();
            return;
        }
        double euro = euroOpt.getAsDouble();
        pointsManager.setStakePerPlayerEuro(euro);
        event.reply("Einsatz pro Spieler gesetzt auf " + String.format(java.util.Locale.GERMANY, "%.2f", euro) + " €").queue();
    }

    private void handleSetPlayer(SlashCommandInteractionEvent event) {
        String invokerId = event.getUser().getId();
        if (!OWNER_ID.equals(invokerId)) {
            event.reply("Nur Niklas darf diesen Befehl verwenden. Er wird ihn nicht abusen. Vallah sogar.").setEphemeral(true).queue();
            return;
        }
        var anzahlOpt = event.getOption("anzahl");
        if (anzahlOpt == null) {
            event.reply("Fehlende Option: anzahl").setEphemeral(true).queue();
            return;
        }
        int count = anzahlOpt.getAsInt();
        pointsManager.setPlayerCount(count);
        event.reply("Spieleranzahl gesetzt auf " + count + ".").queue();
    }

    private void handleSetWakePlayers(SlashCommandInteractionEvent event) {
        String invokerId = event.getUser().getId();
        if (!OWNER_ID.equals(invokerId)) {
            event.reply("Nur Niklas darf diesen Befehl verwenden. Er wird ihn nicht abusen. Vallah sogar.").setEphemeral(true).queue();
            return;
        }
        var listOpt = event.getOption("liste");
        if (listOpt == null) {
            event.reply("Fehlende Option: liste").setEphemeral(true).queue();
            return;
        }
        String raw = listOpt.getAsString();
        java.util.List<String> ids = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<@!?([0-9]+)>").matcher(raw);
        while (m.find()) {
            ids.add(m.group(1));
        }
        pointsManager.setWakePlayers(ids);
        event.reply("WakeUp-Spieler gesetzt: " + ids.size()).queue();
    }

    private void handleKnecht(SlashCommandInteractionEvent event) {
        var memberOpt = event.getOption("member");
        if (memberOpt == null) {
            event.reply("Fehlende Option: member").setEphemeral(true).queue();
            return;
        }
        Member target = memberOpt.getAsMember();
        if (target == null) {
            event.reply("Ungültiger Member.").setEphemeral(true).queue();
            return;
        }
        String invokerId = event.getUser().getId();
        String targetMention = target.getAsMention();

        // Falls jemand versucht, den Owner zu beleidigen, kehrt es sich um
        if (OWNER_ID.equals(target.getId()) && !OWNER_ID.equals(invokerId)) {
            event.reply(event.getUser().getAsMention() + " ist selber ein Knecht.").queue();
            return;
        }

        event.reply(targetMention + " ist ein Knecht.").queue();
    }
}


