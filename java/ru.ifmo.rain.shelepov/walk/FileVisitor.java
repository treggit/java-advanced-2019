package ru.ifmo.rain.shelepov.walk;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

public class FileVisitor extends SimpleFileVisitor<Path> {

    final private int BUFFER_SIZE = 4096;
    final private int FNV_PRIME = 0x01000193;
    final private int INITIAL = 0x811c9dc5;
    private Writer writer;

    public FileVisitor(Writer writer) {
        this.writer = writer;
    }

    private int getFNVHash(Path file) {
        try (InputStream reader = Files.newInputStream(file)) {
            int hash = INITIAL;
            byte[] buf = new byte[BUFFER_SIZE];
            int len = 0;
            while ((len = reader.read(buf)) > 0) {
                for (int i = 0; i < len; i++) {
                    hash = (hash * FNV_PRIME) ^ (buf[i] & 255);
                }
            }
            return hash;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
        writer.write(String.format("%08x", getFNVHash(file)) + " " + file + '\n');
        return CONTINUE;
    }

}
