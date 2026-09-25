import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LauncherGUI extends JFrame {

    private JTextField myTeamField;
    private JTextField opponentField;
    private JTextArea outputArea;

    private JPanel myTeamDropPanel;
    private JPanel opponentDropPanel;

    private JPanel myPlayerSelectionPanel;
    private JPanel opponentPlayerSelectionPanel;

    private final Map<String, JCheckBox> myPlayerCheckBoxes = new LinkedHashMap<>();
    private final Map<String, JCheckBox> opponentPlayerCheckBoxes = new LinkedHashMap<>();

    private JRadioButton fullRosterRadio;
    private JRadioButton top5Radio;
    private JRadioButton manualOpponentRadio;
    private JCheckBox showDetailsCheckBox;

    public LauncherGUI() {
        setTitle("Best Starting Five");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JPanel dropPanelContainer = new JPanel(new GridLayout(1, 2, 12, 12));

        myTeamField = new JTextField();
        myTeamField.setEditable(false);
        opponentField = new JTextField();
        opponentField.setEditable(false);

        myTeamDropPanel = createDropPanel("Drop your team CSV here", myTeamField, true);
        opponentDropPanel = createDropPanel("Drop opponent CSV here", opponentField, false);

        dropPanelContainer.add(myTeamDropPanel);
        dropPanelContainer.add(opponentDropPanel);

        JPanel controlsPanel = new JPanel(new BorderLayout(8, 8));
        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));

        fullRosterRadio = new JRadioButton("Full roster", false);
        top5Radio = new JRadioButton("Top 5 by minutes", true);
        manualOpponentRadio = new JRadioButton("Manual opponent", false);
        showDetailsCheckBox = new JCheckBox("Show details", false);

        ButtonGroup opponentModeGroup = new ButtonGroup();
        opponentModeGroup.add(fullRosterRadio);
        opponentModeGroup.add(top5Radio);
        opponentModeGroup.add(manualOpponentRadio);

        JButton generateButton = new JButton("Generate lineup");
        generateButton.setPreferredSize(new Dimension(150, 32));

        JButton selectAllMineButton = new JButton("Select all mine");
        JButton clearAllMineButton = new JButton("Clear mine");
        JButton selectAllOppButton = new JButton("Select all opponent");
        JButton clearAllOppButton = new JButton("Clear opponent");

        leftControls.add(fullRosterRadio);
        leftControls.add(top5Radio);
        leftControls.add(manualOpponentRadio);
        leftControls.add(showDetailsCheckBox);
        leftControls.add(selectAllMineButton);
        leftControls.add(clearAllMineButton);
        leftControls.add(selectAllOppButton);
        leftControls.add(clearAllOppButton);

        rightControls.add(generateButton);

        controlsPanel.add(leftControls, BorderLayout.CENTER);
        controlsPanel.add(rightControls, BorderLayout.EAST);

        topPanel.add(dropPanelContainer, BorderLayout.NORTH);
        topPanel.add(controlsPanel, BorderLayout.SOUTH);

        myPlayerSelectionPanel = new JPanel();
        myPlayerSelectionPanel.setLayout(new BoxLayout(myPlayerSelectionPanel, BoxLayout.Y_AXIS));
        myPlayerSelectionPanel.setBorder(BorderFactory.createTitledBorder("My available players"));

        opponentPlayerSelectionPanel = new JPanel();
        opponentPlayerSelectionPanel.setLayout(new BoxLayout(opponentPlayerSelectionPanel, BoxLayout.Y_AXIS));
        opponentPlayerSelectionPanel.setBorder(BorderFactory.createTitledBorder("Opponent players used"));

        JScrollPane myScroll = new JScrollPane(myPlayerSelectionPanel);
        myScroll.setPreferredSize(new Dimension(260, 400));

        JScrollPane opponentScroll = new JScrollPane(opponentPlayerSelectionPanel);
        opponentScroll.setPreferredSize(new Dimension(260, 400));

        JPanel checklistContainer = new JPanel(new GridLayout(1, 2, 10, 10));
        checklistContainer.add(myScroll);
        checklistContainer.add(opponentScroll);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputArea.setText("Drop both CSV files and press Generate lineup.\n");

        JSplitPane centerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                checklistContainer,
                new JScrollPane(outputArea)
        );
        centerSplit.setResizeWeight(0.48);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerSplit, BorderLayout.CENTER);
        add(mainPanel);

        generateButton.addActionListener(e -> generateLineup());
        selectAllMineButton.addActionListener(e -> setAllMyPlayerCheckboxes(true));
        clearAllMineButton.addActionListener(e -> setAllMyPlayerCheckboxes(false));
        selectAllOppButton.addActionListener(e -> setAllOpponentCheckboxes(true));
        clearAllOppButton.addActionListener(e -> setAllOpponentCheckboxes(false));
    }

    private JPanel createDropPanel(String title, JTextField targetField, boolean isMyTeamPanel) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.GRAY, 2, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setBackground(new Color(245, 245, 245));
        panel.setPreferredSize(new Dimension(300, 120));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel subtitle = new JLabel("CSV files only", SwingConstants.CENTER);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitle.setForeground(Color.DARK_GRAY);

        targetField.setHorizontalAlignment(JTextField.CENTER);
        targetField.setBackground(Color.WHITE);

        JPanel labelPanel = new JPanel(new GridLayout(2, 1));
        labelPanel.setOpaque(false);
        labelPanel.add(titleLabel);
        labelPanel.add(subtitle);

        panel.add(labelPanel, BorderLayout.NORTH);
        panel.add(targetField, BorderLayout.CENTER);
        panel.setTransferHandler(new FileDropHandler(targetField, panel, isMyTeamPanel));

        return panel;
    }

    private class FileDropHandler extends TransferHandler {
        private final JTextField targetField;
        private final JPanel targetPanel;
        private final boolean isMyTeamPanel;

        FileDropHandler(JTextField targetField, JPanel targetPanel, boolean isMyTeamPanel) {
            this.targetField = targetField;
            this.targetPanel = targetPanel;
            this.isMyTeamPanel = isMyTeamPanel;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            boolean canImport = support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);

            if (canImport) {
                targetPanel.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(0, 120, 215), 3, true),
                        BorderFactory.createEmptyBorder(15, 15, 15, 15)
                ));
            }

            return canImport;
        }

        @Override
        public boolean importData(TransferSupport support) {
            try {
                if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    resetBorder();
                    return false;
                }

                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) support.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);

                if (files.isEmpty()) {
                    resetBorder();
                    return false;
                }

                File file = files.get(0);

                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    JOptionPane.showMessageDialog(
                            LauncherGUI.this,
                            "Please drop a CSV file.",
                            "Invalid file",
                            JOptionPane.WARNING_MESSAGE
                    );
                    resetBorder();
                    return false;
                }

                targetField.setText(file.getAbsolutePath());

                if (isMyTeamPanel) {
                    loadMyPlayersIntoChecklist(file.getAbsolutePath());
                } else {
                    loadOpponentPlayersIntoChecklist(file.getAbsolutePath());
                }

                resetBorder();
                return true;
            } catch (Exception e) {
                resetBorder();
                JOptionPane.showMessageDialog(
                        LauncherGUI.this,
                        "Could not read the file:\n" + e.getMessage(),
                        "File error",
                        JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
        }

        private void resetBorder() {
            targetPanel.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(Color.GRAY, 2, true),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
        }
    }

    private void loadMyPlayersIntoChecklist(String path) {
        try {
            List<Player> players = new PlayerLoader().loadPlayersFromCsv(path);
            myPlayerSelectionPanel.removeAll();
            myPlayerCheckBoxes.clear();

            if (players.isEmpty()) {
                myPlayerSelectionPanel.add(new JLabel("No players found."));
            } else {
                for (Player player : players) {
                    JCheckBox checkBox = new JCheckBox(player.getName() + " (" + player.getPosition() + ")", true);
                    myPlayerCheckBoxes.put(player.getName(), checkBox);
                    myPlayerSelectionPanel.add(checkBox);
                }
            }

            myPlayerSelectionPanel.revalidate();
            myPlayerSelectionPanel.repaint();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load your team CSV:\n" + e.getMessage(),
                    "CSV error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadOpponentPlayersIntoChecklist(String path) {
        try {
            List<Player> players = new PlayerLoader().loadPlayersFromCsv(path);
            opponentPlayerSelectionPanel.removeAll();
            opponentPlayerCheckBoxes.clear();

            if (players.isEmpty()) {
                opponentPlayerSelectionPanel.add(new JLabel("No opponent players found."));
            } else {
                for (Player player : players) {
                    JCheckBox checkBox = new JCheckBox(player.getName() + " (" + player.getPosition() + ")", true);
                    opponentPlayerCheckBoxes.put(player.getName(), checkBox);
                    opponentPlayerSelectionPanel.add(checkBox);
                }
            }

            opponentPlayerSelectionPanel.revalidate();
            opponentPlayerSelectionPanel.repaint();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load opponent CSV:\n" + e.getMessage(),
                    "CSV error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setAllMyPlayerCheckboxes(boolean selected) {
        for (JCheckBox checkBox : myPlayerCheckBoxes.values()) {
            checkBox.setSelected(selected);
        }
    }

    private void setAllOpponentCheckboxes(boolean selected) {
        for (JCheckBox checkBox : opponentPlayerCheckBoxes.values()) {
            checkBox.setSelected(selected);
        }
    }

    private Set<String> getSelectedMyPlayerNames() {
        Set<String> selected = new HashSet<>();

        for (Map.Entry<String, JCheckBox> entry : myPlayerCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }

        return selected;
    }

    private Set<String> getSelectedOpponentNames() {
        Set<String> selected = new HashSet<>();

        for (Map.Entry<String, JCheckBox> entry : opponentPlayerCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }

        return selected;
    }

    private void generateLineup() {
        String myTeamPath = myTeamField.getText().trim();
        String opponentPath = opponentField.getText().trim();

        if (myTeamPath.isEmpty() || opponentPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please drop both CSV files first.",
                    "Missing files",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Set<String> selectedMyPlayers = getSelectedMyPlayerNames();
        if (selectedMyPlayers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please keep at least one player selected for your team.",
                    "No players selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        LineupRunner.OpponentMode opponentMode = getSelectedOpponentMode();
        Set<String> selectedOpponentNames = getSelectedOpponentNames();

        if (opponentMode == LineupRunner.OpponentMode.MANUAL_SELECTION && selectedOpponentNames.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Manual opponent mode needs at least one selected opponent player.",
                    "No opponent selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            outputArea.setText("Generating lineup...\n");

            LineupResult result = LineupRunner.runPipeline(
                    myTeamPath,
                    opponentPath,
                    opponentMode,
                    selectedMyPlayers,
                    selectedOpponentNames
            );

            outputArea.setText(buildOutputText(result, opponentMode));

            JOptionPane.showMessageDialog(this,
                    "Lineup generated.",
                    "Done",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Problem loading CSV files:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Something went wrong:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private LineupRunner.OpponentMode getSelectedOpponentMode() {
        if (manualOpponentRadio.isSelected()) return LineupRunner.OpponentMode.MANUAL_SELECTION;
        if (fullRosterRadio.isSelected()) return LineupRunner.OpponentMode.FULL_ROSTER;
        return LineupRunner.OpponentMode.TOP_5_MINUTES;
    }

    private String buildOutputText(LineupResult result, LineupRunner.OpponentMode mode) {
        Team team = result.getTeam();
        List<String> positions = result.getPositions();
        int[] matchupAssignment = result.getAssignment();
        OpponentProfile fallbackProfile = result.getOpponentProfile();
        Map<String, OpponentProfile> positionProfiles = result.getPositionOpponentProfiles();

        int[] baseAssignment = new HungarianAlgorithm(team.buildBaseCostMatrix(positions)).execute();

        StringBuilder sb = new StringBuilder();
        sb.append("Best Starting Five\n\n");
        sb.append("Opponent view: ").append(modeLabel(mode)).append("\n\n");

        sb.append(team.buildLineupText(positions, matchupAssignment, positionProfiles, fallbackProfile));

        sb.append("\nTeam scores\n");
        sb.append(String.format("Default lineup score: %.3f%n", team.totalBaseScore(positions, baseAssignment)));
        sb.append(String.format("Matchup lineup score: %.3f%n", team.totalMatchupScore(positions, matchupAssignment, positionProfiles, fallbackProfile)));

        sb.append(team.buildChangeSummaryText(positions, baseAssignment, matchupAssignment, positionProfiles, fallbackProfile));
        sb.append("\nOpponent tendencies\n");
        sb.append(formatProfile("Full roster", result.getOpponentTeamProfile()));
        sb.append(formatProfile("Main rotation", result.getOpponentStartersProfile()));

        if (showDetailsCheckBox.isSelected()) {
            sb.append(team.buildClosestCallsText(positions, matchupAssignment, positionProfiles, fallbackProfile, 2));
        }

        return sb.toString();
    }

    private String modeLabel(LineupRunner.OpponentMode mode) {
        switch (mode) {
            case FULL_ROSTER:
                return "full roster";
            case MANUAL_SELECTION:
                return "manual opponent selection";
            case TOP_5_MINUTES:
            default:
                return "top 5 by minutes";
        }
    }

    private String formatProfile(String label, OpponentProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append("\n");
        sb.append(String.format("  Ball pressure:        %-8s %.2f%n", level(profile.getTurnoverPressure()), profile.getTurnoverPressure()));
        sb.append(String.format("  Three-point volume:   %-8s %.2f%n", level(profile.getAllows3()), profile.getAllows3()));
        sb.append(String.format("  Offensive rebounding: %-8s %.2f%n", level(profile.getOffensiveRebRate()), profile.getOffensiveRebRate()));
        sb.append(String.format("  Paint attack:         %-8s %.2f%n", level(profile.getPaintAttack()), profile.getPaintAttack()));
        sb.append(String.format("  Foul drawing:         %-8s %.2f%n", level(profile.getFoulDrawing()), profile.getFoulDrawing()));
        sb.append(String.format("  Turnover weakness:    %-8s %.2f%n", level(profile.getTurnoverWeakness()), profile.getTurnoverWeakness()));
        sb.append(String.format("  Foul weakness:        %-8s %.2f%n", level(profile.getFoulWeakness()), profile.getFoulWeakness()));
        sb.append(String.format("  Rim weakness:         %-8s %.2f%n", level(profile.getRimProtectionWeakness()), profile.getRimProtectionWeakness()));
        sb.append("\n");
        return sb.toString();
    }

    private String level(double value) {
        if (value >= 0.70) return "high";
        if (value <= 0.35) return "low";
        return "medium";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LauncherGUI().setVisible(true));
    }
}
