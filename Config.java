/*
 * Central configuration for the lineup optimizer.
 *
 * All tunable numbers are kept here instead of being hard-coded inside
 * Player, Team or OpponentProfileBuilder.
 *
 * This makes it easier to:
 * - understand what affects the final result,
 * - test different parameter values,
 * - keep the scoring logic consistent,
 * - change the behaviour of the model without rewriting the algorithms.
 */
public final class Config {

    private Config() {}

    // =====================================================
    // 1. BASE PLAYER SCORE
    // =====================================================
    // Final base score = offense + defense + position fit.
    // These weights control how important each part is.
    //
    // Example:
    // higher W_OFFENSE -> offensive players are favored more
    // higher W_DEFENSE -> defensive players are favored more
    // higher W_FIT     -> natural position matters more
    public static final double W_OFFENSE = 0.45;
    public static final double W_DEFENSE = 0.30;
    public static final double W_FIT     = 0.15;


    // =====================================================
    // 2. OFFENSIVE SCORE BREAKDOWN
    // =====================================================
    // These weights are used only when Player computes offenseRaw.
    //
    // offenseRaw =
    //   points contribution
    // + shooting efficiency
    // + playmaking
    // + simple all-around impact
    public static final double OFF_W_PTS_NORM = 0.45; // normalized points per game
    public static final double OFF_W_TS       = 0.20; // True Shooting %
    public static final double OFF_W_EFG      = 0.10; // Effective Field Goal %
    public static final double OFF_W_AST_TOV  = 0.10; // assists / turnovers
    public static final double OFF_W_PER_LITE = 0.15; // simplified impact-per-minute metric

    // Points used as the "full score" reference for points normalization.
    // A player with 20+ points reaches 1.0 for the points component.
    public static final double PTS_STAR_LEVEL = 20.0;


    // =====================================================
    // 3. DEFENSIVE SCORE BREAKDOWN
    // =====================================================
    // Used when Player computes defenseRaw.
    //
    // Positive defensive events:
    // steals, blocks and defensive rebounds.
    //
    // Fouls are subtracted because frequent fouling is treated negatively.
    public static final double DEF_W_STL  = 1.5; // importance of steals
    public static final double DEF_W_BLK  = 1.5; // importance of blocks
    public static final double DEF_W_DREB = 0.5; // importance of defensive rebounds
    public static final double DEF_W_PF   = 1.0; // penalty weight for personal fouls


    // =====================================================
    // 4. RELIABILITY BASED ON MINUTES
    // =====================================================
    // A player with very few minutes has less reliable statistics.
    //
    // reliability = minutes / RELIABILITY_MINUTES, clamped to 0..1.
    //
    // At 16+ minutes the player receives full reliability (1.0).
    public static final double RELIABILITY_MINUTES = 16.0;


    // =====================================================
    // 5. POSITION FIT
    // =====================================================
    // How suitable a player is for each basketball position.
    //
    // 1.0 = natural position
    // lower values = acceptable but less natural
    // 0.0 = forbidden assignment
    public static final double FIT_EXACT      = 1.0; // natural position
    public static final double FIT_GUARD_SWAP = 0.8; // PG <-> SG
    public static final double FIT_WING_SWAP  = 0.8; // SF <-> PF
    public static final double FIT_PF_TO_C    = 0.7; // PF can play C
    public static final double FIT_C_TO_PF    = 0.0; // C -> PF currently disabled

    // Special contextual fits.
    // These are deliberately low because they should only be emergency /
    // small-ball style alternatives, not normal assignments.
    public static final double FIT_SMALLBALL_C = 0.2; // SF/PF combination playing C
    public static final double FIT_HEAVY_SF    = 0.2; // PF/C combination playing SF

    // Used for assignments that should never be allowed.
    public static final double FIT_IMPOSSIBLE = 0.0;


    // =====================================================
    // 6. HUNGARIAN ALGORITHM
    // =====================================================
    // The Hungarian algorithm minimizes cost, while our model wants to
    // maximize player score.
    //
    // Multiplying the score by -1 converts:
    // high score -> low cost
    // so minimizing cost gives the highest-scoring lineup.
    public static final double COST_MULTIPLIER = -1.0;


    // =====================================================
    // 7. PLAYER FEATURE NORMALIZATION CAPS
    // =====================================================
    // Matchup logic compares different basketball statistics.
    // Because AST/TOV, steals/minute, 3P%, etc. use different scales,
    // each feature is converted to a 0..1 value.
    //
    // These constants define what is treated as approximately "very good".
    // Values above the cap are simply clamped to 1.0.

    // Used when exploiting opponent weaknesses.
    public static final double STL_PM_GOOD   = 0.05; // steals per minute considered very good
    public static final double FTA_RATE_GOOD = 0.60; // FTA/FGA considered strong foul drawing
    public static final double TWO_RATE_GOOD = 0.85; // 2PA/FGA considered very inside-oriented

    // General player feature caps.
    public static final double AST_TOV_GOOD   = 3.0;  // AST/TOV = 3 treated as very good ball security
    public static final double THREE_PA_HIGH  = 5.0;  // 5 three-point attempts/game treated as high volume
    public static final double THREE_PCT_GOOD = 0.40; // 40% from three treated as very good shooting
    public static final double DREB_PM_GOOD   = 0.20; // defensive rebounds per minute cap
    public static final double BLK_PM_GOOD    = 0.05; // blocks per minute cap
    public static final double PF_PM_BAD      = 0.15; // fouls per minute considered very foul-prone


