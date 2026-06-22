package com.manoj.simcheck.service;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Core matching engine.
 *
 * Builds a rolling-hash fingerprint index of token-level k-grams for a document,
 * then finds matches between two documents by intersecting their fingerprint
 * indexes. The rolling hash means each shift after the first k-gram costs O(1)
 * instead of O(k), so indexing a document of n tokens is O(n) overall.
 */
@Component
public class RabinKarpMatcher {

    private static final long BASE = 131L;
    private static final long MOD = 1_000_000_007L;

    public static class KGramIndex {
        // hash -> list of starting token positions that produced it
        Map<Long, List<Integer>> hashToPositions = new HashMap<>();
        int totalKgrams;
    }

    public KGramIndex buildIndex(List<String> tokens, int k) {
        KGramIndex index = new KGramIndex();
        int n = tokens.size();
        if (n < k || k <= 0) {
            return index; // not enough tokens to form a single k-gram
        }

        // BASE^(k-1) mod M, needed to "remove" the outgoing token when rolling
        long highOrder = 1L;
        for (int i = 0; i < k - 1; i++) {
            highOrder = (highOrder * BASE) % MOD;
        }

        long hash = 0L;
        for (int i = 0; i < k; i++) {
            hash = (hash * BASE + tokenValue(tokens.get(i))) % MOD;
        }
        addPosition(index, hash, 0);

        for (int start = 1; start <= n - k; start++) {
            long outgoing = tokenValue(tokens.get(start - 1));
            long incoming = tokenValue(tokens.get(start + k - 1));
            hash = (hash - ((outgoing * highOrder) % MOD) + MOD) % MOD;
            hash = (hash * BASE + incoming) % MOD;
            addPosition(index, hash, start);
        }

        index.totalKgrams = n - k + 1;
        return index;
    }

    /**
     * Finds matching k-gram start positions between two documents by
     * intersecting their hash indexes, then verifies actual token equality
     * to rule out hash collisions before accepting a match.
     */
    public List<int[]> findMatches(List<String> tokens1, KGramIndex index1,
                                    List<String> tokens2, KGramIndex index2, int k) {
        List<int[]> matches = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : index1.hashToPositions.entrySet()) {
            long hash = entry.getKey();
            List<Integer> positions2 = index2.hashToPositions.get(hash);
            if (positions2 == null) continue;
            for (int pos1 : entry.getValue()) {
                for (int pos2 : positions2) {
                    if (tokensEqual(tokens1, pos1, tokens2, pos2, k)) {
                        matches.add(new int[]{pos1, pos1 + k, pos2, pos2 + k});
                    }
                }
            }
        }
        return matches;
    }

    // Treats the token's hashCode as an unsigned 32-bit value so we never
    // feed a negative number into the rolling-hash arithmetic.
    private long tokenValue(String token) {
        return token.hashCode() & 0xffffffffL;
    }

    private void addPosition(KGramIndex index, long hash, int position) {
        index.hashToPositions.computeIfAbsent(hash, x -> new ArrayList<>()).add(position);
    }

    private boolean tokensEqual(List<String> t1, int s1, List<String> t2, int s2, int k) {
        for (int i = 0; i < k; i++) {
            if (!t1.get(s1 + i).equals(t2.get(s2 + i))) return false;
        }
        return true;
    }

    /**
     * Counts distinct k-gram PATTERNS shared between the two documents (verified
     * against hash collisions), rather than counting positions. This is bounded
     * by min(uniqueKgrams1, uniqueKgrams2), which guarantees the Dice similarity
     * score computed from it can never exceed 100% — counting matched positions
     * in Doc 1 alone (the previous approach) had no such guarantee and could
     * overshoot 100% whenever the documents had different effective lengths.
     */
    public int countSharedKgrams(KGramIndex index1, KGramIndex index2,
                                  List<String> tokens1, List<String> tokens2, int k) {
        int count = 0;
        for (Map.Entry<Long, List<Integer>> entry : index1.hashToPositions.entrySet()) {
            long hash = entry.getKey();
            List<Integer> positions2 = index2.hashToPositions.get(hash);
            if (positions2 == null) continue;

            boolean verified = false;
            for (int pos1 : entry.getValue()) {
                for (int pos2 : positions2) {
                    if (tokensEqual(tokens1, pos1, tokens2, pos2, k)) {
                        verified = true;
                        break;
                    }
                }
                if (verified) break;
            }
            if (verified) count++;
        }
        return count;
    }
}