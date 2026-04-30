import java.util.stream.Stream;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.*;

// The main plagiarism detection program.
// You only need to change buildIndex() and findSimilarity().
public class Lab3 {
    public static void main(String[] args) {
        try {
            String directory;
            if (args.length == 0) {
                System.out.print("Name of directory to scan: ");
                System.out.flush();
                directory = new Scanner(System.in).nextLine();
            } else directory = args[0];
            Path[] paths = Files.list(Paths.get(directory)).toArray(Path[]::new);
            Arrays.sort(paths);

            // Stopwatches time how long each phase of the program
            // takes to execute.
            Stopwatch stopwatch = new Stopwatch();
            Stopwatch stopwatch2 = new Stopwatch();

            // Read all input files
            BST<Path, Ngram[]> files = readPaths(paths);
            stopwatch.finished("Reading all input files");

            // Build index of n-grams (not implemented yet)
            BST<Ngram, ArrayList<Path>> index = buildIndex(files);
            stopwatch.finished("Building n-gram index");

            // Compute similarity of all file pairs
            BST<PathPair, Integer> similarity = findSimilarity(files, index);
            stopwatch.finished("Computing similarity scores");

            // Find most similar file pairs, arranged in
            // decreasing order of similarity
            ArrayList<PathPair> mostSimilar = findMostSimilar(similarity);
            stopwatch.finished("Finding the most similar files");
            stopwatch2.finished("In total the program");

            // Print out some statistics
            System.out.println("\nBalance statistics:");
            System.out.println("  files: " + files.statistics());
            System.out.println("  index: " + index.statistics());
            System.out.println("  similarity: " + similarity.statistics());
            System.out.println("");

            // Print out the plagiarism report!
            System.out.println("Plagiarism report:");
            mostSimilar.stream().limit(50).forEach((PathPair pair) -> {
                System.out.printf("%5d similarity: %s\n", similarity.get(pair), pair);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Phase 1: Read in each file and chop it into n-grams.
    static BST<Path, Ngram[]> readPaths(Path[] paths) throws IOException {
        BST<Path, Ngram[]> files = new BST<>();
        for (Path path: paths) {
            String contents = new String(Files.readAllBytes(path));
            Ngram[] ngrams = Ngram.ngrams(contents, 5);
            // Remove duplicates from the ngrams list
            // Uses the Java 8 streams API - very handy Java feature
            // which we don't cover in the course. If you want to
            // learn about it, see e.g.
            // https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html#package.description
            // or https://stackify.com/streams-guide-java-8/
            ngrams = Arrays.stream(ngrams).distinct().toArray(Ngram[]::new);
            files.put(path, ngrams);
        }

        return files;
    }

    // Phase 2: build index of n-grams (Implemented)
    static BST<Ngram, ArrayList<Path>> buildIndex(BST<Path, Ngram[]> files) {
        BST<Ngram, ArrayList<Path>> index = new BST<>();
        // Build index of n-grams
        
        for(Path p: files.keys()){
            for(Ngram gram: files.get(p))
                if(index.contains(gram)){
                    index.get(gram).add(p);
                }
                else{
                    ArrayList<Path> paths = new ArrayList<Path>();
                    paths.add(p);
                    index.put(gram, paths);
                }
        }
        return index;
    }

    // Phase 3: Count how many n-grams each pair of files has in common.
    static BST<PathPair, Integer> findSimilarity(BST<Path, Ngram[]> files, BST<Ngram, ArrayList<Path>> index) {
        // Use index to make this loop much more efficient
        // N.B. Path is Java's class for representing filenames
        // PathPair represents a pair of Paths (see PathPair.java)

        BST<PathPair, Integer> similarity = new BST<>();
        
        // Loop through all the unique n-grams in our newly built index
        for (Ngram gram : index.keys()) {
            ArrayList<Path> sharedPaths = index.get(gram);

            // If an n-gram only appears in 1 file, it's not plagiarized. Skip it.
            if (sharedPaths.size() < 2) {
                continue;
            }

            // If it appears in multiple files, compare every possible pair of those files.
            for (int i = 0; i < sharedPaths.size(); i++) {
                for (int j = i + 1; j < sharedPaths.size(); j++) {
                    Path p1 = sharedPaths.get(i);
                    Path p2 = sharedPaths.get(j);

                    // We must enforce a consistent order so we don't count (FileA, FileB) 
                    // and (FileB, FileA) as two separate combinations.
                    PathPair pair;
                    if (p1.compareTo(p2) > 0) {
                        pair = new PathPair(p1, p2);
                    } else {
                        pair = new PathPair(p2, p1);
                    }

                    // Increment the similarity score for this pair of files
                    if (similarity.contains(pair)) {
                        similarity.put(pair, similarity.get(pair) + 1);
                    } else {
                        similarity.put(pair, 1); // First time these files share an n-gram
                    }
                }
            }
        }


        return similarity;
    }

    // Phase 4: find all pairs of files with more than 30 n-grams
    // in common, sorted in descending order of similarity.
    static ArrayList<PathPair> findMostSimilar(BST<PathPair, Integer> similarity) {
        // Find all pairs of files with more than 100 n-grams in common.
        ArrayList<PathPair> mostSimilar = new ArrayList<>();
        for (PathPair pair: similarity.keys()) {
            if (similarity.get(pair) < 30) continue;
            // Only consider each pair of files once - (a, b) and not
            // (b,a) - and also skip pairs consisting of the same file twice
            if (pair.path1.compareTo(pair.path2) <= 0) continue;

            mostSimilar.add(pair);
        }

        // Sort to have the most similar pairs first.
        Collections.sort(mostSimilar, Comparator.comparing((PathPair pair) -> similarity.get(pair)));
        Collections.reverse(mostSimilar);
        return mostSimilar;
    }
}
