package com.manoj.simcheck.service;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TokenizerService {

    // Language keywords/types that should NOT be normalized — they reflect
    // actual code structure, not an arbitrary naming choice by the author.
    private static final Set<String> KEYWORDS = Set.of(
        "public", "private", "protected", "class", "interface", "enum",
        "extends", "implements", "static", "final", "abstract", "void",
        "int", "long", "double", "float", "boolean", "char", "byte", "short",
        "string", "return", "if", "else", "for", "while", "do", "switch",
        "case", "break", "continue", "new", "this", "super", "import",
        "package", "try", "catch", "finally", "throw", "throws", "true",
        "false", "null", "instanceof", "synchronized", "volatile",
        "transient", "native", "const", "let", "var", "function", "def",
        "self", "elif", "from", "as", "with", "lambda", "yield",
        "print", "println", "system", "out"
    );

    /** Returns the raw, original tokens (lowercased, comments stripped for code). */
    public List<String> tokenize(String content, String mode) {
        if (content == null) return new ArrayList<>();
        String cleaned = "CODE".equalsIgnoreCase(mode) ? stripCodeComments(content) : content;
        cleaned = cleaned.toLowerCase();
        String[] rawTokens = cleaned.split("[^a-zA-Z0-9_]+");
        List<String> tokens = new ArrayList<>();
        for (String t : rawTokens) {
            if (!t.isBlank()) tokens.add(t);
        }
        return tokens;
    }

    /**
     * Produces the token list actually used for matching. For code mode,
     * every identifier (variable/method/class name) is replaced with a
     * positional placeholder (id1, id2, ...) assigned in first-seen order,
     * so two structurally identical methods match even if every name was
     * changed — the most common way plagiarized code gets disguised.
     * Text mode is returned unchanged, since exact wording matters there.
     *
     * Note: identifiers are mapped per-document, not per-scope, so the same
     * name in two different methods maps to the same placeholder. A known
     * simplification, fine for an MVP.
     */
    public List<String> toMatchTokens(List<String> originalTokens, String mode) {
        if (!"CODE".equalsIgnoreCase(mode)) {
            return originalTokens;
        }
        Map<String, String> idMap = new HashMap<>();
        int[] counter = {0};
        List<String> normalized = new ArrayList<>();
        for (String token : originalTokens) {
            if (KEYWORDS.contains(token) || isNumeric(token)) {
                normalized.add(token);
            } else {
                String placeholder = idMap.computeIfAbsent(token, k -> "id" + (++counter[0]));
                normalized.add(placeholder);
            }
        }
        return normalized;
    }

    private boolean isNumeric(String token) {
        return !token.isEmpty() && token.chars().allMatch(Character::isDigit);
    }

    private String stripCodeComments(String code) {
        String noBlockComments = code.replaceAll("(?s)/\\*.*?\\*/", " ");
        String noLineComments = noBlockComments.replaceAll("//.*", " ");
        String noHashComments = noLineComments.replaceAll("#.*", " ");
        return noHashComments;
    }
}