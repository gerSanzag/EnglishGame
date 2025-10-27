package com.englishgame.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a Spanish expression with its score and translations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "expression")
public class SpanishExpression {
    private String expression;
    private int score;
    private List<EnglishExpression> translations = new ArrayList<>();
}
