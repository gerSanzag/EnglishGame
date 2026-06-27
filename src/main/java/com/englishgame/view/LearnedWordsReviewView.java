package com.englishgame.view;

import com.englishgame.AppGameMode;
import com.englishgame.AppVersion;
import com.englishgame.UiText;
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
import java.util.List;
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
    private static final int REVIEW_PROMPT_SCROLL_W = 980;
    private static final int REVIEW_PROMPT_SCROLL_MAX_W = 1120;
    private static final int REVIEW_FEEDBACK_SCROLL_W = 980;
    private static final int REVIEW_FEEDBACK_SCROLL_MAX_W = 1120;
    private static final int REVIEW_FEEDBACK_SCROLL_MIN_H = 44;
    private static final int REVIEW_FEEDBACK_SCROLL_DEFAULT_H = 88;
    private static final int REVIEW_FEEDBACK_SCROLL_MAX_H = 260;
    private static final int REVIEW_SCORE_CARD_W = 560;

    /**
     * Sesgo hacia entradas antiguas en learned/review: exponente sobre días desde {@code included_at}.
     * Valores &gt; 1 amplifican la diferencia entre reciente y antiguo.
     */
    private static final double REVIEW_AGE_WEIGHT_EXPONENT = 1.85;
    private static final double REVIEW_MIN_AGE_DAYS = 1.0;
    /** Evita rachas largas de la misma BBDD de origen en sesiones con muchas entradas de una sola fuente. */
    private static final double REVIEW_SAME_SOURCE_WEIGHT_FACTOR = 0.22;
    private static final double REVIEW_REPEAT_CARD_WEIGHT_FACTOR = 0.06;

    private final GameController gameController;
    private final LearnedWordsView learnedWordsOwner;
    private final LandingPageView landingPage;
    private final Runnable whenClosed;

    private volatile boolean navigatedToMainMenu;

    private final List<EnglishExpression> deck = new ArrayList<>();
    private final Random reviewDeckRandom = new Random();
    private int index;
    private int lastPickedDeckIndex = -1;
    private String lastShownPracticeSourceKey;
    private String currentReviewDatabaseKey = ReviewDatabases.LEARNED_WORDS_KEY;

    private JComboBox<String> reviewDatabaseSelector;
    private JComboBox<String> practiceSourceSelector;
    private JPanel practiceSourceRow;
    private JPanel topCardPanel;
    private JPanel scoreCardPanel;
    private JScrollPane feedbackScrollPane;

    private JTextArea promptTextArea;
    private JScrollPane promptScrollPane;
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
        setExtendedState(JFrame.MAXIMIZED_BOTH);

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
                definitelyDb
                        ? ui("Words definitely learned — totals", "Words definitely learned — totales")
                        : ui("Learned words — totals", "Learned words — totales")));
        if (definitelyDb) {
            int mastered = gameController.getWordsDefinitelyMasteredTotal();
            int current = gameController.getEnglishExpressionsFromDatabase(
                    ReviewDatabases.WORDS_DEFINITELY_LEARNED_KEY).size();
            reviewStatsPrimaryLabel.setVisible(true);
            reviewStatsSecondaryLabel.setVisible(true);
            reviewStatsPrimaryLabel.setText(ui("Words learned and deleted: ", "Palabras aprendidas y borradas: ")
                    + mastered);
            reviewStatsSecondaryLabel.setText(ui("Currently in Words definitely learned: ",
                    "Actualmente en Words definitely learned: ") + current);
        } else {
            int current = gameController.getEnglishExpressionsFromDatabase(ReviewDatabases.LEARNED_WORDS_KEY).size();
            reviewStatsPrimaryLabel.setVisible(true);
            reviewStatsSecondaryLabel.setVisible(false);
            reviewStatsPrimaryLabel.setText(ui("Learned words: ", "Learned words: ") + current);
        }
        reviewStatsPanel.revalidate();
        reviewStatsPanel.repaint();
    }

    private void reloadDeck() {
        deck.clear();
        deck.addAll(gameController.getEnglishExpressionsFromDatabase(currentReviewDatabaseKey));
        refreshReviewStatsLabels();
        lastPickedDeckIndex = -1;
        lastShownPracticeSourceKey = null;
        pickNextReviewCardWeighted();
    }

    private String currentReviewDisplayName() {
        return ReviewDatabases.displayNameForKey(currentReviewDatabaseKey);
    }

    private boolean isDefinitelyReviewDatabase() {
        return ReviewDatabases.WORDS_DEFINITELY_LEARNED_KEY.equalsIgnoreCase(currentReviewDatabaseKey);
    }

    /**
     * Review: en cada tirada elige una tarjeta al azar con peso creciente según antigüedad en la lista
     * ({@code includedAtEpochMillis}). Las más recientes salen menos; se penaliza repetir la misma BBDD
     * de origen o la misma tarjeta en la ronda siguiente.
     */
    private void pickNextReviewCardWeighted() {
        if (deck.isEmpty()) {
            index = 0;
            return;
        }
        if (deck.size() == 1) {
            index = 0;
            rememberLastShownCard(deck.get(0));
            return;
        }

        long now = System.currentTimeMillis();
        double[] weights = new double[deck.size()];
        for (int i = 0; i < deck.size(); i++) {
            EnglishExpression card = deck.get(i);
            double w = reviewSelectionWeight(now, card);
            if (i == lastPickedDeckIndex) {
                w *= REVIEW_REPEAT_CARD_WEIGHT_FACTOR;
            }
            String sourceKey = practiceSourceKey(card);
            if (lastShownPracticeSourceKey != null && sourceKey != null
                    && lastShownPracticeSourceKey.equalsIgnoreCase(sourceKey)) {
                w *= REVIEW_SAME_SOURCE_WEIGHT_FACTOR;
            }
            weights[i] = Math.max(1e-9, w);
        }

        index = pickWeightedIndex(weights);
        lastPickedDeckIndex = index;
        rememberLastShownCard(deck.get(index));
    }

    private void rememberLastShownCard(EnglishExpression card) {
        lastShownPracticeSourceKey = practiceSourceKey(card);
    }

    private static String practiceSourceKey(EnglishExpression card) {
        if (card == null) {
            return null;
        }
        String raw = Optional.ofNullable(card.getPracticeSourceDatabase()).map(String::trim).orElse("");
        return raw.isEmpty() ? null : raw;
    }

    private double reviewSelectionWeight(long nowMillis, EnglishExpression e) {
        double days = inclusionAgeDays(nowMillis, e);
        return Math.pow(Math.max(REVIEW_MIN_AGE_DAYS, days), REVIEW_AGE_WEIGHT_EXPONENT);
    }

    private static double inclusionAgeDays(long nowMillis, EnglishExpression e) {
        long inc = e.getIncludedAtEpochMillis();
        if (inc <= 0L) {
            return 365.0;
        }
        long ageMs = Math.max(0L, nowMillis - inc);
        return ageMs / (double) MS_PER_DAY;
    }

    private int pickWeightedIndex(double[] weights) {
        double sum = 0.0;
        for (double w : weights) {
            sum += w;
        }
        if (sum <= 0.0) {
            return reviewDeckRandom.nextInt(weights.length);
        }
        double r = reviewDeckRandom.nextDouble() * sum;
        double acc = 0.0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r < acc) {
                return i;
            }
        }
        return weights.length - 1;
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
        if (buttonText.contains("Comprobar") || buttonText.contains("Check")) {
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
        } else if (buttonText.contains("Detener") || buttonText.contains("Stop reveal")) {
            return new Color(249, 115, 22);
        } else if (buttonText.contains("Mostrar todo") || buttonText.contains("Show all")) {
            return new Color(100, 116, 139);
        } else if (buttonText.contains("Mostrar") || buttonText.contains("Show answer")) {
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

    private boolean isDefinitionMode() {
        return gameController.getAppGameMode() == AppGameMode.DEFINITION;
    }

    private String ui(String en, String es) {
        return UiText.t(gameController.getAppGameMode(), en, es);
    }

    private String practiceSourcePlaceholder() {
        return ui("— Select source database —", "— Selecciona la base de origen —");
    }

    private void applyNormalVisualProfile() {
        if (topCardPanel == null || feedbackScrollPane == null || promptScrollPane == null) {
            return;
        }
        topCardPanel.setBackground(new Color(253, 254, 255));
        topCardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(207, 218, 232), 1),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        if (isDefinitionMode()) {
            promptTextArea.setFont(new Font("Segoe UI", Font.BOLD, 23));
            promptTextArea.setForeground(new Color(0, 118, 92));
            promptScrollPane.setPreferredSize(new Dimension(REVIEW_PROMPT_SCROLL_W, 76));
            promptScrollPane.setMaximumSize(new Dimension(REVIEW_PROMPT_SCROLL_MAX_W, 96));
        } else {
            promptTextArea.setFont(new Font("Arial", Font.BOLD, 28));
            promptTextArea.setForeground(new Color(27, 84, 138));
            promptScrollPane.setPreferredSize(new Dimension(REVIEW_PROMPT_SCROLL_W, 56));
            promptScrollPane.setMaximumSize(new Dimension(REVIEW_PROMPT_SCROLL_MAX_W, 72));
        }
        promptScrollPane.setMinimumSize(new Dimension(360, 44));

        answerField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 39));
        answerField.setPreferredSize(new Dimension(640, 77));
        answerField.setMinimumSize(new Dimension(200, 66));
        answerField.setMaximumSize(new Dimension(REVIEW_PROMPT_SCROLL_MAX_W, 93));

        feedbackScrollPane.setPreferredSize(new Dimension(REVIEW_FEEDBACK_SCROLL_W, REVIEW_FEEDBACK_SCROLL_DEFAULT_H));
        feedbackScrollPane.setMinimumSize(new Dimension(360, REVIEW_FEEDBACK_SCROLL_MIN_H));
        feedbackScrollPane.setMaximumSize(new Dimension(REVIEW_FEEDBACK_SCROLL_MAX_W, REVIEW_FEEDBACK_SCROLL_MAX_H));
        feedbackScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        if (scoreCardPanel != null) {
            scoreCardPanel.setPreferredSize(new Dimension(REVIEW_SCORE_CARD_W, 74));
            scoreCardPanel.setMaximumSize(new Dimension(REVIEW_SCORE_CARD_W, 74));
        }
        feedbackArea.setFont(new Font("Arial", Font.PLAIN, 17));
        updatePracticeSourceUiVisibility();
        topCardPanel.revalidate();
        topCardPanel.repaint();
    }

    private void updatePracticeSourceUiVisibility() {
        if (practiceSourceRow == null) {
            return;
        }
        boolean show = isDefinitionMode();
        practiceSourceRow.setVisible(show);
        if (show) {
            refreshPracticeSourceSelector();
        }
    }

    private void refreshPracticeSourceSelector() {
        if (practiceSourceSelector == null) {
            return;
        }
        String previous = (String) practiceSourceSelector.getSelectedItem();
        practiceSourceSelector.removeAllItems();
        practiceSourceSelector.addItem(practiceSourcePlaceholder());
        gameController.getAvailableDatabases().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(practiceSourceSelector::addItem);
        if (previous != null && !practiceSourcePlaceholder().equals(previous)) {
            for (int i = 0; i < practiceSourceSelector.getItemCount(); i++) {
                String item = practiceSourceSelector.getItemAt(i);
                if (item != null && item.equalsIgnoreCase(previous.trim())) {
                    practiceSourceSelector.setSelectedIndex(i);
                    return;
                }
            }
        }
        practiceSourceSelector.setSelectedIndex(0);
    }

    private String selectedPracticeSourceOrNull() {
        if (!isDefinitionMode() || practiceSourceSelector == null) {
            return null;
        }
        String selected = (String) practiceSourceSelector.getSelectedItem();
        if (selected == null || practiceSourcePlaceholder().equals(selected)) {
            return null;
        }
        return selected.trim();
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

        JPanel reviewDbSection = createSectionPanel(ui("Review database", "Base de datos de repaso"));
        JPanel reviewDbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel reviewDbLabel = new JLabel(ui("Database:", "Base de datos:"));
        reviewDbLabel.setFont(new Font("Arial", Font.BOLD, 14));
        reviewDatabaseSelector = new JComboBox<>(gameController.getReviewDatabaseDisplayNames().toArray(new String[0]));
        reviewDatabaseSelector.setPreferredSize(new Dimension(320, 30));
        reviewDatabaseSelector.setToolTipText(ui("Choose the database for this review session",
                "Elige la base de datos para este repaso"));
        reviewDatabaseSelector.setSelectedItem(ReviewDatabases.displayNameForKey(currentReviewDatabaseKey));
        reviewDatabaseSelector.addActionListener(e -> onReviewDatabaseChanged());
        reviewDbPanel.add(reviewDbLabel);
        reviewDbPanel.add(reviewDatabaseSelector);
        reviewDbSection.add(reviewDbPanel);

        reviewStatsSection = createSectionPanel(ui("Learned words — totals", "Learned words — totales"));
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

        JPanel gameSection = createSectionPanel(ui("Interactive Game", "Juego interactivo"));
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

        promptTextArea = new JTextArea(2, 40);
        promptTextArea.setEditable(false);
        promptTextArea.setLineWrap(true);
        promptTextArea.setWrapStyleWord(true);
        promptTextArea.setBackground(new Color(252, 254, 255));
        promptTextArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        promptScrollPane = new JScrollPane(promptTextArea);
        promptScrollPane.setBorder(BorderFactory.createLineBorder(new Color(202, 214, 228), 1));
        promptScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        promptScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        topCard.add(promptScrollPane, gbc);

        answerField = new JTextField();
        answerField.setToolTipText(ui("Enter your English expression here",
                "Escribe aquí la expresión en inglés"));
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

        practiceSourceSelector = new JComboBox<>();
        practiceSourceSelector.setPreferredSize(new Dimension(420, 34));
        practiceSourceSelector.setToolTipText(ui("Choose the source database for this expression",
                "Elige la base de datos de origen de esta expresión"));
        JLabel practiceSourceLabel = new JLabel(ui("Source database:", "Base de origen:"));
        practiceSourceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        practiceSourceRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        practiceSourceRow.setOpaque(false);
        practiceSourceRow.add(practiceSourceLabel);
        practiceSourceRow.add(practiceSourceSelector);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 10, 0);
        topCard.add(practiceSourceRow, gbc);

        feedbackArea = new JTextArea(5, 32);
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        applyFeedbackAreaToneOk();
        feedbackArea.setToolTipText(ui("Feedback from the last check", "Feedback de la última comprobación"));

        feedbackScrollPane = new JScrollPane(feedbackArea);
        feedbackScrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 208, 219), 1));
        feedbackScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        feedbackScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gbc.gridy = 3;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topCard.add(feedbackScrollPane, gbc);

        scoreCardPanel = new JPanel(new BorderLayout());
        JPanel scoreCenterPill = scoreCardPanel;
        scoreCenterPill.setOpaque(true);
        scoreCenterPill.setBackground(new Color(248, 251, 255));
        scoreCenterPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(186, 201, 219), 1),
                BorderFactory.createEmptyBorder(6, 18, 6, 18)));

        scoreLabel = new JLabel(ui("This phrase score", "Puntuación de esta frase"), SwingConstants.CENTER);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setVerticalAlignment(SwingConstants.CENTER);
        scoreLabel.setForeground(new Color(90, 90, 90));
        scoreCenterPill.add(scoreLabel, BorderLayout.CENTER);

        JPanel scoreRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        scoreRow.setOpaque(false);
        scoreRow.add(scoreCenterPill);
        gbc.gridy = 4;
        gbc.insets = new Insets(4, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        topCard.add(scoreRow, gbc);

        JPanel topCardWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        topCardWrap.setOpaque(false);
        topCardWrap.add(topCard);
        gameUpperPanel.add(topCardWrap, BorderLayout.CENTER);

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

        submitButton = createStyledButton("Submit Answer",
                ui("Submit your answer", "Enviar tu respuesta"));
        newRoundButton = createStyledButton("New Round",
                ui("Another card from the Learned Words review", "Otra tarjeta del repaso Learned Words"));
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

        JPanel navSection = createSectionPanel(ui("Navigation", "Navegación"));
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
        feedbackArea.setText(feedbackArea.getText() + "\n\n"
                + ui("Next card in " + secs + " s...", "Siguiente tarjeta en " + secs + " s..."));
        fitFeedbackScrollToContent();
        correctAnswerNextRoundTimer = new Timer(CORRECT_ANSWER_NEXT_ROUND_DELAY_MS, e -> {
            correctAnswerNextRoundTimer = null;
            if (deck.isEmpty()) {
                JOptionPane.showMessageDialog(LearnedWordsReviewView.this,
                        ui("Session finished.", "Sesión terminada."), "Review",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
                return;
            }
            if (incrementIndex) {
                pickNextReviewCardWeighted();
            }
            showCurrentRound();
        });
        correctAnswerNextRoundTimer.setRepeats(false);
        newRoundButton.setEnabled(false);
        correctAnswerNextRoundTimer.start();
    }

    private void refreshScoreLabel(EnglishExpression en) {
        if (en == null) {
            scoreLabel.setText(ui("This phrase score", "Puntuación de esta frase"));
            scoreLabel.setForeground(new Color(90, 90, 90));
            return;
        }
        int phraseScore = en.getScore();
        scoreLabel.setText(buildScoreLabelHtml(
                ui("This phrase score:", "Puntuación de esta frase:"), phraseScore, ""));
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
        fitFeedbackScrollToContent();
        applyFeedbackAreaToneOk();
        answerField.setText("");
        answerField.setEditable(true);
        if (practiceSourceSelector != null) {
            practiceSourceSelector.setEnabled(true);
            practiceSourceSelector.setSelectedIndex(0);
        }
        submitButton.setEnabled(true);
        newRoundButton.setEnabled(!deck.isEmpty());
        if (deck.isEmpty()) {
            promptTextArea.setText(ui("No entries left.", "No quedan entradas."));
            refreshScoreLabel(null);
            submitButton.setEnabled(false);
            newRoundButton.setEnabled(false);
            return;
        }
        index = Math.max(0, Math.min(index, deck.size() - 1));
        EnglishExpression en = deck.get(index);
        String es = summarizeSpanish(esSourceLines(en));
        promptTextArea.setText(es);
        promptTextArea.setCaretPosition(0);
        refreshPracticeSourceSelector();
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
            list.add(isDefinitionMode()
                    ? ui("(No reference definition on this entry)",
                            "(Sin definición de referencia en esta entrada)")
                    : ui("(No Spanish reference on this entry)",
                            "(Sin referencia en español en esta entrada)"));
        }
        return list;
    }

    private static String summarizeSpanish(List<String> lines) {
        if (lines.size() <= 2) {
            return String.join(" · ", lines);
        }
        return lines.get(0) + " · … +" + (lines.size() - 1);
    }

    private void onSubmit() {
        if (deck.isEmpty()) {
            return;
        }
        if (isDefinitionMode() && selectedPracticeSourceOrNull() == null) {
            JOptionPane.showMessageDialog(this,
                    ui("Select the source database before submitting.",
                            "Selecciona la base de datos de origen antes de enviar."),
                    "Review", JOptionPane.WARNING_MESSAGE);
            return;
        }
        EnglishExpression current = deck.get(Math.max(0, Math.min(index, deck.size() - 1)));
        Optional<LearnedWordsReviewResult> res =
                gameController.submitLearnedWordsReviewAnswer(
                        current, answerField.getText(), currentReviewDatabaseKey, selectedPracticeSourceOrNull());

        if (res.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    ui("Could not apply the correction (the entry is no longer in ",
                            "No se ha podido aplicar la corrección (la entrada ya no está en ")
                            + currentReviewDisplayName() + ").",
                    "Review", JOptionPane.WARNING_MESSAGE);
            reloadDeckIfStillAnyOrClose();
            return;
        }

        LearnedWordsReviewResult r = res.get();
        showReviewMilestoneCongratulations(r);
        feedbackArea.setText(buildFeedbackLines(r));
        feedbackArea.setCaretPosition(0);
        fitFeedbackScrollToContent();
        if (isPenaltyFeedback(r)) {
            applyFeedbackAreaTonePenalty();
        } else {
            applyFeedbackAreaToneOk();
        }
        scoreLabel.setText(buildScoreLabelHtml(
                ui("This phrase score:", "Puntuación de esta frase:"), r.scoreAfter(), ""));
        scoreLabel.setForeground(REVIEW_FEEDBACK_OK_FG);

        if (r.outcome() == LearnedWordsReviewResult.Outcome.DEMOTED_TO_PRACTICE
                || r.outcome() == LearnedWordsReviewResult.Outcome.MASTERED_REMOVED_EVERYWHERE
                || r.outcome() == LearnedWordsReviewResult.Outcome.PROMOTED_TO_DEFINITELY_LEARNED
                || r.outcome() == LearnedWordsReviewResult.Outcome.RETURNED_TO_LEARNED) {
            deck.remove(current);
            lastPickedDeckIndex = -1;
            if (!deck.isEmpty()) {
                index = Math.min(index, deck.size() - 1);
            }
        }
        refreshReviewStatsLabels();

        answerField.setEditable(false);
        if (practiceSourceSelector != null) {
            practiceSourceSelector.setEnabled(false);
        }
        submitButton.setEnabled(false);

        if (deck.isEmpty()) {
            cancelPendingAutoAdvanceAfterCorrect();
            JOptionPane.showMessageDialog(this, ui("Session finished.", "Sesión terminada."), "Review",
                    JOptionPane.INFORMATION_MESSAGE);
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
        lastPickedDeckIndex = -1;
        lastShownPracticeSourceKey = null;
        pickNextReviewCardWeighted();
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
                    ui("Congratulations! \"" + word
                                    + "\" reached score 28 in Learned words and is now in Words definitely learned.\nKeep it up!",
                            "¡Enhorabuena! \"" + word
                                    + "\" alcanzó score 28 en Learned words y pasa a Words definitely learned.\n¡Sigue así!"),
                    ui("Word learned", "Palabra aprendida"),
                    JOptionPane.INFORMATION_MESSAGE);
            case MASTERED_REMOVED_EVERYWHERE -> JOptionPane.showMessageDialog(this,
                    ui("Congratulations! \"" + word
                                    + "\" reached the maximum review score (35) and is mastered.\nKeep it up!",
                            "¡Enhorabuena! \"" + word
                                    + "\" alcanzó el score máximo de repaso (35) y queda dominada.\n¡Sigue así!"),
                    ui("Word learned", "Palabra aprendida"),
                    JOptionPane.INFORMATION_MESSAGE);
            default -> { }
        }
    }

    private String buildFeedbackLines(LearnedWordsReviewResult r) {
        List<String> parts = new ArrayList<>();
        if (r.answeredCorrectly()) {
            switch (r.outcome()) {
                case MASTERED_REMOVED_EVERYWHERE -> parts.add(ui(
                        "Mastered (35): the expression is removed from all databases.",
                        "Dominado (35): la expresión sale de todas las bases."));
                case PROMOTED_TO_DEFINITELY_LEARNED ->
                        parts.add(ui(
                                "28 points: moves to Words definitely learned for further review (29–35).",
                                "28 puntos: pasa a Words definitely learned para seguir repaso (29–35)."));
                default -> parts.add(ui("Correct (+1).", "Correcto (+1)."));
            }
            return String.join("\n", parts);
        }

        parts.add(ui("Expected: ", "Esperado: ") + quote(r.expectedEnglish()));
        parts.add(ui("Your answer: ", "Tu respuesta: ") + quote(r.userEntered()));
        if (isDefinitionMode() && r.expectedPracticeSourceDatabase() != null
                && !r.expectedPracticeSourceDatabase().isBlank()) {
            parts.add(ui("Expected source database: ", "Base de origen esperada: ")
                    + quote(r.expectedPracticeSourceDatabase()));
            parts.add(ui("Selected database: ", "Base elegida: ") + quote(
                    Optional.ofNullable(r.userPracticeSourceDatabase()).orElse("—")));
        }
        parts.add("");
        appendWrongAnswerSummary(parts, r);
        return String.join("\n", parts);
    }

    private void appendWrongAnswerSummary(List<String> parts, LearnedWordsReviewResult r) {
        parts.add(ui("Incorrect (−5).", "Incorrecto (−5)."));

        if (r.outcome() == LearnedWordsReviewResult.Outcome.RETURNED_TO_LEARNED) {
            parts.add(ui("Score below " + ReviewDatabases.DEFINITELY_REVIEW_DEMOTION_UNDER_SCORE
                            + ": the expression returns to " + ReviewDatabases.LEARNED_WORDS_DISPLAY + ".",
                    "Score por debajo de " + ReviewDatabases.DEFINITELY_REVIEW_DEMOTION_UNDER_SCORE
                            + ": la expresión vuelve a " + ReviewDatabases.LEARNED_WORDS_DISPLAY + "."));
            return;
        }

        if (r.outcome() == LearnedWordsReviewResult.Outcome.DEMOTED_TO_PRACTICE) {
            appendPracticeReturnMessage(parts, r, true);
            return;
        }

        if (r.outcome() != LearnedWordsReviewResult.Outcome.STILL_IN_LEARNED) {
            return;
        }

        if (isDefinitionMode() && r.expressionMatched()) {
            parts.add(ui("Expression correct, but the source database does not match.",
                    "Expresión correcta, pero la base de origen no coincide."));
        }
        appendPracticeReturnMessage(parts, r, false);
    }

    private void appendPracticeReturnMessage(List<String> parts, LearnedWordsReviewResult r, boolean demoted) {
        if (isDefinitelyReviewDatabase()) {
            int threshold = ReviewDatabases.DEFINITELY_REVIEW_DEMOTION_UNDER_SCORE;
            if (demoted) {
                parts.add(ui("Score below " + threshold + ": the expression returns to "
                                + ReviewDatabases.LEARNED_WORDS_DISPLAY + ".",
                        "Score por debajo de " + threshold + ": la expresión vuelve a "
                                + ReviewDatabases.LEARNED_WORDS_DISPLAY + "."));
            } else {
                int score = r.scoreAfter();
                parts.add(ui("Remains in " + currentReviewDisplayName() + " (score " + score + ")."
                                + " If a future mistake leaves the score below " + threshold + ", it will return to "
                                + ReviewDatabases.LEARNED_WORDS_DISPLAY + ".",
                        "Permanece en " + currentReviewDisplayName() + " (score " + score + ")."
                                + " Si un fallo futuro deja el score por debajo de " + threshold + ", volverá a "
                                + ReviewDatabases.LEARNED_WORDS_DISPLAY + "."));
            }
            return;
        }

        int threshold = ReviewDatabases.REVIEW_DEMOTION_UNDER_SCORE;
        if (demoted) {
            if (isDefinitionMode()) {
                appendImmediateOriginReturnMessage(parts, r);
            } else {
                String db = r.restoredToPracticeDatabase();
                if (db != null && !db.isBlank()) {
                    parts.add(ui("Score below " + ReviewDatabases.REVIEW_DEMOTION_UNDER_SCORE
                                    + ": the expression returns to practice (" + db + ").",
                            "Score por debajo de " + ReviewDatabases.REVIEW_DEMOTION_UNDER_SCORE
                                    + ": la expresión vuelve a práctica (" + db + ")."));
                } else {
                    parts.add(ui("Score below " + ReviewDatabases.REVIEW_DEMOTION_UNDER_SCORE
                                    + ": the expression returns to practice.",
                            "Score por debajo de " + ReviewDatabases.REVIEW_DEMOTION_UNDER_SCORE
                                    + ": la expresión vuelve a práctica."));
                }
            }
            return;
        }

        int score = r.scoreAfter();
        String reviewDb = currentReviewDisplayName();
        String stayLine = ui("Remains in " + reviewDb + " (score " + score + ").",
                "Permanece en " + reviewDb + " (score " + score + ").");
        if (isDefinitionMode()) {
            String originDb = originDbLabelForWarning(r);
            if (originDb == null || originDb.isBlank()) {
                parts.add(stayLine + ui(" If a future mistake leaves the score below " + threshold
                                + ", it will return to practice.",
                        " Si un fallo futuro deja el score por debajo de " + threshold
                                + ", volverá a práctica."));
            } else {
                parts.add(stayLine + ui(" If a future mistake leaves the score below " + threshold
                                + ", it will return to its source database «" + originDb + "».",
                        " Si un fallo futuro deja el score por debajo de " + threshold
                                + ", regresará a su base de origen «" + originDb + "»."));
            }
        } else {
            parts.add(stayLine + ui(" If a future mistake leaves the score below " + threshold
                            + ", it will return to practice.",
                    " Si un fallo futuro deja el score por debajo de " + threshold
                            + ", volverá a práctica."));
        }
    }

    private void appendImmediateOriginReturnMessage(List<String> parts, LearnedWordsReviewResult r) {
        String originDb = originDbLabelForWarning(r);
        int threshold = ReviewDatabases.REVIEW_DEMOTION_UNDER_SCORE;
        if (originDb == null || originDb.isBlank()) {
            parts.add(ui("Score below " + threshold + ": the expression returns to practice.",
                    "Score por debajo de " + threshold + ": la expresión vuelve a práctica."));
            return;
        }
        parts.add(ui("Score below " + threshold + ": the expression returns to its source database «"
                        + originDb + "».",
                "Score por debajo de " + threshold + ": la expresión regresa a su base de origen «"
                        + originDb + "»."));
    }

    private static String originDbLabelForWarning(LearnedWordsReviewResult r) {
        if (r.expectedPracticeSourceDatabase() != null && !r.expectedPracticeSourceDatabase().isBlank()) {
            return r.expectedPracticeSourceDatabase();
        }
        String restored = r.restoredToPracticeDatabase();
        return (restored != null && !restored.isBlank()) ? restored : null;
    }

    private void fitFeedbackScrollToContent() {
        if (feedbackScrollPane == null || feedbackArea == null) {
            return;
        }
        String text = feedbackArea.getText();
        if (text == null || text.isBlank()) {
            applyFeedbackScrollHeight(REVIEW_FEEDBACK_SCROLL_DEFAULT_H);
            return;
        }

        int width = feedbackScrollPane.getWidth();
        if (width <= 12) {
            width = REVIEW_FEEDBACK_SCROLL_W;
        }
        Insets scrollInsets = feedbackScrollPane.getInsets();
        int innerWidth = width - scrollInsets.left - scrollInsets.right - 8;
        innerWidth = Math.max(360, innerWidth);

        feedbackArea.setSize(new Dimension(innerWidth, Short.MAX_VALUE));
        int contentHeight = feedbackArea.getPreferredSize().height;
        int height = contentHeight + scrollInsets.top + scrollInsets.bottom + 4;
        height = Math.max(REVIEW_FEEDBACK_SCROLL_MIN_H, Math.min(REVIEW_FEEDBACK_SCROLL_MAX_H, height));
        applyFeedbackScrollHeight(height);
    }

    private void applyFeedbackScrollHeight(int height) {
        feedbackScrollPane.setPreferredSize(new Dimension(REVIEW_FEEDBACK_SCROLL_W, height));
        feedbackScrollPane.setMaximumSize(new Dimension(REVIEW_FEEDBACK_SCROLL_MAX_W, height));
        feedbackScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        feedbackScrollPane.revalidate();
        if (topCardPanel != null) {
            topCardPanel.revalidate();
        }
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
        pickNextReviewCardWeighted();
        showCurrentRound();
    }
}
