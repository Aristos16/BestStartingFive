public class OpponentProfile {
    // 0.0 = neutral, 1.0 = extreme / very strong tendency

    // Opponent strengths / threats
    private final double turnoverPressure;  // opponent forces turnovers / aggressive defense
    private final double allows3;           // actually: opponent 3PA tendency / perimeter attack
    private final double offensiveRebRate;  // opponent strong on offensive rebounds
    private final double paintAttack;       // opponent attacks paint
    private final double foulDrawing;       // opponent draws fouls

    // Opponent weaknesses / exploitable areas
    private final double turnoverWeakness;       // opponent commits many turnovers
    private final double foulWeakness;           // opponent commits many fouls
    private final double rimProtectionWeakness;  // opponent has weak rim protection / low blocks

    public OpponentProfile(double turnoverPressure,
                           double allows3,
                           double offensiveRebRate,
                           double paintAttack,
                           double foulDrawing,
                           double turnoverWeakness,
                           double foulWeakness,
                           double rimProtectionWeakness) {

        this.turnoverPressure = normalize01(turnoverPressure);
        this.allows3 = normalize01(allows3);
        this.offensiveRebRate = normalize01(offensiveRebRate);
        this.paintAttack = normalize01(paintAttack);
        this.foulDrawing = normalize01(foulDrawing);

        this.turnoverWeakness = normalize01(turnoverWeakness);
        this.foulWeakness = normalize01(foulWeakness);
        this.rimProtectionWeakness = normalize01(rimProtectionWeakness);
    }

    public static OpponentProfile neutral() {
        return new OpponentProfile(
                0, 0, 0, 0, 0,
                0, 0, 0
        );
    }

    private double normalize01(double x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }

    public double getTurnoverPressure() { return turnoverPressure; }
    public double getAllows3()          { return allows3; }
    public double getOffensiveRebRate() { return offensiveRebRate; }
    public double getPaintAttack()      { return paintAttack; }
    public double getFoulDrawing()      { return foulDrawing; }

    public double getTurnoverWeakness()      { return turnoverWeakness; }
    public double getFoulWeakness()          { return foulWeakness; }
    public double getRimProtectionWeakness() { return rimProtectionWeakness; }
}