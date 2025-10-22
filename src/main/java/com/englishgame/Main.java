package com.englishgame;

import com.englishgame.controller.GameController;
import com.englishgame.repository.implementations.DBRepositoryImpl;
import com.englishgame.repository.implementations.DataBaseImpl;
import com.englishgame.repository.implementations.ExpressionsImpl;
import com.englishgame.service.implementations.DatabaseServiceImpl;
import com.englishgame.service.implementations.GameDataServiceImpl;
import com.englishgame.service.implementations.GameLogicServiceImpl;
import com.englishgame.service.implementations.ScoreServiceImpl;
import com.englishgame.view.MainGameView;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * Main application entry point
 * Initializes all services and starts the game
 */
@Slf4j
public class Main {
    
    public static void main(String[] args) {
        // Initialize application on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                initializeAndStartApplication();
            } catch (Exception e) {
                log.error("Failed to start application", e);
                showErrorDialog("Failed to start application: " + e.getMessage());
            }
        });
    }
    
    private static void initializeAndStartApplication() {
        log.info("Initializing English Learning Game...");
        
        try {
            // Initialize repositories
            DBRepositoryImpl dbRepository = new DBRepositoryImpl();
            DataBaseImpl dataBase = new DataBaseImpl();
            ExpressionsImpl expressions = new ExpressionsImpl();
            
            // Initialize services
            GameDataServiceImpl gameDataService = new GameDataServiceImpl(dbRepository);
            DatabaseServiceImpl databaseService = new DatabaseServiceImpl(gameDataService);
            ScoreServiceImpl scoreService = new ScoreServiceImpl();
            GameLogicServiceImpl gameLogicService = new GameLogicServiceImpl(gameDataService);
            
            // Initialize controller
            GameController gameController = new GameController(gameLogicService, databaseService, gameDataService);
            
            // Initialize and show main view
            MainGameView mainView = new MainGameView(gameController);
            mainView.showView();
            
            log.info("English Learning Game started successfully");
            
        } catch (Exception e) {
            log.error("Error initializing application", e);
            throw new RuntimeException("Failed to initialize application", e);
        }
    }
    
    private static void showErrorDialog(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                null,
                message,
                "Application Error",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        });
    }
}