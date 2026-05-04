package com.englishgame.view;

import com.englishgame.controller.GameController;
import com.englishgame.model.AnswerResult;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;

/**
 * Interactive Game Window
 * Main game interface for learning English
 */
@Slf4j
public class GameView extends JFrame {

    private final GameController gameController;
    private final LandingPageView landingPage;

    // Game components
    private JComboBox<String> databaseSelector;
    private JLabel spanishExpressionLabel;
    private JTextField englishTranslationField;
    private JButton submitButton;
    private JButton newRoundButton;
    private JLabel feedbackLabel;
    private JLabel scoreLabel;

    private JCheckBox practiceModeCheckBox;
    private JButton revealAnswerButton;
    private JButton revealAllButton;
    private JTextArea revealAnswerArea;
    private javax.swing.Timer revealCharTimer;
    private String revealFullText = "";
    private int revealCharIndex;
    private boolean revealCommittedThisRound;

    private static final int REVEAL_CHAR_INTERVAL_MS = 380;

    // Navigation components
    private JButton backToLandingButton;
    private JButton dataManagementButton;
    private JButton viewWordsButton;
    private JButton learnedWordsButton;

    // Game state
    private SpanishExpression currentSpanishExpression;

    private static final int CORRECT_ANSWER_NEXT_ROUND_DELAY_MS = 2000;
    private Timer correctAnswerNextRoundTimer;

