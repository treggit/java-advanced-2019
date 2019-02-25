package ru.ifmo.rain.shelepov.walk;


import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;



public class RecursiveWalk {

    static private Path inputPath = null;
    static private Path outputPath = null;

    private static void check(final String input, final String output) throws WalkException {

        try {
            inputPath = Paths.get(input);
            outputPath = Paths.get(output);
        } catch (InvalidPathException | NullPointerException e) {
            throw new WalkException("Input or output path is invalid: " + e.getMessage());
        }
        if (outputPath.getParent() != null) {
            try {
                Files.createDirectories(outputPath.getParent());
            } catch (IOException e) {
                throw new WalkException("Couldn't create output file: " + e.getMessage());
            }
        }
    }

    private static void process(String input, String output) throws WalkException {
        check(input, output);
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(input), Charset.forName("UTF-8"))) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(output))) {
                try {
                    String path;
                    FileVisitor visitor = new FileVisitor(writer);
                    while ((path = reader.readLine()) != null) {
                        try {
                            Files.walkFileTree(Paths.get(path), visitor);
                        } catch (Exception e) {
                            writer.write("00000000 " + path + "\n");
                        }
                    }
                } catch (IOException e) {
                    throw new WalkException(e.getMessage());
                }
            } catch (IOException e) {
                throw new WalkException("Couldn't handle output file: " + e.getMessage());
            }
        } catch (IOException e) {
            throw new WalkException("Couldn't handle input file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Two arguments expected: <input file> <output file>");
            return;
        }
        try {
            process(args[0], args[1]);
        } catch (WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
