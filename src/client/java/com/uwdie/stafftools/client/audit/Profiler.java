package com.uwdie.stafftools.client.audit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight sampling profiler for the mod's hot paths.
 *
 * <p>Call sites wrap a piece of work in {@link #measure(String, Runnable)}
 * or report a duration with {@link #record(String, long)}. The profiler
 * keeps per-section aggregates (calls, total, average, peak) plus a short
 * trace of the most recent samples, which is what the profiling panel
 * shows next to the audit journal.
 *
 * <p>Cost is a single {@link System#nanoTime()} pair per sample and a
 * map lookup, so it is safe to leave enabled on chat and render paths.
 * Nothing is allocated per sample beyond the trace entry.
 */
public final class Profiler {

    /** Sections tracked by the profiler. */
    public static final String CHAT_PROCESS = "chat.process";
    public static final String CHAT_WHOLE = "chat.whole";
    public static final String DETECT = "detect";
    public static final String MACRO_EXEC = "macro.exec";
    public static final String PUNISH_SEND = "punish.send";
    public static final String PUNISH_REPLY = "punish.reply";
    public static final String UI_RENDER = "ui.render";
    public static final String TOAST_RENDER = "toast.render";

    /** Samples kept in the trace list. */
    private static final int MAX_TRACE = 120;

    private static final Profiler INSTANCE = new Profiler();

    /** Aggregate of one named section. */
    public static final class Section {

        private final String name;
        private long calls;
        private long totalNanos;
        private long maxNanos;
        private long lastNanos;
        private long lastAt;

        private Section(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public long calls() {
            return calls;
        }

        public long totalNanos() {
            return totalNanos;
        }

        public long maxNanos() {
            return maxNanos;
        }

        public long lastNanos() {
            return lastNanos;
        }

        public long lastAt() {
            return lastAt;
        }

        /** Average duration in microseconds. */
        public double avgMicros() {
            return calls == 0 ? 0 : totalNanos / 1000.0 / calls;
        }

        public double totalMillis() {
            return totalNanos / 1_000_000.0;
        }

        public double maxMicros() {
            return maxNanos / 1000.0;
        }
    }

    /** One trace sample. */
    public record Sample(long at, String section, long nanos) {
    }

    private final Map<String, Section> sections =
            new LinkedHashMap<>();

    private final Deque<Sample> trace = new ArrayDeque<>();

    private Profiler() {
    }

    public static Profiler get() {
        return INSTANCE;
    }

    /** Times {@code work} and records it under {@code section}. */
    public void measure(String section, Runnable work) {

        long start = System.nanoTime();

        try {
            work.run();
        } finally {
            record(section, System.nanoTime() - start);
        }
    }

    /** Records a duration that was measured by the caller. */
    public void record(String section, long nanos) {

        if (section == null || nanos < 0) {
            return;
        }

        if (!enabled()) {
            return;
        }

        long now = System.currentTimeMillis();

        synchronized (sections) {

            Section s = sections.computeIfAbsent(
                    section,
                    Section::new
            );

            s.calls++;
            s.totalNanos += nanos;
            s.lastNanos = nanos;
            s.lastAt = now;

            if (nanos > s.maxNanos) {
                s.maxNanos = nanos;
            }

            trace.addLast(new Sample(now, section, nanos));

            while (trace.size() > MAX_TRACE) {
                trace.pollFirst();
            }
        }
    }

    /** Sections sorted by total time, heaviest first. */
    public List<Section> sections() {

        List<Section> out;

        synchronized (sections) {
            out = new ArrayList<>(sections.values());
        }

        out.sort(
                Comparator.comparingLong(Section::totalNanos)
                        .reversed()
        );

        return out;
    }

    /** Newest trace samples first. */
    public List<Sample> trace() {

        List<Sample> out = new ArrayList<>();

        synchronized (sections) {

            var it = trace.descendingIterator();

            while (it.hasNext()) {
                out.add(it.next());
            }
        }

        return out;
    }

    /** Total time of every section, in milliseconds. */
    public double totalMillis() {

        double sum = 0;

        synchronized (sections) {
            for (Section s : sections.values()) {
                sum += s.totalMillis();
            }
        }

        return sum;
    }

    public void reset() {

        synchronized (sections) {
            sections.clear();
            trace.clear();
        }
    }

    /** Null-safe switch: the profiler may run before the config exists. */
    private static boolean enabled() {

        try {
            var config = com.uwdie.stafftools.client.StafftoolsClient
                    .getConfig();

            return config == null || config.isProfilerEnabled();
        } catch (RuntimeException notReadyYet) {
            return true;
        }
    }
}
