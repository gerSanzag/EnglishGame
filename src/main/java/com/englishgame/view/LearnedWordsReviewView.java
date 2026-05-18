package com.englishgame.view;

import com.englishgame.AppVersion;
import com.englishgame.controller.GameController;
import com.englishgame.model.EnglishExpression;
import com.englishgame.model.LearnedWordsReviewResult;
import com.englishgame.model.ReviewDatabases;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Repaso de bases {@link ReviewDatabases}: layout alineado con {@link GameView} (solo sección Interactive Game),
 * con selector de BBDD de review y sin controles de práctica/revelar.
 */
@Slf4j
public class LearnedWordsReviewView extends JFrame {
    private static final int CORRECT_ANSWER_NEXT_ROUND_DELAY_MS = 2000;
    private static final long MS_PER_DAY = 86_400_000L;

    /** Acierto / mensaje positivo (alineado con This phrase score en verde). */
    private static final Color REVIEW_FEEDBACK_OK_FG = new Color(0, 130, 50);
    private static final Color REVIEW_FEEDBACK_OK_BG = new Color(246, 252, 248);
    private static final Color REVIEW_FEEDBACK_OK_BORDER = new Color(186, 220, 200);
    /** Penalización o fallo (alineado con MainGameView resultLabel incorrecto). */
    private static final Color REVIEW_FEEDBACK_ERR_FG = new Color(231, 76, 60);
    private static final Color REVIEW_FEEDBACK_ERR_BG = new Color(253, 242, 242);
    private static final Color REVIEW_FEEDBACK_ERR_BORDER = new Color(230, 165, 158);

    private final GameController gameController;
    private final LearnedWordsView learnedWordsOwner;
    private final LandingPageView landingPage;
    private final Runnable whenClosed;

    private volatile boolean navigatedToMainMenu;

    private final List<EnglishExpression> deck = new ArrayList<>();
    private final Random reviewDeckRandom = new Random();
    private int index;
    private String currentReviewDatabaseKey = ReviewDatabases.LEARNED_WORDS_KEY;

    private JComboBox<String> reviewDatabaseSelector;
    private JPanel topCardPanel;
    private JPanel scoreCardPanel;
    private JScrollPane feedbackScrollPane;

    private JLabel promptLabel;
    private JLabel scoreLabel;
    private JTextField answerField;
    private JTextArea feedbackArea;

    private JButton submitButton;
    private JButton newRoundButton;
    private JButton backToLandingButton;
    private JButton dataManagementButton;
    private JButton viewWordsButton;
    private JButton learnedWordsButton;

    private JPanel reviewStatsSection;
    private JPanel reviewStatsPanel;
    private JLabel reviewStatsPrimaryLabel;
    private JLabel reviewStatsSecondaryLabel;

    /** Igual que GameView: tras acierto pasa sola a la siguiente tarjeta. */
    private Timer correctAnswerNextRoundTimer;

    public LearnedWordsReviewView(GameController gameController, LearnedWordsView learnedWordsOwner,
            LandingPageView landingPage, Runnable whenClosed) {
        super();
        Objects.requireNonNull(gameController);
        Objects.requireNonNull(learnedWordsOwner);
        Objects.requireNonNull(landingPage);
        this.gameController = gameController;
        this.learnedWordsOwner = learnedWordsOwner;
        this.landingPage = landingPage;
        this.whenClosed = whenClosed == null ? () -> {} : whenClosed;

        setTitle("Review — " + ReviewDatabases.displayNameForKey(currentReviewDatabaseKey) + " ["
                + AppVersion.getDisplayVersion() + "]");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1080, 920);
        setMinimumSize(new Dimension(960, 720));
        setLocationRelativeTo(null);
        setResizable(true);

