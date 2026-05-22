package com.englishgame;

import com.englishgame.controller.GameController;
import com.englishgame.AppGameMode;
import com.englishgame.repository.implementations.DBRepositoryImpl;
import com.englishgame.repository.implementations.DataBaseImpl;
import com.englishgame.repository.implementations.ExpressionsImpl;
import com.englishgame.service.implementations.DatabaseServiceImpl;
import com.englishgame.service.implementations.GameDataServiceImpl;
import com.englishgame.service.implementations.GameLogicServiceImpl;
import com.englishgame.service.implementations.ScoreServiceImpl;
import com.englishgame.view.LandingPageView;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * Main application entry point
 * Initializes all services and starts the game
 */
@Slf4j
public class Main {
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                AppGameMode mode = resolveGameMode(args);
                if (mode == null) {
                    log.info("Application closed before mode selection");
                    return;
                }
                initializeAndStartApplication(mode);
            } catch (Exception e) {
                log.error("Failed to start application", e);
                showErrorDialog("Failed to start application: " + e.getMessage());
            }
        });
    }

    private static AppGameMode resolveGameMode(String[] args) {
        AppGameMode fromArgs = parseModeFromArgs(args);
        if (fromArgs != null) {
            return fromArgs;
        }
        return GameModeSelector.show();
    }

    private static AppGameMode parseModeFromArgs(String[] args) {
        if (args == null) {
            return null;
        }
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String trimmed = arg.trim();
            if (trimmed.startsWith("--mode=")) {
                return AppGameMode.fromProgramArgument(trimmed.substring("--mode=".length()));
            }
            if ("--classic".equalsIgnoreCase(trimmed)) {
                return AppGameMode.CLASSIC;
            }
            if ("--definition".equalsIgnoreCase(trimmed)) {
                return AppGameMode.DEFINITION;
            }
        }
        return null;
    }
    
    private static void initializeAndStartApplication(AppGameMode mode) {
        log.info("Initializing English Learning Game (build {}, mode {})...",
                AppVersion.getDisplayVersion(), mode.getTitleSuffix());
        
        try {
            // Initialize repositories
            DBRepositoryImpl dbRepository = new DBRepositoryImpl();
            DataBaseImpl dataBase = new DataBaseImpl();
            ExpressionsImpl expressions = new ExpressionsImpl();
            
            // Initialize services
            GameDataServiceImpl gameDataService = new GameDataServiceImpl(dbRepository, mode);
            DatabaseServiceImpl databaseService = new DatabaseServiceImpl(gameDataService);
            ScoreServiceImpl scoreService = new ScoreServiceImpl();
            GameLogicServiceImpl gameLogicService = new GameLogicServiceImpl(gameDataService, databaseService);
            
            GameController gameController = new GameController(gameLogicService, databaseService, gameDataService, mode);
            
            LandingPageView landingPageView = new LandingPageView(gameController, mode);
            landingPageView.setVisible(true);
            
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