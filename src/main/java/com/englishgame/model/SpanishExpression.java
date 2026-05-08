package com.englishgame.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Entity representing a Spanish expression with its score and translations
 */
@Data
@NoArgsConstructor
public class SpanishExpression {
    private String expression;
    private int score;
    private List<EnglishExpression> translations = new ArrayList<>();
    /** Epoch millis when the record was first added; 0 = legacy / unknown. Not used in equals. */
    private long includedAtEpochMillis;

    public SpanishExpression(String expression, int score, List<EnglishExpression> translations) {
        this.expression = expression;
        this.score = score;
        this.translations = translations != null ? translations : new ArrayList<>();
        this.includedAtEpochMillis = 0L;
    }

    private String normalizedExpression() {
        return expression == null ? "" : expression.trim().toLowerCase();
    }

    private List<String> normalizedTranslationExpressions() {
        if (translations == null) {
            return List.of();
        }
        return translations.stream()
                .map(EnglishExpression::getExpression)
                .filter(Objects::nonNull)
                .map(s -> s.trim().toLowerCase())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpanishExpression that)) {
            return false;
        }
        return Objects.equals(normalizedExpression(), that.normalizedExpression())
                && Objects.equals(normalizedTranslationExpressions(), that.normalizedTranslationExpressions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalizedExpression(), normalizedTranslationExpressions());
    }
}
