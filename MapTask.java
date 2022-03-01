import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.Callable;

public class MapTask implements Callable<MapResult> {
    private final File document;
    private long offset;
    private long size;
    private final int inputFilePos;

    public MapTask(File document, long offset, long size, int inputFilePos) {
        this.document = document;
        this.offset = offset;
        this.size = size;
        this.inputFilePos = inputFilePos;
    }

    // Checks whether the fragment needs to be adjusted left-side/right-side or not
    private boolean adjustFragment(RandomAccessFile raFile, long offset) throws IOException {
        // Check if we are at the beginning of the document
        if (offset - 1 < 0) {
            return false;
        }

        // Check if the previous character was also a letter
        raFile.seek(offset - 1);
        char c1 = (char) raFile.readByte();

        // Check if we are at the end of the document
        if (raFile.getFilePointer() == raFile.length()) {
            return false;
        }

        // Check if the current character is a letter
        char c2 = (char) raFile.readByte();

        // If both characters are letters, then we need to adjust the fragment
        // Any other combination means we have a string delimiter and the fragment is correct
        return Character.isLetter(c1) && Character.isLetter(c2);
    }

    // Creates a new offset for the current fragment so that is follows the rule
    private long modifyOffset(RandomAccessFile raFile, long offset) throws IOException {
        long newOffset = offset;
        boolean eof = false;

        raFile.seek(offset);
        // While we still have letters, and we did not reach the end of file, move the pointer
        // to the right
        while (!eof && Character.isLetter((char) raFile.readByte())) {
            if (raFile.getFilePointer() == raFile.length()) {
                eof = true;
            }
            newOffset++;
        }

        return newOffset;
    }

    @Override
    public MapResult call() throws Exception {
        try {
            RandomAccessFile raFile = new RandomAccessFile(document, "r");

            long offsetCopy = offset; // used for adjustFragmentEnd
            long endOffset = offsetCopy + this.size;

            // Check if fragment start is correct
            if (adjustFragment(raFile, offset)) {
                offset = modifyOffset(raFile, offset);
            }
            // Check if fragment end is correct
            if (adjustFragment(raFile, offsetCopy + size)) {
                endOffset = modifyOffset(raFile, offsetCopy + size);
            }
            // Compute the fragment size
            this.size = endOffset - offset;
            // Go to the beginning of the fragment
            raFile.seek(offset);
            // Read [size] characters into the buffer
            byte[] chars = new byte[(int) size];
            raFile.read(chars);
            raFile.close();
            // Create a string with the characters in the buffer
            String test = new String(chars);
            // Eliminate any whitespace
            test = test.trim();

            if (test.isEmpty()) {
                return new MapResult(this.document, null, null, inputFilePos);
            }
            // Tokenize the string
            ArrayList<String> tokens;
            tokens = new ArrayList<>(Arrays.asList(test.split("[;:/?~.,><`\\]\\[{}()!@#$%^&\\-_+'=*|\\s\\t\\r\\n\"]", -1)));
            tokens.removeAll(List.of(""));
            // Get the longest word
            int longestWordLength =  Collections.max(tokens, Comparator.comparing(String::length)).length();

            // Insert the values requested into the hashmap
            HashMap<Integer, Integer> lengthFrequency = new HashMap<>();
            ArrayList<String> longestWords = new ArrayList<>();
            for (String token : tokens) {
                if (token.length() == longestWordLength) {
                    longestWords.add(token);
                }
                if (lengthFrequency.containsKey(token.length())) {
                    lengthFrequency.replace(token.length(), lengthFrequency.get(token.length()) + 1);
                } else {
                    lengthFrequency.put(token.length(), 1);
                }
            }
            return new MapResult(this.document, longestWords, lengthFrequency, inputFilePos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
