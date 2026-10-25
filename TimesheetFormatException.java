/**
 * Контролируемая ошибка разбора табеля.
 * position = 0 означает ошибку структуры строки (пустая строка, не 5 значений),
 * position = 1..5 - номер (с единицы) некорректного значения.
 */
public class TimesheetFormatException extends IllegalArgumentException {

    private final int position;

    public TimesheetFormatException(String message, int position) {
        super(message);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
