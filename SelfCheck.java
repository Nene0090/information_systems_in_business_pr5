import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Самопроверка без внешних библиотек. Код возврата 1, если хотя бы одна проверка не прошла.
 * Запуск: java SelfCheck [путь к sample.csv]
 */
public class SelfCheck {

    private static final TimesheetAnalyzer A = new TimesheetAnalyzer();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        // 1. Корректное типовое значение
        expectOk("1. типовое значение", "8;9;7;10;6", 40, 8.0, 2, 0);

        // 2. Нижняя граница: 0 включается
        expectOk("2. нижняя граница 0;0;0;0;0", "0;0;0;0;0", 0, 0.0, 0, 0);
        expectError("2a. значение -1 исключается (позиция 1)", "-1;8;8;8;8", 1);

        // 3. Верхняя граница (вариант 5: MAX_HOURS = 30): 30 включается, 31 исключается
        expectOk("3. верхняя граница 30 включается", "30;30;30;30;30", 150, 30.0, 5, 110);
        expectError("3a. значение 31 исключается (позиция 3)", "8;8;31;8;8", 3);
        // Значение 17 было бы ошибкой при базовой границе 16, но в варианте 5 оно допустимо
        expectOk("3b. значение 17 допустимо в варианте 5", "17;8;8;8;8", 49, 9.8, 1, 9);

        // 4. Пустое или отсутствующее значение
        expectError("4. пустая строка", "", 0);
        expectError("4a. строка из пробелов", "   ", 0);
        expectError("4b. null", null, 0);
        expectError("4c. пустое значение в середине (позиция 3)", "8;8;;8;8", 3);
        expectError("4d. хвостовая ';' даёт 6 значений", "8;8;8;8;8;", 0);

        // 5. Неверный формат
        expectError("5. буква вместо числа (позиция 2)", "8;abc;8;8;8", 2);
        expectError("5a. дробное число (позиция 4)", "8;8;8;7.5;8", 4);
        expectError("5b. знак '+' не допускается (позиция 1)", "+8;8;8;8;8", 1);
        expectError("5c. переполнение int (позиция 5)", "8;8;8;8;99999999999", 5);
        expectError("5d. мало значений", "8;8;8;8", 0);
        expectError("5e. много значений", "8;8;8;8;8;8", 0);

        // 6. Повтор и конфликт данных
        expectOk("6. одинаковые значения допустимы", "8;8;8;8;8", 40, 8.0, 0, 0);
        expectError("6a. несколько ошибок: сообщается первая (позиция 2)", "8;x;99;8;8", 2);

        // Границы правил переработки
        expectOk("7. ровно 8 ч - не переработка", "8;8;8;8;8", 40, 8.0, 0, 0);
        expectOk("7a. 9 ч - переработка", "9;8;8;8;8", 41, 8.2, 1, 1);
        expectOk("7b. сумма 39 - переработки нет", "8;8;8;8;7", 39, 7.8, 0, 0);
        expectOk("7c. сумма 40 - переработки нет", "8;8;8;8;8", 40, 8.0, 0, 0);

        // Метод не падает на массиве другой длины (защита от i <= length)
        check("8. total не выходит за границы массива", A.total(new int[] {1, 2, 3}) == 6);
        check("8a. average пустого массива = 0", A.average(new int[0]) == 0.0);

        checkSampleCsv(args.length == 1 ? args[0] : "data/sample.csv");

        System.out.println();
        System.out.println("Итого: пройдено " + passed + ", не пройдено " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void expectOk(String name, String line, int total, double avg, int days, int weekly) {
        try {
            int[] h = A.parseHours(line);
            boolean ok = A.total(h) == total
                    && Math.abs(A.average(h) - avg) < 1e-9
                    && A.overtimeDays(h) == days
                    && A.weeklyOvertime(h) == weekly;
            check(name, ok);
        } catch (RuntimeException e) {
            check(name + " (неожиданная ошибка: " + e.getMessage() + ")", false);
        }
    }

    private static void expectError(String name, String line, int position) {
        try {
            A.parseHours(line);
            check(name + " (ошибка не возникла)", false);
        } catch (TimesheetFormatException e) {
            boolean posInMessage = position == 0 || e.getMessage().startsWith("Позиция " + position + ":");
            check(name, e.getPosition() == position && posInMessage);
        }
    }

    private static void checkSampleCsv(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (int i = 1; i < lines.size(); i++) {
            String[] c = lines.get(i).split(",", -1);
            String name = "CSV " + c[0];
            String expected = c[2];
            String actual;
            try {
                int[] h = A.parseHours(c[1]);
                actual = String.format(Locale.ROOT, "OK|%d|%.2f|%d|%d",
                        A.total(h), A.average(h), A.overtimeDays(h), A.weeklyOvertime(h));
            } catch (TimesheetFormatException e) {
                actual = "ERROR|" + e.getPosition();
            }
            check(name + " (ожидалось " + expected + ", получено " + actual + ")", expected.equals(actual));
        }
    }

    private static void check(String name, boolean cond) {
        if (cond) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name);
        }
    }
}
