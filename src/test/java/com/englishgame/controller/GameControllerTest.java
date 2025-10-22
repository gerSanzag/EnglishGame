package com.englishgame.controller;

import com.englishgame.model.EnglishExpression;
import com.englishgame.model.SpanishExpression;
import com.englishgame.service.interfaces.DatabaseService;
import com.englishgame.service.interfaces.GameDataService;
import com.englishgame.service.interfaces.GameLogicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("GameController Tests")
class GameControllerTest {

    private GameController gameController;
    private GameLogicService gameLogicService;
    private DatabaseService databaseService;
    private GameDataService gameDataService;

    @BeforeEach
    void setUp() {
        gameLogicService = Mockito.mock(GameLogicService.class);
        databaseService = Mockito.mock(DatabaseService.class);
        gameDataService = Mockito.mock(GameDataService.class);
        
        gameController = new GameController(gameLogicService, databaseService, gameDataService);
    }

    @Test
    @DisplayName("Should initialize game controller successfully")
    void shouldInitializeGameControllerSuccessfully() {
        // Given
        when(databaseService.getAvailableDatabases()).thenReturn(Arrays.asList("test_db", "learned_words"));
        
        // When
        List<String> databases = gameController.getAvailableDatabases();
        
        // Then
        assertNotNull(databases);
        assertEquals(2, databases.size());
        verify(gameDataService).loadGameData();
    }

    @Test
    @DisplayName("Should select existing database successfully")
    void shouldSelectExistingDatabaseSuccessfully() {
        // Given
        String databaseName = "test_db";
        when(databaseService.databaseExists(databaseName)).thenReturn(true);
        
        // When
        boolean result = gameController.selectDatabase(databaseName);
        
        // Then
        assertTrue(result);
        assertEquals(databaseName, gameController.getCurrentDatabase());
    }

    @Test
    @DisplayName("Should not select non-existent database")
    void shouldNotSelectNonExistentDatabase() {
        // Given
        String databaseName = "non_existent_db";
        when(databaseService.databaseExists(databaseName)).thenReturn(false);
        
        // When
        boolean result = gameController.selectDatabase(databaseName);
        
        // Then
        assertFalse(result);
        assertNull(gameController.getCurrentDatabase());
    }

    @Test
    @DisplayName("Should start new round successfully")
    void shouldStartNewRoundSuccessfully() {
        // Given
        String databaseName = "test_db";
        SpanishExpression spanishExpression = new SpanishExpression("casa", 0, 
                Arrays.asList(new EnglishExpression("house", 0, Collections.emptyList())));
        
        when(databaseService.databaseExists(databaseName)).thenReturn(true);
        gameController.selectDatabase(databaseName);
        when(gameLogicService.getRandomSpanishExpression(databaseName)).thenReturn(spanishExpression);
        
        // When
        SpanishExpression result = gameController.startNewRound();
        
        // Then
        assertNotNull(result);
        assertEquals("casa", result.getExpression());
        assertEquals(spanishExpression, gameController.getCurrentSpanishExpression());
    }

    @Test
    @DisplayName("Should not start new round without selected database")
    void shouldNotStartNewRoundWithoutSelectedDatabase() {
        // When
        SpanishExpression result = gameController.startNewRound();
        
        // Then
        assertNull(result);
        assertNull(gameController.getCurrentSpanishExpression());
    }

    @Test
    @DisplayName("Should process correct answer successfully")
    void shouldProcessCorrectAnswerSuccessfully() {
        // Given
        String databaseName = "test_db";
        SpanishExpression spanishExpression = new SpanishExpression("casa", 0, 
                Arrays.asList(new EnglishExpression("house", 5, Collections.emptyList())));
        EnglishExpression updatedEnglishExpression = new EnglishExpression("house", 6, Collections.emptyList());
        
        when(databaseService.databaseExists(databaseName)).thenReturn(true);
        when(gameLogicService.getRandomSpanishExpression(databaseName)).thenReturn(spanishExpression);
        
        gameController.selectDatabase(databaseName);
        gameController.startNewRound();
        
        when(gameLogicService.validateTranslation(spanishExpression, "house")).thenReturn(true);
        when(gameLogicService.processCorrectAnswer(spanishExpression, "house")).thenReturn(updatedEnglishExpression);
        when(gameLogicService.isExpressionLearned(updatedEnglishExpression)).thenReturn(false);
        
        // When
        boolean result = gameController.processAnswer("house");
        
        // Then
        assertTrue(result);
        verify(gameLogicService).processCorrectAnswer(spanishExpression, "house");
    }

