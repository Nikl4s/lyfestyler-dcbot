package de.nikl4s;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Verwalter für Punkte, Streaks und Monats-Rollovers.
 *
 * Kernfunktionen:
 * - handleGym(..): vergibt Tagespunkte und aktualisiert Streaks
 * - buildRankMessage(): erzeugt ein sortiertes Ranking als Text
 * - buildMonthEndMessage(): generiert Monatsabschluss samt Gewinner
 * - rolloverToNextMonth(..): setzt für neuen Monat zurück
 */
public class PointsManager {
    public static final int DEFAULT_GYM_POINTS = 10;

    private final Map<String, UserStats> userIdToStats = new HashMap<>();
    private YearMonth currentMonth = YearMonth.now();
    private final int pointsPerGym;

    /**
     * Erstellt den Manager mit Standard-Punkten pro Gym-Eintrag.
     */
    public PointsManager() {
        this(DEFAULT_GYM_POINTS);
    }

    /**
     * Erstellt den Manager mit konfigurierbaren Punkten pro Gym-Eintrag.
     * @param pointsPerGym Anzahl der Punkte, die pro gültigem "!gym" vergeben werden
     */
    public PointsManager(int pointsPerGym) {
        this.pointsPerGym = pointsPerGym;
    }

    /**
     * Vergibt Punkte für einen Nutzer am aktuellen Tag und pflegt Streaks.
     * Führt automatisch einen Monatswechsel durch, falls erforderlich.
     *
     * @param userId eindeutige Nutzer-ID (Discord User ID)
     * @param displayName Name, der im Ranking angezeigt wird
     * @param today aktuelles Datum
     * @return Ergebnisobjekt mit hinzugefügten Punkten, Gesamtpunkten und Streak-Infos
     */
    public synchronized UserStats.AwardResult handleGym(String userId, String displayName, LocalDate today) {
        rolloverIfNewMonth(today);
        UserStats stats = userIdToStats.computeIfAbsent(userId, id -> new UserStats(id, displayName));
        stats.setDisplayName(displayName);
        return stats.awardGymPoints(today, pointsPerGym);
    }

    /**
     * Baut einen formatierten Ranking-Text für den aktuellen Monat.
     */
    public synchronized String buildRankMessage() {
        List<UserStats> ranking = getRanking();
        if (ranking.isEmpty()) {
            return "Es gibt noch keine Punkte.";
        }
        StringBuilder sb = new StringBuilder();
        String monthLabel = currentMonth.atDay(1)
                .format(DateTimeFormatter.ofPattern("LLL. - yyyy", Locale.GERMAN));
        sb.append(":trophy: Aktuelles Ranking (").append(monthLabel).append(")\n");
        int place = 1;
        for (UserStats s : ranking) {
            sb.append(place++)
              .append(". ")
              .append(s.getDisplayName())
              .append(" — Punkte: ")
              .append(s.getPoints())
              .append(", Streak: ")
              .append(s.getCurrentStreak())
              .append(" (Best: ")
              .append(s.getBestStreak())
              .append(")\n");
        }
        return sb.toString();
    }

    /**
     * Erzeugt die Monatsabschluss-Nachricht mit Gewinner und Gesamtübersicht.
     */
    public synchronized String buildMonthEndMessage() {
        List<UserStats> ranking = getRanking();
        if (ranking.isEmpty()) {
            return "Diesen Monat keine Teilnehmer.";
        }
        UserStats winner = ranking.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append(":confetti_ball: Gewinner ")
          .append(currentMonth)
          .append(": ")
          .append(winner.getDisplayName())
          .append(" mit ")
          .append(winner.getPoints())
          .append(" Punkten!\n\nGesamtergebnis:\n")
          .append(buildRankMessage());
        return sb.toString();
    }

    /**
     * Passt Punkte eines Nutzers an (z.B. -5 bei Betrugsversuch). Beeinflusst keine Streaks.
     * @return neue Gesamtpunkte
     */
    public synchronized int adjustPoints(String userId, String displayName, int delta) {
        UserStats stats = userIdToStats.computeIfAbsent(userId, id -> new UserStats(id, displayName));
        stats.setDisplayName(displayName);
        return stats.adjustPoints(delta);
    }

    /**
     * Setzt die Punkte eines Nutzers direkt auf einen Wert. Beeinflusst keine Streaks.
     * @return neue Gesamtpunkte
     */
    public synchronized int setPoints(String userId, String displayName, int newPoints) {
        UserStats stats = userIdToStats.computeIfAbsent(userId, id -> new UserStats(id, displayName));
        stats.setDisplayName(displayName);
        int delta = newPoints - stats.getPoints();
        return stats.adjustPoints(delta);
    }

    /**
     * Setzt den Manager auf einen neuen Monat und leert die Monatswerte.
     */
    public synchronized void rolloverToNextMonth(YearMonth newMonth) {
        this.currentMonth = newMonth;
        for (UserStats s : userIdToStats.values()) {
            s.resetForNewMonth();
        }
    }

    private void rolloverIfNewMonth(LocalDate today) {
        YearMonth ym = YearMonth.from(today);
        if (!ym.equals(currentMonth)) {
            rolloverToNextMonth(ym);
        }
    }

    /**
     * Liefert die Nutzer sortiert nach Punkten (desc), Best-Streak (desc) und Name (asc).
     */
    private List<UserStats> getRanking() {
        Collection<UserStats> values = new ArrayList<>(userIdToStats.values());
        return values.stream()
                .sorted(Comparator.comparingInt(UserStats::getPoints).reversed()
                        .thenComparing(UserStats::getBestStreak, Comparator.reverseOrder())
                        .thenComparing(UserStats::getDisplayName))
                .collect(Collectors.toList());
    }
}


