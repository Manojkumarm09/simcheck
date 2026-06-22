package com.manoj.simcheck.util;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class IntervalMerger {

    public static class Range {
        public int start;
        public int end;
        public Range(int start, int end) { this.start = start; this.end = end; }
    }

    /**
     * Sorts ranges by start index and merges any that overlap or touch,
     * turning scattered k-gram hits into clean, contiguous highlighted blocks.
     * Classic merge-intervals: O(n log n) for the sort, O(n) for the merge pass.
     */
    public List<Range> merge(List<Range> ranges) {
        if (ranges.isEmpty()) return ranges;
        List<Range> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(r -> r.start));

        List<Range> merged = new ArrayList<>();
        Range current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            Range next = sorted.get(i);
            if (next.start <= current.end) {
                current = new Range(current.start, Math.max(current.end, next.end));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }
}