    public GameView(GameController gameController, LandingPageView landingPage) {
        this.gameController = gameController;
        this.landingPage = landingPage;
        
        setTitle("Interactive Game - English Learning Game");
        setSize(1000, 800);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        
        initComponents();
        setupLayout();
        addListeners();
        refreshDatabaseSelector();
        updatePracticeDependentUi();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelPendingAutoNextRound();
            }
        });
        
        log.info("Game window initialized");
    }

    private void initComponents() {
        // Database selector
        databaseSelector = new JComboBox<>();
        databaseSelector.setPreferredSize(new Dimension(200, 30));
        
        // Game area
        spanishExpressionLabel = new JLabel("Select a database and start a new round!", SwingConstants.CENTER);
        spanishExpressionLabel.setFont(new Font("Arial", Font.BOLD, 24));
        spanishExpressionLabel.setForeground(new Color(0, 102, 204));
        
        englishTranslationField = new JTextField(20);
        englishTranslationField.setFont(new Font("Arial", Font.PLAIN, 16));
        englishTranslationField.setToolTipText("Enter your English translation here");
        
        submitButton = createStyledButton("Submit Answer", "Submit your translation");
        newRoundButton = createStyledButton("New Round", "Get a new Spanish expression");
        
        feedbackLabel = new JLabel("", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        
        scoreLabel = new JLabel("Phrase score (this word): -", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 20));
        scoreLabel.setForeground(new Color(0, 130, 50));
        
        // Navigation buttons
        backToLandingButton = createStyledButton("Back to Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("Manage Data", "Go to data management");
        viewWordsButton = createStyledButton("View Words", "View saved words");
        learnedWordsButton = createStyledButton("Learned Words", "View learned words");

        practiceModeCheckBox = new JCheckBox("Modo práctica (sin puntuación)");
        practiceModeCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        practiceModeCheckBox.setToolTipText(
                "Las comprobaciones no suman ni restan puntos. Permite usar \"Mostrar respuesta\".");

        revealAnswerButton = createStyledButton("Mostrar respuesta",
                "Revela la respuesta escrita gradualmente", false);
        revealAnswerButton.setPreferredSize(new Dimension(200, 36));
        revealAnswerButton.setEnabled(false);
        wireRevealToggleButtonAdaptiveHover();

        revealAllButton = createStyledButton("Mostrar todo", "Muestra la respuesta completa de golpe");
        revealAllButton.setPreferredSize(new Dimension(130, 36));
        revealAllButton.setEnabled(false);

        revealAnswerArea = new JTextArea(3, 40);
        revealAnswerArea.setEditable(false);
        revealAnswerArea.setLineWrap(true);
        revealAnswerArea.setWrapStyleWord(true);
        revealAnswerArea.setFont(new Font("Arial", Font.PLAIN, 16));
        revealAnswerArea.setForeground(new Color(40, 40, 40));
        revealAnswerArea.setBackground(new Color(250, 250, 255));
        revealAnswerArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 200)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        revealAnswerArea.setToolTipText("Aquí aparece la respuesta de referencia cuando usas modo práctica");
    }

    private JButton createStyledButton(String text, String tooltip) {
        return createStyledButton(text, tooltip, true);
    }

    /**
     * @param hoverGlow when false, no custom mouse listener is added (use for buttons where the LAF
     *                  must keep its own {@code MouseListener}s, e.g. reveal toggle with adaptive hover).
     */
    private JButton createStyledButton(String text, String tooltip, boolean hoverGlow) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(150, 40));
        button.setMinimumSize(new Dimension(120, 35));
        button.setMaximumSize(new Dimension(200, 45));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        
        // Assign different colors based on button function
        Color buttonColor = getButtonColor(text);
        button.setBackground(buttonColor);
        
        // Create darker border color
        Color borderColor = new Color(
            Math.max(0, buttonColor.getRed() - 30),
            Math.max(0, buttonColor.getGreen() - 30),
            Math.max(0, buttonColor.getBlue() - 30)
        );
        button.setBorder(BorderFactory.createLineBorder(borderColor, 2));
        
        if (hoverGlow) {
            // Enhanced hover effect with subtle glow
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    Color hoverColor = new Color(
                        Math.min(255, buttonColor.getRed() + 25),
                        Math.min(255, buttonColor.getGreen() + 25),
                        Math.min(255, buttonColor.getBlue() + 25)
                    );
                    button.setBackground(hoverColor);
                    // Create a subtle glow effect with lighter border
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
        }
        
        return button;
    }

    private Color getButtonColor(String buttonText) {
        // Assign colors based on button function
        if (buttonText.contains("Comprobar")) {
            return new Color(56, 189, 248);
        } else if (buttonText.contains("Submit") || buttonText.contains("Answer")) {
            return new Color(16, 185, 129); // Vibrant green for submit actions
        } else if (buttonText.contains("New") || buttonText.contains("Round")) {
            return new Color(59, 130, 246); // Vibrant blue for new actions
        } else if (buttonText.contains("Manage") || buttonText.contains("Data")) {
            return new Color(37, 99, 235); // Vibrant blue for data management
        } else if (buttonText.contains("View") || buttonText.contains("Words")) {
            return new Color(16, 185, 129); // Vibrant green for view actions
        } else if (buttonText.contains("Learned")) {
            return new Color(124, 58, 237); // Vibrant purple for learned words
        } else if (buttonText.contains("Detener")) {
            return new Color(249, 115, 22);
        } else if (buttonText.contains("Mostrar todo")) {
            return new Color(100, 116, 139);
        } else if (buttonText.contains("Mostrar")) {
            return new Color(14, 165, 233);
        } else if (buttonText.contains("Back") || buttonText.contains("Main")) {
            return new Color(220, 38, 127); // Vibrant pink for return actions
        } else {
            return new Color(59, 130, 246); // Default vibrant blue
        }
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));
        
        // Create main panel with soft background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create soft gradient from light cream to very light blue
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(248, 250, 252),  // Very light cream at top
                    0, getHeight(), new Color(240, 248, 255)  // Very light blue at bottom
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Database Selection Section
        JPanel dbSection = createSectionPanel("Database Selection");
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        dbPanel.add(new JLabel("Database:"));
        dbPanel.add(databaseSelector);
        dbSection.add(dbPanel);
        
        // Game Area Section
        JPanel gameSection = createSectionPanel("Interactive Game");
        JPanel gamePanel = new JPanel();
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Spanish expression
        spanishExpressionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gamePanel.add(spanishExpressionLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // English input
        englishTranslationField.setAlignmentX(Component.CENTER_ALIGNMENT);
        englishTranslationField.setMaximumSize(new Dimension(300, 35));
        gamePanel.add(englishTranslationField);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel practiceBanner = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        practiceBanner.setOpaque(false);
        practiceBanner.setAlignmentX(Component.CENTER_ALIGNMENT);
        practiceBanner.add(practiceModeCheckBox);
        gamePanel.add(practiceBanner);

        JPanel revealButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        revealButtons.setOpaque(false);
        revealButtons.setAlignmentX(Component.CENTER_ALIGNMENT);
        revealButtons.add(revealAnswerButton);
        revealButtons.add(revealAllButton);
        gamePanel.add(revealButtons);

        JLabel revealHint = new JLabel("Respuesta de referencia (modo práctica):", SwingConstants.CENTER);
        revealHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        revealHint.setFont(new Font("Arial", Font.PLAIN, 13));
        revealHint.setForeground(new Color(80, 80, 90));
        gamePanel.add(revealHint);

        JScrollPane revealScroll = new JScrollPane(revealAnswerArea);
        revealScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        revealScroll.setPreferredSize(new Dimension(520, 90));
        revealScroll.setMaximumSize(new Dimension(700, 160));
        gamePanel.add(revealScroll);

        gamePanel.add(Box.createRigidArea(new Dimension(0, 12)));
        
        // Game buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(submitButton);
        buttonPanel.add(newRoundButton);
        gamePanel.add(buttonPanel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Feedback
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gamePanel.add(feedbackLabel);
        gamePanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(16, 12, 8, 12)));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(scoreLabel);
        gamePanel.add(statsPanel);
        
        gameSection.add(gamePanel);
        
        // Navigation Section
        JPanel navSection = createSectionPanel("Navigation");
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        navPanel.add(dataManagementButton);
        navPanel.add(viewWordsButton);
        navPanel.add(learnedWordsButton);
        navPanel.add(backToLandingButton);
        navSection.add(navPanel);
        
        // Add all sections to main panel
        mainPanel.add(dbSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(gameSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(navSection);
        
        // Add to frame with scroll
        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(mainScrollPane, BorderLayout.CENTER);
    }

    private JPanel createSectionPanel(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2), title));
        section.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white
        section.setOpaque(true);
        return section;
    }

    private void addListeners() {
        // Database selection
        databaseSelector.addActionListener(e -> syncControllerWithComboSelection(true));
        
        // Game buttons
        newRoundButton.addActionListener(e -> startNewRound());
        submitButton.addActionListener(e -> processAnswer());
        
        // Enter key for answer submission
        englishTranslationField.addActionListener(e -> processAnswer());

        practiceModeCheckBox.addActionListener(e -> {
            if (!practiceModeCheckBox.isSelected()) {
                stopRevealCharTimer();
                if (!revealCommittedThisRound) {
                    revealAnswerArea.setText("");
                    revealFullText = "";
                    revealCharIndex = 0;
                } else if (!revealFullText.isEmpty()) {
                    revealAnswerArea.setText(revealFullText);
                }
            }
            updatePracticeDependentUi();
            refreshCurrentWordScores();
        });
        revealAnswerButton.addActionListener(e -> {
            if (revealCharTimer != null) {
                stopProgressiveRevealByUser();
            } else {
                startProgressiveReveal();
            }
        });
        revealAllButton.addActionListener(e -> revealAllAnswersAtOnce());
        
        // Navigation buttons
        backToLandingButton.addActionListener(e -> returnToLanding());
        dataManagementButton.addActionListener(e -> openDataManagement());
        viewWordsButton.addActionListener(e -> openViewWords());
        learnedWordsButton.addActionListener(e -> openLearnedWords());
    }

    private void refreshDatabaseSelector() {
        databaseSelector.removeAllItems();
        gameController.getAvailableDatabases().forEach(databaseSelector::addItem);
        log.debug("Database selector refreshed with {} databases", databaseSelector.getItemCount());
        /*
         * One shared GameController for all frames: combo selection must sync currentDatabase after repopulating,
         * because ActionListener firing is not guaranteed for every LAF once removeAllItems() ran.
         */
        syncControllerWithComboSelection(false);
    }

    /**
     * Keeps GameController.currentDatabase aligned with this window's combo.
     *
     * @param resetDisplay when true and a DB is selected, clears round UI text (same behavior as manual DB change).
     */
    private void syncControllerWithComboSelection(boolean resetDisplay) {
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb != null && gameController.selectDatabase(selectedDb)) {
            if (resetDisplay) {
                resetGameDisplay();
            }
            log.info("Database selected: {}", selectedDb);
        }
    }

    private void startNewRound() {
        beginNewRoundCore();
    }

    private void beginNewRoundCore() {
        preparePracticeRevealStateForNewRound();
        cancelPendingAutoNextRound();
        syncControllerWithComboSelection(false);
        String selectedDb = (String) databaseSelector.getSelectedItem();
        if (selectedDb == null) {
            JOptionPane.showMessageDialog(this, "Please select a database first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentSpanishExpression = gameController.startNewRound();
        if (currentSpanishExpression != null) {
            spanishExpressionLabel.setText("Translate: \"" + currentSpanishExpression.getExpression() + "\"");
            englishTranslationField.setText("");
            feedbackLabel.setText("");
            englishTranslationField.requestFocus();
            log.info("New round started with Spanish expression: '{}'", currentSpanishExpression.getExpression());
            updatePracticeDependentUi();
            refreshCurrentWordScores();
        } else {
            spanishExpressionLabel.setText("No expressions available. Add some words or select another database.");
            feedbackLabel.setText("Please add expressions to this database or select another one.");
            updatePracticeDependentUi();
            refreshCurrentWordScores();
        }
    }

    private void setRoundInteractionEnabled(boolean enabled) {
        databaseSelector.setEnabled(enabled);
        englishTranslationField.setEnabled(enabled);
        submitButton.setEnabled(enabled);
        newRoundButton.setEnabled(enabled);
        practiceModeCheckBox.setEnabled(enabled);
        if (!enabled) {
            revealAnswerButton.setEnabled(false);
            revealAllButton.setEnabled(false);
        }
    }

    private void cancelPendingAutoNextRound() {
        stopRevealCharTimer();
        if (correctAnswerNextRoundTimer != null) {
            correctAnswerNextRoundTimer.stop();
            correctAnswerNextRoundTimer = null;
        }
        setRoundInteractionEnabled(true);
        updatePracticeDependentUi();
    }

    private void scheduleAutoAdvanceAfterCorrect() {
        cancelPendingAutoNextRound();
        correctAnswerNextRoundTimer = new Timer(CORRECT_ANSWER_NEXT_ROUND_DELAY_MS, e -> {
            correctAnswerNextRoundTimer = null;
            setRoundInteractionEnabled(true);
            beginNewRoundCore();
        });
        correctAnswerNextRoundTimer.setRepeats(false);
        setRoundInteractionEnabled(false);
        updatePracticeDependentUi();
        correctAnswerNextRoundTimer.start();
    }

    private void processAnswer() {
        if (currentSpanishExpression == null) {
            JOptionPane.showMessageDialog(this, "Please start a new round first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String userTranslation = englishTranslationField.getText().trim();
        if (userTranslation.isEmpty()) {
            feedbackLabel.setText("Please enter a translation.");
            feedbackLabel.setForeground(Color.RED);
            return;
        }

        if (isNeutralScoringRound()) {
            Optional<Boolean> probe = gameController.checkAnswerWithoutScoring(userTranslation);
            if (probe.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Empieza una ronda nueva antes de comprobar.",
                        "Sin frase actual",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean correctPractice = probe.get();
            if (correctPractice) {
                feedbackLabel.setText("Correcto — solo comprobación, sin cambiar puntajes.");
                feedbackLabel.setForeground(new Color(0, 130, 60));
            } else {
                feedbackLabel.setText("Incorrecto — sin penalización en esta ronda.");
                feedbackLabel.setForeground(new Color(200, 80, 0));
            }
            refreshCurrentWordScores();
            englishTranslationField.setText("");
            log.info("Practice-only check {} for '{}'",
                    correctPractice ? "PASS" : "FAIL", userTranslation);
            return;
        }
        
        AnswerResult answerResult = gameController.processAnswer(userTranslation);
        boolean isCorrect = answerResult.correct();
        
        if (isCorrect) {
            if (answerResult.isNewlyLearned()) {
                JOptionPane.showMessageDialog(this,
                        "Congratulations! \"" + answerResult.newlyLearnedEnglishWord()
                                + "\" reached the threshold and is now in Learned Words.\nKeep it up!",
                        "Word learned",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            int seconds = Math.max(1, CORRECT_ANSWER_NEXT_ROUND_DELAY_MS / 1000);
            feedbackLabel.setText("Correct! Well done. Next round in " + seconds + " seconds...");
            feedbackLabel.setForeground(new Color(0, 150, 0));
            log.info("Correct answer: '{}' for '{}'", userTranslation, currentSpanishExpression.getExpression());
            scheduleAutoAdvanceAfterCorrect();
        } else {
            feedbackLabel.setText("Incorrect. Try again or start a new round.");
            feedbackLabel.setForeground(Color.RED);
            log.info("Incorrect answer: '{}' for '{}'", userTranslation, currentSpanishExpression.getExpression());
        }
        
        refreshCurrentWordScores();
        englishTranslationField.setText("");
    }

    private void refreshCurrentWordScores() {
        if (currentSpanishExpression == null) {
            scoreLabel.setText("Phrase score (this word): -");
            scoreLabel.setForeground(new Color(90, 90, 90));
            return;
        }

        int phraseScore = currentSpanishExpression.getScore();
        if (isNeutralScoringRound()) {
            scoreLabel.setText("Phrase score (reference): " + phraseScore
                    + " — esta ronda no cuenta para puntaje");
            scoreLabel.setForeground(new Color(105, 105, 115));
        } else {
            scoreLabel.setText("Phrase score (this word): " + phraseScore);
            scoreLabel.setForeground(new Color(0, 130, 50));
        }
    }

    private boolean isNeutralScoringRound() {
        return practiceModeCheckBox.isSelected() || revealCommittedThisRound;
    }

    private void preparePracticeRevealStateForNewRound() {
        stopRevealCharTimer();
        revealCommittedThisRound = false;
        revealFullText = "";
        revealCharIndex = 0;
        revealAnswerArea.setText("");
    }

    private void stopRevealCharTimer() {
        if (revealCharTimer != null) {
            revealCharTimer.stop();
            revealCharTimer = null;
        }
    }

    /**
     * Hover uses {@link #getButtonColor(String)} so it stays correct when the label switches
     * between "Mostrar respuesta" and "Detener revelación".
     * <p>Never remove all {@code MouseListener}s from this button: the LAF installs listeners
     * (e.g. {@code BasicButtonListener}) required for clicks to fire {@code ActionListener}s.
     */
    private void wireRevealToggleButtonAdaptiveHover() {
        revealAnswerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                Color base = getButtonColor(revealAnswerButton.getText());
                Color hover = new Color(
                        Math.min(255, base.getRed() + 25),
                        Math.min(255, base.getGreen() + 25),
                        Math.min(255, base.getBlue() + 25));
                revealAnswerButton.setBackground(hover);
                Color glowBorder = new Color(
                        Math.min(255, base.getRed() + 50),
                        Math.min(255, base.getGreen() + 50),
                        Math.min(255, base.getBlue() + 50));
                revealAnswerButton.setBorder(BorderFactory.createLineBorder(glowBorder, 2));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                applyRevealToggleButtonChrome();
            }
        });
    }

    private void applyRevealToggleButtonChrome() {
        boolean revealing = revealCharTimer != null;
        if (revealing) {
            revealAnswerButton.setText("Detener revelación");
            revealAnswerButton.setToolTipText(
                    "Detiene la animación; puedes seguir intentando (sin puntaje esta ronda).");
        } else {
            revealAnswerButton.setText("Mostrar respuesta");
            revealAnswerButton.setToolTipText("Revela la respuesta de referencia gradualmente.");
        }
        Color base = getButtonColor(revealAnswerButton.getText());
        revealAnswerButton.setBackground(base);
        Color borderColor = new Color(
                Math.max(0, base.getRed() - 30),
                Math.max(0, base.getGreen() - 30),
                Math.max(0, base.getBlue() - 30));
        revealAnswerButton.setBorder(BorderFactory.createLineBorder(borderColor, 2));
    }

    private void stopProgressiveRevealByUser() {
        if (revealCharTimer == null) {
            return;
        }
        stopRevealCharTimer();
        log.info("Progressive reveal suspended by user (practice mode)");
        updatePracticeDependentUi();
        refreshCurrentWordScores();
    }

    private void startProgressiveReveal() {
        if (!practiceModeCheckBox.isSelected()) {
            return;
        }
        Optional<String> line = gameController.getRevealAnswersLine();
        if (line.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Esta tarjeta no tiene traducciones para mostrar.",
                    "Sin respuesta",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        revealCommittedThisRound = true;
        revealFullText = line.get();
        stopRevealCharTimer();
        revealCharIndex = 0;
        revealAnswerArea.setText("");

        revealCharTimer = new javax.swing.Timer(REVEAL_CHAR_INTERVAL_MS, e -> {
            if (revealCharIndex < revealFullText.length()) {
                revealCharIndex++;
                revealAnswerArea.setText(revealFullText.substring(0, revealCharIndex));
            } else {
                stopRevealCharTimer();
                applyRevealToggleButtonChrome();
                updatePracticeDependentUi();
            }
        });
        revealCharTimer.setRepeats(true);
        revealCharTimer.start();
        applyRevealToggleButtonChrome();
        updatePracticeDependentUi();
        refreshCurrentWordScores();
        log.info("Progressive reveal started (practice mode)");
    }

    private void revealAllAnswersAtOnce() {
        if (!practiceModeCheckBox.isSelected()) {
            return;
        }
        Optional<String> line = gameController.getRevealAnswersLine();
        if (line.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Esta tarjeta no tiene traducciones para mostrar.",
                    "Sin respuesta",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        revealCommittedThisRound = true;
        revealFullText = line.get();
        stopRevealCharTimer();
        revealCharIndex = revealFullText.length();
        revealAnswerArea.setText(revealFullText);
        applyRevealToggleButtonChrome();
        updatePracticeDependentUi();
        refreshCurrentWordScores();
        log.info("Full reveal displayed (practice mode)");
    }

    private void updatePracticeDependentUi() {
        boolean hasPhrase = currentSpanishExpression != null;
        boolean practiceOn = practiceModeCheckBox.isSelected();
        boolean hasRevealTarget = gameController.getRevealAnswersLine().isPresent();
        boolean timerRunning = revealCharTimer != null;

        revealAnswerButton.setEnabled(hasPhrase && practiceOn && hasRevealTarget);
        applyRevealToggleButtonChrome();
        revealAllButton.setEnabled(hasPhrase && practiceOn && hasRevealTarget && !timerRunning);

        boolean neutral = isNeutralScoringRound();
        submitButton.setText(neutral ? "Comprobar (sin puntos)" : "Submit Answer");
        submitButton.setBackground(getButtonColor(submitButton.getText()));
        submitButton.setToolTipText(neutral
                ? "Comprueba la traducción sin modificar puntajes."
                : "Submit your translation for scoring.");
    }

    private void resetGameDisplay() {
        cancelPendingAutoNextRound();
        spanishExpressionLabel.setText("Select a database and start a new round!");
        englishTranslationField.setText("");
        feedbackLabel.setText("");
        preparePracticeRevealStateForNewRound();
        currentSpanishExpression = null;
        updatePracticeDependentUi();
        refreshCurrentWordScores();
    }

    private void openDataManagement() {
        cancelPendingAutoNextRound();
        log.info("Opening data management window");
        this.setVisible(false);
        DataManagementView dataManagementView = new DataManagementView(gameController, landingPage);
        dataManagementView.setVisible(true);
    }

    private void openViewWords() {
        cancelPendingAutoNextRound();
        log.info("Opening view words window");
        this.setVisible(false);
        ViewWordsView viewWordsView = new ViewWordsView(gameController, landingPage);
        viewWordsView.setVisible(true);
    }

    private void openLearnedWords() {
        cancelPendingAutoNextRound();
        log.info("Opening learned words window");
        this.setVisible(false);
        LearnedWordsView learnedWordsView = new LearnedWordsView(gameController, landingPage);
        learnedWordsView.setVisible(true);
    }

    private void returnToLanding() {
        cancelPendingAutoNextRound();
        log.info("Returning to landing page");
        this.setVisible(false);
        landingPage.returnToLanding();
    }
}
