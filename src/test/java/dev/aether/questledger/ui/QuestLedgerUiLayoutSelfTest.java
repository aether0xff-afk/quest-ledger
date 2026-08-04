package dev.aether.questledger.ui;

import java.util.List;
import java.util.function.ToIntFunction;

public final class QuestLedgerUiLayoutSelfTest {
    private static final ToIntFunction<String> WIDTH = value -> value.codePointCount(0, value.length()) * 6;

    public static void main(String[] args) {
        validatesCommonGuiSizes();
        truncatesWithoutOverflow();
        wrapsLongKoreanAndEnglishText();
        sizesButtonsWithinLimits();
        System.out.println("Quest Ledger UI layout self-test passed (4 groups, 7 resolutions).");
    }

    private static void validatesCommonGuiSizes() {
        int[][] sizes = {
                {320, 240},
                {360, 270},
                {426, 240},
                {640, 360},
                {854, 480},
                {1280, 720},
                {1920, 1080}
        };
        for (int[] size : sizes) {
            QuestLedgerUiLayout.Frame frame = QuestLedgerUiLayout.frame(size[0], size[1]);
            require(frame.left() >= 0, "Frame left escaped at " + label(size));
            require(frame.top() >= 0, "Frame top escaped at " + label(size));
            require(frame.right() <= size[0], "Frame right escaped at " + label(size));
            require(frame.bottom() <= size[1], "Frame bottom escaped at " + label(size));

            QuestLedgerUiLayout.Editor editor = QuestLedgerUiLayout.editor(frame);
            require(editor.fieldLeft() >= frame.left(), "Editor field starts outside frame");
            require(editor.fieldLeft() + editor.fieldWidth() <= frame.right(),
                    "Editor field ends outside frame at " + label(size));
            require(editor.contentTop() < editor.contentBottom(), "Editor content collapsed");
            require(editor.rowStep() >= 22, "Editor row became unusably short");
            int visibleRows = frame.tiny() ? 5 : 6;
            int lastWidgetBottom = editor.contentTop()
                    + editor.rowStep() * (visibleRows - 1)
                    + 20;
            require(lastWidgetBottom + 15 <= editor.contentBottom(),
                    "Editor widgets collide with status area at " + label(size));

            QuestLedgerUiLayout.ListLayout list = QuestLedgerUiLayout.list(frame);
            require(list.questsPerPage() >= 2 && list.questsPerPage() <= 6,
                    "Unexpected quest page size");
            if (frame.tiny()) {
                require(list.questsPerPage() >= 3,
                        "Compact list wastes space at " + label(size));
            }
            int used = list.questsPerPage() * list.cardHeight()
                    + (list.questsPerPage() - 1) * list.gap();
            require(used <= list.contentBottom() - list.contentTop(),
                    "Quest cards overflow content at " + label(size));
        }
    }

    private static void truncatesWithoutOverflow() {
        String original = "아주 길어서 버튼과 카드 밖으로 절대 삐져나오면 안 되는 퀘스트 제목";
        String fitted = QuestLedgerUiLayout.ellipsize(WIDTH, original, 120);
        require(WIDTH.applyAsInt(fitted) <= 120, "Ellipsized text still overflows");
        require(fitted.endsWith("…"), "Ellipsized text lacks visual indication");
        require(QuestLedgerUiLayout.ellipsize(WIDTH, "short", 120).equals("short"),
                "Short text was modified");
    }

    private static void wrapsLongKoreanAndEnglishText() {
        String text = "금 공장을 완성하고 gold ingot sixty four items를 보유하면 완료되는 혼합 퀘스트";
        List<String> lines = QuestLedgerUiLayout.wrap(WIDTH, text, 108, 3);
        require(!lines.isEmpty() && lines.size() <= 3, "Unexpected wrap line count");
        for (String line : lines) {
            require(WIDTH.applyAsInt(line) <= 108, "Wrapped line overflows: " + line);
        }
    }

    private static void sizesButtonsWithinLimits() {
        int shortButton = QuestLedgerUiLayout.buttonWidth(WIDTH, "뒤로", 68, 120);
        int naturalButton = QuestLedgerUiLayout.buttonWidth(
                WIDTH,
                "완료 확인을 진행합니다",
                68,
                120
        );
        int cappedButton = QuestLedgerUiLayout.buttonWidth(
                WIDTH,
                "완료 확인을 진행하고 퀘스트를 영구적으로 저장합니다",
                68,
                120
        );
        require(shortButton >= 68 && shortButton <= 120, "Short button escaped limits");
        require(naturalButton >= 68 && naturalButton <= 120, "Natural button escaped limits");
        require(cappedButton == 120, "Overlong button was not capped");
    }

    private static String label(int[] size) {
        return size[0] + "x" + size[1];
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
