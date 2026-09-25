# BestStartingFive

BestStartingFive is a Java Swing decision-support application for selecting a
basketball starting lineup based on player statistics, positional suitability,
and the profile of the opposing team.

This project was developed in the context of my undergraduate thesis at the
Computer Science Department of the University of Crete, under the supervision
of Prof. Yannis Tzitzikas.

## Features

- Imports player statistics from CSV files.
- Evaluates offensive and defensive performance.
- Considers player suitability for the five basketball positions.
- Creates a statistical profile of the opposing team.
- Supports full-roster, top-five-by-minutes, and manual opponent analysis.
- Adjusts player scores according to the selected matchup.
- Uses the Hungarian Algorithm to assign players to the positions PG, SG, SF,
  PF, and C.
- Presents the recommended lineup and its scores through a Java Swing interface.

## Requirements

- Java Development Kit (JDK) 8 or newer.
- No external libraries are required.

## How to Run

Clone or download the repository and open a terminal inside its folder.

Compile the source files:

```bash
mkdir out
javac -encoding UTF-8 -d out *.java
```

Start the graphical application:

```bash
java -cp out LauncherGUI
```

On Windows, the included Makefile can also be used with GNU Make:

```bash
make build
make run
```

## How to Use

1. Drag your team's CSV file into the left input area.
2. Drag the opponent's CSV file into the right input area.
3. Select the available players from your team.
4. Choose an opponent-analysis mode:
   - Full roster
   - Top 5 by minutes
   - Manual opponent selection
5. Optionally enable **Show details**.
6. Press **Generate lineup**.

The application displays the recommended player for each position, the default
and matchup-adjusted scores, opponent tendencies, and a summary of changes.

## CSV Format

CSV files must contain the following header:

```text
Name,Position,Team,MIN,PTS,FGM,FGA,3PM,3PA,FTM,FTA,OREB,DREB,REB,AST,TOV,STL,BLK,PF
```

The repository includes sample CSV files that can be used to test the
application.

## Project Structure

- `LauncherGUI.java`: graphical user interface and application entry point.
- `Player.java`: player data and score calculations.
- `PlayerLoader.java`: CSV loading and normalization.
- `OpponentProfile.java`: representation of opponent characteristics.
- `OpponentProfileBuilder.java`: construction of opponent profiles.
- `Team.java`: lineup matrices and team-score calculations.
- `HungarianAlgorithm.java`: optimal assignment algorithm.
- `LineupRunner.java`: execution of the lineup-selection pipeline.
- `LineupResult.java`: storage of the final results.
- `Config.java`: model parameters, weights, and thresholds.
- `Main.java`: command-line execution.
- `Makefile`: build and run commands for Windows.

## Author

Aristotelis Moulas  
Computer Science Department  
University of Crete
