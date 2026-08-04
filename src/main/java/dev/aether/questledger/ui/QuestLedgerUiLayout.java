package dev.aether.questledger.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * Pure layout and text-fitting helpers shared by every Quest Ledger screen.
 * Keeping this class independent from Minecraft makes the critical overflow rules testable.
 */
public final class QuestLedgerUiLayout {
    private QuestLedgerUiLayout() {
    }

    public static Frame frame(int screenWidth, int screenHeight) {
        int margin = screenWidth < 560 || screenHeight < 360 ? 10 : 18;
        int availableWidth = Math.max(1, screenWidth - margin * 2);
        int availableHeight = Math.max(1, screenHeight - margin * 2);
        int width = Math.min(720, availableWidth);
        int height = Math.min(440, availableHeight);
        int left = (screenWidth - width) / 2;
        int top = (screenHeight - height) / 2;
        boolean compact = width < 540 || height < 340;
        boolean tiny = width < 420 || height < 285;
        return new Frame(left, top, width, height, compact, tiny);
    }

    public static Editor editor(Frame frame) {
        int padding = frame.compact() ? 14 : 20;
        int headerHeight = frame.tiny() ? 54 : 62;
        int footerHeight = frame.tiny() ? 38 : 44;
        int contentTop = frame.top() + headerHeight;
        int contentBottom = frame.bottom() - footerHeight;
        int labelWidth = clamp(frame.width() / 4, 76, 132);
        int gap = frame.compact() ? 8 : 12;
        int fieldLeft = frame.left() + padding + labelWidth + gap;
        int fieldWidth = frame.right() - padding - fieldLeft;
        int availableHeight = Math.max(120, contentBottom - contentTop);
        int rowStep = clamp(availableHeight / 6, 22, 36);
        return new Editor(
                padding,
                headerHeight,
                footerHeight,
                contentTop,
                contentBottom,
                labelWidth,
                fieldLeft,
                Math.max(96, fieldWidth),
                rowStep
        );
    }

    public static ListLayout list(Frame frame) {
        int padding = frame.compact() ? 12 : 18;
        int headerHeight = frame.tiny() ? 50 : 58;
        int footerHeight = frame.tiny() ? 38 : 44;
        int contentTop = frame.top() + headerHeight;
        int contentBottom = frame.bottom() - footerHeight;
        int available = Math.max(80, contentBottom - contentTop);
        int perPage = clamp(available / 50, 2, 6);
        int gap = frame.tiny() ? 4 : 6;
        int cardHeight = Math.max(36, (available - Math.max(0, perPage - 1) * gap) / perPage);
        return new ListLayout(padding, headerHeight, footerHeight, contentTop, contentBottom,
                perPage, cardHeight, gap);
    }

    public static int buttonWidth(ToIntFunction<String> width, String label, int minimum, int maximum) {
        Objects.requireNonNull(width, "width");
        return clamp(width.applyAsInt(label) + 24, minimum, maximum);
    }

    public static String ellipsize(ToIntFunction<String> width, String text, int maximumWidth) {
        Objects.requireNonNull(width, "width");
        String value = text == null ? "" : text;
        if (maximumWidth <= 0) {
            return "";
        }
        if (width.applyAsInt(value) <= maximumWidth) {
            return value;
        }
        String suffix = "…";
        if (width.applyAsInt(suffix) > maximumWidth) {
            return "";
        }
        int low = 0;
        int high = value.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            String candidate = value.substring(0, middle).stripTrailing() + suffix;
            if (width.applyAsInt(candidate) <= maximumWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return value.substring(0, low).stripTrailing() + suffix;
    }

    public static List<String> wrap(
            ToIntFunction<String> width,
            String text,
            int maximumWidth,
            int maximumLines
    ) {
        Objects.requireNonNull(width, "width");
        if (maximumLines <= 0 || maximumWidth <= 0 || text == null || text.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        String remaining = text.strip();
        while (!remaining.isEmpty() && result.size() < maximumLines) {
            if (width.applyAsInt(remaining) <= maximumWidth) {
                result.add(remaining);
                remaining = "";
                break;
            }

            int cut = bestCut(width, remaining, maximumWidth);
            int whitespace = lastWhitespace(remaining, cut);
            if (whitespace > 0) {
                cut = whitespace;
            }
            if (cut <= 0) {
                cut = 1;
            }
            String line = remaining.substring(0, cut).strip();
            if (line.isEmpty()) {
                line = remaining.substring(0, Math.min(1, remaining.length()));
                cut = line.length();
            }
            result.add(line);
            remaining = remaining.substring(cut).stripLeading();
        }

        if (!remaining.isEmpty() && !result.isEmpty()) {
            int last = result.size() - 1;
            result.set(last, ellipsize(width, result.get(last) + " " + remaining, maximumWidth));
        }
        return List.copyOf(result);
    }

    private static int bestCut(ToIntFunction<String> width, String text, int maximumWidth) {
        int low = 0;
        int high = text.length();
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (width.applyAsInt(text.substring(0, middle)) <= maximumWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    private static int lastWhitespace(String text, int before) {
        for (int index = Math.min(before - 1, text.length() - 1); index >= 0; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    public static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Frame(int left, int top, int width, int height, boolean compact, boolean tiny) {
        public int right() {
            return left + width;
        }

        public int bottom() {
            return top + height;
        }
    }

    public record Editor(
            int padding,
            int headerHeight,
            int footerHeight,
            int contentTop,
            int contentBottom,
            int labelWidth,
            int fieldLeft,
            int fieldWidth,
            int rowStep
    ) {
    }

    public record ListLayout(
            int padding,
            int headerHeight,
            int footerHeight,
            int contentTop,
            int contentBottom,
            int questsPerPage,
            int cardHeight,
            int gap
    ) {
    }
}
