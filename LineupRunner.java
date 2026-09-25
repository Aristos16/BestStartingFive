import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LineupRunner {

    public enum OpponentMode {
        FULL_ROSTER,
        TOP_5_MINUTES,
        MANUAL_SELECTION
    }

    public static LineupResult runPipeline(String myTeamCsvPath, String opponentCsvPath, boolean useTop5Opponent) throws IOException 
    {
        OpponentMode mode = useTop5Opponent ? OpponentMode.TOP_5_MINUTES : OpponentMode.FULL_ROSTER;
        return runPipeline(myTeamCsvPath, opponentCsvPath, mode, null, null);
    }

    public static LineupResult runPipeline(String myTeamCsvPath,
                                           String opponentCsvPath,
                                           boolean useTop5Opponent,
                                           Set<String> availablePlayerNames) throws IOException {
        OpponentMode mode = useTop5Opponent ? OpponentMode.TOP_5_MINUTES : OpponentMode.FULL_ROSTER;
        return runPipeline(myTeamCsvPath, opponentCsvPath, mode, availablePlayerNames, null);
    }

    public static LineupResult runPipeline(String myTeamCsvPath,
                                           String opponentCsvPath,
                                           OpponentMode opponentMode,
                                           Set<String> availablePlayerNames,
                                           Set<String> selectedOpponentNames) throws IOException {

        PlayerLoader loader = new PlayerLoader();

        List<Player> players = loader.loadPlayersFromCsv(myTeamCsvPath); // load players from CSV

        if (availablePlayerNames != null && !availablePlayerNames.isEmpty()) {
            players = players.stream()   .filter(p -> availablePlayerNames.contains(p.getName()))
                    .collect(Collectors.toList());
        } // filter to available players and selected

        Team team = new Team("My Team", players);// create team

        List<String> positions = Arrays.asList("PG", "SG", "SF", "PF", "C");

        OpponentProfile oppTeam = OpponentProfileBuilder.fromRosterCsv(opponentCsvPath);

        OpponentProfile oppStarters = OpponentProfileBuilder.fromRosterCsvTopMinutes(opponentCsvPath, 5);

        Map<String, OpponentProfile> positionOpponentProfiles = null;

        OpponentProfile opp;


        switch (opponentMode) {
            case TOP_5_MINUTES:
                opp = oppStarters;
                break;
            case MANUAL_SELECTION:
                opp = OpponentProfileBuilder.fromRosterCsvSelected(opponentCsvPath, selectedOpponentNames);
                break;
            case FULL_ROSTER:
            default:
                opp = oppTeam;
                break;
        }




        // Build position-specific opponent profiles when the selected mode is top-minutes.
        // This creates more team-vs-team tactical variation than using one average profile
        // for all five positions.
        if (opponentMode == OpponentMode.TOP_5_MINUTES) {
            positionOpponentProfiles = OpponentProfileBuilder.fromRosterCsvTopMinutesByPosition(
                    opponentCsvPath,
                    Config.POSITION_PROFILE_TOP_N
            );
        }

        int[] baseAssignment = new HungarianAlgorithm(team.buildBaseCostMatrix(positions)).execute();

        double[][] costMatrix;
        if (positionOpponentProfiles != null) {
            costMatrix = team.buildCostMatrix(positions, positionOpponentProfiles, opp, baseAssignment);
        } else {
            costMatrix = team.buildCostMatrix(positions, opp);
        }

        HungarianAlgorithm solver = new HungarianAlgorithm(costMatrix);// solve assignment problem to find optimal lineup
        int[] assignment = solver.execute();// get assigned player indices for each position




        return new LineupResult(team, positions, assignment, opp, oppTeam, oppStarters, positionOpponentProfiles);
    }
}