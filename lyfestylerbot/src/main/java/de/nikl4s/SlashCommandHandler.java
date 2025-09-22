package de.nikl4s;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.entities.Member;

/**
 * Handler für Slash-Commands: /setpoints und /knecht
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

        CommandData knecht = Commands.slash("knecht", "Bezeichne einen Member als Knecht")
            .addOptions(
                new OptionData(OptionType.USER, "member", "Zielmember", true)
            );

        return new CommandData[] { setPoints, knecht };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        if ("setpoints".equals(name)) {
            handleSetPoints(event);
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


