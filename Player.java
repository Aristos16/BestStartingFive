/*
    layer: Represents a player with raw stats, derived metrics, position fit logic, and opponent-aware scoring.
*/

public class Player {

    private final String name;
    private final String position;   // "PG", "PG/SG", "SF-PF" κλπ
    private final String team;

    // Raw stats
    private final double minutes;
    private final double points;
    private final double fgm;
    private final double fga;
    private final double threePm;
    private final double threePa;
    private final double ftm;
    private final double fta;
    private final double oreb;
    private final double dreb;
    private final double reb;
    private final double ast;
    private final double tov;
    private final double stl;
    private final double blk;
    private final double pf;

    // Derived
    private double ts;
    private double efg;
    private double astTov;
    private double perLite;
    private double offenseRaw;
    private double defenseRaw;

    // Normalized (set by loader)
    private double offenseNorm;
    private double defenseNorm;

    public Player(String name, String position, String team,
                  double minutes, double points,
                  double fgm, double fga,
                  double threePm, double threePa,
                  double ftm, double fta,
                  double oreb, double dreb, double reb,
                  double ast, double tov,
                  double stl, double blk, double pf) {

        this.name = name;
        this.position = position;
        this.team = team;

        this.minutes = minutes;
        this.points = points;
        this.fgm = fgm;
        this.fga = fga;
        this.threePm = threePm;
        this.threePa = threePa;
        this.ftm = ftm;
        this.fta = fta;
        this.oreb = oreb;
        this.dreb = dreb;
        this.reb = reb;
        this.ast = ast;
        this.tov = tov;
        this.stl = stl;
        this.blk = blk;
        this.pf = pf;

        //Makes the advanced metrics 
        computeDerivedMetrics();
    }

    private double clamp(double x, double lo, double hi) {
    if (x < lo) return lo;
    if (x > hi) return hi;
    return x;
}

    private void computeDerivedMetrics() {
        ts = computeTS();
        efg = computeEFG();
        astTov = computeAstTov();
        perLite = computePerLite();

        offenseRaw = computeOffenseRaw();
        defenseRaw = computeDefenseRaw();
    }

    // TS% = PTS / (2*(FGA + 0.44*FTA))
    private double computeTS() {
        double shootingDenominator = 2.0 * (fga + 0.44 * fta);
        if (shootingDenominator == 0) return 0.0;
        return points / shootingDenominator;
    }

    // eFG% = (FGM + 0.5*3PM) / FGA
    private double computeEFG() {
        if (fga == 0) return 0.0;
        return (fgm + 0.5 * threePm) / fga;
    }

    private double computeAstTov() {
        if (tov == 0) return ast;
        return ast / tov;
    }

    // super simplified "impact per minute"
    private double computePerLite() {
        if (minutes == 0) return 0.0;
        double raw = points + reb + ast + stl + blk - tov;
        return raw / minutes;
    }

    private double computeOffenseRaw() {
        double ptsNorm = points / Config.PTS_STAR_LEVEL;
        if (ptsNorm > 1.0) ptsNorm = 1.0;
        if (ptsNorm < 0.0) ptsNorm = 0.0;

        return Config.OFF_W_PTS_NORM * ptsNorm
             + Config.OFF_W_TS       * ts
             + Config.OFF_W_EFG      * efg
             + Config.OFF_W_AST_TOV  * astTov
             + Config.OFF_W_PER_LITE * perLite;
    }

    private double computeDefenseRaw() {
        if (minutes == 0) return 0.0;

        double defPerMinute =
                (Config.DEF_W_STL  * stl
               + Config.DEF_W_BLK  * blk
               + Config.DEF_W_DREB * dreb
               - Config.DEF_W_PF   * pf) / minutes;

        return defPerMinute;
    }

