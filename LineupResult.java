import java.util.List;
import java.util.Map;

public class LineupResult {
    private final Team team;
    private final List<String> positions;
    private final int[] assignment;
    private final OpponentProfile opponentProfile;
    private final OpponentProfile opponentTeamProfile;
    private final OpponentProfile opponentStartersProfile;
    private final Map<String, OpponentProfile> positionOpponentProfiles;

    public LineupResult(Team team,
                        List<String> positions,
                        int[] assignment,
                        OpponentProfile opponentProfile,
                        OpponentProfile opponentTeamProfile,
                        OpponentProfile opponentStartersProfile,
                        Map<String, OpponentProfile> positionOpponentProfiles) {
        this.team = team;
        this.positions = positions;
        this.assignment = assignment;
        this.opponentProfile = opponentProfile;
        this.opponentTeamProfile = opponentTeamProfile;
        this.opponentStartersProfile = opponentStartersProfile;
        this.positionOpponentProfiles = positionOpponentProfiles;
    }

    public Team getTeam() {
        return team;
    }

    public List<String> getPositions() {
        return positions;
    }

    public int[] getAssignment() {
        return assignment;
    }

    public OpponentProfile getOpponentProfile() {
        return opponentProfile;
    }

    public OpponentProfile getOpponentTeamProfile() {
        return opponentTeamProfile;
    }

    public OpponentProfile getOpponentStartersProfile() {
        return opponentStartersProfile;
    }

    public Map<String, OpponentProfile> getPositionOpponentProfiles() {
        return positionOpponentProfiles;
    }
}