        buildUi();
        reloadDeck();
        showCurrentRound();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelPendingAutoAdvanceAfterCorrect();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if (navigatedToMainMenu) {
                    return;
                }
                LearnedWordsReviewView.this.whenClosed.run();
            }
        });

        log.info("LearnedWordsReviewView opened ({}): {} entries", currentReviewDatabaseKey, deck.size());
    }

    private void refreshReviewStatsLabels() {
        if (reviewStatsSection == null || reviewStatsPrimaryLabel == null || reviewStatsSecondaryLabel == null) {
            return;
        }
        boolean definitelyDb = ReviewDatabases.WORDS_DEFINITELY_LEARNED_KEY.equalsIgnoreCase(currentReviewDatabaseKey);
        reviewStatsSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                definitelyDb ? "Words definitely learned — totals" : "Learned words — totals"));
        if (definitelyDb) {
            int mastered = gameController.getWordsDefinitelyMasteredTotal();
            int current = gameController.getEnglishExpressionsFromDatabase(
                    ReviewDatabases.WORDS_DEFINITELY_LEARNED_KEY).size();
            reviewStatsPrimaryLabel.setVisible(true);
            reviewStatsSecondaryLabel.setVisible(true);
            reviewStatsPrimaryLabel.setText("Words definitely learned: " + mastered);
            reviewStatsSecondaryLabel.setText("Actualmente en Words definitely learned: " + current);
        } else {
            int current = gameController.getEnglishExpressionsFromDatabase(ReviewDatabases.LEARNED_WORDS_KEY).size();
            reviewStatsPrimaryLabel.setVisible(true);
            reviewStatsSecondaryLabel.setVisible(false);
            reviewStatsPrimaryLabel.setText("Learned words: " + current);
        }
        reviewStatsPanel.revalidate();
        reviewStatsPanel.repaint();
    }

    private void reloadDeck() {
        deck.clear();
        deck.addAll(gameController.getEnglishExpressionsFromDatabase(currentReviewDatabaseKey));
        refreshReviewStatsLabels();
        orderReviewDeckByInclusionAgeBias();
        index = 0;
    }

    private String currentReviewDisplayName() {
        return ReviewDatabases.displayNameForKey(currentReviewDatabaseKey);
    }

    /**
     * Solo Review: orden aleatorio sesgado por antigüedad en la lista ({@code includedAtEpochMillis}).
     * Cuanto más tiempo lleva la entrada en learned_words, más peso y más suele ir al principio del mazo,
     * para dar tiempo a que la memoria se estabilice antes de insistir en lo recién añadido.
     */
    private void orderReviewDeckByInclusionAgeBias() {
        if (deck.size() <= 1) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<EnglishExpression, Double> sortKeys = new HashMap<>(deck.size());
        for (EnglishExpression e : deck) {
            sortKeys.put(e, weightedReviewSortKey(now, e));
        }
        deck.sort(Comparator.comparingDouble(e -> sortKeys.getOrDefault(e, 0.0)));
    }

    private double weightedReviewSortKey(long nowMillis, EnglishExpression e) {
        double w = inclusionWeightDaysSinceLearned(nowMillis, e);
        double u = reviewDeckRandom.nextDouble();
        u = Math.max(u, 1e-12);
        return -Math.log(u) / w;
    }

    private static double inclusionWeightDaysSinceLearned(long nowMillis, EnglishExpression e) {
        long inc = e.getIncludedAtEpochMillis();
        if (inc <= 0L) {
            inc = 0L;
        }
        long ageMs = Math.max(0L, nowMillis - inc);
        double days = ageMs / (double) MS_PER_DAY;
        return Math.max(1.0, days);
    }

    private JPanel createSectionPanel(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2), title));
        section.setBackground(new Color(255, 255, 255, 200));
        section.setOpaque(true);
        return section;
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 15));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(170, 44));
        button.setMinimumSize(new Dimension(140, 40));
        button.setMaximumSize(new Dimension(230, 52));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);

        Color buttonColor = getButtonColor(text);
        button.setBackground(buttonColor);

        Color borderColor = new Color(
                Math.max(0, buttonColor.getRed() - 30),
                Math.max(0, buttonColor.getGreen() - 30),
                Math.max(0, buttonColor.getBlue() - 30)
        );
        button.setBorder(BorderFactory.createLineBorder(borderColor, 2));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                Color hoverColor = new Color(
                        Math.min(255, buttonColor.getRed() + 25),
                        Math.min(255, buttonColor.getGreen() + 25),
                        Math.min(255, buttonColor.getBlue() + 25)
                );
                button.setBackground(hoverColor);
                Color glowBorder = new Color(
                        Math.min(255, buttonColor.getRed() + 50),
                        Math.min(255, buttonColor.getGreen() + 50),
                        Math.min(255, buttonColor.getBlue() + 50)
                );
                button.setBorder(BorderFactory.createLineBorder(glowBorder, 2));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(buttonColor);
                button.setBorder(BorderFactory.createLineBorder(borderColor, 2));
            }
        });

        return button;
    }

    private Color getButtonColor(String buttonText) {
        if (buttonText.contains("Comprobar")) {
            return new Color(56, 189, 248);
        } else if (buttonText.contains("Submit") || buttonText.contains("Answer")) {
            return new Color(16, 185, 129);
        } else if (buttonText.contains("New") || buttonText.contains("Round")) {
            return new Color(59, 130, 246);
        } else if (buttonText.contains("Manage") || buttonText.contains("Data")) {
            return new Color(37, 99, 235);
        } else if (buttonText.contains("View") || buttonText.contains("Words")) {
            return new Color(16, 185, 129);
        } else if (buttonText.contains("Learned")) {
            return new Color(124, 58, 237);
        } else if (buttonText.contains("Detener")) {
            return new Color(249, 115, 22);
        } else if (buttonText.contains("Mostrar todo")) {
            return new Color(100, 116, 139);
        } else if (buttonText.contains("Mostrar")) {
            return new Color(14, 165, 233);
        } else if (buttonText.contains("Back") || buttonText.contains("Main")) {
            return new Color(220, 38, 127);
        } else {
            return new Color(59, 130, 246);
        }
    }

    private String buildScoreLabelHtml(String prefix, int score, String suffix) {
        String safePrefix = prefix == null ? "" : prefix;
        String safeSuffix = suffix == null ? "" : suffix;
        return "<html>" + safePrefix + " <span style='font-size:1.18em;font-weight:700;'>" + score
                + "</span>" + safeSuffix + "</html>";
    }

    private void applyNormalVisualProfile() {
        if (topCardPanel == null || feedbackScrollPane == null) {
            return;
        }
        topCardPanel.setBackground(new Color(253, 254, 255));
        topCardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 218, 232), 1),
                BorderFactory.createEmptyBorder(22, 22, 22, 22)));
        promptLabel.setFont(new Font("Arial", Font.BOLD, 36));
        answerField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 39));
        answerField.setPreferredSize(new Dimension(640, 77));
        answerField.setMinimumSize(new Dimension(200, 66));
        answerField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 93));
        feedbackScrollPane.setPreferredSize(new Dimension(320, 170));
        feedbackScrollPane.setMinimumSize(new Dimension(120, 120));
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 29));
        if (scoreCardPanel != null) {
            scoreCardPanel.setPreferredSize(new Dimension(560, 132));
            scoreCardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        }
        feedbackArea.setFont(new Font("Arial", Font.PLAIN, 19));
        topCardPanel.revalidate();
        topCardPanel.repaint();
    }

    private void bindEnterOnAnswerField() {
        Action act = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (submitButton.isEnabled()) {
                    onSubmit();
                } else if (newRoundButton.isEnabled()) {
                    onNewRound();
                }
            }
        };
        InputMap im = answerField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = answerField.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "learnedWordsReviewSubmitOrAdvance");
        am.put("learnedWordsReviewSubmitOrAdvance", act);
    }

    private void buildUi() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(248, 250, 252),
                        0, getHeight(), new Color(240, 248, 255)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel reviewDbSection = createSectionPanel("Review database");
        JPanel reviewDbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel reviewDbLabel = new JLabel("Database:");
        reviewDbLabel.setFont(new Font("Arial", Font.BOLD, 14));
        reviewDatabaseSelector = new JComboBox<>(gameController.getReviewDatabaseDisplayNames().toArray(new String[0]));
        reviewDatabaseSelector.setPreferredSize(new Dimension(320, 30));
        reviewDatabaseSelector.setToolTipText("Elige la base de datos para este repaso");
        reviewDatabaseSelector.setSelectedItem(ReviewDatabases.displayNameForKey(currentReviewDatabaseKey));
        reviewDatabaseSelector.addActionListener(e -> onReviewDatabaseChanged());
        reviewDbPanel.add(reviewDbLabel);
        reviewDbPanel.add(reviewDatabaseSelector);
        reviewDbSection.add(reviewDbPanel);

        reviewStatsSection = createSectionPanel("Learned words — totals");
        reviewStatsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 64, 10));
        reviewStatsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        reviewStatsPanel.setBorder(BorderFactory.createEmptyBorder(6, 4, 10, 4));
        reviewStatsPrimaryLabel = new JLabel();
        reviewStatsSecondaryLabel = new JLabel();
        Font statsFont = new Font("Arial", Font.PLAIN, 14);
        reviewStatsPrimaryLabel.setFont(statsFont);
        reviewStatsSecondaryLabel.setFont(statsFont);
        reviewStatsPanel.add(reviewStatsPrimaryLabel);
        reviewStatsPanel.add(reviewStatsSecondaryLabel);
        reviewStatsSection.add(reviewStatsPanel);
        refreshReviewStatsLabels();

        JPanel gameSection = createSectionPanel("Interactive Game");
        gameSection.setLayout(new BorderLayout());

        JPanel gameUpperPanel = new JPanel(new BorderLayout());
        gameUpperPanel.setOpaque(false);
        gameUpperPanel.setBorder(BorderFactory.createEmptyBorder(24, 12, 14, 12));

        topCardPanel = new JPanel(new GridBagLayout());
        JPanel topCard = topCardPanel;
        topCard.setOpaque(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        promptLabel = new JLabel("", SwingConstants.CENTER);
        promptLabel.setHorizontalAlignment(SwingConstants.CENTER);
        promptLabel.setForeground(new Color(27, 84, 138));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        topCard.add(promptLabel, gbc);

        answerField = new JTextField();
        answerField.setToolTipText("Enter your English translation here");
        answerField.setBackground(new Color(248, 252, 255));
        answerField.setForeground(new Color(34, 45, 58));
        answerField.setCaretColor(new Color(34, 45, 58));
        answerField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 220), 1),
                        BorderFactory.createLineBorder(new Color(176, 197, 220), 2)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JPanel answerRow = new JPanel(new BorderLayout(0, 0));
        answerRow.setOpaque(false);
        answerRow.add(answerField, BorderLayout.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        topCard.add(answerRow, gbc);

        feedbackArea = new JTextArea(4, 32);
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        applyFeedbackAreaToneOk();
        feedbackArea.setToolTipText("Feedback de la última comprobación");

        feedbackScrollPane = new JScrollPane(feedbackArea);
        feedbackScrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 208, 219), 1));
        gbc.gridy = 2;
        gbc.insets = new Insets(14, 0, 0, 0);
        topCard.add(feedbackScrollPane, gbc);

        gameUpperPanel.add(topCard, BorderLayout.NORTH);

        JPanel centerScoreWrap = new JPanel();
        centerScoreWrap.setLayout(new BoxLayout(centerScoreWrap, BoxLayout.Y_AXIS));
        centerScoreWrap.setOpaque(false);
        centerScoreWrap.setBorder(BorderFactory.createEmptyBorder(28, 0, 12, 0));

        scoreCardPanel = new JPanel(new BorderLayout());
        JPanel scoreCenterPill = scoreCardPanel;
        scoreCenterPill.setOpaque(true);
        scoreCenterPill.setBackground(new Color(248, 251, 255));
        scoreCenterPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(186, 201, 219), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        scoreCenterPill.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoreLabel = new JLabel("This phrase score", SwingConstants.CENTER);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setVerticalAlignment(SwingConstants.CENTER);
        scoreLabel.setForeground(new Color(90, 90, 90));
        scoreCenterPill.add(scoreLabel, BorderLayout.CENTER);
        centerScoreWrap.add(scoreCenterPill);

        gameUpperPanel.add(centerScoreWrap, BorderLayout.CENTER);

        JScrollPane gameUpperScroll = new JScrollPane(gameUpperPanel);
        gameUpperScroll.setBorder(BorderFactory.createEmptyBorder());
        gameUpperScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        gameUpperScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gameUpperScroll.getVerticalScrollBar().setUnitIncrement(24);
        gameSection.add(gameUpperScroll, BorderLayout.CENTER);

        mainPanel.add(reviewDbSection);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(reviewStatsSection);
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(gameSection);

        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(24);

        submitButton = createStyledButton("Submit Answer", "Submit your translation");
        newRoundButton = createStyledButton("New Round", "Otra tarjeta del repaso Learned Words");
        submitButton.setPreferredSize(new Dimension(180, 48));
        newRoundButton.setPreferredSize(new Dimension(180, 48));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        buttonPanel.setOpaque(false);
        buttonPanel.add(submitButton);
        buttonPanel.add(newRoundButton);

        backToLandingButton = createStyledButton("Back to Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("Manage Data", "Go to data management");
        viewWordsButton = createStyledButton("View Words", "View saved words");
        learnedWordsButton = createStyledButton("Learned Words", "View learned words");

        JPanel navSection = createSectionPanel("Navigation");
        navSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        navPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        navPanel.add(dataManagementButton);
        navPanel.add(viewWordsButton);
        navPanel.add(learnedWordsButton);
        navPanel.add(backToLandingButton);
        navSection.add(navPanel);

        JPanel stickySouth = new JPanel();
        stickySouth.setLayout(new BoxLayout(stickySouth, BoxLayout.Y_AXIS));
        stickySouth.setOpaque(true);
        stickySouth.setBackground(new Color(246, 248, 251));
        stickySouth.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 214, 220)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        stickySouth.add(buttonPanel);
        stickySouth.add(Box.createVerticalStrut(8));
        stickySouth.add(navSection);
        stickySouth.add(Box.createVerticalStrut(6));

        add(mainScrollPane, BorderLayout.CENTER);
        add(stickySouth, BorderLayout.SOUTH);

        applyNormalVisualProfile();

        submitButton.addActionListener(e -> onSubmit());
        newRoundButton.addActionListener(e -> onNewRound());
        dataManagementButton.addActionListener(e -> openDataManagement());
        viewWordsButton.addActionListener(e -> openViewWords());
        learnedWordsButton.addActionListener(e -> openLearnedWords());
        backToLandingButton.addActionListener(e -> returnToMainMenu());

        bindEnterOnAnswerField();
        SwingUtilities.invokeLater(() -> getRootPane().setDefaultButton(submitButton));
    }

    private void openDataManagement() {
        log.info("Opening data management from LearnedWordsReviewView");
        cancelPendingAutoAdvanceAfterCorrect();
        setVisible(false);
        new DataManagementView(gameController, landingPage).setVisible(true);
    }

    private void openViewWords() {
        log.info("Opening view words from LearnedWordsReviewView");
        cancelPendingAutoAdvanceAfterCorrect();
        setVisible(false);
        new ViewWordsView(gameController, landingPage).setVisible(true);
    }

    private void openLearnedWords() {
        log.info("Opening learned words from LearnedWordsReviewView");
        cancelPendingAutoAdvanceAfterCorrect();
        setVisible(false);
        new LearnedWordsView(gameController, landingPage).setVisible(true);
    }

    private void onReviewDatabaseChanged() {
        String selected = (String) reviewDatabaseSelector.getSelectedItem();
        if (selected == null) {
            return;
        }
        String newKey = ReviewDatabases.keyForDisplayName(selected);
        if (newKey.equalsIgnoreCase(currentReviewDatabaseKey)) {
            return;
        }
        cancelPendingAutoAdvanceAfterCorrect();
        currentReviewDatabaseKey = newKey;
        setTitle("Review — " + selected + " [" + AppVersion.getDisplayVersion() + "]");
        reloadDeck();
        showCurrentRound();
        log.info("Review database switched to {}", currentReviewDatabaseKey);
    }

    private void returnToMainMenu() {
        log.info("Returning to landing from LearnedWordsReviewView");
        cancelPendingAutoAdvanceAfterCorrect();
        navigatedToMainMenu = true;
        learnedWordsOwner.setVisible(false);
        landingPage.returnToLanding();
        dispose();
    }

    private void cancelPendingAutoAdvanceAfterCorrect() {
        if (correctAnswerNextRoundTimer != null) {
            correctAnswerNextRoundTimer.stop();
            correctAnswerNextRoundTimer = null;
        }
    }

    private void scheduleAutoAdvanceAfterCorrect(LearnedWordsReviewResult.Outcome outcome) {
        cancelPendingAutoAdvanceAfterCorrect();
        final boolean incrementIndex =
                outcome == LearnedWordsReviewResult.Outcome.STILL_IN_LEARNED;
        int secs = Math.max(1, CORRECT_ANSWER_NEXT_ROUND_DELAY_MS / 1000);
        feedbackArea.setText(feedbackArea.getText() + "\n\nSiguiente tarjeta en " + secs + " s...");
        correctAnswerNextRoundTimer = new Timer(CORRECT_ANSWER_NEXT_ROUND_DELAY_MS, e -> {
            correctAnswerNextRoundTimer = null;
            if (deck.isEmpty()) {
                JOptionPane.showMessageDialog(LearnedWordsReviewView.this, "Sesión terminada.", "Review",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
                return;
            }
            if (incrementIndex) {
                index = (index + 1) % deck.size();
            }
            showCurrentRound();
        });
        correctAnswerNextRoundTimer.setRepeats(false);
        newRoundButton.setEnabled(false);
        correctAnswerNextRoundTimer.start();
    }

    private void refreshScoreLabel(EnglishExpression en) {
        if (en == null) {
            scoreLabel.setText("This phrase score");
            scoreLabel.setForeground(new Color(90, 90, 90));
            return;
        }
        int phraseScore = en.getScore();
        scoreLabel.setText(buildScoreLabelHtml("This phrase score:", phraseScore, ""));
        scoreLabel.setForeground(REVIEW_FEEDBACK_OK_FG);
    }

    private void applyFeedbackAreaToneOk() {
        feedbackArea.setForeground(REVIEW_FEEDBACK_OK_FG);
        feedbackArea.setCaretColor(REVIEW_FEEDBACK_OK_FG);
        feedbackArea.setBackground(REVIEW_FEEDBACK_OK_BG);
        feedbackArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(REVIEW_FEEDBACK_OK_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    private void applyFeedbackAreaTonePenalty() {
        feedbackArea.setForeground(REVIEW_FEEDBACK_ERR_FG);
        feedbackArea.setCaretColor(REVIEW_FEEDBACK_ERR_FG);
        feedbackArea.setBackground(REVIEW_FEEDBACK_ERR_BG);
        feedbackArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(REVIEW_FEEDBACK_ERR_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    private static boolean isPenaltyFeedback(LearnedWordsReviewResult r) {
        if (r.outcome() == LearnedWordsReviewResult.Outcome.DEMOTED_TO_PRACTICE
                || r.outcome() == LearnedWordsReviewResult.Outcome.RETURNED_TO_LEARNED) {
            return true;
        }
        return r.outcome() == LearnedWordsReviewResult.Outcome.STILL_IN_LEARNED && !r.answeredCorrectly();
    }

    private void showCurrentRound() {
        cancelPendingAutoAdvanceAfterCorrect();
        feedbackArea.setText("");
        applyFeedbackAreaToneOk();
        answerField.setText("");
        answerField.setEditable(true);
        submitButton.setEnabled(true);
        newRoundButton.setEnabled(!deck.isEmpty());
        if (deck.isEmpty()) {
            promptLabel.setText("<html><div style='text-align:center;'>No quedan entradas.</div></html>");
            refreshScoreLabel(null);
            submitButton.setEnabled(false);
            newRoundButton.setEnabled(false);
            return;
        }
        index = Math.max(0, Math.min(index, deck.size() - 1));
        EnglishExpression en = deck.get(index);
        String es = summarizeSpanish(esSourceLines(en));
        promptLabel.setText("<html><div style='text-align:center;'>" + escapeHtml(es) + "</div></html>");
        refreshScoreLabel(en);
        answerField.requestFocusInWindow();
        SwingUtilities.invokeLater(() -> getRootPane().setDefaultButton(submitButton));
    }

    private List<String> esSourceLines(EnglishExpression en) {
        List<String> list = new ArrayList<>();
        if (en.getTranslations() != null) {
            en.getTranslations().stream()
                    .map(SpanishExpression::getExpression)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(list::add);
        }
        if (list.isEmpty()) {
            list.add("(Sin referencia en español en esta entrada)");
        }
        return list;
    }

    private static String summarizeSpanish(List<String> lines) {
        if (lines.size() <= 2) {
            return String.join(" · ", lines);
        }
        return lines.get(0) + " · … +" + (lines.size() - 1);
    }

    private static String escapeHtml(String plain) {
        if (plain == null) {
            return "";
        }
        return plain.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void onSubmit() {
        if (deck.isEmpty()) {
            return;
        }
        EnglishExpression current = deck.get(Math.max(0, Math.min(index, deck.size() - 1)));
        Optional<LearnedWordsReviewResult> res =
                gameController.submitLearnedWordsReviewAnswer(current, answerField.getText(), currentReviewDatabaseKey);

        if (res.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No se ha podido aplicar la corrección (la entrada ya no está en "
                            + currentReviewDisplayName() + ").",
                    "Review", JOptionPane.WARNING_MESSAGE);
            reloadDeckIfStillAnyOrClose();
            return;
        }

        LearnedWordsReviewResult r = res.get();
        showReviewMilestoneCongratulations(r);
        feedbackArea.setText(buildFeedbackLines(r));
        if (isPenaltyFeedback(r)) {
            applyFeedbackAreaTonePenalty();
        } else {
            applyFeedbackAreaToneOk();
        }
        scoreLabel.setText(buildScoreLabelHtml("This phrase score:", r.scoreAfter(), ""));
        scoreLabel.setForeground(REVIEW_FEEDBACK_OK_FG);

        if (r.outcome() == LearnedWordsReviewResult.Outcome.DEMOTED_TO_PRACTICE
                || r.outcome() == LearnedWordsReviewResult.Outcome.MASTERED_REMOVED_EVERYWHERE
                || r.outcome() == LearnedWordsReviewResult.Outcome.PROMOTED_TO_DEFINITELY_LEARNED
                || r.outcome() == LearnedWordsReviewResult.Outcome.RETURNED_TO_LEARNED) {
            deck.remove(current);
            if (!deck.isEmpty()) {
                index = Math.min(index, deck.size() - 1);
            }
        }
        refreshReviewStatsLabels();

        answerField.setEditable(false);
        submitButton.setEnabled(false);

        if (deck.isEmpty()) {
            cancelPendingAutoAdvanceAfterCorrect();
            JOptionPane.showMessageDialog(this, "Sesión terminada.", "Review", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        boolean shouldAutoAdvance = r.answeredCorrectly()
                && r.outcome() != LearnedWordsReviewResult.Outcome.DEMOTED_TO_PRACTICE;
        if (shouldAutoAdvance) {
            scheduleAutoAdvanceAfterCorrect(r.outcome());
        } else {
            newRoundButton.setEnabled(true);
            SwingUtilities.invokeLater(() -> getRootPane().setDefaultButton(newRoundButton));
        }
    }

    private void reloadDeckIfStillAnyOrClose() {
        List<EnglishExpression> fresh = gameController.getEnglishExpressionsFromDatabase(currentReviewDatabaseKey);
        if (fresh.isEmpty()) {
            dispose();
            return;
        }
        deck.clear();
        deck.addAll(fresh);
        orderReviewDeckByInclusionAgeBias();
        index = 0;
        showCurrentRound();
    }

    private void showReviewMilestoneCongratulations(LearnedWordsReviewResult r) {
        if (!r.answeredCorrectly()) {
            return;
        }
        String word = Optional.ofNullable(r.expectedEnglish()).map(String::trim).orElse("");
        if (word.isEmpty()) {
            return;
        }
        switch (r.outcome()) {
            case PROMOTED_TO_DEFINITELY_LEARNED -> JOptionPane.showMessageDialog(this,
                    "Congratulations! \"" + word
                            + "\" reached score 28 in Learned words and is now in Words definitely learned.\nKeep it up!",
                    "Word learned",
                    JOptionPane.INFORMATION_MESSAGE);
            case MASTERED_REMOVED_EVERYWHERE -> JOptionPane.showMessageDialog(this,
                    "Congratulations! \"" + word
                            + "\" reached the maximum review score (35) and is mastered.\nKeep it up!",
                    "Word learned",
                    JOptionPane.INFORMATION_MESSAGE);
            default -> { }
        }
    }

    private String buildFeedbackLines(LearnedWordsReviewResult r) {
        List<String> parts = new ArrayList<>();
        switch (r.outcome()) {
            case MASTERED_REMOVED_EVERYWHERE -> parts.add("Dominado (35): la expresión sale de todas las bases.");
            case PROMOTED_TO_DEFINITELY_LEARNED ->
                    parts.add("28 puntos: pasa a Words definitely learned para seguir repaso (29–35).");
            case RETURNED_TO_LEARNED ->
                    parts.add("Incorrecto (−5): vuelve a Learned words con el nuevo score.");
            case DEMOTED_TO_PRACTICE -> {
                String db = r.restoredToPracticeDatabase();
                if (db != null && !db.isBlank()) {
                    parts.add("Por debajo de 21: vuelve a práctica (" + db + ").");
                } else {
                    parts.add("Por debajo de 21: vuelve a práctica.");
                }
            }
            default -> parts.add(r.answeredCorrectly() ? "Correcto (+1)." : "Incorrecto (−5).");
        }
        if (!r.answeredCorrectly()) {
            parts.add("Esperado: " + quote(r.expectedEnglish()));
            parts.add("Tu respuesta: " + quote(r.userEntered()));
        }
        return String.join("\n", parts);
    }

    private static String quote(String s) {
        return "\"" + Optional.ofNullable(s).orElse("").trim() + "\"";
    }

    private void onNewRound() {
        cancelPendingAutoAdvanceAfterCorrect();
        if (deck.isEmpty()) {
            dispose();
            return;
        }
        index = (index + 1) % deck.size();
        showCurrentRound();
    }
}
