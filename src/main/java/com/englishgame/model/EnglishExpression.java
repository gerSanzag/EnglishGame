package com.englishgame.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Entity representing an English expression with its score and translations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnglishExpression {
    private String expression;
    private int score;
    private List<SpanishExpression> translations = new ArrayList<>();

    private String normalizedExpression() {
        return expression == null ? "" : expression.trim().toLowerCase();
    }

    private List<String> normalizedTranslationExpressions() {
        if (translations == null) {
            return List.of();
        }
        return translations.stream()
                .map(SpanishExpression::getExpression)
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
        if (!(o instanceof EnglishExpression that)) {
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