    //Position fit logic: Returns 0.0 for impossible fit, up to 1.0 for perfect fit, with context adjustments.
    public double getFitForPosition(String targetPos) {
        if (targetPos == null) return Config.FIT_IMPOSSIBLE;

        targetPos = targetPos.toUpperCase().trim();

        if (position == null || position.trim().isEmpty()) {
            return Config.FIT_IMPOSSIBLE;
        }

        String posUpper = position.toUpperCase();

        boolean hasPG = posUpper.contains("PG");
        boolean hasSG = posUpper.contains("SG");
        boolean hasSF = posUpper.contains("SF");
        boolean hasPF = posUpper.contains("PF");
        boolean hasC  = posUpper.contains("C");

        String[] natural = posUpper.split("[/\\-]");
        double best = 0.0;

        for (String natRaw : natural) {
            String nat = natRaw.trim();
            if (nat.isEmpty()) continue;

            double fit = singlePosFit(nat, targetPos);
            if (fit > best) best = fit;
        }

        if (best == 0.0) return 0.0;

        // context adjustments
        if (targetPos.equals("C") && hasSF && hasPF && !hasC) {
            best = Math.min(best, Config.FIT_SMALLBALL_C);
        }

        if (targetPos.equals("SF") && hasPF && hasC && !hasSF) {
            best = Math.min(best, Config.FIT_HEAVY_SF);
        }

        return best;
    }

    private double singlePosFit(String nat, String targetPos) {
        nat = nat.toUpperCase();
        targetPos = targetPos.toUpperCase();

        boolean natGuard = nat.equals("PG") || nat.equals("SG");
        boolean targetGuard = targetPos.equals("PG") || targetPos.equals("SG");

        boolean natWing = nat.equals("SF") || nat.equals("PF");
        boolean targetWing = targetPos.equals("SF") || targetPos.equals("PF");

        // hard bans
        if (natGuard && (targetPos.equals("PF") || targetPos.equals("C"))) return 0.0;
        if (nat.equals("C") && (targetPos.equals("PG") || targetPos.equals("SG") || targetPos.equals("SF"))) return 0.0;

        if (nat.equals(targetPos)) return Config.FIT_EXACT;

        if (natGuard && targetGuard) return Config.FIT_GUARD_SWAP;
        if (natWing && targetWing)   return Config.FIT_WING_SWAP;

        if (nat.equals("PF") && targetPos.equals("C")) return Config.FIT_PF_TO_C;
        if (nat.equals("C")  && targetPos.equals("PF")) return Config.FIT_C_TO_PF;

        return 0.0;
    }



    //Playes with less minitue get penalized
    private double reliability() {
        double r = minutes / Config.RELIABILITY_MINUTES;
        if (r < 0.0) r = 0.0;
        if (r > 1.0) r = 1.0;
        return r;
    }

    // Base score for position fit + offense/defense, without opponent adjustments.
    public double getScoreForPosition(String targetPos) {
        double fit = getFitForPosition(targetPos);
        if (fit == 0.0) return 0.0;

        double r = reliability();
        double off = offenseNorm * r;
        double def = defenseNorm * r;

        return Config.W_OFFENSE * off
             + Config.W_DEFENSE * def
             + Config.W_FIT     * fit;
    }

