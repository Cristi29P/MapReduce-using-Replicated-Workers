import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public final class MapResult {
    private final File document;
    private final ArrayList<String> longestWords;
    private final HashMap<Integer, Integer> lengthFrequency;
    private final int inputFilePos;

    public MapResult(File document, ArrayList<String> longestWords,
                     HashMap<Integer, Integer> lengthFrequency, int inputFilePos) {
        this.document = document;
        this.longestWords = longestWords;
        this.lengthFrequency = lengthFrequency;
        this.inputFilePos = inputFilePos;
    }

    public File document() {
        return document;
    }

    public ArrayList<String> longestWords() {
        return longestWords;
    }

    public HashMap<Integer, Integer> lengthFrequency() {
        return lengthFrequency;
    }

    public int inputFilePos() {
        return inputFilePos;
    }
}
