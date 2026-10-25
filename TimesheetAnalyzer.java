import java.util.Locale;

/**
 * Обработка строки табеля одного сотрудника за рабочую неделю.
 * Формат строки: пять целых чисел через точку с запятой, например "8;8;9;7;10".
 * Stream API не используется.
 *
 * Все числовые правила собраны в константах ниже, чтобы изменение правила
 * (в том числе индивидуальный вариант) затрагивало одно место в одном классе.
 */
public class TimesheetAnalyzer {

    /** Сколько значений должно быть в строке. */
    private static final int DAYS_COUNT = 5;
    /** Нижняя граница часов за день (включительно). */
    private static final int MIN_HOURS = 0;
    /**
     * Верхняя граница часов за день (включительно).
     * Вариант 5 ("верхняя граница часов", параметр 30): вместо базовых 16 действует 30.
     */
    private static final int MAX_HOURS = 30;
    /** День считается переработкой, если часов БОЛЬШЕ этой нормы. */
    private static final int DAILY_NORM = 8;
    /** Недельная норма: переработка = max(0, сумма - WEEKLY_NORM). */
    private static final int WEEKLY_NORM = 40;

    /**
     * Строгий разбор строки в массив из пяти чисел.
     * Любая ошибка одного значения делает весь табель некорректным;
     * сообщение содержит номер позиции (с единицы).
     *
     * @throws TimesheetFormatException при любой ошибке структуры или значения
     */
    public int[] parseHours(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new TimesheetFormatException("Пустая строка: ожидается ровно "
                    + DAYS_COUNT + " значений через ';'", 0);
        }
        // limit = -1 сохраняет пустые элементы в конце ("8;8;8;8;8;" даёт 6 элементов)
        String[] parts = line.split(";", -1);
        if (parts.length != DAYS_COUNT) {
            throw new TimesheetFormatException("Ожидается ровно " + DAYS_COUNT
                    + " значений, получено " + parts.length, 0);
        }
        int[] hours = new int[DAYS_COUNT];
        for (int i = 0; i < parts.length; i++) {
            hours[i] = parseValue(parts[i], i + 1);
        }
        return hours;
    }

    /** Разбор и проверка одного значения; position - номер значения с единицы. */
    private int parseValue(String raw, int position) {
        String token = raw.trim();
        if (token.isEmpty()) {
            throw new TimesheetFormatException("Позиция " + position
                    + ": пустое значение", position);
        }
        if (token.charAt(0) == '+') {
            throw new TimesheetFormatException("Позиция " + position + ": значение '"
                    + token + "' не является целым числом", position);
        }
        int value;
        try {
            value = Integer.parseInt(token);
        } catch (NumberFormatException e) {
            throw new TimesheetFormatException("Позиция " + position + ": значение '"
                    + token + "' не является целым числом", position);
        }
        if (value < MIN_HOURS || value > MAX_HOURS) {
            throw new TimesheetFormatException("Позиция " + position + ": значение "
                    + value + " вне диапазона " + MIN_HOURS + ".." + MAX_HOURS, position);
        }
        return value;
    }

    /** Суммарное время за неделю. Цикл строго i < hours.length. */
    public int total(int[] hours) {
        int sum = 0;
        for (int i = 0; i < hours.length; i++) {
            sum += hours[i];
        }
        return sum;
    }

    /** Среднее время в день. Для пустого массива возвращает 0. */
    public double average(int[] hours) {
        if (hours.length == 0) {
            return 0.0;
        }
        return (double) total(hours) / hours.length;
    }

    /** Количество дней, где указано БОЛЕЕ DAILY_NORM часов. */
    public int overtimeDays(int[] hours) {
        int count = 0;
        for (int i = 0; i < hours.length; i++) {
            if (hours[i] > DAILY_NORM) {
                count++;
            }
        }
        return count;
    }

    /** Недельная переработка: max(0, сумма - WEEKLY_NORM). */
    public int weeklyOvertime(int[] hours) {
        return Math.max(0, total(hours) - WEEKLY_NORM);
    }

    /** Итоговый отчёт по строке табеля или сообщение об ошибке. */
    public String report(String line) {
        try {
            int[] hours = parseHours(line);
            return String.format(Locale.ROOT,
                    "Сумма: %d; среднее: %.2f; дней с переработкой: %d; недельная переработка: %d",
                    total(hours), average(hours), overtimeDays(hours), weeklyOvertime(hours));
        } catch (TimesheetFormatException e) {
            return "ОШИБКА: " + e.getMessage();
        }
    }
}
