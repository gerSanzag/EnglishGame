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
    private static final String DEFAULT_DATA_DIR = "data";
    private static final String GAME_DATA_FILE = "game_data.json";
    private static final String BACKUP_DIR = "backups";
    
    public GameDataServiceImpl(DBRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.dataDirectory = DEFAULT_DATA_DIR;
        initializeDataDirectory();
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
            // Get all data from repository
            List<List<Map<String, Object>>> allData = repository.findAll();
            
            // Create data directory if it doesn't exist
            Path dataPath = Paths.get(dataDirectory);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                log.debug("Created data directory: {}", dataDirectory);
            }
            
            // Save to JSON file
            Path filePath = dataPath.resolve(filename);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), allData);
            
            log.debug("Game data saved successfully to: {}", filePath);
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
}
