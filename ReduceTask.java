import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class ReduceTask implements Callable<ReduceResult> {
    private final String document;
    private final ArrayList<MapResult> taskComponents;
    private final int inputFilePos;

    public ReduceTask(String document, ArrayList<MapResult> taskComponents, int inputFilePos) {
        this.document = document;
        this.taskComponents = taskComponents;
        this.inputFilePos = inputFilePos;
    }

    private int fibonacci(int n) {
        int a = 0, b = 1, c;
        if (n == 0)
            return a;
        for (int i = 2; i <= n; i++) {
            c = a + b;
            a = b;
            b = c;
        }
        return b;
    }

    private double calculateRank(HashMap<Integer, Integer> combinedDictionary) {
        double sum = 0;
        for (Map.Entry<Integer, Integer> entry : combinedDictionary.entrySet()) {
            sum += fibonacci(entry.getKey() + 1) * entry.getValue();
        }
        int sumValues = combinedDictionary.values().stream().mapToInt(aux -> aux).sum();
        return sum / sumValues;
    }
    @Override
    public ReduceResult call() throws Exception {
        HashMap<Integer, Integer> combinedDictionary = new HashMap<>();

        // Combining dictionaries content into a single one for each individual document
        for (MapResult taskComponent : taskComponents) {
            if (combinedDictionary.isEmpty()) {
                combinedDictionary.putAll(taskComponent.lengthFrequency());
            } else {
                for (Map.Entry<Integer, Integer> entry : taskComponent.lengthFrequency().entrySet()) {
                    if (combinedDictionary.putIfAbsent(entry.getKey(), entry.getValue()) != null) {
                        combinedDictionary.put(entry.getKey(), combinedDictionary.get(entry.getKey()) + entry.getValue());
                    }
                }
            }
        }

        // Calculate the requested metrics for each document
        double rank = calculateRank(combinedDictionary);
        int maximumLength = Collections.max(combinedDictionary.keySet());
        int frequency = combinedDictionary.get(maximumLength);

        return new ReduceResult(this.document, rank, maximumLength, frequency, inputFilePos);
    }
}
