package de.nikl4s;

import java.util.Scanner;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.time.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class mainClass {
    public static void main(String[] args) {
        
        Scanner scan = new Scanner(System.in);
        System.out.println("Gebe deinen Discord-App-Token ein: ");
        String token = scan.next();

        JDABuilder confyg = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS);
        
        confyg.setStatus(OnlineStatus.ONLINE);
        confyg.setActivity(Activity.watching("auf dein Arsch 0.0"));
        PointsManager pointsManager = new PointsManager();
        confyg.addEventListeners(new GymTracker(pointsManager));
        confyg.addEventListeners(new WakeUpTracker(pointsManager));
        confyg.addEventListeners(new SlashCommandHandler(pointsManager));

        JDA lyfeBot = confyg.build();
        lyfeBot.updateCommands().addCommands(SlashCommandHandler.commandDefinitions()).queue();
        try {
            lyfeBot.awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        scheduleMonthlyLyrics(lyfeBot);
        
        System.out.println();
        System.out.println( "\033[32m" + "4LyfeStyler is ready");
        System.out.println();
        System.out.println("Online-Status: " + lyfeBot.getPresence().getStatus());
        Activity currentActivity = lyfeBot.getPresence().getActivity();
        String activityText = currentActivity == null ? "Keine" : (currentActivity.getType() + ": " + currentActivity.getName());
        System.out.println("Aktivität: " + activityText);
        System.out.println("\033[0m");


    }

    private static void scheduleMonthlyLyrics(JDA jda) {
        ScheduledExecutorService scheduler = jda.getGatewayPool();
        if (scheduler == null) return;

        Runnable task = () -> {
            LocalDate today = LocalDate.now();
            if (today.getDayOfMonth() != 1) return;
            var channels = jda.getTextChannelsByName("╠►frühe-vögel", true);
            if (channels.isEmpty()) return;
            String lyrics = "Wake up (Wake up)\n" +
                            "It's the first of the month (slatt, slatt)\n" +
                            "I brush my teeth and count up (What? Slatt, slatt, slatt, slatt, woah)";
            channels.get(0).sendMessage(lyrics).queue();
        };

        long initialDelay = computeInitialDelayToNext(LocalTime.of(7, 0));
        long period = TimeUnit.DAYS.toSeconds(1);
        scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
    }

    private static long computeInitialDelayToNext(LocalTime runAt) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime next = now.withHour(runAt.getHour()).withMinute(runAt.getMinute()).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return Duration.between(now, next).getSeconds();
    }
}


