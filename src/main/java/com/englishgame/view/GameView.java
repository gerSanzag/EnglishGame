package com.englishgame.view;

import com.englishgame.AppVersion;
import com.englishgame.controller.GameController;
import com.englishgame.model.AnswerResult;
import com.englishgame.model.EnglishExpression;
import com.englishgame.model.SpanishExpression;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
    private JCheckBox noScoreCheckBox;
    private JButton revealAnswerButton;
    private JButton revealAllButton;
    private JTextArea revealAnswerArea;
    private JPanel phrasalBuilderPanel;
    /** Una sola fila: 1) Verbo | 2) Partícula | 3) Partícula/preposición | 4) cuarta pieza. */
    private JPanel phrasalColumnsBand;
    private JPanel phrasalThirdSlotPanel;
    private JPanel phrasalFourthSlotPanel;
    private JTextField phrasalVerbInput;
    private JTextField phrasalParticle1Input;
    private JTextField phrasalParticle2Input;
    private JTextField phrasalParticle3Input;
    private JTextArea phrasalVerbOptionsArea;
    private JTextArea phrasalParticle1OptionsArea;
    private JTextArea phrasalParticle2OptionsArea;
    private JTextArea phrasalParticle3OptionsArea;
    private List<String> currentVerbOptions = new ArrayList<>();
    private List<String> currentParticle1Options = new ArrayList<>();
    private List<String> currentParticle2Options = new ArrayList<>();
    private List<String> currentParticle3Options = new ArrayList<>();
    /** Number of editable token slots after the optional leading \"to \" (2-4 words in the lemma). */
    private int currentPhrasalSlotsNeeded = 2;
    private javax.swing.Timer revealCharTimer;
    private String revealFullText = "";
    private int revealCharIndex;
    private boolean revealCommittedThisRound;
    /** Tras feedback de error phrasal en modo con puntuación, limita acciones hasta New Round o activar práctica. */
    private boolean postIncorrectPhrasalLock;

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

    /** Ancho de reserva si aún no hay viewport medible (phrasal). */
    private static final int PHRASAL_WRAP_FALLBACK_PX = 820;
    /** Opciones phrasal y JTextField deben compartir el mismo ancho (alineación). */
    private static final int PHRASAL_INPUT_COL_MAX_W = 280;
    private static final int PHRASAL_INPUT_COL_MIN_W = 170;
    /** ~+28 % respecto al alto previo para acomodar texto más grande sin recortes. */
    private static final int PHRASAL_INPUT_FIELD_H = 41;
    /** Titulares 1), 2), 3)… (~+25 % frente a 12). */
    private static final int PHRASAL_ROW_TITLE_PX = 15;
    /** Lista \"Opciones: …\" activa (~+31 % frente a 13). */
    private static final int PHRASAL_OPTIONS_ACTIVE_PX = 17;
    /** Mensaje gris cuando la ranura está inactiva (~+25 % frente a 12). */
    private static final int PHRASAL_OPTIONS_INACTIVE_PX = 15;
    /** Lo que escribe el usuario en cada pieza (~+29 % frente a 14). */
    private static final int PHRASAL_INPUT_TEXT_PX = 18;
    /** Separación horizontal entre las cuatro columnas del bloque phrasal. */
    private static final int PHRASAL_COL_HGAP = 10;
    private static final Color PHRASAL_OPTIONS_FG = new Color(52, 56, 64);
    private static final Color PHRASAL_OPTIONS_FG_MUTED = new Color(118, 125, 140);
    private JScrollPane mainGameScrollPane;
    /** Scroll interno de la sección de juego (ancho útil para wrap de opciones phrasal). */
    private JScrollPane gameUpperScrollPane;
    /** Tarjeta central del juego (se estiliza distinto en normal vs phrasal). */
    private JPanel topCardPanel;
    /** Contenedor visual del score central (se escala por modo). */
    private JPanel scoreCardPanel;
    /** Scroll del área de respuesta/referencia (se dimensiona distinto por modo). */
    private JScrollPane revealAnswerScrollPane;

    public GameView(GameController gameController, LandingPageView landingPage) {
        this.gameController = gameController;
        this.landingPage = landingPage;
        
        setTitle("Interactive Game [" + AppVersion.getDisplayVersion() + "]");
        setSize(1080, 920);
        setMinimumSize(new Dimension(960, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
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
        
        installGameWindowLayoutRefreshOnResize();
        log.info("Game window initialized");
    }

    /**
     * Tras maximizar / pantalla completa / \"New Round\", el viewport ya tiene ancho real; las etiquetas
     * HTML de opciones deben recalcularse o quedan más anchas que el área útil y la vista se corre o corta.
     */
    private void installGameWindowLayoutRefreshOnResize() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (isPhrasalRound()) {
                    SwingUtilities.invokeLater(() -> deferRefreshPhrasalOptionsLayout());
                }
            }
        });
    }

    private void initComponents() {
        // Database selector
        databaseSelector = new JComboBox<>();
        databaseSelector.setPreferredSize(new Dimension(200, 30));
        
        // Game area
        spanishExpressionLabel = new JLabel("Select a database and start a new round!", SwingConstants.CENTER);
        spanishExpressionLabel.setFont(new Font("Arial", Font.BOLD, 28));
        spanishExpressionLabel.setForeground(new Color(0, 102, 204));
        
        englishTranslationField = new JTextField(20);
        englishTranslationField.setFont(new Font("Arial", Font.PLAIN, 23));
        englishTranslationField.setToolTipText("Enter your English translation here");
        englishTranslationField.setBackground(new Color(248, 252, 255));
        englishTranslationField.setForeground(new Color(34, 45, 58));
        englishTranslationField.setCaretColor(new Color(34, 45, 58));
        englishTranslationField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 220), 1),
                        BorderFactory.createLineBorder(new Color(176, 197, 220), 2)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        
        submitButton = createStyledButton("Submit Answer", "Submit your translation");
        newRoundButton = createStyledButton("New Round", "Get a new Spanish expression");
        
        feedbackLabel = new JLabel("", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("Arial", Font.ITALIC, 15));
        feedbackLabel.setBorder(BorderFactory.createEmptyBorder(0, 24, 20, 24));
        feedbackLabel.setVisible(false);
        
        scoreLabel = new JLabel("Phrase score (this word)", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        scoreLabel.setForeground(new Color(0, 130, 50));
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setVerticalAlignment(SwingConstants.CENTER);
        
        // Navigation buttons
        backToLandingButton = createStyledButton("Back to Main Menu", "Return to main menu");
        dataManagementButton = createStyledButton("Manage Data", "Go to data management");
        viewWordsButton = createStyledButton("View Words", "View saved words");
        learnedWordsButton = createStyledButton("Learned Words", "View learned words");

        practiceModeCheckBox = new JCheckBox("Modo práctica (sin puntuación)");
        practiceModeCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        practiceModeCheckBox.setToolTipText(
                "Las comprobaciones no suman ni restan puntos. Permite usar \"Mostrar respuesta\".");
        styleSelectionToggle(practiceModeCheckBox, new Color(232, 248, 240), new Color(46, 156, 112));

        noScoreCheckBox = new JCheckBox("Comprobar sin puntuación (juego real)");
        noScoreCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        noScoreCheckBox.setToolTipText(
                "Comprueba tu respuesta sin recompensa ni penalización, sin entrar en modo práctica.");
        styleSelectionToggle(noScoreCheckBox, new Color(234, 241, 253), new Color(62, 110, 202));

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

        phrasalVerbInput = new PhrasalTokenField();
        phrasalParticle1Input = new PhrasalTokenField();
        phrasalParticle2Input = new PhrasalTokenField();
        phrasalParticle3Input = new PhrasalTokenField();
        stylePhrasalInput(phrasalVerbInput, "Escribe el verbo");
        stylePhrasalInput(phrasalParticle1Input, "Primera partícula o palabra siguiente al verbo");
        stylePhrasalInput(phrasalParticle2Input, "Segunda partícula o preposición (p. ej. of, with)");
        stylePhrasalInput(phrasalParticle3Input,
                "Cuarta parte del phrasal (solo si la respuesta lleva más de tres palabras sin contar \"to\")");

        phrasalVerbOptionsArea = buildPhrasalOptionsTextArea();
        phrasalParticle1OptionsArea = buildPhrasalOptionsTextArea();
        phrasalParticle2OptionsArea = buildPhrasalOptionsTextArea();
        phrasalParticle3OptionsArea = buildPhrasalOptionsTextArea();

        phrasalBuilderPanel = new JPanel();
        phrasalBuilderPanel.setLayout(new BoxLayout(phrasalBuilderPanel, BoxLayout.Y_AXIS));
        phrasalBuilderPanel.setOpaque(false);
        // Evita centrar un ancho mal calculado y que la fila phrasal “se vaya” a la derecha.
        phrasalBuilderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        phrasalColumnsBand = new JPanel(new GridLayout(1, 4, PHRASAL_COL_HGAP, 0));
        phrasalColumnsBand.setOpaque(false);
        phrasalColumnsBand.add(buildPhrasalInputRow("1) Verbo", phrasalVerbOptionsArea, phrasalVerbInput));
        phrasalColumnsBand.add(buildPhrasalInputRow("2) Partícula", phrasalParticle1OptionsArea, phrasalParticle1Input));
        phrasalThirdSlotPanel = buildPhrasalInputRow(
                "3) Partícula / preposición", phrasalParticle2OptionsArea, phrasalParticle2Input);
        phrasalColumnsBand.add(phrasalThirdSlotPanel);
        phrasalFourthSlotPanel = buildPhrasalInputRow(
                "4) Siguiente (si aplica)", phrasalParticle3OptionsArea, phrasalParticle3Input);
        phrasalColumnsBand.add(phrasalFourthSlotPanel);
        phrasalBuilderPanel.add(phrasalColumnsBand);
        phrasalBuilderPanel.setVisible(false);
    }

    private JTextArea buildPhrasalOptionsTextArea() {
        JTextArea ta = new JTextArea(3, 32);
        ta.setEditable(false);
        ta.setFocusable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setOpaque(true);
        ta.setFont(new Font("Arial", Font.BOLD, PHRASAL_OPTIONS_ACTIVE_PX));
        ta.setForeground(PHRASAL_OPTIONS_FG);
        ta.setBackground(new Color(246, 248, 252));
        ta.setMargin(new Insets(5, 6, 5, 6));
        ta.setAlignmentY(Component.TOP_ALIGNMENT);
        ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(198, 204, 218), 1),
                BorderFactory.createEmptyBorder(1, 2, 2, 2)));
        ta.setTabSize(4);
        ta.setText("Opciones: -");
        ta.setCaretPosition(0);
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        return ta;
    }

    private void applyPhrasalOptionsAreaTone(JTextArea area, boolean activeList) {
        if (area == null) {
            return;
        }
        if (activeList) {
            area.setFont(new Font("Arial", Font.BOLD, PHRASAL_OPTIONS_ACTIVE_PX));
            area.setForeground(PHRASAL_OPTIONS_FG);
        } else {
            area.setFont(new Font("Arial", Font.ITALIC, PHRASAL_OPTIONS_INACTIVE_PX));
            area.setForeground(PHRASAL_OPTIONS_FG_MUTED);
        }
    }

    /** Ancho de cada una de las cuatro columnas: (ancho útil del contenido − huecos) / 4. */
    private int phrasalSlotColumnWidthPx() {
        int inner = phrasalBandInnerWidthPx();
        int gaps = PHRASAL_COL_HGAP * 3;
        return Math.max(48, (inner - gaps) / 4);
    }

    /**
     * Ancho interior real para la banda phrasal: acota con la tarjeta visible y con el viewport
     * para que la fila de 4 columnas no sobrepase el contenedor ni se centre mal.
     */
    private int phrasalBandInnerWidthPx() {
        int fromTopCard = -1;
        if (topCardPanel != null && topCardPanel.getWidth() > 40) {
            Insets in = topCardPanel.getInsets();
            fromTopCard = topCardPanel.getWidth() - in.left - in.right;
        }
        int fromViewport = -1;
        if (gameUpperScrollPane != null) {
            int iw = gameUpperScrollPane.getViewport().getWidth();
            if (iw > 0) {
                fromViewport = Math.max(120, iw - 52);
            }
        }
        if (fromViewport < 0 && mainGameScrollPane != null) {
            int vw = mainGameScrollPane.getViewport().getWidth();
            if (vw > 0) {
                fromViewport = Math.max(120, vw - 80);
            }
        }
        int inner;
        if (fromTopCard >= 0 && fromViewport >= 0) {
            inner = Math.min(fromTopCard, fromViewport);
        } else if (fromTopCard >= 0) {
            inner = fromTopCard;
        } else if (fromViewport >= 0) {
            inner = fromViewport;
        } else if (getWidth() > 0) {
            inner = Math.max(160, getWidth() - 160);
        } else {
            inner = Math.min(PHRASAL_WRAP_FALLBACK_PX, 720);
        }
        return Math.max(160, inner);
    }

    private void applyPhrasalOptionPlainText(JTextArea area, String plain) {
        area.setText(plain == null ? "" : plain);
        area.setCaretPosition(0);
    }

    private void layoutOnePhrasalOptionArea(JTextArea area) {
        if (area == null) {
            return;
        }
        int w = phrasalSlotColumnWidthPx();
        area.setSize(new Dimension(w, 50_000));
        Dimension pref = area.getPreferredSize();
        int h = Math.max(24, pref.height);
        area.setPreferredSize(new Dimension(w, h));
        area.setMaximumSize(new Dimension(w, h + 14));
        area.setMinimumSize(new Dimension(Math.min(w, 100), Math.min(h, 16)));
    }

    private void layoutPhrasalOptionsTextAreas() {
        if (!isPhrasalRound()) {
            return;
        }
        layoutOnePhrasalOptionArea(phrasalVerbOptionsArea);
        layoutOnePhrasalOptionArea(phrasalParticle1OptionsArea);
        layoutOnePhrasalOptionArea(phrasalParticle2OptionsArea);
        layoutOnePhrasalOptionArea(phrasalParticle3OptionsArea);
        normalizePhrasalOptionHeights();
    }

    /** Fuerza misma altura visual en los 4 cuadros de opciones para no romper alineación. */
    private void normalizePhrasalOptionHeights() {
        JTextArea[] areas = {
                phrasalVerbOptionsArea,
                phrasalParticle1OptionsArea,
                phrasalParticle2OptionsArea,
                phrasalParticle3OptionsArea
        };
        int maxH = 0;
        for (JTextArea area : areas) {
            if (area != null) {
                maxH = Math.max(maxH, area.getPreferredSize().height);
            }
        }
        if (maxH <= 0) {
            return;
        }
        for (JTextArea area : areas) {
            if (area != null) {
                int w = area.getPreferredSize().width;
                area.setPreferredSize(new Dimension(w, maxH));
                area.setMaximumSize(new Dimension(w, maxH + 14));
                area.setMinimumSize(new Dimension(Math.min(w, 100), Math.min(maxH, 16)));
            }
        }
    }

    private void syncPhrasalInputsToColumnWidth() {
        int col = phrasalSlotColumnWidthPx();
        Dimension fld = new Dimension(col, PHRASAL_INPUT_FIELD_H);
        Dimension minFld = new Dimension(col, PHRASAL_INPUT_FIELD_H - 2);
        applyPhrasalFieldWidth(phrasalVerbInput, fld, minFld);
        applyPhrasalFieldWidth(phrasalParticle1Input, fld, minFld);
        applyPhrasalFieldWidth(phrasalParticle2Input, fld, minFld);
        applyPhrasalFieldWidth(phrasalParticle3Input, fld, minFld);
    }

    private static void applyPhrasalFieldWidth(JTextField f, Dimension prefMinMax, Dimension minSz) {
        if (f == null) {
            return;
        }
        f.setPreferredSize(prefMinMax);
        f.setMinimumSize(minSz);
        f.setMaximumSize(prefMinMax);
    }

    private void constrainPhrasalRowPanelsToColumnWidth(int colW) {
        if (phrasalColumnsBand != null) {
            int inner = phrasalBandInnerWidthPx();
            int maxH = 0;
            for (Component partCol : phrasalColumnsBand.getComponents()) {
                if (partCol != null) {
                    Dimension ps = partCol.getPreferredSize();
                    int colH = ps.height;
                    partCol.setPreferredSize(new Dimension(colW, colH));
                    partCol.setMaximumSize(new Dimension(colW, Integer.MAX_VALUE));
                    maxH = Math.max(maxH, colH);
                }
            }
            phrasalColumnsBand.setPreferredSize(new Dimension(inner, maxH));
            phrasalColumnsBand.setMaximumSize(new Dimension(inner, Integer.MAX_VALUE));
            phrasalColumnsBand.setMinimumSize(new Dimension(inner, 0));
        }
    }

    private void deferRefreshPhrasalOptionsLayout() {
        if (!isPhrasalRound()) {
            return;
        }
        reapplyPhrasalOptionsText();
        int col = phrasalSlotColumnWidthPx();
        syncPhrasalInputsToColumnWidth();
        layoutPhrasalOptionsTextAreas();
        constrainPhrasalRowPanelsToColumnWidth(col);
        int inner = phrasalBandInnerWidthPx();
        phrasalBuilderPanel.setMaximumSize(new Dimension(inner, Integer.MAX_VALUE));
        phrasalBuilderPanel.setMinimumSize(new Dimension(inner, 0));
        phrasalBuilderPanel.revalidate();
        revalidate();
        repaint();
    }

    private void reapplyPhrasalOptionsText() {
        if (!isPhrasalRound()) {
            return;
        }
        applyPhrasalOptionPlainText(phrasalVerbOptionsArea, "Opciones: " + joinOptions(currentVerbOptions));
        applyPhrasalOptionsAreaTone(phrasalVerbOptionsArea, true);

        applyPhrasalOptionPlainText(phrasalParticle1OptionsArea,
                "Opciones: " + joinOptions(currentParticle1Options));
        applyPhrasalOptionsAreaTone(phrasalParticle1OptionsArea, true);

        boolean thirdSlotActive = currentPhrasalSlotsNeeded >= 3;
        applyPhrasalOptionPlainText(phrasalParticle2OptionsArea,
                thirdSlotActive
                        ? "Opciones: " + joinOptions(currentParticle2Options)
                        : "Opciones: inactivas — Esta tarjeta solo usa dos piezas; rellena solo 1) y 2).");
        applyPhrasalOptionsAreaTone(phrasalParticle2OptionsArea, thirdSlotActive);
        phrasalParticle2Input.setToolTipText(thirdSlotActive
                ? "Segunda partícula o preposición (p. ej. of, with)"
                : "Inactivo en esta tarjeta: no hace falta tercera pieza.");

        boolean fourthSlotActive = currentPhrasalSlotsNeeded >= 4;
        if (fourthSlotActive) {
            applyPhrasalOptionPlainText(phrasalParticle3OptionsArea,
                    "Opciones: " + joinOptions(currentParticle3Options));
            applyPhrasalOptionsAreaTone(phrasalParticle3OptionsArea, true);
            phrasalParticle3Input.setToolTipText(
                    "Cuarta pieza del phrasal (solo si la respuesta lleva más de tres palabras sin contar \"to\")");
        } else {
            String fourthInactiveMsg;
            if (currentPhrasalSlotsNeeded <= 2) {
                fourthInactiveMsg = "Opciones: inactivas — Esta tarjeta solo usa dos piezas; rellena solo 1) y 2).";
            } else {
                fourthInactiveMsg = "Opciones: inactivas — Esta tarjeta no usa cuarta pieza (basta con 1), 2) y 3).";
            }
            applyPhrasalOptionPlainText(phrasalParticle3OptionsArea, fourthInactiveMsg);
            applyPhrasalOptionsAreaTone(phrasalParticle3OptionsArea, false);
            phrasalParticle3Input.setToolTipText("Inactivo en esta tarjeta: no hace falta cuarta pieza.");
        }
    }

    private void installPhrasalOptionsViewportWrapListener(JScrollPane mainScrollPane) {
        mainScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!isDisplayable()) {
                    return;
                }
                int viewportW = mainScrollPane.getViewport().getWidth();
                if (viewportW <= 0) {
                    return;
                }
                SwingUtilities.invokeLater(() -> deferRefreshPhrasalOptionsLayout());
            }
        });
    }

    private JPanel buildPhrasalInputRow(String title, JTextArea optionsArea, JTextField inputField) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setAlignmentY(Component.TOP_ALIGNMENT);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, PHRASAL_ROW_TITLE_PX));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setAlignmentY(Component.TOP_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        optionsArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsArea.setAlignmentY(Component.TOP_ALIGNMENT);
        inputField.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputField.setAlignmentY(Component.TOP_ALIGNMENT);
        row.add(titleLabel);
        row.add(optionsArea);
        row.add(Box.createVerticalStrut(1));
        row.add(inputField);
        row.add(Box.createVerticalGlue());
        return row;
    }

    private void stylePhrasalInput(JTextField input, String tooltip) {
        input.setFont(new Font("Arial", Font.PLAIN, PHRASAL_INPUT_TEXT_PX));
        input.setAlignmentY(Component.TOP_ALIGNMENT);
        input.setToolTipText(tooltip);
        Dimension d = new Dimension(PHRASAL_INPUT_COL_MAX_W, PHRASAL_INPUT_FIELD_H);
        input.setPreferredSize(d);
        input.setMaximumSize(d);
        input.setMinimumSize(new Dimension(PHRASAL_INPUT_COL_MIN_W, PHRASAL_INPUT_FIELD_H - 2));
    }

    private void styleSelectionToggle(JCheckBox toggle, Color bg, Color accent) {
        toggle.setOpaque(false);
        toggle.setForeground(new Color(35, 42, 52));
        toggle.setFont(new Font("Arial", Font.BOLD, 15));
        toggle.setFocusPainted(false);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        toggle.setMargin(new Insets(2, 2, 2, 2));
        toggle.setIconTextGap(10);
        toggle.setPreferredSize(new Dimension(360, 34));
        toggle.setMinimumSize(new Dimension(340, 34));
        toggle.setMaximumSize(new Dimension(390, 36));

        Icon off = buildCheckIcon(20, new Color(255, 255, 255), accent, null, true);
        Icon on = buildCheckIcon(20, bg, accent, accent, true);
        Icon offDisabled = buildCheckIcon(20, new Color(245, 245, 245), new Color(180, 184, 190), null, false);
        Icon onDisabled = buildCheckIcon(20, new Color(236, 239, 244), new Color(180, 184, 190), new Color(146, 152, 160), false);
        toggle.setIcon(off);
        toggle.setSelectedIcon(on);
        toggle.setDisabledIcon(offDisabled);
        toggle.setDisabledSelectedIcon(onDisabled);
    }

    private static Icon buildCheckIcon(int size, Color fill, Color border, Color check, boolean enabled) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillRoundRect(x, y, size, size, 6, 6);
                g2.setColor(border);
                g2.drawRoundRect(x, y, size - 1, size - 1, 6, 6);

                if (check != null) {
                    g2.setColor(check);
                    g2.setStroke(new BasicStroke(enabled ? 2.7f : 2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int x1 = x + (int) (size * 0.23);
                    int y1 = y + (int) (size * 0.54);
                    int x2 = x + (int) (size * 0.45);
                    int y2 = y + (int) (size * 0.76);
                    int x3 = x + (int) (size * 0.79);
                    int y3 = y + (int) (size * 0.28);
                    g2.drawLine(x1, y1, x2, y2);
                    g2.drawLine(x2, y2, x3, y3);
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    private JPanel buildSelectionToggleCard(JCheckBox toggle, Color bg, Color accent) {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        card.setOpaque(true);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 220), 1),
                        BorderFactory.createLineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190), 2)),
                BorderFactory.createEmptyBorder(2, 4, 2, 6)));
        card.setPreferredSize(new Dimension(380, 44));
        card.setMinimumSize(new Dimension(360, 44));
        card.setMaximumSize(new Dimension(410, 46));
        card.add(toggle);
        return card;
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
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JLabel dbLabel = new JLabel("Database:");
        dbLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dbPanel.add(dbLabel);
        dbPanel.add(databaseSelector);
        dbSection.add(dbPanel);
        JLabel buildHint = new JLabel("Build " + AppVersion.getDisplayVersion()
                + " — Phrasal: 3) siempre visible; 4) solo si la tarjeta tiene cuarta pieza.");
        buildHint.setFont(new Font("Arial", Font.PLAIN, 12));
        buildHint.setForeground(new Color(95, 100, 115));
        JPanel buildHintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        buildHintRow.setOpaque(false);
        buildHintRow.add(buildHint);
        dbSection.add(buildHintRow);

        // Game Area Section: scroll interno para el contenido alto; acciones y navegación fuera del scroll central.
        JPanel gameSection = createSectionPanel("Interactive Game");
        gameSection.setLayout(new BorderLayout());
        JPanel gameUpperPanel = new JPanel(new BorderLayout());
        gameUpperPanel.setOpaque(false);
        gameUpperPanel.setBorder(BorderFactory.createEmptyBorder(24, 12, 14, 12));

        JPanel topCard = new JPanel(new GridBagLayout());
        topCardPanel = topCard;
        topCard.setOpaque(true);
        topCard.setBackground(new Color(252, 254, 255));
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(202, 214, 228), 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        // En phrasal el contenido puede crecer (filas 1..4 + opciones + feedback).
        // No fijamos altura rígida para evitar recortes.

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        spanishExpressionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        spanishExpressionLabel.setForeground(new Color(27, 84, 138));
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 10, 0);
        topCard.add(spanishExpressionLabel, gbc);

        englishTranslationField.setPreferredSize(new Dimension(620, 44));
        englishTranslationField.setMinimumSize(new Dimension(500, 44));
        englishTranslationField.setMaximumSize(new Dimension(760, 44));
        JPanel answerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        answerRow.setOpaque(false);
        answerRow.add(englishTranslationField);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        topCard.add(answerRow, gbc);

        phrasalBuilderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 8, 0);
        topCard.add(phrasalBuilderPanel, gbc);

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        modeRow.setOpaque(false);
        modeRow.add(buildSelectionToggleCard(practiceModeCheckBox, new Color(232, 248, 240), new Color(46, 156, 112)));
        modeRow.add(buildSelectionToggleCard(noScoreCheckBox, new Color(234, 241, 253), new Color(62, 110, 202)));
        gbc.gridy = 3;
        gbc.insets = new Insets(18, 0, 8, 0);
        topCard.add(modeRow, gbc);

        JPanel revealButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        revealButtons.setOpaque(false);
        revealButtons.add(revealAnswerButton);
        revealButtons.add(revealAllButton);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 8, 0);
        topCard.add(revealButtons, gbc);

        JLabel revealHint = new JLabel(" ", SwingConstants.CENTER);
        revealHint.setFont(new Font("Arial", Font.BOLD, 13));
        revealHint.setForeground(new Color(66, 74, 84));
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 4, 0);
        topCard.add(revealHint, gbc);

        JPanel feedbackInlineRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        feedbackInlineRow.setOpaque(false);
        feedbackLabel.setBorder(BorderFactory.createEmptyBorder(0, 24, 6, 24));
        feedbackInlineRow.add(feedbackLabel);
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 2, 0);
        topCard.add(feedbackInlineRow, gbc);

        JScrollPane revealScroll = new JScrollPane(revealAnswerArea);
        revealAnswerScrollPane = revealScroll;
        revealScroll.setPreferredSize(new Dimension(860, 112));
        revealScroll.setMaximumSize(new Dimension(920, 150));
        revealScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 208, 219), 1));
        JPanel revealRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        revealRow.setOpaque(false);
        revealRow.add(revealScroll);
        gbc.gridy = 7;
        gbc.insets = new Insets(14, 0, 0, 0);
        topCard.add(revealRow, gbc);

        // No forzar preferred size: Swing calcula altura según contenido visible.

        JPanel topCardWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        topCardWrap.setOpaque(false);
        topCardWrap.add(topCard);
        gameUpperPanel.add(topCardWrap, BorderLayout.NORTH);
        applyGameLayoutModel(false);

        JPanel centerScoreWrap = new JPanel();
        centerScoreWrap.setLayout(new BoxLayout(centerScoreWrap, BoxLayout.Y_AXIS));
        centerScoreWrap.setOpaque(false);
        centerScoreWrap.setBorder(BorderFactory.createEmptyBorder(28, 0, 12, 0));

        JPanel scoreCenterPill = new JPanel(new BorderLayout());
        scoreCardPanel = scoreCenterPill;
        scoreCenterPill.setOpaque(true);
        scoreCenterPill.setBackground(new Color(248, 251, 255));
        scoreCenterPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(186, 201, 219), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        scoreCenterPill.setMaximumSize(new Dimension(640, 92));
        scoreCenterPill.setPreferredSize(new Dimension(640, 92));
        scoreCenterPill.add(scoreLabel, BorderLayout.CENTER);
        centerScoreWrap.add(scoreCenterPill);

        gameUpperPanel.add(centerScoreWrap, BorderLayout.CENTER);

        JScrollPane gameUpperScroll = new JScrollPane(gameUpperPanel);
        gameUpperScroll.setBorder(BorderFactory.createEmptyBorder());
        gameUpperScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        gameUpperScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gameUpperScroll.getVerticalScrollBar().setUnitIncrement(24);
        gameUpperScrollPane = gameUpperScroll;
        installPhrasalOptionsViewportWrapListener(gameUpperScroll);
        gameSection.add(gameUpperScroll, BorderLayout.CENTER);

        JPanel navSection = createSectionPanel("Navigation");
        // Sin TitledBorder para evitar la línea/blanqueo superior del título.
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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 8));
        buttonPanel.setOpaque(false);
        submitButton.setPreferredSize(new Dimension(180, 48));
        newRoundButton.setPreferredSize(new Dimension(180, 48));
        buttonPanel.add(submitButton);
        buttonPanel.add(newRoundButton);

        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

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

        mainPanel.add(dbSection);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(gameSection);

        JScrollPane mainScrollPane = new JScrollPane(mainPanel);
        mainScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(24);
        mainGameScrollPane = mainScrollPane;
        installPhrasalOptionsViewportWrapListener(mainScrollPane);
        add(mainScrollPane, BorderLayout.CENTER);
        add(stickySouth, BorderLayout.SOUTH);
    }

    private JPanel createSectionPanel(String title) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.CENTER_ALIGNMENT);
        section.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2), title));
        section.setBackground(new Color(255, 255, 255, 200)); // Semi-transparent white
        section.setOpaque(true);
        return section;
    }

    /** Enter en el campo (y teclado numérico) envía la respuesta; en macOS a veces falla solo el ActionListener. */
    private void bindEnterSubmitsAnswer(JTextField field) {
        Action submit = new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processAnswer();
            }
        };
        InputMap im = field.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = field.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "englishGameSubmitAnswer");
        am.put("englishGameSubmitAnswer", submit);
    }

    private void addListeners() {
        // Database selection
        databaseSelector.addActionListener(e -> syncControllerWithComboSelection(true));
        
        // Game buttons
        newRoundButton.addActionListener(e -> startNewRound());
        submitButton.addActionListener(e -> processAnswer());
        
        bindEnterSubmitsAnswer(englishTranslationField);
        bindEnterSubmitsAnswer(phrasalVerbInput);
        bindEnterSubmitsAnswer(phrasalParticle1Input);
        bindEnterSubmitsAnswer(phrasalParticle2Input);
        bindEnterSubmitsAnswer(phrasalParticle3Input);
        SwingUtilities.invokeLater(() -> getRootPane().setDefaultButton(submitButton));

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
        noScoreCheckBox.addActionListener(e -> {
            if (!noScoreCheckBox.isSelected() && !practiceModeCheckBox.isSelected()
                    && currentSpanishExpression != null) {
                log.info("No-score check disabled by user; starting a new round automatically");
                beginNewRoundCore();
                return;
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
        attachPhrasalStepListener(phrasalVerbInput, this::updatePhrasalInputStepState);
        attachPhrasalStepListener(phrasalParticle1Input, this::updatePhrasalInputStepState);
        attachPhrasalStepListener(phrasalParticle2Input, this::updatePhrasalInputStepState);
        attachPhrasalStepListener(phrasalParticle3Input, this::updatePhrasalInputStepState);
        
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
        postIncorrectPhrasalLock = false;
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
            configurePhrasalRoundUi();
            showFeedbackInRevealArea("", new Color(40, 40, 40));
            requestFocusForCurrentRound();
            log.info("New round started with Spanish expression: '{}'", currentSpanishExpression.getExpression());
            updatePracticeDependentUi();
            refreshCurrentWordScores();
        } else {
            spanishExpressionLabel.setText("No expressions available. Add some words or select another database.");
            showFeedbackInRevealArea("Please add expressions to this database or select another one.", Color.RED);
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
        noScoreCheckBox.setEnabled(enabled && !practiceModeCheckBox.isSelected());
        if (!enabled) {
            revealAnswerButton.setEnabled(false);
            revealAllButton.setEnabled(false);
        }
    }

    private void requestFocusForCurrentRound() {
        SwingUtilities.invokeLater(() -> {
            if (isPhrasalRound()) {
                phrasalVerbInput.requestFocusInWindow();
            } else {
                englishTranslationField.requestFocusInWindow();
            }
        });
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
        
        String userTranslation = canonicalUserTranslationFromField();
        if (userTranslation.isEmpty()) {
            showFeedbackInRevealArea("Please enter a translation.", Color.RED);
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
                showFeedbackInRevealArea("Correcto — solo comprobación, sin cambiar puntajes.", new Color(0, 130, 60));
            } else {
                if (isPhrasalRound()) {
                    showPhrasalPedagogicFeedback(userTranslation, true);
                } else {
                    String detail = buildExpectedVsTypedDetail(userTranslation);
                    showFeedbackInRevealArea(
                            "Incorrecto — sin penalización en esta ronda.\n" + detail,
                            new Color(200, 80, 0));
                }
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
            showFeedbackInRevealArea("Correct! Well done. Next round in " + seconds + " seconds...",
                    new Color(0, 150, 0));
            log.info("Correct answer: '{}' for '{}'", userTranslation, currentSpanishExpression.getExpression());
            scheduleAutoAdvanceAfterCorrect();
        } else {
            if (isPhrasalRound()) {
                showPhrasalPedagogicFeedback(userTranslation, false);
                activatePostIncorrectPhrasalLock();
            } else {
                String detail = buildExpectedVsTypedDetail(userTranslation);
                showFeedbackInRevealArea("Incorrect. Try again or start a new round.\n" + detail, Color.RED);
            }
            log.info("Incorrect answer: '{}' for '{}'", userTranslation, currentSpanishExpression.getExpression());
        }
        
        refreshCurrentWordScores();
        englishTranslationField.setText("");
    }

    private void showPhrasalPedagogicFeedback(String userTranslation, boolean noScoreMode) {
        List<String> userTokens = tokensAfterOptionalTo(userTranslation);
        List<String> expectedTokens = bestReferencePhrasalTokens(userTokens);

        String modePrefix = noScoreMode
                ? "Incorrecto (sin penalización)"
                : "Incorrecto";
        StringBuilder msg = new StringBuilder();
        msg.append(modePrefix).append("\n");
        msg.append(slotDiagnosisPlain(userTokens, expectedTokens)).append("\n");
        msg.append("Correcto: ").append(String.join(" ", expectedTokens));
        showFeedbackInRevealArea(msg.toString(), new Color(188, 48, 48));
    }

    private List<String> bestReferencePhrasalTokens(List<String> userTokens) {
        if (currentSpanishExpression == null) {
            return Collections.emptyList();
        }
        List<List<String>> candidates = new ArrayList<>();
        for (EnglishExpression en : cohortEnglishTranslationsOrCurrent()) {
            List<String> tokens = tokensAfterOptionalTo(en.getExpression());
            if (!tokens.isEmpty()) {
                candidates.add(tokens);
            }
        }
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        if (userTokens == null || userTokens.isEmpty()) {
            return candidates.get(0);
        }
        List<String> best = candidates.get(0);
        int bestPenalty = slotDistancePenalty(userTokens, best);
        for (int i = 1; i < candidates.size(); i++) {
            List<String> cand = candidates.get(i);
            int penalty = slotDistancePenalty(userTokens, cand);
            if (penalty < bestPenalty) {
                bestPenalty = penalty;
                best = cand;
            }
        }
        return best;
    }

    private int slotDistancePenalty(List<String> user, List<String> expected) {
        int max = Math.max(user.size(), expected.size());
        int penalty = 0;
        for (int i = 0; i < max; i++) {
            String u = i < user.size() ? user.get(i) : null;
            String e = i < expected.size() ? expected.get(i) : null;
            if (u == null || e == null) {
                penalty += 2;
            } else if (!u.equalsIgnoreCase(e)) {
                penalty += 1;
            }
        }
        return penalty;
    }

    private String slotDiagnosisPlain(List<String> userTokens, List<String> expectedTokens) {
        int slots = Math.max(2, Math.min(4, expectedTokens.size()));
        String[] slotNames = { "Verbo", "Partícula 1", "Partícula 2", "Partícula 3" };
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < slots; i++) {
            String expected = i < expectedTokens.size() ? expectedTokens.get(i) : "—";
            String typed = i < userTokens.size() ? userTokens.get(i) : "—";
            boolean ok = i < expectedTokens.size()
                    && i < userTokens.size()
                    && expected.equalsIgnoreCase(typed);
            out.append(slotNames[i]).append(": ");
            if (ok) {
                out.append("OK");
            } else if (i >= userTokens.size()) {
                out.append("Falta (esperado: ").append(expected).append(")");
            } else if (i >= expectedTokens.size()) {
                out.append("No aplica");
            } else {
                out.append("esperado ").append(expected).append(", escribiste ").append(typed);
            }
            if (i < slots - 1) {
                out.append("\n");
            }
        }
        return out.toString();
    }

    private void activatePostIncorrectPhrasalLock() {
        postIncorrectPhrasalLock = true;
        englishTranslationField.setEnabled(false);
        phrasalVerbInput.setEnabled(false);
        phrasalParticle1Input.setEnabled(false);
        phrasalParticle2Input.setEnabled(false);
        phrasalParticle3Input.setEnabled(false);
        submitButton.setEnabled(false);
        noScoreCheckBox.setEnabled(false);
        revealAnswerButton.setEnabled(false);
        revealAllButton.setEnabled(false);
        newRoundButton.setEnabled(true);
        practiceModeCheckBox.setEnabled(true);
    }

    private void refreshPostIncorrectPhrasalLockState() {
        if (!postIncorrectPhrasalLock) {
            return;
        }
        if (practiceModeCheckBox.isSelected()) {
            postIncorrectPhrasalLock = false;
            englishTranslationField.setEnabled(true);
            updatePhrasalInputStepState();
            updatePracticeDependentUi();
            return;
        }
        englishTranslationField.setEnabled(false);
        phrasalVerbInput.setEnabled(false);
        phrasalParticle1Input.setEnabled(false);
        phrasalParticle2Input.setEnabled(false);
        phrasalParticle3Input.setEnabled(false);
        submitButton.setEnabled(false);
        noScoreCheckBox.setEnabled(false);
        revealAnswerButton.setEnabled(false);
        revealAllButton.setEnabled(false);
        newRoundButton.setEnabled(true);
        practiceModeCheckBox.setEnabled(true);
    }

    private void refreshCurrentWordScores() {
        if (currentSpanishExpression == null) {
            scoreLabel.setText("This phrase score");
            scoreLabel.setForeground(new Color(90, 90, 90));
            return;
        }

        int phraseScore = currentSpanishExpression.getScore();
        if (isNeutralScoringRound()) {
            scoreLabel.setText(buildScoreLabelHtml("This phrase score:", phraseScore, " (sin puntuación)"));
            scoreLabel.setForeground(new Color(105, 105, 115));
        } else {
            scoreLabel.setText(buildScoreLabelHtml("This phrase score:", phraseScore, ""));
            scoreLabel.setForeground(new Color(0, 130, 50));
        }
    }

    private String buildScoreLabelHtml(String prefix, int score, String suffix) {
        String safePrefix = prefix == null ? "" : prefix;
        String safeSuffix = suffix == null ? "" : suffix;
        return "<html>" + safePrefix + " <span style='font-size:1.18em;font-weight:700;'>" + score
                + "</span>" + safeSuffix + "</html>";
    }

    private boolean isNeutralScoringRound() {
        return practiceModeCheckBox.isSelected() || revealCommittedThisRound || isNoScoreCheckRequested();
    }

    private boolean isNoScoreCheckRequested() {
        return !practiceModeCheckBox.isSelected() && noScoreCheckBox.isSelected();
    }

    private boolean isPhrasalDatabaseSelected() {
        String selected = (String) databaseSelector.getSelectedItem();
        return selected != null && selected.toLowerCase().contains("phrasal");
    }

    private boolean isPhrasalRound() {
        return isPhrasalDatabaseSelected() && currentSpanishExpression != null;
    }

    /**
     * Modelos de diseño desacoplados por modo: normal vs phrasal.
     * Esto evita que un ajuste visual de un modo afecte al otro.
     */
    private void applyGameLayoutModel(boolean phrasalMode) {
        if (topCardPanel == null || revealAnswerScrollPane == null) {
            return;
        }
        if (phrasalMode) {
            topCardPanel.setBackground(new Color(252, 254, 255));
            topCardPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(202, 214, 228), 1),
                    BorderFactory.createEmptyBorder(14, 14, 14, 14)));
            spanishExpressionLabel.setFont(new Font("Arial", Font.BOLD, 28));
            englishTranslationField.setPreferredSize(new Dimension(620, 44));
            englishTranslationField.setMinimumSize(new Dimension(500, 44));
            englishTranslationField.setMaximumSize(new Dimension(760, 44));
            revealAnswerButton.setPreferredSize(new Dimension(200, 40));
            revealAllButton.setPreferredSize(new Dimension(130, 40));
            revealAnswerScrollPane.setPreferredSize(new Dimension(860, 112));
            revealAnswerScrollPane.setMaximumSize(new Dimension(920, 150));
            scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
            if (scoreCardPanel != null) {
                scoreCardPanel.setPreferredSize(new Dimension(640, 92));
                scoreCardPanel.setMaximumSize(new Dimension(640, 92));
            }
        } else {
            // Perfil normal separado: escalado visual más amplio.
            topCardPanel.setBackground(new Color(253, 254, 255));
            topCardPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(207, 218, 232), 1),
                    BorderFactory.createEmptyBorder(22, 22, 22, 22)));
            spanishExpressionLabel.setFont(new Font("Arial", Font.BOLD, 36));
            englishTranslationField.setPreferredSize(new Dimension(760, 60));
            englishTranslationField.setMinimumSize(new Dimension(660, 60));
            englishTranslationField.setMaximumSize(new Dimension(900, 60));
            revealAnswerButton.setPreferredSize(new Dimension(250, 52));
            revealAllButton.setPreferredSize(new Dimension(188, 52));
            revealAnswerScrollPane.setPreferredSize(new Dimension(1020, 170));
            revealAnswerScrollPane.setMaximumSize(new Dimension(1120, 210));
            scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 29));
            if (scoreCardPanel != null) {
                scoreCardPanel.setPreferredSize(new Dimension(820, 132));
                scoreCardPanel.setMaximumSize(new Dimension(820, 132));
            }
        }
        revealAnswerArea.setFont(new Font("Arial", Font.PLAIN, phrasalMode ? 16 : 19));
        topCardPanel.revalidate();
        topCardPanel.repaint();
    }

    /**
     * Traducciones inglesas válidas para la frase en pantalla: incluye todas las filas ES con el mismo texto en español.
     */
    private List<EnglishExpression> cohortEnglishTranslationsOrCurrent() {
        if (gameController == null || currentSpanishExpression == null) {
            return Collections.emptyList();
        }
        List<EnglishExpression> cohortEn = gameController.getCurrentPhraseCohortEnglishTranslations();
        if (!cohortEn.isEmpty()) {
            return cohortEn;
        }
        List<EnglishExpression> t = currentSpanishExpression.getTranslations();
        return t != null ? t : Collections.emptyList();
    }

    private void configurePhrasalRoundUi() {
        boolean phrasalMode = isPhrasalRound();
        applyGameLayoutModel(phrasalMode);
        phrasalBuilderPanel.setVisible(phrasalMode);
        if (!phrasalMode) {
            englishTranslationField.setEditable(true);
            englishTranslationField.setToolTipText("Enter your English translation here");
            phrasalColumnsBand.setVisible(true);
            phrasalBuilderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            phrasalVerbInput.setEnabled(true);
            phrasalParticle1Input.setEnabled(true);
            phrasalParticle2Input.setEnabled(true);
            phrasalParticle3Input.setEnabled(true);
            return;
        }

        englishTranslationField.setEditable(true);
        englishTranslationField.setToolTipText(
                "Construye el phrasal escribiendo cada parte desde las opciones mostradas.");
        buildPhrasalOptionsForCurrentRound();
        clearPhrasalInputs();
        // Siempre rearmamos el estado base de inputs al empezar ronda phrasal nueva.
        phrasalVerbInput.setEnabled(true);
        phrasalParticle1Input.setEnabled(false);
        phrasalParticle2Input.setEnabled(false);
        phrasalParticle3Input.setEnabled(false);
        updatePhrasalInputStepState();
        SwingUtilities.invokeLater(this::deferRefreshPhrasalOptionsLayout);
    }

    /**
     * Tokens for phrasal-slot UI: strips a leading {@code to} (infinitive) so "to come back"
     * becomes {@code [come, back]} instead of misclassifying {@code to} as the verb.
     */
    private static List<String> tokensAfterOptionalTo(String englishExpr) {
        if (englishExpr == null || englishExpr.isBlank()) {
            return Collections.emptyList();
        }
        String[] raw = englishExpr.trim().toLowerCase().split("\\s+");
        int i = 0;
        if (raw.length > 0 && "to".equals(raw[0])) {
            i = 1;
        }
        if (i >= raw.length) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (; i < raw.length; i++) {
            out.add(raw[i]);
        }
        return out;
    }

    /** Distribuye 2-4 palabras del lema (tras quitar un {@code to} inicial del infinitivo) en los pools. */
    private void addPhrasalTokensToPools(String englishExpr, Set<String> verbs, Set<String> p1,
            Set<String> p2, Set<String> p3) {
        List<String> t = tokensAfterOptionalTo(englishExpr);
        if (t.size() < 2 || t.size() > 4) {
            return;
        }
        if ("to".equals(t.get(0))) {
            return;
        }
        verbs.add(t.get(0));
        p1.add(t.get(1));
        if (t.size() >= 3) {
            p2.add(t.get(2));
        }
        if (t.size() >= 4) {
            p3.add(t.get(3));
        }
    }

    /** True if any official translation for this card starts with {@code to } (infinitive). */
    private boolean currentTranslationsUseLeadingTo() {
        if (currentSpanishExpression == null) {
            return false;
        }
        return cohortEnglishTranslationsOrCurrent().stream()
                .map(EnglishExpression::getExpression)
                .filter(s -> s != null && !s.isBlank())
                .anyMatch(s -> s.trim().toLowerCase(Locale.ROOT).startsWith("to "));
    }

    /**
     * Incluye todas las piezas válidas de la tarjeta y completa con distractores del mazo;
     * el orden final se mezcla para que la correcta no quede siempre al principio de la lista mostrada.
     */
    private static List<String> buildShuffledPhrasalOptions(Set<String> required, Set<String> fullPool,
            int maxOptions, java.util.function.Predicate<String> allowToken, Random rnd) {
        LinkedHashSet<String> normReq = new LinkedHashSet<>();
        if (required != null) {
            for (String s : required) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                String n = s.trim().toLowerCase(Locale.ROOT);
                if (allowToken.test(n)) {
                    normReq.add(n);
                }
            }
        }
        List<String> chosen = new ArrayList<>(normReq);
        List<String> extra = new ArrayList<>();
        if (fullPool != null) {
            for (String s : fullPool) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                String n = s.trim().toLowerCase(Locale.ROOT);
                if (!allowToken.test(n) || normReq.contains(n)) {
                    continue;
                }
                extra.add(n);
            }
        }
        Collections.shuffle(extra, rnd);
        for (String e : extra) {
            if (chosen.size() >= maxOptions) {
                break;
            }
            if (!chosen.contains(e)) {
                chosen.add(e);
            }
        }
        Collections.shuffle(chosen, rnd);
        return chosen;
    }

    private void refreshCurrentPhrasalSlotsNeeded() {
        if (currentSpanishExpression == null) {
            currentPhrasalSlotsNeeded = 2;
            return;
        }
        int max = 2;
        for (EnglishExpression en : cohortEnglishTranslationsOrCurrent()) {
            int sz = tokensAfterOptionalTo(en.getExpression()).size();
            if (sz >= 2) {
                max = Math.max(max, sz);
            }
        }
        currentPhrasalSlotsNeeded = Math.min(4, max);
    }

    private String canonicalUserTranslationFromField() {
        String raw = englishTranslationField.getText();
        String t = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (!isPhrasalRound()) {
            return t;
        }
        if (t.isEmpty()) {
            return t;
        }
        if (currentTranslationsUseLeadingTo()) {
            String lc = t.toLowerCase(Locale.ROOT);
            if (!lc.startsWith("to ")) {
                return "to " + lc;
            }
        }
        return t;
    }

    private void buildPhrasalOptionsForCurrentRound() {
        refreshCurrentPhrasalSlotsNeeded();

        LinkedHashSet<String> reqVerbs = new LinkedHashSet<>();
        LinkedHashSet<String> reqP1 = new LinkedHashSet<>();
        LinkedHashSet<String> reqP2 = new LinkedHashSet<>();
        LinkedHashSet<String> reqP3 = new LinkedHashSet<>();
        for (SpanishExpression sp : gameController.getCurrentPhraseCohort()) {
            if (sp.getTranslations() != null) {
                sp.getTranslations().forEach(en ->
                        addPhrasalTokensToPools(en.getExpression(), reqVerbs, reqP1, reqP2, reqP3));
            }
        }

        Set<String> poolVerbs = new LinkedHashSet<>();
        Set<String> poolP1 = new LinkedHashSet<>();
        Set<String> poolP2 = new LinkedHashSet<>();
        Set<String> poolP3 = new LinkedHashSet<>();

        List<SpanishExpression> pool = gameController.getSpanishExpressionsFromDatabase(
                (String) databaseSelector.getSelectedItem());
        for (SpanishExpression expr : pool) {
            expr.getTranslations().forEach(en ->
                    addPhrasalTokensToPools(en.getExpression(), poolVerbs, poolP1, poolP2, poolP3));
        }

        /*
         * poolP2 solo se llena con la 3ª pieza de lemmas de >=3 tokens; si el mazo tiene muchos phrasales
         * cortos (2 piezas), el pool casi está vacío y solo aparece la opción correcta (p. ej. "of").
         * Unimos todas las pools de piezas como distractores creíbles para ranuras altas.
         */
        LinkedHashSet<String> poolParticle2Combined = new LinkedHashSet<>(poolP2);
        poolParticle2Combined.addAll(poolP1);
        poolParticle2Combined.addAll(poolP3);
        LinkedHashSet<String> poolParticle3Combined = new LinkedHashSet<>(poolP3);
        poolParticle3Combined.addAll(poolP1);
        poolParticle3Combined.addAll(poolP2);

        Random rnd = ThreadLocalRandom.current();
        currentVerbOptions = buildShuffledPhrasalOptions(reqVerbs, poolVerbs, 6, tok -> !"to".equals(tok), rnd);
        currentParticle1Options = buildShuffledPhrasalOptions(reqP1, poolP1, 6, tok -> true, rnd);
        currentParticle2Options = buildShuffledPhrasalOptions(reqP2, poolParticle2Combined, 6, tok -> true, rnd);
        currentParticle3Options = buildShuffledPhrasalOptions(reqP3, poolParticle3Combined, 6, tok -> true, rnd);
    }

    private String joinOptions(List<String> options) {
        if (options.isEmpty()) {
            return "-";
        }
        return String.join(" | ", options);
    }

    private void clearPhrasalInputs() {
        phrasalVerbInput.setText("");
        phrasalParticle1Input.setText("");
        phrasalParticle2Input.setText("");
        phrasalParticle3Input.setText("");
    }

    private boolean matchesListedOption(JTextField field, List<String> options) {
        String typed = field.getText() == null ? "" : field.getText().trim();
        if (typed.isEmpty()) {
            return false;
        }
        return options.stream().anyMatch(opt -> opt.equalsIgnoreCase(typed));
    }

    private void updatePhrasalInputStepState() {
        if (!isPhrasalRound()) {
            return;
        }
        // El verbo debe quedar siempre editable en rondas phrasal.
        phrasalVerbInput.setEnabled(true);
        boolean needsThird = currentPhrasalSlotsNeeded >= 3;
        boolean needsFourth = currentPhrasalSlotsNeeded >= 4;

        // En modo práctica permitimos corregirse libremente por piezas sin bloqueo secuencial.
        if (practiceModeCheckBox.isSelected()) {
            phrasalVerbInput.setEnabled(true);
            phrasalParticle1Input.setEnabled(true);
            phrasalParticle2Input.setEnabled(needsThird);
            if (!needsThird) {
                phrasalParticle2Input.setText("");
            }
            phrasalParticle3Input.setEnabled(needsFourth);
            if (!needsFourth) {
                phrasalParticle3Input.setText("");
            }
            syncPhrasalTypedInputToAnswerField();
            return;
        }

        boolean verbOk = matchesListedOption(phrasalVerbInput, currentVerbOptions);
        phrasalParticle1Input.setEnabled(verbOk);
        if (!verbOk) {
            phrasalParticle1Input.setText("");
            phrasalParticle2Input.setText("");
            phrasalParticle3Input.setText("");
            syncPhrasalTypedInputToAnswerField();
            return;
        }

        boolean p1Ok = matchesListedOption(phrasalParticle1Input, currentParticle1Options);
        if (!p1Ok) {
            phrasalParticle2Input.setText("");
            phrasalParticle3Input.setText("");
            phrasalParticle2Input.setEnabled(false);
            phrasalParticle3Input.setEnabled(false);
            syncPhrasalTypedInputToAnswerField();
            return;
        }

        if (!needsThird) {
            phrasalParticle2Input.setEnabled(false);
            phrasalParticle2Input.setText("");
            phrasalParticle3Input.setEnabled(false);
            phrasalParticle3Input.setText("");
            syncPhrasalTypedInputToAnswerField();
            return;
        }

        phrasalParticle2Input.setEnabled(true);

        boolean p2Ok = matchesListedOption(phrasalParticle2Input, currentParticle2Options);
        if (!p2Ok) {
            phrasalParticle3Input.setEnabled(false);
            phrasalParticle3Input.setText("");
            syncPhrasalTypedInputToAnswerField();
            return;
        }

        if (!needsFourth) {
            phrasalParticle3Input.setEnabled(false);
            phrasalParticle3Input.setText("");
            syncPhrasalTypedInputToAnswerField();
            return;
        }

        phrasalParticle3Input.setEnabled(true);

        syncPhrasalTypedInputToAnswerField();
    }

    private void syncPhrasalTypedInputToAnswerField() {
        List<String> parts = new ArrayList<>();
        String verb = phrasalVerbInput.getText().trim();
        String particle1 = phrasalParticle1Input.getText().trim();
        String particle2 = phrasalParticle2Input.getText().trim();
        String particle3 = phrasalParticle3Input.getText().trim();

        boolean needsThird = currentPhrasalSlotsNeeded >= 3;
        boolean needsFourth = currentPhrasalSlotsNeeded >= 4;

        if (!verb.isEmpty()) {
            parts.add(verb);
        }
        if (!particle1.isEmpty()) {
            parts.add(particle1);
        }
        if (needsThird && phrasalParticle2Input.isEnabled() && !particle2.isEmpty()) {
            parts.add(particle2);
        }
        if (needsFourth && phrasalParticle3Input.isEnabled() && !particle3.isEmpty()) {
            parts.add(particle3);
        }

        String core = String.join(" ", parts);
        englishTranslationField.setText(core);
    }

    private void attachPhrasalStepListener(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onChange.run();
            }
        });
    }

    private void preparePracticeRevealStateForNewRound() {
        stopRevealCharTimer();
        revealCommittedThisRound = false;
        revealFullText = "";
        revealCharIndex = 0;
        revealAnswerArea.setText("");
        revealAnswerArea.setForeground(new Color(40, 40, 40));
    }

    private void showFeedbackInRevealArea(String message, Color color) {
        revealAnswerArea.setText(message == null ? "" : message);
        revealAnswerArea.setForeground(color == null ? new Color(40, 40, 40) : color);
    }

    private String buildExpectedVsTypedDetail(String userTranslation) {
        String typed = userTranslation == null || userTranslation.isBlank() ? "(vacío)" : userTranslation.trim();
        String expected = gameController.getRevealAnswersLine()
                .filter(line -> !line.isBlank())
                .orElse("(sin referencia disponible)");
        return "Se esperaba: " + expected + "\nHas escrito: " + typed;
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

        noScoreCheckBox.setEnabled(hasPhrase && !practiceOn);
        if (practiceOn && noScoreCheckBox.isSelected()) {
            noScoreCheckBox.setSelected(false);
        }

        boolean noScoreRequested = isNoScoreCheckRequested();
        boolean neutral = isNeutralScoringRound();
        submitButton.setText(noScoreRequested ? "Comprobar (sin puntos)" : "Submit Answer");
        submitButton.setBackground(getButtonColor(submitButton.getText()));
        submitButton.setToolTipText(neutral
                ? "Comprueba la traducción sin modificar puntajes."
                : "Submit your translation for scoring.");
        refreshPostIncorrectPhrasalLockState();
    }

    private void resetGameDisplay() {
        postIncorrectPhrasalLock = false;
        cancelPendingAutoNextRound();
        applyGameLayoutModel(false);
        spanishExpressionLabel.setText("Select a database and start a new round!");
        englishTranslationField.setText("");
        showFeedbackInRevealArea("", new Color(40, 40, 40));
        preparePracticeRevealStateForNewRound();
        phrasalBuilderPanel.setVisible(false);
        englishTranslationField.setEditable(true);
        clearPhrasalInputs();
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

    /**
     * Campo por piezas del phrasal: máximo una palabra (sin whitespace) y sin pegado,
     * para mantener práctica manual.
     */
    private static final class PhrasalTokenField extends JTextField {
        PhrasalTokenField() {
            super();
            Document doc = getDocument();
            if (doc instanceof AbstractDocument abstractDocument) {
                abstractDocument.setDocumentFilter(new SingleTokenWhitespaceRejectFilter());
            }
        }

        @Override
        public void paste() {
        }

        /** Si el texto nuevo contiene espacio u otro whitespace, la inserción no se aplica. */
        private static final class SingleTokenWhitespaceRejectFilter extends DocumentFilter {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (string != null && containsWhitespace(string)) {
                    return;
                }
                fb.insertString(offset, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text != null && containsWhitespace(text)) {
                    return;
                }
                fb.replace(offset, length, text, attrs);
            }

            private static boolean containsWhitespace(String s) {
                for (int i = 0; i < s.length(); i++) {
                    if (Character.isWhitespace(s.charAt(i))) {
                        return true;
                    }
                }
                return false;
            }
        }
    }
}
