package com.englishgame.service.implementations;

import com.englishgame.repository.interfaces.DBRepository;
import com.englishgame.service.interfaces.GameDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of GameDataService for managing game data persistence
 * Handles saving and loading game data to/from JSON files
 */
@Slf4j
public class GameDataServiceImpl implements GameDataService {
    
    private final DBRepository repository;
    private final ObjectMapper objectMapper;
    private String dataDirectory;
    private com.englishgame.service.interfaces.DatabaseService databaseService;
    private static final String DEFAULT_DATA_DIR = "data";
    private static final String GAME_DATA_FILE = "game_data.json";
    private static final String BACKUP_DIR = "backups";
    
    public GameDataServiceImpl(DBRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.dataDirectory = getAbsoluteDataDirectory();
        initializeDataDirectory();
    }
    
    /**
     * Sets the database service reference for building current state
     */
    public void setDatabaseService(com.englishgame.service.interfaces.DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
    
    @Override
    public boolean saveGameData() {
        return saveGameDataToFile(GAME_DATA_FILE);
    }
    
    @Override
    public boolean loadGameData() {
        return loadGameDataFromFile(GAME_DATA_FILE);
    }
    
    @Override
    public boolean saveGameDataToFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            log.warn("Attempt to save with null or empty filename");
            return false;
        }
        
        try {
            // Get current state from database service instead of repository
            List<List<Map<String, Object>>> currentState = buildCurrentStateFromDatabases();
            
            // Create data directory if it doesn't exist
            Path dataPath = Paths.get(dataDirectory);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                log.debug("Created data directory: {}", dataDirectory);
            }
            
