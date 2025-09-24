package de.nikl4s;

import java.time.LocalDate;
import java.time.Year;
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
    private Year currentYear = Year.now();
    private final int pointsPerGym;

    // Einsatz-Logik
    private int stakePerPlayerCents = 0; // in Cent gespeichert
    private int playerCount = 0;

    // WakeUp-Tracking: Liste der teilnehmenden Spieler (Discord User IDs)
    private final List<String> wakePlayers = new ArrayList<>();
    // Heutige Wake-Order (User IDs) und Zeitpunkte
    private final Map<String, java.time.LocalTime> todayWakeTimes = new HashMap<>();
    private LocalDate wakeDate = null; // Datum, für das todayWakeTimes gilt

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
        rolloverIfPeriodChanged(today);
        UserStats stats = userIdToStats.computeIfAbsent(userId, id -> new UserStats(id, displayName));
        stats.setDisplayName(displayName);
        UserStats.AwardResult res = stats.awardGymPoints(today, pointsPerGym);
        if (res.accepted && res.pointsAdded > 0) {
            stats.addYearPoints(res.pointsAdded);
        }
        return res;
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
     * Setzt den Streak eines Nutzers direkt auf einen Wert und aktualisiert ggf. den Best-Streak.
     * @return der gesetzte aktuelle Streak
     */
    public synchronized int setStreak(String userId, String displayName, int newStreak) {
        UserStats stats = userIdToStats.computeIfAbsent(userId, id -> new UserStats(id, displayName));
        stats.setDisplayName(displayName);
        return stats.setStreak(newStreak);
    }

    // WakeUp: Verwaltung der Teilnehmer
    public synchronized void setWakePlayers(List<String> userIds) {
        wakePlayers.clear();
        wakePlayers.addAll(userIds);
    }

    public synchronized List<String> getWakePlayers() {
        return new ArrayList<>(wakePlayers);
    }

    /**
     * Verarbeitet einen !awake-Checkin. Erwartet, dass im Channel ein Bild gepostet wurde (Prüfung im Listener).
     * Liefert Informationen, ob erster/letzter etc.
     */
    public static class WakeResult {
        public final boolean accepted; // ignoriert, wenn bereits wach registriert
        public final boolean isFirst;
        public final boolean isLast;
        public final int position; // 1-basiert
        public final LocalDate date;
        public final java.time.LocalTime time;

        public WakeResult(boolean accepted, boolean isFirst, boolean isLast, int position, LocalDate date, java.time.LocalTime time) {
            this.accepted = accepted;
            this.isFirst = isFirst;
            this.isLast = isLast;
            this.position = position;
            this.date = date;
            this.time = time;
        }
    }

    public synchronized WakeResult handleAwake(String userId, String displayName, LocalDate today, java.time.LocalTime now) {
        rolloverIfPeriodChanged(today);
        if (wakeDate == null || !wakeDate.isEqual(today)) {
            todayWakeTimes.clear();
            wakeDate = today;
        }
        if (todayWakeTimes.containsKey(userId)) {
            return new WakeResult(false, false, false, new ArrayList<>(todayWakeTimes.keySet()).indexOf(userId) + 1, today, todayWakeTimes.get(userId));
        }
        todayWakeTimes.put(userId, now);
        int pos = todayWakeTimes.size();
        boolean isFirst = pos == 1;
        boolean isLast = !wakePlayers.isEmpty() && pos >= Math.min(playerCount, wakePlayers.size());

        // Frühster Vogel Streak beim ersten
        if (isFirst) {
            UserStats stats = userIdToStats.computeIfAbsent(userId, id -> new UserStats(id, displayName));
            stats.setDisplayName(displayName);
            stats.recordWakeFirst(today);
        }
        return new WakeResult(true, isFirst, isLast, pos, today, now);
    }

    public synchronized String buildWakeOrderMessage() {
        if (wakeDate == null || todayWakeTimes.isEmpty()) {
            return "Heute noch keine Wake-Ups.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(":alarm_clock: Aufsteh-Reihenfolge für ").append(wakeDate).append("\n");
        var ordered = todayWakeTimes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .toList();
        int i = 1;
        for (var e : ordered) {
            String userId = e.getKey();
            java.time.LocalTime t = e.getValue();
            UserStats stats = userIdToStats.get(userId);
            String name = stats != null ? stats.getDisplayName() : userId;
            sb.append(i++)
              .append(". ")
              .append(name)
              .append(" — ")
              .append(t.toString())
              .append("\n");
        }
        if (!ordered.isEmpty()) {
            String firstId = ordered.get(0).getKey();
            UserStats first = userIdToStats.get(firstId);
            if (first != null) {
                sb.append("\n:bird: Frühster Vogel: ")
                  .append(first.getDisplayName())
                  .append(" — Streak: ")
                  .append(first.getWakeFirstCurrentStreak())
                  .append(" (Best: ")
                  .append(first.getWakeFirstBestStreak())
                  .append(")");
            }
        }
        return sb.toString();
    }

    /**
     * Setzt den Manager auf einen neuen Monat und leert die Monatswerte.
     */
    public synchronized void rolloverToNextMonth(YearMonth newMonth) {
        boolean yearChanged = newMonth.getYear() != this.currentMonth.getYear();
        // Monat abschließen
        for (UserStats s : userIdToStats.values()) {
            s.finalizeMonthHighscore();
        }
        // Jahr ggf. abschließen
        if (yearChanged) {
            for (UserStats s : userIdToStats.values()) {
                s.finalizeYearHighscore();
                s.resetForNewYear();
            }
            this.currentYear = Year.of(newMonth.getYear());
        }
        // Monat zurücksetzen
        this.currentMonth = newMonth;
        for (UserStats s : userIdToStats.values()) {
            s.resetForNewMonth();
        }
    }

    private void rolloverIfPeriodChanged(LocalDate today) {
        YearMonth ym = YearMonth.from(today);
        if (!ym.equals(currentMonth)) {
            rolloverToNextMonth(ym);
            return;
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

    /**
     * Liefert die Nutzer sortiert nach Jahrespunkten (desc), Best-Streak (desc) und Name (asc).
     */
    private List<UserStats> getYearRanking() {
        Collection<UserStats> values = new ArrayList<>(userIdToStats.values());
        return values.stream()
                .sorted(Comparator.comparingInt(UserStats::getYearPoints).reversed()
                        .thenComparing(UserStats::getBestStreak, Comparator.reverseOrder())
                        .thenComparing(UserStats::getDisplayName))
                .collect(Collectors.toList());
    }

    /**
     * Baut eine Ranking-Nachricht für das aktuelle Jahr.
     */
    public synchronized String buildYearRankMessage() {
        List<UserStats> ranking = getYearRanking();
        if (ranking.isEmpty()) {
            return "Es gibt noch keine Jahrespunkte.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(":trophy: Jahresranking (").append(currentYear).append(")\n");
        int place = 1;
        for (UserStats s : ranking) {
            sb.append(place++)
              .append(". ")
              .append(s.getDisplayName())
              .append(" — Jahrespunkte: ")
              .append(s.getYearPoints());
            if (s.getBestStreak() > 0) sb.append(", Best-Streak: ").append(s.getBestStreak());
            if (s.getBestMonthlyPoints() > 0) sb.append(", Monats-HS: ").append(s.getBestMonthlyPoints());
            if (s.getBestYearlyPoints() > 0) sb.append(", Jahres-HS: ").append(s.getBestYearlyPoints());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Erweiterte Monats-Ranking-Nachricht mit Highscores (wenn >0).
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
              .append(")");
            if (s.getBestMonthlyPoints() > 0) sb.append(", Monats-HS: ").append(s.getBestMonthlyPoints());
            if (s.getBestYearlyPoints() > 0) sb.append(", Jahres-HS: ").append(s.getBestYearlyPoints());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Monatsabschluss mit Gewinner und Auszahlung gemäß arithmetischer Folge.
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
          .append(" Punkten!\n\n");

        sb.append(buildPayoutsMessage(ranking));
        sb.append("\n\n");
        sb.append(buildRankMessage());
        return sb.toString();
    }

    /**
     * Nachricht zum Jahresende inkl. Pflicht für den Letztplatzierten.
     */
    public synchronized String buildYearEndMessage() {
        List<UserStats> ranking = getYearRanking();
        if (ranking.isEmpty()) {
            return "Dieses Jahr keine Teilnehmer.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(":confetti_ball: Jahresgewinner ")
          .append(currentYear)
          .append(": ")
          .append(ranking.get(0).getDisplayName())
          .append(" mit ")
          .append(ranking.get(0).getYearPoints())
          .append(" Punkten!\n\n");

        UserStats last = ranking.get(ranking.size() - 1);
        sb.append(buildYearRankMessage())
          .append("\n")
          .append("<@")
          .append(last.getUserId())
          .append("> muss einmal fett Essen ausgeben habibi");
        return sb.toString();
    }

    /**
      * Setzt den Einsatz pro Spieler in Euro (als Zahl mit bis zu 2 Nachkommastellen).
      */
    public synchronized void setStakePerPlayerEuro(double euro) {
        this.stakePerPlayerCents = (int) Math.round(euro * 100.0);
    }

    public synchronized void setPlayerCount(int count) {
        this.playerCount = Math.max(0, count);
    }

    public synchronized int getPlayerCount() { return playerCount; }
    public synchronized int getStakePerPlayerCents() { return stakePerPlayerCents; }

    private String buildPayoutsMessage(List<UserStats> ranking) {
        if (playerCount <= 0 || stakePerPlayerCents <= 0) {
            return "Keine Einsatzdaten gesetzt.";
        }
        int n = Math.min(playerCount, ranking.size());
        if (n <= 1) {
            int potCents = playerCount * stakePerPlayerCents;
            return "Gesamteinsatz: " + formatEuroCents(potCents) + " — keine Verteilung (zu wenige Spieler).";
        }
        int potCents = playerCount * stakePerPlayerCents;
        int sumWeights = n * (n - 1) / 2; // 0..n-1
        StringBuilder sb = new StringBuilder();
        sb.append(":moneybag: Einsatz-Topf: ").append(formatEuroCents(potCents)).append(" bei ").append(playerCount).append(" Spielern (je ").append(formatEuroCents(stakePerPlayerCents)).append(")\n");
        for (int i = 0; i < n; i++) {
            int weight = (n - 1 - i); // 1.,2.,...,n → n-1,...,0
            int payout = sumWeights == 0 ? 0 : (int) Math.round((double) potCents * weight / (double) sumWeights);
            UserStats s = ranking.get(i);
            sb.append(i + 1)
              .append(". ")
              .append(s.getDisplayName())
              .append(": ")
              .append(formatEuroCents(payout))
              .append("\n");
        }
        return sb.toString();
    }

    private static String formatEuroCents(int cents) {
        int abs = Math.abs(cents);
        int eur = abs / 100;
        int rem = abs % 100;
        String sign = cents < 0 ? "-" : "";
        return sign + eur + "," + String.format("%02d", rem) + " €";
    }
}


