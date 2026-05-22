package com.englishgame;

/**
 * Runtime learning mode: classic Spanish prompt or English dictionary-style definition prompt.
 */
public enum AppGameMode {

    CLASSIC(
            "spanish",
            "spanish_expression",
            "",
            "Clásico (español → inglés)",
            "Spanish",
            "Translate: \"%s\"",
            "Español",
            "Format: Spanish - English (one per line)",
            "Example: Casa - house, home"
    ),
    DEFINITION(
            "english_definition",
            "english_definition_expression",
            "definition",
            "Definición (inglés → expresión)",
            "Definition",
            "Type the English expression for:\n\n%s",
            "Definición",
            "Format: Definition - English expression (one per line)",
            "Example: To deal with something continuously - live and breathe"
    );

    private final String promptLanguage;
    private final String promptExpressionType;
    private final String dataSubdirectory;
    private final String selectorLabel;
    private final String promptFieldLabel;
    private final String roundPromptFormat;
    private final String browseSortPromptLabel;
    private final String bulkFormatHint;
    private final String bulkExampleHint;

    AppGameMode(String promptLanguage, String promptExpressionType, String dataSubdirectory,
                String selectorLabel, String promptFieldLabel, String roundPromptFormat,
                String browseSortPromptLabel, String bulkFormatHint, String bulkExampleHint) {
        this.promptLanguage = promptLanguage;
        this.promptExpressionType = promptExpressionType;
        this.dataSubdirectory = dataSubdirectory;
        this.selectorLabel = selectorLabel;
        this.promptFieldLabel = promptFieldLabel;
        this.roundPromptFormat = roundPromptFormat;
        this.browseSortPromptLabel = browseSortPromptLabel;
        this.bulkFormatHint = bulkFormatHint;
        this.bulkExampleHint = bulkExampleHint;
    }

    public String getPromptLanguage() {
        return promptLanguage;
    }

    public String getPromptExpressionType() {
        return promptExpressionType;
    }

    public String getDataSubdirectory() {
        return dataSubdirectory;
    }

    public String getSelectorLabel() {
        return selectorLabel;
    }

    public String getPromptFieldLabel() {
        return promptFieldLabel;
    }

    public String getBrowseSortPromptLabel() {
        return browseSortPromptLabel;
    }

    public String getBulkFormatHint() {
        return bulkFormatHint;
    }

    public String getBulkExampleHint() {
        return bulkExampleHint;
    }

    public String getTitleSuffix() {
        return this == CLASSIC ? "Classic" : "Definition";
    }

    public boolean matchesPromptLanguage(String language) {
        return language != null && promptLanguage.equals(language);
    }

    public String formatRoundPrompt(String expression) {
        if (expression == null) {
            return "";
        }
        return String.format(roundPromptFormat, expression);
    }

    /** Primera línea del prompt en modo definición (sin el texto de la definición). */
    public String getRoundPromptInstructionLine() {
        if (this != DEFINITION) {
            return "";
        }
        int placeholder = roundPromptFormat.indexOf("%s");
        if (placeholder <= 0) {
            return "Type the English expression for:";
        }
        return roundPromptFormat.substring(0, placeholder).replaceAll("[\\n\\r]+$", "").trim();
    }

    public String emptyPairMessage() {
        return this == CLASSIC
                ? "Please enter both Spanish and English expressions"
                : "Please enter both the definition and the English expression";
    }

    public String duplicatePairMessage(String databaseName, String prompt, String english) {
        String promptLabel = this == CLASSIC ? "Español" : "Definición";
        return "Registro rechazado por duplicado exacto.\n\n"
                + "Ya existe en la base \"" + databaseName + "\" el mismo par:\n"
                + "• " + promptLabel + ": " + prompt + "\n"
                + "• Inglés: " + english;
    }

    public static AppGameMode fromProgramArgument(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "classic", "clasico", "es", "spanish" -> CLASSIC;
            case "definition", "definicion", "def", "english_definition" -> DEFINITION;
            default -> null;
        };
    }
}