    @Test
    @DisplayName("Should process incorrect answer successfully")
    void shouldProcessIncorrectAnswerSuccessfully() {
        // Given
        String databaseName = "test_db";
        SpanishExpression spanishExpression = new SpanishExpression("casa", 0, 
                Arrays.asList(new EnglishExpression("house", 5, Collections.emptyList())));
        List<EnglishExpression> penalizedTranslations = Arrays.asList(
                new EnglishExpression("house", 2, Collections.emptyList()));
        
        when(databaseService.databaseExists(databaseName)).thenReturn(true);
        when(gameLogicService.getRandomSpanishExpression(databaseName)).thenReturn(spanishExpression);
        
        gameController.selectDatabase(databaseName);
        gameController.startNewRound();
        
        when(gameLogicService.validateTranslation(spanishExpression, "wrong")).thenReturn(false);
        when(gameLogicService.processIncorrectAnswer(spanishExpression, "wrong")).thenReturn(penalizedTranslations);
        
        // When
        boolean result = gameController.processAnswer("wrong");
        
        // Then
        assertFalse(result);
        verify(gameLogicService).processIncorrectAnswer(spanishExpression, "wrong");
    }

    @Test
    @DisplayName("Should not process answer without current expression")
    void shouldNotProcessAnswerWithoutCurrentExpression() {
        // When
        boolean result = gameController.processAnswer("house");
        
        // Then
        assertFalse(result);
        verify(gameLogicService, never()).validateTranslation(any(), anyString());
    }

    @Test
    @DisplayName("Should create new database successfully")
    void shouldCreateNewDatabaseSuccessfully() {
        // Given
        String databaseName = "new_db";
        when(databaseService.createDatabase(databaseName)).thenReturn(true);
        
        // When
        boolean result = gameController.createNewDatabase(databaseName);
        
        // Then
        assertTrue(result);
        verify(databaseService).createDatabase(databaseName);
        verify(gameDataService).saveGameData();
    }

    @Test
    @DisplayName("Should not create database with empty name")
    void shouldNotCreateDatabaseWithEmptyName() {
        // When
        boolean result = gameController.createNewDatabase("");
        
        // Then
        assertFalse(result);
        verify(databaseService, never()).createDatabase(anyString());
    }

    @Test
    @DisplayName("Should add expression to existing database")
    void shouldAddExpressionToExistingDatabase() {
        // Given
        String databaseName = "test_db";
        SpanishExpression spanishExpression = new SpanishExpression("casa", 0, Collections.emptyList());
        
        when(databaseService.databaseExists(databaseName)).thenReturn(true);
        when(databaseService.addSpanishExpression(databaseName, spanishExpression)).thenReturn(true);
        
        // When
        boolean result = gameController.addExpressionToDatabase(databaseName, spanishExpression);
        
        // Then
        assertTrue(result);
        verify(databaseService).addSpanishExpression(databaseName, spanishExpression);
        verify(gameDataService).saveGameData();
    }

    @Test
    @DisplayName("Should not add expression to non-existent database")
    void shouldNotAddExpressionToNonExistentDatabase() {
        // Given
        String databaseName = "non_existent_db";
        SpanishExpression spanishExpression = new SpanishExpression("casa", 0, Collections.emptyList());
        
        when(databaseService.databaseExists(databaseName)).thenReturn(false);
        
        // When
        boolean result = gameController.addExpressionToDatabase(databaseName, spanishExpression);
        
        // Then
        assertFalse(result);
        verify(databaseService, never()).addSpanishExpression(anyString(), any());
    }

    @Test
    @DisplayName("Should save and load game state")
    void shouldSaveAndLoadGameState() {
        // When
        gameController.saveGameState();
        gameController.loadGameState();
        
        // Then
        verify(gameDataService).saveGameData();
        verify(gameDataService, times(2)).loadGameData(); // Once in constructor, once in loadGameState
    }
}
