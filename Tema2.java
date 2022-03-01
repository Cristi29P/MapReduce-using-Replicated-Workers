import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class Tema2 {
    private static int chunkSize;
    private static final ArrayList<String> fileNames = new ArrayList<>();

    public static void readInput(File inputFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(inputFile);

        chunkSize = scanner.nextInt();
        scanner.nextInt();
        while (scanner.hasNext()) {
            fileNames.add(scanner.next());
        }
        scanner.close();
    }

    public static ArrayList<MapTask> generateForOneFile(File file, int chunkSize, int inputFilePos) {
        ArrayList<MapTask> info = new ArrayList<>();
        long fileSize = file.length();

        for (long i = 0; i < fileSize; i += chunkSize) {
            info.add(new MapTask(file, i, Math.min(chunkSize, fileSize - i), inputFilePos));
        }

        return info;
    }

    public static ArrayList<MapTask> generateMapTasks(int chunkSize, ArrayList<String> fileNames) {
        ArrayList<MapTask> tasks = new ArrayList<>();
        int i = 0;
        for (String fileName: fileNames) {
            tasks.addAll(generateForOneFile(new File(fileName), chunkSize, i));
            i++;
        }
        return tasks;
    }

    private static void combineMapResults(List<Future<MapResult>> mapResults, HashMap<String,
            ArrayList<MapResult>> reducePairing) throws InterruptedException, ExecutionException {
        for (Future<MapResult> result : mapResults) {
            if (result.get().longestWords() == null) {
                continue;
            }
            if (reducePairing.containsKey(result.get().document().getName())) {
                reducePairing.get(result.get().document().getName()).add(result.get());
            } else {
                ArrayList<MapResult> aux = new ArrayList<>();
                aux.add(result.get());
                reducePairing.put(result.get().document().getName(), aux);
            }
        }
    }

    private static void generateReduceTasks(HashMap<String, ArrayList<MapResult>> reducePairing,
                                            ArrayList<String> listOfKeys, ArrayList<ReduceTask> reduceTasks) {
        for (String fileName : listOfKeys) {
            reduceTasks.add(new ReduceTask(fileName, reducePairing.get(fileName),
                    reducePairing.get(fileName).get(0).inputFilePos()));
        }
    }

    private static void writeResults(String outputFile, List<Future<ReduceResult>> reduceResults)
            throws IOException, InterruptedException, ExecutionException {
        FileWriter writer = new FileWriter(outputFile);
        for (var aux : reduceResults) {
            writer.write(aux.get().toString());
        }
        writer.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: Tema2 <workers> <in_file> <out_file>");
            return;
        }
        // Get command line arguments
        int numberOfWorkers = Integer.parseInt(args[0]);
        String inputFile = args[1];
        String outputFile = args[2];

        // Get file input
        readInput(new File(inputFile));

        // Generate the array of tasks
        ArrayList<MapTask> mapTasks = generateMapTasks(chunkSize, fileNames);

        // Map phase
        ForkJoinPool fjpMap = new ForkJoinPool(numberOfWorkers);
        var mapResults = fjpMap.invokeAll(mapTasks);
        fjpMap.shutdown();

        // We combine the results from the map phase by their document name
        HashMap<String, ArrayList<MapResult>> reducePairing = new HashMap<>();
        combineMapResults(mapResults, reducePairing);

        // Create reduce tasks
        ArrayList<String> listOfKeys = new ArrayList<>(reducePairing.keySet());
        ArrayList<ReduceTask> reduceTasks = new ArrayList<>();
        generateReduceTasks(reducePairing, listOfKeys, reduceTasks);

        // Reduce phase
        ForkJoinPool fjpReduce = new ForkJoinPool(numberOfWorkers);
        var reduceResults = fjpReduce.invokeAll(reduceTasks);
        fjpReduce.shutdown();

        // Final sort and write to the output file
        reduceResults.sort((o1, o2) -> {
            int c = 0;
            try {
                c = Double.compare(o2.get().rank(), o1.get().rank());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (c == 0) {
                try {
                    c = Integer.compare(o1.get().inputFilePos(), o2.get().inputFilePos());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
            return c;
        });
        writeResults(outputFile, reduceResults);
    }
}
