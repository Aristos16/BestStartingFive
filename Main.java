import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        try {
            String myTeamCsv = args.length > 0 ? args[0] : "../csv/test2.csv";
            String[] opponentCsvs = readOpponentFiles(args);

            for (String opponentCsv : opponentCsvs) {
                LineupResult result = LineupRunner.runPipeline(
                        myTeamCsv,
                        opponentCsv,
                        LineupRunner.OpponentMode.TOP_5_MINUTES,
                        null,
                        null
                );

                printResult(opponentCsv, result);
            }
        } catch (IOException e) {
            System.err.println("Could not load CSV file: " + e.getMessage());
        }
    }

    private static String[] readOpponentFiles(String[] args) {
        if (args.length <= 1) {
            return new String[] { "../csv/pao24and25.csv" };
        }

        String[] files = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            files[i - 1] = args[i];
        }
        return files;
    }

    private static void printResult(String opponentCsv, LineupResult result) {
        Team team = result.getTeam();
        List<String> positions = result.getPositions();
        int[] matchupAssignment = result.getAssignment();
        OpponentProfile fallbackProfile = result.getOpponentProfile();
        Map<String, OpponentProfile> positionProfiles = result.getPositionOpponentProfiles();
        int[] baseAssignment = new HungarianAlgorithm(team.buildBaseCostMatrix(positions)).execute();

        System.out.println();
        System.out.println("============================================================");
        System.out.println("Opponent file: " + opponentCsv);
        System.out.println("============================================================");
        System.out.println();

        System.out.print(team.buildLineupText(positions, matchupAssignment, positionProfiles, fallbackProfile));
        System.out.println();
        System.out.printf("Default lineup score: %.3f%n", team.totalBaseScore(positions, baseAssignment));
        System.out.printf("Matchup lineup score: %.3f%n", team.totalMatchupScore(positions, matchupAssignment, positionProfiles, fallbackProfile));
        System.out.print(team.buildChangeSummaryText(positions, baseAssignment, matchupAssignment, positionProfiles, fallbackProfile));
    }
}
