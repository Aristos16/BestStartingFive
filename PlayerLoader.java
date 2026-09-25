/*
PlayerLoader: Reads CSV, creates Player objects, normalizes offense/defense scores.
*/

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PlayerLoader {

    //Reads CSV,  makes players
    public List<Player> loadPlayersFromCsv(String path) throws IOException {
        List<Player> players = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(path));

        boolean firstLine = true;
        for (String line : lines) {
            if (firstLine) { firstLine = false; continue; }
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(",");

            String name     = parts[0].trim();
            String position = parts[1].trim();
            String team     = parts[2].trim();

            double min     = parse(parts[3]);
            double pts     = parse(parts[4]);
            double fgm     = parse(parts[5]);
            double fga     = parse(parts[6]);
            double threePm = parse(parts[7]);
            double threePa = parse(parts[8]);
            double ftm     = parse(parts[9]);
            double fta     = parse(parts[10]);
            double oreb    = parse(parts[11]);
            double dreb    = parse(parts[12]);
            double reb     = parse(parts[13]);
            double ast     = parse(parts[14]);
            double tov     = parse(parts[15]);
            double stl     = parse(parts[16]);
            double blk     = parse(parts[17]);
            double pf      = parse(parts[18]);

            players.add(new Player(
                    name, position, team,
                    min, pts,
                    fgm, fga,
                    threePm, threePa,
                    ftm, fta,
                    oreb, dreb, reb,
                    ast, tov,
                    stl, blk, pf
            ));
        }

        normalize(players);
        return players;
    }

    //helper to parse double with default 0.0 on failure
    private double parse(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }

    // Normalizes offense and defense scores to 0-1 range based on min/max in dataset.
    private void normalize(List<Player> players) {
        double minOff = Double.POSITIVE_INFINITY, maxOff = Double.NEGATIVE_INFINITY;
        double minDef = Double.POSITIVE_INFINITY, maxDef = Double.NEGATIVE_INFINITY;

        for (Player p : players) {
            minOff = Math.min(minOff, p.getOffenseRaw());
            maxOff = Math.max(maxOff, p.getOffenseRaw());
            minDef = Math.min(minDef, p.getDefenseRaw());
            maxDef = Math.max(maxDef, p.getDefenseRaw());
        }

        for (Player p : players) {
            p.setOffenseNorm(scale01(p.getOffenseRaw(), minOff, maxOff));
            p.setDefenseNorm(scale01(p.getDefenseRaw(), minDef, maxDef));
        }
    }

    private double scale01(double v, double min, double max) {
        if (max == min) return 0.5;
        return (v - min) / (max - min);
    }
}
