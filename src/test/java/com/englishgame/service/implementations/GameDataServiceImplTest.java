package com.englishgame.service.implementations;

import com.englishgame.repository.interfaces.DBRepository;
import com.englishgame.repository.implementations.DBRepositoryImpl;
import com.englishgame.service.interfaces.GameDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for GameDataServiceImpl
 * Tests persistence functionality for game data
 */
@DisplayName("GameDataService Tests")
class GameDataServiceImplTest {
    
    private DBRepository repository;
    private GameDataService gameDataService;
    private String testDataDirectory;
    
    @BeforeEach
    void setUp() {
        repository = new DBRepositoryImpl();
        testDataDirectory = "test_data";
        gameDataService = new GameDataServiceImpl(repository);
        gameDataService.setDataDirectory(testDataDirectory);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up test files
        try {
            Path testPath = Paths.get(testDataDirectory);
            if (Files.exists(testPath)) {
                Files.walk(testPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }
    
    @Test
    @DisplayName("Should save game data successfully")
    void shouldSaveGameDataSuccessfully() {
        // Given
        populateRepositoryWithTestData();
        
        // When
        boolean result = gameDataService.saveGameData();
        
        // Then
        assertTrue(result);
        assertTrue(gameDataService.gameDataExists());
        assertTrue(gameDataService.getGameDataSize() > 0);
    }
    
    @Test
    @DisplayName("Should load game data successfully")
    void shouldLoadGameDataSuccessfully() {
        // Given
        populateRepositoryWithTestData();
        gameDataService.saveGameData();
        repository.clear(); // Clear to test loading
        
        // When
        boolean result = gameDataService.loadGameData();
        
        // Then
        assertTrue(result);
        assertFalse(repository.isEmpty());
        assertEquals(2, repository.size());
    }
    
    @Test
    @DisplayName("Should save to specific file")
    void shouldSaveToSpecificFile() {
        // Given
        populateRepositoryWithTestData();
        String filename = "custom_game_data.json";
        
        // When
        boolean result = gameDataService.saveGameDataToFile(filename);
        
        // Then
        assertTrue(result);
        Path filePath = Paths.get(testDataDirectory, filename);
        assertTrue(Files.exists(filePath));
    }
    
    @Test
    @DisplayName("Should load from specific file")
    void shouldLoadFromSpecificFile() {
        // Given
        populateRepositoryWithTestData();
        String filename = "custom_game_data.json";
        gameDataService.saveGameDataToFile(filename);
        repository.clear();
        
        // When
        boolean result = gameDataService.loadGameDataFromFile(filename);
        
        // Then
        assertTrue(result);
        assertFalse(repository.isEmpty());
    }
    
    @Test
    @DisplayName("Should not save with null filename")
    void shouldNotSaveWithNullFilename() {
        // Given
        populateRepositoryWithTestData();
        
        // When
        boolean result = gameDataService.saveGameDataToFile(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should not load with null filename")
    void shouldNotLoadWithNullFilename() {
        // When
        boolean result = gameDataService.loadGameDataFromFile(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should not load from non-existent file")
    void shouldNotLoadFromNonExistentFile() {
        // When
        boolean result = gameDataService.loadGameDataFromFile("non_existent.json");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should create backup successfully")
    void shouldCreateBackupSuccessfully() {
        // Given
        populateRepositoryWithTestData();
        
        // When
        boolean result = gameDataService.createBackup();
        
        // Then
        assertTrue(result);
        Path backupDir = Paths.get(testDataDirectory, "backups");
        assertTrue(Files.exists(backupDir));
        
        // Check that backup file exists
        File[] backupFiles = backupDir.toFile().listFiles((dir, name) -> 
            name.startsWith("game_data_backup_") && name.endsWith(".json"));
        assertNotNull(backupFiles);
        assertTrue(backupFiles.length > 0);
    }
    
    @Test
    @DisplayName("Should restore from backup successfully")
    void shouldRestoreFromBackupSuccessfully() {
        // Given
        populateRepositoryWithTestData();
        gameDataService.createBackup();
        repository.clear();
        
        // When
        boolean result = gameDataService.restoreFromBackup();
        
        // Then
        assertTrue(result);
        assertFalse(repository.isEmpty());
        assertEquals(2, repository.size());
    }
    
    @Test
    @DisplayName("Should not restore from non-existent backup")
    void shouldNotRestoreFromNonExistentBackup() {
        // When
        boolean result = gameDataService.restoreFromBackup();
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should export to JSON string")
    void shouldExportToJSONString() {
        // Given
        populateRepositoryWithTestData();
        
        // When
        String jsonData = gameDataService.exportToJSON();
        
        // Then
        assertNotNull(jsonData);
        assertFalse(jsonData.isEmpty());
        assertTrue(jsonData.contains("spanish"));
        assertTrue(jsonData.contains("english"));
    }
    
    @Test
    @DisplayName("Should import from JSON string")
    void shouldImportFromJSONString() {
        // Given
        String jsonData = createTestJSONData();
        repository.clear();
        
        // When
        boolean result = gameDataService.importFromJSON(jsonData);
        
        // Then
        assertTrue(result);
        assertFalse(repository.isEmpty());
        assertEquals(2, repository.size());
    }
    
    @Test
    @DisplayName("Should not import null JSON")
    void shouldNotImportNullJSON() {
        // When
        boolean result = gameDataService.importFromJSON(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should not import empty JSON")
    void shouldNotImportEmptyJSON() {
        // When
        boolean result = gameDataService.importFromJSON("");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should get data directory")
    void shouldGetDataDirectory() {
        // When
        String directory = gameDataService.getDataDirectory();
        
        // Then
        assertEquals(testDataDirectory, directory);
    }
    
    @Test
    @DisplayName("Should set data directory")
    void shouldSetDataDirectory() {
        // Given
        String newDirectory = "new_test_data";
        
        // When
        gameDataService.setDataDirectory(newDirectory);
        
        // Then
        assertEquals(newDirectory, gameDataService.getDataDirectory());
    }
    
    @Test
    @DisplayName("Should not set null directory")
    void shouldNotSetNullDirectory() {
        // Given
        String originalDirectory = gameDataService.getDataDirectory();
        
        // When
        gameDataService.setDataDirectory(null);
        
        // Then
        assertEquals(originalDirectory, gameDataService.getDataDirectory());
    }
    
    @Test
    @DisplayName("Should check if game data exists")
    void shouldCheckIfGameDataExists() {
        // Initially should not exist
        assertFalse(gameDataService.gameDataExists());
        
        // After saving should exist
        populateRepositoryWithTestData();
        gameDataService.saveGameData();
        assertTrue(gameDataService.gameDataExists());
    }
    
    @Test
    @DisplayName("Should get game data size")
    void shouldGetGameDataSize() {
        // Initially should be -1 (no file)
        assertEquals(-1, gameDataService.getGameDataSize());
        
        // After saving should have size > 0
        populateRepositoryWithTestData();
        gameDataService.saveGameData();
        assertTrue(gameDataService.getGameDataSize() > 0);
    }
    
    // Helper methods
    private void populateRepositoryWithTestData() {
        List<Map<String, Object>> spanishRecord = createSpanishExpressionRecord();
        List<Map<String, Object>> englishRecord = createEnglishExpressionRecord();
        
        repository.save(spanishRecord);
        repository.save(englishRecord);
    }
    
    private List<Map<String, Object>> createSpanishExpressionRecord() {
        List<Map<String, Object>> record = new ArrayList<>();
        
        Map<String, Object> expression1 = new HashMap<>();
        expression1.put("language", "spanish");
        expression1.put("expression", "Hola");
        expression1.put("score", 10);
        expression1.put("translations", Arrays.asList("Hello", "Hi"));
        
        Map<String, Object> expression2 = new HashMap<>();
        expression2.put("language", "spanish");
        expression2.put("expression", "Adiós");
        expression2.put("score", 15);
        expression2.put("translations", Arrays.asList("Goodbye", "Bye"));
        
        record.add(expression1);
        record.add(expression2);
        
        return record;
    }
    
    private List<Map<String, Object>> createEnglishExpressionRecord() {
        List<Map<String, Object>> record = new ArrayList<>();
        
        Map<String, Object> expression1 = new HashMap<>();
        expression1.put("language", "english");
        expression1.put("expression", "Hello");
        expression1.put("score", 10);
        expression1.put("translations", Arrays.asList("Hola", "Saludos"));
        
        Map<String, Object> expression2 = new HashMap<>();
        expression2.put("language", "english");
        expression2.put("expression", "Goodbye");
        expression2.put("score", 15);
        expression2.put("translations", Arrays.asList("Adiós", "Hasta luego"));
        
        record.add(expression1);
        record.add(expression2);
        
        return record;
    }
    
    private String createTestJSONData() {
        return """
            [
              [
                {
                  "language": "spanish",
                  "expression": "Hola",
                  "score": 10,
                  "translations": ["Hello", "Hi"]
                }
              ],
              [
                {
                  "language": "english",
                  "expression": "Hello",
                  "score": 10,
                  "translations": ["Hola", "Saludos"]
                }
              ]
            ]
            """;
    }
}
