package com.englishgame.view;

import com.englishgame.controller.GameController;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.RenderingHints;

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
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        setMinimumSize(new Dimension(600, 500));
        pack();
        
        initComponents();
        setupLayout();
        addListeners();
        
        log.info("Landing page initialized");
    }

    private void initComponents() {
        // Main title
        JLabel titleLabel = new JLabel("English Learning Game", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Master English through interactive learning", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        subtitleLabel.setForeground(Color.WHITE);
        
        // Welcome message
        JLabel welcomeLabel = new JLabel("Welcome to your English learning journey!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        welcomeLabel.setForeground(Color.WHITE);
        
        // Instruction
        JLabel instructionLabel = new JLabel("Choose an option below to get started.", SwingConstants.CENTER);
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        instructionLabel.setForeground(Color.WHITE);
        
        // Navigation buttons with different colors
        JButton dataManagementButton = createStyledButton("Manage Databases & Data", 
            "Create and manage your learning databases", new Color(52, 152, 219)); // Blue
        
        JButton viewWordsButton = createStyledButton("View Saved Words", 
            "Browse and review your vocabulary", new Color(46, 204, 113)); // Green
        
        JButton playGameButton = createStyledButton("Play Interactive Game", 
            "Start learning with our game system", new Color(241, 196, 15)); // Orange
        
        JButton learnedWordsButton = createStyledButton("Learned Words", 
            "See your mastered vocabulary", new Color(155, 89, 182)); // Purple
        
        JButton exitButton = createStyledButton("Exit Application", 
            "Close the application", new Color(231, 76, 60)); // Red
        
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
        this.welcomeLabel = welcomeLabel;
        this.instructionLabel = instructionLabel;
    }

    private JButton createStyledButton(String text, String tooltip, Color buttonColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(350, 50));
        button.setMinimumSize(new Dimension(300, 50));
        button.setMaximumSize(new Dimension(400, 50));
        button.setToolTipText(tooltip);
        button.setBackground(buttonColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        
        // Create darker border color
        Color borderColor = new Color(
            Math.max(0, buttonColor.getRed() - 30),
            Math.max(0, buttonColor.getGreen() - 30),
            Math.max(0, buttonColor.getBlue() - 30)
        );
        button.setBorder(BorderFactory.createLineBorder(borderColor, 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        
        // Hover effect with lighter color
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                Color hoverColor = new Color(
                    Math.min(255, buttonColor.getRed() + 30),
                    Math.min(255, buttonColor.getGreen() + 30),
                    Math.min(255, buttonColor.getBlue() + 30)
                );
                button.setBackground(hoverColor);
                button.setBorder(BorderFactory.createLineBorder(buttonColor, 2));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(buttonColor);
                button.setBorder(BorderFactory.createLineBorder(borderColor, 2));
            }
        });
        
        return button;
    }

    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Blue banner panel with gradient
        JPanel bannerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Create gradient from light blue to dark blue
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(100, 149, 237),  // Light blue at top
                    0, getHeight(), new Color(70, 130, 180)  // Dark blue at bottom
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bannerPanel.setLayout(new BoxLayout(bannerPanel, BoxLayout.Y_AXIS));
        bannerPanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
        bannerPanel.setPreferredSize(new Dimension(600, 180));
        bannerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        
        // Add title and subtitle to banner
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        bannerPanel.add(Box.createVerticalStrut(10));
        bannerPanel.add(titleLabel);
        bannerPanel.add(Box.createVerticalStrut(10));
        bannerPanel.add(subtitleLabel);
        bannerPanel.add(Box.createVerticalStrut(20));
        bannerPanel.add(welcomeLabel);
        bannerPanel.add(Box.createVerticalStrut(5));
        bannerPanel.add(instructionLabel);
        
        // Center panel with buttons
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setPreferredSize(new Dimension(600, 400));
        
        // Center buttons horizontally
        dataManagementButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewWordsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        learnedWordsButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        
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
        
        add(bannerPanel, BorderLayout.NORTH);
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
    private JLabel welcomeLabel;
    private JLabel instructionLabel;
    private JButton dataManagementButton;
    private JButton viewWordsButton;
    private JButton playGameButton;
    private JButton learnedWordsButton;
    private JButton exitButton;
}
