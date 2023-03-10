import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public abstract class MathMethods {
    public static ArrayList<String> getOptimalWay(Table targetTable) {
        // Получаем ответ как набор вершин формата {из, в}
        ArrayList<Road> answerRoads = getOptimalWay(targetTable, 0L);
        if (answerRoads == null) {
            return null;
        }
        // Преобразуем ответ в вид списка, упорядоченного по порядку прохождения
        ArrayList<String> answer = new ArrayList<>(Collections.singletonList(targetTable.getDepartures().get(0)));
        while (answer.size() < targetTable.getDepartures().size() + 1) {
            for (Road answerRoad : answerRoads) {
                if (answerRoad.departure().equals(answer.get(answer.size() - 1))) {
                    answer.add(answerRoad.arrival());
                    break;
                }
            }
        }
        return answer;
    }

    private static ArrayList<Road> getOptimalWay(Table targetTable, Long sumOfConstants) {
        Table table = new Table(targetTable);
        System.out.println("\nИзначальная таблица:\n" + table);

        // Таблица после редукции
        Table tableFullReduced = reduction(table);
        Long previousSumOfConstant = sumOfConstants + tableFullReduced.get("_Минимум", "_Минимум");
        // Убираем столбцы содержащие данные о минимумах строк и столбцов
        Table tableWithoutMinimum = new Table(tableFullReduced);
        tableWithoutMinimum.removeDeparture("_Минимум");
        tableWithoutMinimum.removeArrival("_Минимум");
        Table tableWithBranchingEdges = getBranchEdges(tableWithoutMinimum);
        System.out.println("\nТаблица с рёбрами ветвления\n" + tableWithBranchingEdges);

        // Получаем из таблицы с рёбрами ветвления максимальные элементы. Если таких несколько, то нужно разобрать все
        ArrayList<Road> maxElementRoads = new ArrayList<>();
        Long maxElement = tableWithBranchingEdges.getMaxValue();
        for (String departure : targetTable.getDepartures()) {
            for (String arrival : targetTable.getArrivals()) {
                Long currentValue = tableWithBranchingEdges.get(departure, arrival);
                if (currentValue == null) {
                    continue;
                } else if (currentValue.equals(maxElement)) {
                    maxElementRoads.add(new Road(departure, arrival));
                }
            }
        }

        // Для каждого из максимальных элементов нужно проверить - не оптимальный ли это путь и, если нет, то либо
        // перейти на другую ветку, либо продолжить оптимизировать вглубь
        for (Road maxElementRoad : maxElementRoads) {
            System.out.println("Таблица до оптимизации:\n" + tableWithoutMinimum);

            // Нам нужно провести редукцию, так как в основной таблице могут появляться новые недопустимые элементы
            Table tableFullReducedWhenExcludeOrInclude = reduction(tableWithoutMinimum);
            tableFullReducedWhenExcludeOrInclude.removeDeparture("_Минимум");
            tableFullReducedWhenExcludeOrInclude.removeArrival("_Минимум");

            // Проверяем случай когда указанный путь НЕ будет использоваться
            Table tableWhenExclude = new Table(tableFullReducedWhenExcludeOrInclude);

            tableWhenExclude.set(maxElementRoad.departure(), maxElementRoad.arrival(), null);
            System.out.println("Таблица если исключаем ребро:\n" + tableWhenExclude);
            Long sumOfConstantsWhenExclude = calcEdge(tableWhenExclude);
            System.out.println("Сумма констант если исключаем ребро: " + sumOfConstantsWhenExclude);

            // Проверяем случай когда указанный путь будет использоваться
            Table tableWhenInclude = new Table(tableFullReducedWhenExcludeOrInclude);
            if (tableWhenInclude.getDepartures().size() == 2) {
                if (tableWhenInclude.get(maxElementRoad.departure(), maxElementRoad.arrival()) == null) {
                    continue;
                }
            }
            tableWhenInclude.set(maxElementRoad.arrival(), maxElementRoad.departure(), null);
            tableWhenInclude.removeDeparture(maxElementRoad.departure());
            tableWhenInclude.removeArrival(maxElementRoad.arrival());

            // Если на этом этапе получили таблицу 1 на 1, то нашли оптимальное решение
            if (tableWhenInclude.getDepartures().size() == 1) {
                Long lastValue = tableWhenInclude.get(tableWhenInclude.getDepartures().get(0), tableWhenInclude.getArrivals().get(0));
                if (lastValue != null) {
                    return new ArrayList<>(Arrays.asList(
                            new Road(tableWhenInclude.getDepartures().get(0), tableWhenInclude.getArrivals().get(0)),
                            new Road(maxElementRoad.departure(), maxElementRoad.arrival())));
                }
            }

            System.out.println("Таблица если включаем ребро:\n" + tableWhenInclude);
            Long sumOfConstantsWhenInclude = calcEdge(tableWhenInclude);
            System.out.println("Сумма констант если включаем ребро: " + sumOfConstantsWhenInclude);

            // Если от того, что мы будем использовать путь мы не проиграем, то продолжаем исследовать в глубину
            if (sumOfConstantsWhenInclude <= sumOfConstantsWhenExclude) {
                ArrayList<Road> answer = getOptimalWay(tableWhenInclude, previousSumOfConstant + sumOfConstantsWhenInclude);
                if (answer == null) {
                    tableWithoutMinimum.set(maxElementRoad.departure(), maxElementRoad.arrival(), null);
                } else if (!haveCycle(answer)) {
                    answer.add(maxElementRoad);
                    return answer;
                }
            }
        }

        return null;
    }

    private static boolean haveCycle(ArrayList<Road> roads) {
        if (roads.size() == 0) {
            return false;
        }
        ArrayList<Road> testRoads = new ArrayList<>();
        testRoads.add(roads.get(0));
        int counter = roads.size();
        while (testRoads.size() + 1 != roads.size()) {
            if (testRoads.get(0).departure().equals(testRoads.get(testRoads.size() - 1).arrival())) {
                return true;
            }
            for (Road road : roads) {
                if (road.departure().equals(testRoads.get(testRoads.size() - 1).arrival())) {
                    testRoads.add(road);
                }
            }
            --counter;
            if (counter == 0) {
                return false;
            }
        }
        return false;
    }

    // Приведение таблицы к виду, когда уменьшать её значения нельзя. По теореме, если вычесть из строки или столбца
    // одно значение, то ответ не поменяется
    private static Table reduction(Table targetTable) {
        Table table = new Table(targetTable);

        // Вычитаем значения в строках
        Table tableRowReduced = reduceRows(table);
        System.out.println("\nПосле приведения строк:\n" + tableRowReduced);

        // Вычитаем значения в столбцах
        Table tableColumnReduced = reduceColumns(tableRowReduced);
        System.out.println("\nПосле приведения столбцов:\n" + tableColumnReduced);

        // Добавляем значение в правый нижний угол - сумму всех вычтенных элементов
        Table tableFullReduced = setSumOfMinimumsToRightBottomCorner(tableColumnReduced);
        System.out.println("\nТаблица после приведений:\n" + tableFullReduced);

        return tableFullReduced;
    }

    // Вычисляем значение для нижнего правого угла - сумму всех вычтенных значений
    private static Long calcEdge(Table targetTable) {
        Table table = new Table(targetTable);
        Table tableRowReduced = reduceRows(table);
        Table tableColumnReduced = reduceColumns(tableRowReduced);
        Table tableFullReduced = setSumOfMinimumsToRightBottomCorner(tableColumnReduced);
        return tableFullReduced.get("_Минимум", "_Минимум");
    }

    // Вычитаем значения из строк
    private static Table reduceRows(Table targetTable) {
        Table table = new Table(targetTable);
        for (String departure : table.getDepartures()) {
            if (departure.startsWith("_")) {
                continue;
            }
            Long min = table.getMinInDeparture(departure);
            table.set(departure, "_Минимум", min);
            for (String arrival : table.getArrivals()) {
                if (arrival.startsWith("_")) {
                    continue;
                }
                table.dec(departure, arrival, min);
            }
        }
        return table;
    }

    // Вычитаем значения из столбцов
    private static Table reduceColumns(Table targetTable) {
        Table table = new Table(targetTable);
        for (String arrival : table.getArrivals()) {
            if (arrival.startsWith("_")) {
                continue;
            }
            Long min = table.getMinInArrival(arrival);
            table.set("_Минимум", arrival, min);
            for (String departure : table.getDepartures()) {
                if (departure.startsWith("_")) {
                    continue;
                }
                table.dec(departure, arrival, min);
            }
        }
        return table;
    }

    // Устанавливаем сумму вычтенных значений в правый нижний угол
    private static Table setSumOfMinimumsToRightBottomCorner(Table targetTable) {
        Table table = new Table(targetTable);
        long sumOfMinimums = 0;
        for (String departure : table.getDepartures()) {
            if (departure.startsWith("_")) {
                continue;
            }
            Long value = table.get(departure, "_Минимум");
            if (value != null) {
                sumOfMinimums += value;
            }
        }
        for (String arrival : table.getArrivals()) {
            if (arrival.startsWith("_")) {
                continue;
            }
            Long value = table.get("_Минимум", arrival);
            if (value != null) {
                sumOfMinimums += value;
            }
        }
        table.set("_Минимум", "_Минимум", sumOfMinimums);
        return table;
    }

    // Получение таблицы с рёбрами ветвления. Максимальное из значений будем использовать для продвижения в алгоритме.
    private static Table getBranchEdges(Table targetTable) {
        Table table = new Table(targetTable);
        table.removeAllElements();

        for (String departure : targetTable.getDepartures()) {
            for (String arrival : targetTable.getArrivals()) {
                Long value = targetTable.get(departure, arrival);
                if (value == null) {
                    continue;
                } else if (value.equals(0L)) {
                    Table tmpTable = new Table(targetTable);
                    tmpTable.set(departure, arrival, null);
                    table.set(departure, arrival,
                            tmpTable.getMinInDeparture(departure) + tmpTable.getMinInArrival(arrival));
                }
            }
        }

        return table;
    }
}