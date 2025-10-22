package com.englishgame.view;

import com.englishgame.controller.GameController;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Landing page for the English Learning Game
 * Main navigation hub for all application features
 */
@Slf4j
public class LandingPageView extends JFrame {

    private final GameController gameController;

    public LandingPageView(GameController gameController) {
        this.gameController = gameController;
        setTitle("English Learning Game - Main Menu");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        initComponents();
        setupLayout();
        addListeners();
        
        log.info("Landing page initialized");
    }

    private void initComponents() {
        // Main title
        JLabel titleLabel = new JLabel("English Learning Game", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(new Color(0, 102, 204));
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Master English through interactive learning", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        subtitleLabel.setForeground(Color.GRAY);
        
        // Navigation buttons
        JButton dataManagementButton = createStyledButton("📊 Manage Databases & Data", 
            "Create and manage your learning databases");
        
        JButton viewWordsButton = createStyledButton("📚 View Saved Words", 
            "Browse and review your vocabulary");
        
        JButton playGameButton = createStyledButton("🎮 Play Interactive Game", 
            "Start learning with our game system");
        
        JButton learnedWordsButton = createStyledButton("🏆 Learned Words", 
            "See your mastered vocabulary");
        
        JButton exitButton = createStyledButton("❌ Exit Application", 
            "Close the application");
        
        // Add action listeners
        dataManagementButton.addActionListener(e -> openDataManagement());
        viewWordsButton.addActionListener(e -> openViewWords());
        playGameButton.addActionListener(e -> openGame());
        learnedWordsButton.addActionListener(e -> openLearnedWords());
        exitButton.addActionListener(e -> System.exit(0));
        
        // Store buttons for layout
        this.dataManagementButton = dataManagementButton;
        this.viewWordsButton = viewWordsButton;
        this.playGameButton = playGameButton;
        this.learnedWordsButton = learnedWordsButton;
        this.exitButton = exitButton;
        this.titleLabel = titleLabel;
        this.subtitleLabel = subtitleLabel;
    }

    private JButton createStyledButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(300, 50));
        button.setToolTipText(tooltip);
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(100, 149, 237));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(70, 130, 180));
            }
        });
        
        return button;
    }

    private void setupLayout() {
        setLayout(new BorderLayout(20, 20));
        
        // Top panel with title
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 20, 20));
        topPanel.setBackground(new Color(248, 249, 250));
        
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        // Center panel with buttons
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        centerPanel.setBackground(Color.WHITE);
        
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(dataManagementButton);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(viewWordsButton);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(playGameButton);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(learnedWordsButton);
        centerPanel.add(Box.createVerticalStrut(30));
        centerPanel.add(exitButton);
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void addListeners() {
        // Window closing
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                log.info("Application closing from landing page");
                System.exit(0);
            }
        });
    }

    private void openDataManagement() {
        log.info("Opening data management window");
        this.setVisible(false);
        DataManagementView dataManagementView = new DataManagementView(gameController, this);
        dataManagementView.setVisible(true);
    }

    private void openViewWords() {
        log.info("Opening view words window");
        this.setVisible(false);
        ViewWordsView viewWordsView = new ViewWordsView(gameController, this);
        viewWordsView.setVisible(true);
    }

    private void openGame() {
        log.info("Opening game window");
        this.setVisible(false);
        GameView gameView = new GameView(gameController, this);
        gameView.setVisible(true);
    }

    private void openLearnedWords() {
        log.info("Opening learned words window");
        this.setVisible(false);
        LearnedWordsView learnedWordsView = new LearnedWordsView(gameController, this);
        learnedWordsView.setVisible(true);
    }

    // Navigation methods for other windows to return to landing page
    public void returnToLanding() {
        log.info("Returning to landing page");
        this.setVisible(true);
    }

    // UI Components
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JButton dataManagementButton;
    private JButton viewWordsButton;
    private JButton playGameButton;
    private JButton learnedWordsButton;
    private JButton exitButton;
}
