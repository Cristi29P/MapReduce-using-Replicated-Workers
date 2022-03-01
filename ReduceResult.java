import java.util.Locale;

public final class ReduceResult {
    private final String documentName;
    private final double rank;
    private final int longestWord;
    private final int frequency;
    private final int inputFilePos;

    public ReduceResult(String documentName, double rank, int longestWord, int frequency, int inputFilePos) {
        this.documentName = documentName;
        this.rank = rank;
        this.longestWord = longestWord;
        this.frequency = frequency;
        this.inputFilePos = inputFilePos;
    }

    public double rank() {
        return rank;
    }

    public int inputFilePos() {
        return inputFilePos;
    }

    @Override
    public String toString() {
        return documentName + ',' + String.format(Locale.US, "%.2f", rank) + ',' + longestWord + ',' + frequency + '\n';
    }
}
