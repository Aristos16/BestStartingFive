import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Team {
    private static final double IMPOSSIBLE_ASSIGNMENT_COST = 1e9;

    private final String name;
    private final List<Player> players;

    public Team(String name, List<Player> players) {
        this.name = name;
        this.players = players;
    }

    public double[][] buildCostMatrix(List<String> positions, OpponentProfile opponentProfile) {
        return buildCostMatrix(positions, null, opponentProfile, null);
    }

    public double[][] buildCostMatrix(List<String> positions,
                                      Map<String, OpponentProfile> positionOpponentProfiles,
                                      OpponentProfile fallbackOpponentProfile) {
        return buildCostMatrix(positions, positionOpponentProfiles, fallbackOpponentProfile, null);
    }

    public double[][] buildCostMatrix(List<String> positions,
                                      Map<String, OpponentProfile> positionOpponentProfiles,
                                      OpponentProfile fallbackOpponentProfile,
                                      int[] baseAssignment) {
        double[][] cost = new double[positions.size()][players.size()];
        Set<Integer> baseLineupPlayers = collectAssignedPlayers(baseAssignment);

        for (int i = 0; i < positions.size(); i++) {
            String position = positions.get(i);
            OpponentProfile positionProfile = getOpponentProfileForPosition(
                    position,
                    positionOpponentProfiles,
                    fallbackOpponentProfile
            );

            for (int j = 0; j < players.size(); j++) {
                Player player = players.get(j);

                if (player.getFitForPosition(position) == 0.0) {
                    cost[i][j] = IMPOSSIBLE_ASSIGNMENT_COST;
                    continue;
                }

                double score = player.getScoreForPositionVsOpponent(position, positionProfile);

                if (baseAssignment != null && !baseLineupPlayers.contains(j)) {
                    double base = player.getScoreForPosition(position);
                    double delta = score - base;
                    double rotationSignal = clamp01((delta - Config.TACTICAL_ROTATION_MIN_DELTA)
                            / Math.max(1e-9, Config.TACTICAL_ROTATION_FULL_DELTA - Config.TACTICAL_ROTATION_MIN_DELTA));
                    score += Config.TACTICAL_ROTATION_BONUS * rotationSignal;
                }

                cost[i][j] = Config.COST_MULTIPLIER * score;
            }
        }

        return cost;
    }

    public double[][] buildBaseCostMatrix(List<String> positions) {
        double[][] cost = new double[positions.size()][players.size()];

        for (int i = 0; i < positions.size(); i++) {
            String position = positions.get(i);

            for (int j = 0; j < players.size(); j++) {
                Player player = players.get(j);

                if (player.getFitForPosition(position) == 0.0) {
                    cost[i][j] = IMPOSSIBLE_ASSIGNMENT_COST;
                } else {
                    cost[i][j] = Config.COST_MULTIPLIER * player.getScoreForPosition(position);
                }
            }
        }

        return cost;
    }

    public String buildLineupText(List<String> positions,
                                  int[] assignment,
                                  Map<String, OpponentProfile> positionOpponentProfiles,
                                  OpponentProfile fallbackOpponentProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recommended lineup\n");

        for (int i = 0; i < positions.size(); i++) {
            String position = positions.get(i);
            Player player = getPlayerByAssignment(assignment, i);

            if (player == null) {
                sb.append(String.format("%-2s  %-24s%n", position, "[no player]"));
                continue;
            }

            OpponentProfile profile = getOpponentProfileForPosition(position, positionOpponentProfiles, fallbackOpponentProfile);
            double base = player.getScoreForPosition(position);
            double matchup = player.getScoreForPositionVsOpponent(position, profile);

            sb.append(String.format(
                    "%-2s  %-24s  base %.3f   matchup %.3f   %+,.3f%n",
                    position,
                    trimName(player.getName(), 24),
                    base,
                    matchup,
                    matchup - base
            ));
        }

        return sb.toString();
    }

    public String buildChangeSummaryText(List<String> positions,
                                         int[] baseAssignment,
                                         int[] matchupAssignment,
                                         Map<String, OpponentProfile> positionOpponentProfiles,
                                         OpponentProfile fallbackOpponentProfile) {
        StringBuilder sb = new StringBuilder();
        int changed = 0;

        sb.append("\nChanges from the default lineup\n");

        for (int i = 0; i < positions.size(); i++) {
            String position = positions.get(i);
            Player basePlayer = getPlayerByAssignment(baseAssignment, i);
            Player matchupPlayer = getPlayerByAssignment(matchupAssignment, i);

            String baseName = basePlayer == null ? "[none]" : basePlayer.getName();
            String matchupName = matchupPlayer == null ? "[none]" : matchupPlayer.getName();

            if (baseName.equals(matchupName)) {
                sb.append(String.format("%-2s  kept %s%n", position, baseName));
            } else {
                changed++;
                sb.append(String.format("%-2s  %s  ->  %s%n", position, baseName, matchupName));
            }
        }


        return sb.toString();
    }

    public String buildClosestCallsText(List<String> positions,
                                        int[] matchupAssignment,
                                        Map<String, OpponentProfile> positionOpponentProfiles,
                                        OpponentProfile fallbackOpponentProfile,
                                        int topN) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nClosest calls\n");
        sb.append("These are the players who were closest to entering the lineup for each position.\n\n");

        for (int i = 0; i < positions.size(); i++) {
            String position = positions.get(i);
            Player selected = getPlayerByAssignment(matchupAssignment, i);
            int selectedIndex = getAssignmentIndex(matchupAssignment, i);

            if (selected == null) {
                continue;
            }

            OpponentProfile profile = getOpponentProfileForPosition(position, positionOpponentProfiles, fallbackOpponentProfile);
            double selectedScore = selected.getScoreForPositionVsOpponent(position, profile);
            List<ScoreRow> rows = scorePlayersForPosition(position, profile);

            sb.append(String.format("%s selected: %s (%.3f)%n", position, selected.getName(), selectedScore));

            int shown = 0;
            for (ScoreRow row : rows) {
                if (row.index == selectedIndex) {
                    continue;
                }

                double gap = selectedScore - row.matchup;

                if (gap >= 0) {
                    sb.append(String.format("   %s was %.3f behind%n", row.player.getName(), gap));
                } else {
                    sb.append(String.format("   %s rated %.3f higher here, but the final five used him elsewhere%n",
                            row.player.getName(), Math.abs(gap)));
                }

                shown++;
                if (shown >= topN) break;
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    public double totalBaseScore(List<String> positions, int[] assignment) {
        double total = 0.0;

        for (int i = 0; i < positions.size(); i++) {
            Player player = getPlayerByAssignment(assignment, i);
            if (player != null) {
                total += player.getScoreForPosition(positions.get(i));
            }
        }

        return total;
    }

    public double totalMatchupScore(List<String> positions,
                                    int[] assignment,
                                    Map<String, OpponentProfile> positionOpponentProfiles,
                                    OpponentProfile fallbackOpponentProfile) {
        double total = 0.0;

        for (int i = 0; i < positions.size(); i++) {
            String position = positions.get(i);
            Player player = getPlayerByAssignment(assignment, i);

            if (player != null) {
                OpponentProfile profile = getOpponentProfileForPosition(position, positionOpponentProfiles, fallbackOpponentProfile);
                total += player.getScoreForPositionVsOpponent(position, profile);
            }
        }

        return total;
    }

    public Player getPlayerByAssignment(int[] assignment, int positionIndex) {
        int playerIndex = getAssignmentIndex(assignment, positionIndex);
        if (playerIndex < 0 || playerIndex >= players.size()) return null;
        return players.get(playerIndex);
    }

    public void printBestLineup(List<String> positions, int[] assignment, OpponentProfile opponentProfile) {
        System.out.print(buildLineupText(positions, assignment, null, opponentProfile));
    }

    public List<Player> getPlayers() {
        return players;
    }

    private List<ScoreRow> scorePlayersForPosition(String position, OpponentProfile profile) {
        List<ScoreRow> rows = new ArrayList<>();

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player.getFitForPosition(position) == 0.0) continue;
            double matchup = player.getScoreForPositionVsOpponent(position, profile);
            rows.add(new ScoreRow(i, player, matchup));
        }

        rows.sort(Comparator.comparingDouble((ScoreRow row) -> row.matchup).reversed());
        return rows;
    }

    private OpponentProfile getOpponentProfileForPosition(String position,
                                                          Map<String, OpponentProfile> positionOpponentProfiles,
                                                          OpponentProfile fallbackOpponentProfile) {
        if (positionOpponentProfiles == null || position == null) {
            return fallbackOpponentProfile;
        }

        OpponentProfile profile = positionOpponentProfiles.get(position.toUpperCase().trim());
        return profile == null ? fallbackOpponentProfile : profile;
    }

    private static Set<Integer> collectAssignedPlayers(int[] assignment) {
        Set<Integer> result = new HashSet<>();
        if (assignment == null) return result;

        for (int index : assignment) {
            if (index >= 0) result.add(index);
        }

        return result;
    }

    private static int getAssignmentIndex(int[] assignment, int positionIndex) {
        if (assignment == null || positionIndex < 0 || positionIndex >= assignment.length) {
            return -1;
        }
        return assignment[positionIndex];
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    private static String trimName(String name, int maxLength) {
        if (name == null) return "";
        if (name.length() <= maxLength) return name;
        return name.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private static class ScoreRow {
        final int index;
        final Player player;
        final double matchup;

        ScoreRow(int index, Player player, double matchup) {
            this.index = index;
            this.player = player;
            this.matchup = matchup;
        }
    }
}
