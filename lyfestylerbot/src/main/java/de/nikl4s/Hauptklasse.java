package de.nikl4s;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;


public class Hauptklasse {
    public static void main(String[] args) {
        String token = "Your Toke Here";
        JDABuilder confyg = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS);
        
        confyg.setStatus(OnlineStatus.ONLINE);
        // Präsenz und Aktivität setzen (sichtbar im Discord-Client)
        confyg.setActivity(Activity.watching("auf dein Arsch 0.0"));
        // Punkteverwaltung instanziieren und Listener registrieren
        PointsManager pointsManager = new PointsManager();
        confyg.addEventListeners(new gymTracker(pointsManager));
        confyg.addEventListeners(new SlashCommandHandler(pointsManager));

        // Slash-Commands global registrieren/aktualisieren
        confyg.build().updateCommands().addCommands(SlashCommandHandler.commandDefinitions()).queue();
        
        System.out.println("4LyfeStyler is ready");


    }


}