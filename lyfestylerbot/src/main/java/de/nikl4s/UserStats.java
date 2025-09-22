package de.nikl4s;

import java.time.LocalDate;

/**
 * Hält monatliche Statistikdaten pro Nutzer: Punkte, aktueller/bester Streak und letztes Gym-Datum.
 */
public class UserStats {
    private final String userId;
    private String displayName;
    private int points;
    private int currentStreak;
    private int bestStreak;
    private LocalDate lastGymDate;

    /**
     * Legt einen neuen Nutzer-Datensatz an.
     * @param userId Discord User ID
     * @param displayName Anzeigename für das Ranking
     */
    public UserStats(String userId, String displayName) {
        this.userId = userId;
        this.displayName = displayName;
        this.points = 0;
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.lastGymDate = null;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getPoints() {
        return points;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public LocalDate getLastGymDate() {
        return lastGymDate;
    }

    /**
     * Vergibt Punkte für einen validen Gym-Eintrag am Tag "today" und pflegt Streaks.
     * Vergibt nur einmal pro Tag Punkte.
     *
     * @param today aktuelles Datum
     * @param pointsPerGym Punkte pro Eintrag
     * @return Ergebnis mit hinzugefügten Punkten, Gesamtpunkten und Streak-Infos; accepted=false, wenn bereits gezählt
     */
    public AwardResult awardGymPoints(LocalDate today, int pointsPerGym) {
        boolean isNewDay = lastGymDate == null || !lastGymDate.isEqual(today);
        boolean consecutiveDay = lastGymDate != null && lastGymDate.plusDays(1).isEqual(today);

        if (isNewDay) {
            if (consecutiveDay) {
                currentStreak += 1;
            } else {
                currentStreak = 1;
            }
            if (currentStreak > bestStreak) {
                bestStreak = currentStreak;
            }
            points += pointsPerGym;
            lastGymDate = today;
            return new AwardResult(pointsPerGym, points, currentStreak, bestStreak, true);
        }
        return new AwardResult(0, points, currentStreak, bestStreak, false);
    }

    /**
     * Setzt die Monatswerte zurück (für Rollover am Monatsanfang).
     */
    public void resetForNewMonth() {
        points = 0;
        currentStreak = 0;
        lastGymDate = null;
    }

    /**
     * Direkter Punktezuwachs/-abzug ohne Streak-Änderung (z.B. -5 bei Verstoß).
     * @param delta positive oder negative Punkteänderung
     * @return neue Gesamtpunkte
     */
    public int adjustPoints(int delta) {
        this.points += delta;
        return this.points;
    }

    /**
     * Ergebnisobjekt der Punktevergabe.
     */
    public static class AwardResult {
        public final int pointsAdded;
        public final int totalPoints;
        public final int currentStreak;
        public final int bestStreak;
        public final boolean accepted;

        public AwardResult(int pointsAdded, int totalPoints, int currentStreak, int bestStreak, boolean accepted) {
            this.pointsAdded = pointsAdded;
            this.totalPoints = totalPoints;
            this.currentStreak = currentStreak;
            this.bestStreak = bestStreak;
            this.accepted = accepted;
        }
    }
}


