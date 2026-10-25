import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Запуск:
 *   java Main                       - обработать data/sample.csv
 *   java Main путь/к/файлу.csv      - обработать указанный файл (формат: id,hours,expected)
 *   java Main --line "8;8;9;7;10"   - обработать одну строку
 */
public class Main {

    public static void main(String[] args) throws IOException {
        TimesheetAnalyzer analyzer = new TimesheetAnalyzer();
        if (args.length == 2 && args[0].equals("--line")) {
            System.out.println(analyzer.report(args[1]));
            return;
        }
        String path = args.length == 1 ? args[0] : "data/sample.csv";
        List<String> lines = Files.readAllLines(Paths.get(path));
        for (int i = 1; i < lines.size(); i++) { // строка 0 - заголовок
            String[] cols = lines.get(i).split(",", -1);
            if (cols.length < 2) {
                continue;
            }
            System.out.println(cols[0] + ": " + analyzer.report(cols[1]));
        }
    }
}