    // The same as above but with oppoent-aware adjustments
    public double getScoreForPositionVsOpponent(String targetPos, OpponentProfile opp) {



    double fit = getFitForPosition(targetPos);
    if (fit == 0.0) return 0.0;

    double r = reliability();
    double off = offenseNorm * r;
    double def = defenseNorm * r;

    double astTov01 = clamp01(astTov / Config.AST_TOV_GOOD);

    double threePct = (threePa > 0) ? (threePm / threePa) : 0.0;


    double threePct01 = clamp01(threePct / Config.THREE_PCT_GOOD);
    double threeVol01 = clamp01(threePa / Config.THREE_PA_HIGH);



    double shooter01 = 0.6 * threePct01 + 0.4 * threeVol01;

    double drebPerMin = (minutes > 0) ? (dreb / minutes) : 0.0;
    double dreb01 = clamp01(drebPerMin / Config.DREB_PM_GOOD);


    
    double blkPerMin = (minutes > 0) ? (blk / minutes) : 0.0;
    double blk01 = clamp01(blkPerMin / Config.BLK_PM_GOOD);

    double pfPerMin = (minutes > 0) ? (pf / minutes) : 0.0;
    double pfBad01 = clamp01(pfPerMin / Config.PF_PM_BAD); // higher = worse

    // Extra features used to exploit opponent weaknesses

    // Steal ability: useful when opponent commits many turnovers
    double stlPerMin = (minutes > 0) ? (stl / minutes) : 0.0;
    double stl01 = clamp01(stlPerMin / Config.STL_PM_GOOD);

    // Foul drawing ability: useful when opponent commits many fouls
    double ftaRate = (fga > 0) ? (fta / fga) : 0.0;
    double ftaRate01 = clamp01(ftaRate / Config.FTA_RATE_GOOD);

    // Inside attack tendency: useful when opponent has weak rim protection
    double twoRate = (fga > 0) ? ((fga - threePa) / fga) : 0.0;
    double twoRate01 = clamp01(twoRate / Config.TWO_RATE_GOOD);

    String pos = targetPos.toUpperCase().trim();


    double isGuard = (pos.equals("PG") || pos.equals("SG")) ? 1.0 : 0.6;
    double isWing  = (pos.equals("SF") || pos.equals("PF")) ? 1.0 : 0.7;
    double isBig   = (pos.equals("PF") || pos.equals("C"))  ? 1.0 : 0.6;

    // Soft role weights for matchup-specific logic.
    double stealRole;
    if (pos.equals("PG") || pos.equals("SG") || pos.equals("SF")) {
        stealRole = 1.0;
    } else {
        stealRole = 0.4;
    }

    double slasherRole;
    if (pos.equals("PG") || pos.equals("SG") || pos.equals("SF")) {
        slasherRole = 1.0;
    } else if (pos.equals("PF")) {
        slasherRole = 0.7;
    } else {
        slasherRole = 0.4;
    }

    double insideRole;
    if (pos.equals("PF") || pos.equals("C")) {
        insideRole = 1.0;
    } else if (pos.equals("SG") || pos.equals("SF")) {
        insideRole = 0.7;
    } else {
        insideRole = 0.5;
    }

    double rimProtectRole;
    if (pos.equals("PF") || pos.equals("C")) {
        rimProtectRole = 1.0;
    } else if (pos.equals("SF")) {
        rimProtectRole = 0.6;
    } else {
        rimProtectRole = 0.2;
    }

    double reboundRole;
    if (pos.equals("PF") || pos.equals("C")) {
        reboundRole = 1.0;
    } else if (pos.equals("SF")) {
        reboundRole = 0.7;
    } else {
        reboundRole = 0.3;
    }

    double ballHandlerRole;
    if (pos.equals("PG")) {
        ballHandlerRole = 1.0;
    } else if (pos.equals("SG")) {
        ballHandlerRole = 0.8;
    } else if (pos.equals("SF")) {
        ballHandlerRole = 0.5;
    } else {
        ballHandlerRole = 0.2;
    }

    // Perimeter defense role:
    // Guards and wings are the main perimeter defenders.
    // PF can help sometimes, C rarely.
    double perimeterRole;
    if (pos.equals("PG") || pos.equals("SG") || pos.equals("SF")) {
        perimeterRole = 1.0;
    } else if (pos.equals("PF")) {
        perimeterRole = 0.4;
    } else {
        perimeterRole = 0.2;
    }

    // Convert opponent profile sliders into tactical signals.
    // Example: a normal 0.55 does not activate much, but a strong 0.80+ does.
    // This makes different opponents produce different lineups, instead of every
    // average team activating every matchup rule.
    double oppTurnoverPressure      = profileSignal(opp.getTurnoverPressure());
    double oppAllows3               = profileSignal(opp.getAllows3());
    double oppOffensiveRebRate      = profileSignal(opp.getOffensiveRebRate());
    double oppPaintAttack           = profileSignal(opp.getPaintAttack());
    double oppFoulDrawing           = profileSignal(opp.getFoulDrawing());
    double oppTurnoverWeakness      = profileSignal(opp.getTurnoverWeakness());
    double oppFoulWeakness          = profileSignal(opp.getFoulWeakness());
    double oppRimProtectionWeakness = profileSignal(opp.getRimProtectionWeakness());

    // Strong rim protection is the opposite of rim-protection weakness.
    double oppRimProtectionStrength = profileSignal(1.0 - opp.getRimProtectionWeakness());

    double offMult = 1.0;
    double defMult = 1.0;

  
    // OFFENSE adjustments based on opponent profile

    // A) vs opponent that pressures turnovers: good ball handlers (high ast/tov) are more valuable
    double tp = oppTurnoverPressure * isGuard;
    offMult += Config.OPP_STRENGTH_WEIGHT * tp * astTov01;          // bonus
    offMult -= Config.OPP_STRENGTH_WEIGHT * tp * (1.0 - astTov01);  // penalty

    // B) vs opponent style that allow 3s : good shooters more valuable
    double a3 = oppAllows3 * isWing;
    offMult += Config.OPP_STRENGTH_WEIGHT * a3 * shooter01;          // bonus
    offMult -= Config.OPP_STRENGTH_WEIGHT * a3 * (1.0 - shooter01);  // penalty



    // DEFENSE adjustments based on opponent profile

    // C) vs opponent that get a lot of offensive boards: good rebounders more valuable
    double or = oppOffensiveRebRate * isBig;
    defMult += Config.OPP_STRENGTH_WEIGHT * or * dreb01;          // bonus
    defMult -= Config.OPP_STRENGTH_WEIGHT * or * (1.0 - dreb01);  // penalty

    // D) vs opponent that attacks paint: good blockers more valuable
    double pa = oppPaintAttack * isBig;
    defMult += Config.OPP_STRENGTH_WEIGHT * pa * blk01;          // bonus
    defMult -= Config.OPP_STRENGTH_WEIGHT * pa * (1.0 - blk01);  // penalty

    // E) vs opponent that draws fouls: disciplined players (low pf) more valuable
    double discipline01 = 1.0 - pfBad01;
    double fd = oppFoulDrawing;
    defMult += Config.OPP_STRENGTH_WEIGHT * fd * discipline01;          // bonus
    defMult -= Config.OPP_STRENGTH_WEIGHT * fd * (1.0 - discipline01);  // penalty
    
        // ----------------------------------------------------
        // Exploit opponent weaknesses
        // ----------------------------------------------------

    // F) If opponent commits many turnovers, steal-heavy defenders are more valuable
    double tow = oppTurnoverWeakness;
    defMult += Config.OPP_WEAKNESS_WEIGHT * tow * stl01;

    // G) If opponent commits many fouls, players who draw contact are more valuable
    double fw = oppFoulWeakness;
    offMult += Config.OPP_WEAKNESS_WEIGHT * fw * ftaRate01;

    // H) If opponent has weak rim protection, inside attackers are more valuable
    double rw = oppRimProtectionWeakness * isBig;
    offMult += Config.OPP_WEAKNESS_WEIGHT * rw * twoRate01;


    // Keep opponent multipliers controlled
    offMult = clamp(offMult, Config.OPP_MULT_MIN, Config.OPP_MULT_MAX);
    defMult = clamp(defMult, Config.OPP_MULT_MIN, Config.OPP_MULT_MAX);

    double offAdj = off * offMult;
    double defAdj = def * defMult;


    // Direct matchup bonus:
    // This gives extra value to specialists who directly match the opponent profile.
    double matchupBonus = 0.0;



    // Opponent makes many turnovers -> steal creators become more valuable
    matchupBonus += Config.MATCHUP_WEIGHT
            * oppTurnoverWeakness
            * stl01
            * stealRole;

    // Opponent commits many fouls -> contact attackers / slashers become more valuable
    matchupBonus += Config.MATCHUP_WEIGHT
            * oppFoulWeakness
            * ftaRate01
            * slasherRole;

    // Opponent has weak rim protection -> inside attackers and slashers become more valuable
    matchupBonus += Config.MATCHUP_WEIGHT
            * oppRimProtectionWeakness
            * twoRate01
            * insideRole;

    // Opponent attacks the paint -> rim protectors become more valuable
    matchupBonus += Config.MATCHUP_WEIGHT
            * oppPaintAttack
            * blk01
            * rimProtectRole;

    // Opponent gets many offensive rebounds -> defensive rebounders become more valuable
    matchupBonus += Config.MATCHUP_WEIGHT
            * oppOffensiveRebRate
            * dreb01
            * reboundRole;

    // Opponent pressures the ball -> safe ball handlers become more valuable
    matchupBonus += Config.MATCHUP_WEIGHT
            * oppTurnoverPressure
            * astTov01
            * ballHandlerRole;

        // ----------------------------------------------------
        // Extra tactical matchup logic
        // ----------------------------------------------------

        // 1) If opponent has strong rim protection,
        // stretch bigs become more valuable.
        // Strong rim protection means RimWeak is low.
        double rimStrength = oppRimProtectionStrength;
        double stretchBig01 = shooter01 * isBig;

        matchupBonus += Config.MATCHUP_WEIGHT
                * rimStrength
                * stretchBig01;


        // 2) If opponent shoots many threes,
        // perimeter defenders become more valuable.
        // We use defenseNorm + steals as a proxy for perimeter defense.
        double perimeterDefense01 = clamp01(0.6 * def + 0.4 * stl01);

        matchupBonus += Config.MATCHUP_WEIGHT
                * oppAllows3
                * perimeterDefense01
                * perimeterRole;


    // Missed-opportunity penalty.
    // This slightly penalizes players who cannot exploit a clear opponent weakness.
    double matchupPenalty = 0.0;

    // Opponent makes many turnovers -> low-steal players miss an opportunity
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.6
            * oppTurnoverWeakness
            * stealRole
            * (1.0 - stl01);

    // Opponent commits many fouls -> players who do not draw contact miss an opportunity
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.6
            * oppFoulWeakness
            * slasherRole
            * (1.0 - ftaRate01);

    // Opponent has weak rim protection -> players who do not attack inside miss an opportunity
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.6
            * oppRimProtectionWeakness
            * insideRole
            * (1.0 - twoRate01);
    
    // ----------------------------------------------------
    // Extra tactical matchup penalties
    // ----------------------------------------------------

    // 1) If opponent has strong rim protection,
    // non-shooting bigs are easier to contain.
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.4
            * rimStrength
            * isBig
            * (1.0 - shooter01);


    // 2) If opponent shoots many threes,
    // weak perimeter defenders are punished.
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.4
            * oppAllows3
            * perimeterRole
            * (1.0 - perimeterDefense01);


    // 3) If opponent gets many offensive rebounds,
    // weak defensive rebounders are punished more strongly.
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.6
            * oppOffensiveRebRate
            * reboundRole
        * (1.0 - dreb01);


        // 4) If opponent attacks the paint,
    // weak rim protectors are punished.
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.5
            * oppPaintAttack
            * rimProtectRole
            * (1.0 - blk01);


    // 5) If opponent applies strong turnover pressure,
    // bad ball handlers are punished.
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.5
            * oppTurnoverPressure
            * ballHandlerRole
            * (1.0 - astTov01);


    // 6) If opponent draws many fouls,
    // players who foul a lot are punished.
    matchupPenalty += Config.MATCHUP_WEIGHT
            * 0.5
            * oppFoulDrawing
            * pfBad01;


    // 7) If opponent has weak rim protection,
    // players who cannot attack inside miss a good opportunity.
    // This mostly punishes guards/wings/bigs who do not use 2-point/inside attempts.
    matchupPenalty += Config.MATCHUP_WEIGHT
        * 0.4
        * oppRimProtectionWeakness
        * insideRole
        * (1.0 - twoRate01);
        double adjustedScore =
        Config.W_OFFENSE * offAdj
      + Config.W_DEFENSE * defAdj
      + Config.W_FIT     * fit;


      double badMatchupPenalty = 0.0;

        // Opponent shoots many threes -> weak perimeter defenders are a bad matchup
        badMatchupPenalty += Config.BAD_MATCHUP_PENALTY_WEIGHT
                * oppAllows3
                * perimeterRole
                * Math.max(0.0, 0.65 - perimeterDefense01);

        // Opponent attacks paint -> weak rim protectors are a bad matchup
        badMatchupPenalty += Config.BAD_MATCHUP_PENALTY_WEIGHT
                * oppPaintAttack
                * rimProtectRole
                * Math.max(0.0, 0.65 - blk01);

        // Opponent gets offensive rebounds -> weak rebounders are a bad matchup
        badMatchupPenalty += Config.BAD_MATCHUP_PENALTY_WEIGHT
                * oppOffensiveRebRate
                * reboundRole
                * Math.max(0.0, 0.65 - dreb01);

        // Opponent pressures the ball -> unsafe ball handlers are a bad matchup
        badMatchupPenalty += Config.BAD_MATCHUP_PENALTY_WEIGHT
                * oppTurnoverPressure
                * ballHandlerRole
                * Math.max(0.0, 0.65 - astTov01);

        // Opponent draws fouls -> foul-prone players are a bad matchup
        badMatchupPenalty += Config.BAD_MATCHUP_PENALTY_WEIGHT
                * oppFoulDrawing
                * pfBad01;

        // Opponent has weak rim protection -> non-inside attackers lose value
        badMatchupPenalty += Config.BAD_MATCHUP_PENALTY_WEIGHT
                * oppRimProtectionWeakness
                * insideRole
                * Math.max(0.0, 0.60 - twoRate01);


        double matchupScore = matchupBonus - matchupPenalty;

        // Raw opponent-aware score can become too dominant if used directly.
        // Blend it with the original base score so matchup changes matter,
        // but do not completely destroy the normal best-five quality.
        double baseScore = Config.W_OFFENSE * off
                       + Config.W_DEFENSE * def
                       + Config.W_FIT     * fit;

        double rawVsOpponentScore = adjustedScore + matchupScore - badMatchupPenalty;

        return baseScore
             + Config.MATCHUP_BLEND * (rawVsOpponentScore - baseScore);

}

private double profileSignal(double x) {
    double denom = Config.OPP_SIGNAL_FULL - Config.OPP_SIGNAL_START;
    if (denom <= 0) return clamp01(x);
    return clamp01((x - Config.OPP_SIGNAL_START) / denom);
}

private double clamp01(double x) {
    if (x < 0) return 0;
    if (x > 1) return 1;
    return x;
}


    // Getters / setters
    public String getName() { return name; }
    public String getPosition() { return position; }
    

    public double getOffenseRaw() { return offenseRaw; }
    public double getDefenseRaw() { return defenseRaw; }

    public double getOffenseNorm() { return offenseNorm; }
    public double getDefenseNorm() { return defenseNorm; }

    public void setOffenseNorm(double v) { offenseNorm = v; }
    public void setDefenseNorm(double v) { defenseNorm = v; }



public double getMinutes() {
    return minutes;
}

public double getAstTov() {
    return astTov;
}

public double getThreePa() {
    return threePa;
}

public double getThreePm() {
    return threePm;
}

public double getDreb() {
    return dreb;
}

public double getBlk() {
    return blk;
}

public double getPf() {
    return pf;
}

public double getFga() {
    return fga;
}

public double getFgm() {
    return fgm;
}

public double getFta() {
    return fta;
}

public double getFtm() {
    return ftm;
}

public double getOreb() {
    return oreb;
}

public double getStl() {
    return stl;
}

public double getAst() {
    return ast;

}
public double getTov() {
    return tov;
}

}
