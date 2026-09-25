/*
Makes an OpponentProfile from a roster CSV.(0-1 sliders)
*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;


public class OpponentProfileBuilder {




    /* Build profile from the entire roster CSV . */
    public static OpponentProfile fromRosterCsv(String csvPath) throws IOException {
        Totals totals = readTotalsAllRows(csvPath);
        return buildProfileFromTotals(totals);
    }

    /*
     * Build profile from ONLY the top N players by minutes
     */
    public static OpponentProfile fromRosterCsvTopMinutes(String csvPath, int topN) throws IOException {
        if (topN <= 0) return OpponentProfile.neutral();

        List<Row> rows = readRows(csvPath);
        rows.sort((a, b) -> Double.compare(b.min, a.min)); // desc by minutes
        if (rows.isEmpty()) return OpponentProfile.neutral();

        int take = Math.min(topN, rows.size());
        Totals totals = new Totals();
        for (int i = 0; i < take; i++) {
            totals.add(rows.get(i));
        }
        return buildProfileFromTotals(totals);
    }

    // -----------------------------
    // Internal: reading + building
    // -----------------------------

    private static class Row {
    double min, fgm, fga, threePm, threePa, ftm, fta, oreb, ast, tov, stl, blk, pf;

    Row(double min, double fgm, double fga,
        double threePm, double threePa,
        double ftm, double fta,
        double oreb, double ast, double tov,
        double stl, double blk, double pf) {

        this.min = min;
        this.fgm = fgm;
        this.fga = fga;
        this.threePm = threePm;
        this.threePa = threePa;
        this.ftm = ftm;
        this.fta = fta;
        this.oreb = oreb;
        this.ast = ast;
        this.tov = tov;
        this.stl = stl;
        this.blk = blk;
        this.pf = pf;
    }
}

    private static class Totals {
    double min = 0, fgm = 0, fga = 0, threePm = 0, threePa = 0, ftm = 0, fta = 0;
    double oreb = 0, ast = 0, tov = 0, stl = 0, blk = 0, pf = 0;

    void add(Row r) {
        min     += r.min;
        fgm     += r.fgm;
        fga     += r.fga;
        threePm += r.threePm;
        threePa += r.threePa;
        ftm     += r.ftm;
        fta     += r.fta;
        oreb    += r.oreb;
        ast     += r.ast;
        tov     += r.tov;
        stl     += r.stl;
        blk     += r.blk;
        pf      += r.pf;
    }
}

    private static Totals readTotalsAllRows(String csvPath) throws IOException {
        List<Row> rows = readRows(csvPath);
        Totals totals = new Totals();
        for (Row r : rows) totals.add(r);
        return totals;
    }

    private static List<Row> readRows(String csvPath) throws IOException {
        List<Row> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String header = br.readLine();
            if (header == null) throw new IOException("Empty CSV: " + csvPath);

            Map<String, Integer> idx = parseHeaderIndex(header);

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] a = line.split(",", -1);

                double min     = getDouble(a, idx, "MIN");
                double fgm     = getDouble(a, idx, "FGM");
                double fga     = getDouble(a, idx, "FGA");
                double threePm = getDouble(a, idx, "3PM");
                double threePa = getDouble(a, idx, "3PA");
                double ftm     = getDouble(a, idx, "FTM");
                double fta     = getDouble(a, idx, "FTA");
                double oreb    = getDouble(a, idx, "OREB");
                double ast     = getDouble(a, idx, "AST");
                double tov     = getDouble(a, idx, "TOV");
                double stl     = getDouble(a, idx, "STL");
                double blk     = getDouble(a, idx, "BLK");
                double pf      = getDouble(a, idx, "PF");

                rows.add(new Row(min, fgm, fga, threePm, threePa, ftm, fta, oreb, ast, tov, stl, blk, pf));
                            }
        }

        return rows;
    }

    private static Map<String, Integer> parseHeaderIndex(String headerLine) {
        String[] cols = headerLine.split(",");
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < cols.length; i++) {
            idx.put(cols[i].trim(), i);
        }
        return idx;
    }

    private static double getDouble(String[] a, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i < 0 || i >= a.length) return 0.0;
        String s = a[i].trim();
        if (s.isEmpty()) return 0.0;
        return Double.parseDouble(s);
    }

    private static OpponentProfile buildProfileFromTotals(Totals t) {
    // Safety to avoid divide-by-zero
    double safeMin = Math.max(t.min, 1e-9);
    double safeFga = Math.max(t.fga, 1e-9);

    // ----------------------------------------------------
    // Opponent strengths / threats
    // ----------------------------------------------------

    // Turnover pressure proxy:
    // We do not have forced turnovers, so we use steals + some blocks per minute.
    double turnoverPressureRaw = (t.stl + 0.4 * t.blk) / safeMin;

    // 3-heavy style proxy:
    // Opponent offensive tendency to take 3s.
    double threeRateRaw = t.threePa / safeFga;

    // Offensive rebounding strength proxy:
    // OREB per missed shot estimate.
    double misses = (t.fga - t.fgm) + 0.44 * (t.fta - t.ftm);
    double orebRateRaw = t.oreb / Math.max(misses, 1e-9);

    // Paint attack proxy:
    // More 2PA + some foul pressure via FTr.
    double twoRateRaw = (t.fga - t.threePa) / safeFga; // 2PA/FGA
    double foulDrawRaw = t.fta / safeFga;              // FTr proxy
    double paintAttackRaw = 0.7 * twoRateRaw + 0.3 * foulDrawRaw;

    // ----------------------------------------------------
    // Opponent weaknesses / exploitable areas
    // ----------------------------------------------------

    // If opponent commits many turnovers, our steal-heavy defenders become more valuable.
    double turnoverWeaknessRaw = t.tov / Math.max(t.ast + t.tov, 1e-9);

    // If opponent commits many fouls, players who draw contact/free throws become more valuable.
    double foulWeaknessRaw = t.pf / safeMin;

    // If opponent has low blocks per minute, they have weaker rim protection.
    // High weakness = low block rate.
    double blockRateRaw = t.blk / safeMin;
    

    // ----------------------------------------------------
    // Normalize to 0..1 with caps
    // ----------------------------------------------------
    double rimProtectionWeaknessRaw =
        1.0 - clamp01(blockRateRaw / Config.CAP_BLK_RATE);

    double turnoverPressure = clamp01(turnoverPressureRaw / Config.CAP_TOV_PRESSURE);
    double allows3          = clamp01(threeRateRaw / Config.CAP_THREE_RATE);
    double offensiveRebRate = clamp01(orebRateRaw / Config.CAP_OREB_RATE);
    double paintAttack      = clamp01(paintAttackRaw / Config.CAP_PAINT_ATTACK);
    double foulDrawing      = clamp01(foulDrawRaw / Config.CAP_FOUL_DRAW);

    double turnoverWeakness = clamp01(turnoverWeaknessRaw / Config.CAP_TOV_WEAKNESS);
    double foulWeakness     = clamp01(foulWeaknessRaw / Config.CAP_FOUL_WEAKNESS);
    double rimProtectionWeakness = clamp01(rimProtectionWeaknessRaw);

    // Sharpen the profile a little so normal teams do not all look the same.
    // Values above 0.50 become more clearly high; values below 0.50 become more clearly low.
    turnoverPressure     = contrastProfileValue(turnoverPressure);
    allows3              = contrastProfileValue(allows3);
    offensiveRebRate     = contrastProfileValue(offensiveRebRate);
    paintAttack          = contrastProfileValue(paintAttack);
    foulDrawing          = contrastProfileValue(foulDrawing);
    turnoverWeakness     = contrastProfileValue(turnoverWeakness);
    foulWeakness         = contrastProfileValue(foulWeakness);
    rimProtectionWeakness = contrastProfileValue(rimProtectionWeakness);

    return new OpponentProfile(
            turnoverPressure,
            allows3,
            offensiveRebRate,
            paintAttack,
            foulDrawing,
            turnoverWeakness,
            foulWeakness,
            rimProtectionWeakness
    );
}

    private static double clamp01(double x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }

    private static double contrastProfileValue(double x) {
        return clamp01(0.5 + (x - 0.5) * Config.OPP_PROFILE_CONTRAST);
    }

        /** Build profile directly from a list of Player objects. */
    public static OpponentProfile fromPlayers(List<Player> players) {
        if (players == null || players.isEmpty()) return OpponentProfile.neutral();

        Totals totals = new Totals();
        for (Player p : players) {
            totals.min     += p.getMinutes();
            totals.fga     += p.getFga();
            totals.fgm     += p.getFgm();
            totals.threePa += p.getThreePa();
            totals.fta     += p.getFta();
            totals.ftm     += p.getFtm();
            totals.oreb    += p.getOreb();
            totals.ast     += p.getAst();
            totals.tov     += p.getTov();
            totals.stl     += p.getStl();
            totals.blk     += p.getBlk();
            totals.pf      += p.getPf();
        }

        return buildProfileFromTotals(totals);
    }

    /** Build profile from only selected players from a roster CSV. */
    public static OpponentProfile fromRosterCsvSelected(String csvPath, Set<String> selectedNames) throws IOException {
        if (selectedNames == null || selectedNames.isEmpty()) return OpponentProfile.neutral();

        PlayerLoader loader = new PlayerLoader();
        List<Player> players = loader.loadPlayersFromCsv(csvPath);

        List<Player> filtered = players.stream()
                .filter(p -> selectedNames.contains(p.getName()))
                .collect(Collectors.toList());

        return fromPlayers(filtered);
    }



    /**
     * Builds a different opponent profile for each target position.
     * This is important for team-vs-team variety: using one team-wide profile
     * makes every position react to the same average opponent identity, so the
     * same strong all-around five tends to win against many opponents.
     */
    public static Map<String, OpponentProfile> fromRosterCsvTopMinutesByPosition(String csvPath, int topN) throws IOException {
        Map<String, OpponentProfile> result = new HashMap<>();

        PlayerLoader loader = new PlayerLoader();
        List<Player> allPlayers = loader.loadPlayersFromCsv(csvPath);

        List<Player> fallbackTop = allPlayers.stream()
                .sorted((a, b) -> Double.compare(b.getMinutes(), a.getMinutes()))
                .limit(Math.max(1, topN))
                .collect(Collectors.toList());

        String[] positions = {"PG", "SG", "SF", "PF", "C"};
        for (String targetPos : positions) {
            List<Player> selected = allPlayers.stream()
                    .filter(p -> isRelevantOpponentForTarget(p.getPosition(), targetPos))
                    .sorted((a, b) -> Double.compare(b.getMinutes(), a.getMinutes()))
                    .limit(Math.max(1, topN))
                    .collect(Collectors.toList());

            if (selected.isEmpty()) {
                selected = fallbackTop;
            }

            result.put(targetPos, fromPlayers(selected));
        }

        return result;
    }

    private static boolean isRelevantOpponentForTarget(String opponentPosition, String targetPos) {
        if (opponentPosition == null || targetPos == null) return false;

        String p = opponentPosition.toUpperCase().trim();
        String t = targetPos.toUpperCase().trim();

        boolean hasPG = p.contains("PG");
        boolean hasSG = p.contains("SG");
        boolean hasSF = p.contains("SF");
        boolean hasPF = p.contains("PF");
        boolean hasC  = p.contains("C");

        switch (t) {
            case "PG":
                return hasPG || hasSG;
            case "SG":
                return hasPG || hasSG || hasSF;
            case "SF":
                return hasSG || hasSF || hasPF;
            case "PF":
                return hasSF || hasPF || hasC;
            case "C":
                return hasPF || hasC;
            default:
                return false;
        }
    }

}
