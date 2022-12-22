import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        TableReader tableReader = new TableReader();
        Table table = null;
        try {
            table = tableReader.read(args[0]);
        } catch (CsvException e) {
            System.err.println("Не удалось распознать Csv");
        } catch (IOException e) {
            System.err.println("Не удалось считать файл");
        }
        ArrayList<String> optimalWay = MathMethods.getOptimalWay(table);
        System.out.println("Оптимальный путь:");
        System.out.print(optimalWay.get(0));
        for (int i = 1; i < optimalWay.size(); ++i) {
            System.out.print(" -> " + optimalWay.get(i));
        }
        long sum = 0;
        for (int i = 0; i < optimalWay.size() - 1; i++){
            sum = sum + table.get(optimalWay.get(i), optimalWay.get(i+1));
        }
        System.out.println();
        System.out.println("Длинна пути: " + sum);
    }
}