            // Save to JSON file
            Path filePath = dataPath.resolve(filename);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), currentState);
            
            log.debug("Game data saved successfully to: {}", filePath);
            log.info("Saved {} records to JSON file", currentState.size());
            return true;
            
        } catch (IOException e) {
            log.error("Error saving game data to file '{}': {}", filename, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean loadGameDataFromFile(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            log.warn("Attempt to load with null or empty filename");
            return false;
        }
        
        try {
            Path filePath = Paths.get(dataDirectory, filename);
            log.info("Attempting to load game data from: {}", filePath.toAbsolutePath());
            
            if (!Files.exists(filePath)) {
                log.warn("Game data file does not exist: {}", filePath);
                return false;
            }
            
            // Read JSON file
            @SuppressWarnings("unchecked")
            List<List<Map<String, Object>>> loadedData = objectMapper.readValue(
                filePath.toFile(), 
                objectMapper.getTypeFactory().constructCollectionType(List.class, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class))
            );
            
            // Clear current repository and load new data
            repository.clear();
            for (List<Map<String, Object>> record : loadedData) {
                repository.save(record);
            }
            
            log.debug("Game data loaded successfully from: {}", filePath);
            log.info("Loaded {} records from JSON file", loadedData.size());
            return true;
            
        } catch (IOException e) {
            log.error("Error loading game data from file '{}': {}", filename, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean createBackup() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFilename = "game_data_backup_" + timestamp + ".json";
        
        try {
            Path backupPath = Paths.get(dataDirectory, BACKUP_DIR);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
                log.debug("Created backup directory: {}", backupPath);
            }
            
            Path backupFilePath = backupPath.resolve(backupFilename);
            List<List<Map<String, Object>>> allData = repository.findAll();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(backupFilePath.toFile(), allData);
            
            log.debug("Backup created successfully: {}", backupFilePath);
            return true;
            
        } catch (IOException e) {
            log.error("Error creating backup: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean restoreFromBackup() {
        try {
            Path backupPath = Paths.get(dataDirectory, BACKUP_DIR);
            if (!Files.exists(backupPath)) {
                log.warn("Backup directory does not exist: {}", backupPath);
                return false;
            }
            
            // Find the most recent backup file
            File[] backupFiles = backupPath.toFile().listFiles((dir, name) -> 
                name.startsWith("game_data_backup_") && name.endsWith(".json"));
            
            if (backupFiles == null || backupFiles.length == 0) {
                log.warn("No backup files found in: {}", backupPath);
                return false;
            }
            
            File latestBackup = backupFiles[0];
            for (File file : backupFiles) {
                if (file.lastModified() > latestBackup.lastModified()) {
                    latestBackup = file;
                }
            }
            
            // Load from backup
            @SuppressWarnings("unchecked")
            List<List<Map<String, Object>>> backupData = objectMapper.readValue(
                latestBackup, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class))
            );
            
            repository.clear();
            for (List<Map<String, Object>> record : backupData) {
                repository.save(record);
            }
            
            log.debug("Game data restored from backup: {}", latestBackup.getName());
            return true;
            
        } catch (IOException e) {
            log.error("Error restoring from backup: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getDataDirectory() {
        return dataDirectory;
    }
    
    @Override
    public void setDataDirectory(String directory) {
        if (directory != null && !directory.trim().isEmpty()) {
            this.dataDirectory = directory;
            initializeDataDirectory();
            log.debug("Data directory set to: {}", directory);
        }
    }
    
    @Override
    public boolean gameDataExists() {
        Path filePath = Paths.get(dataDirectory, GAME_DATA_FILE);
        boolean exists = Files.exists(filePath);
        log.debug("Game data file exists: {}", exists);
        return exists;
    }
    
    @Override
    public long getGameDataSize() {
        try {
            Path filePath = Paths.get(dataDirectory, GAME_DATA_FILE);
            if (Files.exists(filePath)) {
                long size = Files.size(filePath);
                log.debug("Game data file size: {} bytes", size);
                return size;
            }
        } catch (IOException e) {
            log.error("Error getting game data size: {}", e.getMessage());
        }
        return -1;
    }
    
    @Override
    public String exportToJSON() {
        try {
            List<List<Map<String, Object>>> allData = repository.findAll();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allData);
        } catch (IOException e) {
            log.error("Error exporting to JSON: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public boolean importFromJSON(String jsonData) {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            log.warn("Attempt to import null or empty JSON data");
            return false;
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<List<Map<String, Object>>> importedData = objectMapper.readValue(
                jsonData, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class))
            );
            
            repository.clear();
            for (List<Map<String, Object>> record : importedData) {
                repository.save(record);
            }
            
            log.debug("Game data imported successfully from JSON");
            return true;
            
        } catch (IOException e) {
            log.error("Error importing from JSON: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public List<List<Map<String, Object>>> getAllData() {
        return repository.findAll();
    }
    
    @Override
    public com.englishgame.repository.interfaces.DBRepository getRepository() {
        return repository;
    }
    
    /**
     * Gets the absolute path to the data directory based on the JAR location
     * This ensures the data directory is always relative to where the JAR is located
     */
    private String getAbsoluteDataDirectory() {
        try {
            // Get the path of the JAR file
            String jarPath = GameDataServiceImpl.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();
            
            // Get the directory containing the JAR
            File jarFile = new File(jarPath);
            File jarDirectory = jarFile.getParentFile();
            
            // If JAR is in target/ directory, go up one level to find the data directory
            String absoluteDataDir;
            if (jarDirectory.getName().equals("target")) {
                // JAR is in target/, data directory is in parent directory
                absoluteDataDir = new File(jarDirectory.getParentFile(), DEFAULT_DATA_DIR).getAbsolutePath();
            } else {
                // JAR is elsewhere, data directory is next to JAR
                absoluteDataDir = new File(jarDirectory, DEFAULT_DATA_DIR).getAbsolutePath();
            }
            
            log.info("Using absolute data directory: {}", absoluteDataDir);
            return absoluteDataDir;
            
        } catch (Exception e) {
            log.warn("Could not determine JAR location, using relative path: {}", e.getMessage());
            return DEFAULT_DATA_DIR;
        }
    }
    
    private void initializeDataDirectory() {
        try {
            Path dataPath = Paths.get(dataDirectory);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                log.debug("Initialized data directory: {}", dataDirectory);
            }
        } catch (IOException e) {
            log.error("Error initializing data directory: {}", e.getMessage());
        }
    }
    
    /**
     * Builds current state from database service instead of repository
     * This prevents duplicate records
     */
    private List<List<Map<String, Object>>> buildCurrentStateFromDatabases() {
        List<List<Map<String, Object>>> currentState = new ArrayList<>();
        
        if (databaseService == null) {
            log.warn("Database service not set, falling back to repository");
            return repository.findAll();
        }
        
        // Get all available databases
        List<String> databases = databaseService.getAvailableDatabases();
        
        for (String databaseName : databases) {
            // Add database metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "database_metadata");
            metadata.put("database", databaseName);
            metadata.put("created_at", System.currentTimeMillis());
            currentState.add(Arrays.asList(metadata));
            
            // Add all Spanish expressions from this database
            List<com.englishgame.model.SpanishExpression> spanishExpressions = databaseService.getSpanishExpressions(databaseName);
            for (com.englishgame.model.SpanishExpression spanishExpr : spanishExpressions) {
                Map<String, Object> expressionData = new HashMap<>();
                expressionData.put("type", "spanish_expression");
                expressionData.put("database", databaseName);
                expressionData.put("language", "spanish");
                expressionData.put("expression", spanishExpr.getExpression());
                expressionData.put("score", spanishExpr.getScore());
                
                // Add translations
                List<String> translations = new ArrayList<>();
                for (com.englishgame.model.EnglishExpression translation : spanishExpr.getTranslations()) {
                    translations.add(translation.getExpression());
                }
                expressionData.put("translations", translations);
                
                currentState.add(Arrays.asList(expressionData));
            }
        }
        
        log.debug("Built current state with {} records from {} databases", currentState.size(), databases.size());
        return currentState;
    }
}
