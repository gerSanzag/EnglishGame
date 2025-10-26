package com.englishgame.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
}