    // =====================================================
    // 8. OPPONENT PROFILE NORMALIZATION CAPS
    // =====================================================
    // OpponentProfileBuilder first calculates raw opponent tendencies.
    // These caps convert those raw values into 0..1 profile sliders.
    //
    // Example:
    // normalized turnover pressure =
    // raw turnover pressure / CAP_TOV_PRESSURE
    public static final double CAP_TOV_PRESSURE = 0.08;
    public static final double CAP_THREE_RATE   = 0.55;
    public static final double CAP_OREB_RATE    = 0.55;
    public static final double CAP_PAINT_ATTACK = 0.85;
    public static final double CAP_FOUL_DRAW    = 0.60;

    // Opponent weaknesses are normalized separately.
    public static final double CAP_TOV_WEAKNESS  = 0.60;
    public static final double CAP_FOUL_WEAKNESS = 0.25;
    public static final double CAP_BLK_RATE      = 0.04;


    // =====================================================
    // 9. LIMITS FOR OPPONENT-BASED OFFENSE / DEFENSE MULTIPLIERS
    // =====================================================
    // Matchup logic can increase or decrease a player's offense/defense.
    // These limits prevent one opponent characteristic from making the
    // player's score unrealistically huge or tiny.
    //
    // Example:
    // 0.60 means the adjusted component cannot fall below 60% of normal.
    // 1.45 means it cannot rise above 145% of normal.
    public static final double OPP_MULT_MIN = 0.60;
    public static final double OPP_MULT_MAX = 1.45;


    // =====================================================
    // 10. GENERAL STRENGTH / WEAKNESS MATCHUP EFFECT
    // =====================================================
    // Used inside Player's offMult and defMult.
    //
    // OPP_STRENGTH_WEIGHT:
    // how strongly we react to something the opponent does well.
    //
    // OPP_WEAKNESS_WEIGHT:
    // how strongly we reward players who can exploit an opponent weakness.
    public static final double OPP_STRENGTH_WEIGHT = 0.28;
    public static final double OPP_WEAKNESS_WEIGHT = 0.28;


    // =====================================================
    // 11. DIRECT MATCHUP BONUS / PENALTY
    // =====================================================
    // Controls direct specialist bonuses and missed-opportunity penalties.
    //
    // Example:
    // opponent turns the ball over a lot
    // -> high-steal player gets a direct matchup bonus.
    public static final double MATCHUP_WEIGHT = 0.28;

    // Extra penalty for a clearly bad matchup.
    //
    // Example:
    // opponent attacks the paint heavily
    // + our center has very weak shot blocking
    // -> stronger penalty than a normal small mismatch.
    public static final double BAD_MATCHUP_PENALTY_WEIGHT = 0.35;


    // =====================================================
    // 12. HOW MUCH THE OPPONENT CAN CHANGE THE BASE SCORE
    // =====================================================
    // Final score is a blend between:
    // - the player's normal/base score
    // - the opponent-adjusted score
    //
    // 0.0 = completely ignore opponent
    // 1.0 = use the fully opponent-adjusted score
    //
    // 0.75 means matchup information matters a lot, but the original
    // player quality still remains part of the final score.
    public static final double MATCHUP_BLEND = 0.75;


    // =====================================================
    // 13. OPPONENT PROFILE CONTRAST
    // =====================================================
    // After normalization, many teams may otherwise look too similar.
    //
    // This pushes values away from 0.50:
    // > 0.50 becomes more clearly high
    // < 0.50 becomes more clearly low
    //
    // 1.0 = no contrast change
    // > 1.0 = stronger team identities
    public static final double OPP_PROFILE_CONTRAST = 1.75;


    // =====================================================
    // 14. WHEN AN OPPONENT FEATURE BECOMES A REAL TACTICAL SIGNAL
    // =====================================================
    // We do not want every average opponent tendency to activate
    // matchup rules.
    //
    // OPP_SIGNAL_START:
    // below this value -> treated as ordinary / no meaningful signal
    //
    // OPP_SIGNAL_FULL:
    // around this value or higher -> treated as a full tactical signal
    //
    // Values in between are scaled gradually from 0 to 1.
    public static final double OPP_SIGNAL_START = 0.52;
    public static final double OPP_SIGNAL_FULL  = 0.85;


    // =====================================================
    // 15. POSITION-SPECIFIC OPPONENT PROFILES
    // =====================================================
    // In TOP_5_MINUTES mode, each of our positions can use a different
    // opponent profile instead of every position reacting to the same
    // team-wide average.
    //
    // This number tells the builder how many relevant opponent players
    // to use for each positional profile.
    public static final int POSITION_PROFILE_TOP_N = 3;


    // =====================================================
    // 16. TACTICAL ROTATION BONUS
    // =====================================================
    // A strong base lineup can otherwise remain unchanged against almost
    // every opponent.
    //
    // If a bench / non-base-lineup player improves significantly against
    // the current matchup, he receives a small extra bonus.
    //
    // This is intentionally limited so the opponent can create realistic
    // rotations without replacing the whole starting five arbitrarily.

    // Maximum extra bonus that a non-base-lineup player can receive.
    public static final double TACTICAL_ROTATION_BONUS = 0.28;

    // Minimum matchup improvement before the rotation bonus starts.
    public static final double TACTICAL_ROTATION_MIN_DELTA = 0.08;

    // Matchup improvement at which the player receives the full rotation bonus.
    public static final double TACTICAL_ROTATION_FULL_DELTA = 0.30;
}

// Build command:
// mingw32-make build
