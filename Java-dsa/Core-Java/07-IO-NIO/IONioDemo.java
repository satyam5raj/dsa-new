/*
 * ============================================================================
 *  TOPIC 7 : JAVA I/O and NIO BASICS
 * ============================================================================
 *
 *  Run:  java IONioDemo.java
 *  (All files are created in a temp directory and deleted at the end.)
 *
 *  CLASSIC I/O  (java.io, Java 1.0)  - STREAM oriented, BLOCKING
 *  -------------------------------------------------------------
 *   BYTE streams (raw binary: images, any file)
 *       InputStream  -> FileInputStream, BufferedInputStream, ObjectInputStream
 *       OutputStream -> FileOutputStream, BufferedOutputStream, ObjectOutputStream
 *   CHARACTER streams (text, handles encoding)
 *       Reader -> FileReader, BufferedReader, InputStreamReader
 *       Writer -> FileWriter, BufferedWriter, PrintWriter
 *
 *   DECORATOR PATTERN: you WRAP streams to add features:
 *       new BufferedReader(new InputStreamReader(new FileInputStream(f), UTF_8))
 *         ^ adds buffering    ^ bytes -> chars         ^ reads raw bytes
 *
 *   WHY BUFFER? Every unbuffered read() can be a system call (slow). A buffer
 *   reads a big chunk (8 KB) once, then serves small reads from memory.
 *
 *  NIO / NIO.2  (java.nio, Java 1.4 / Java 7)
 *  ------------------------------------------
 *   - Path / Paths / Files  -> modern, simple file API (USE THIS for files)
 *   - Buffer + Channel      -> block-oriented I/O (ByteBuffer, FileChannel)
 *   - Selector              -> one thread can watch MANY channels (non-blocking
 *                              network I/O; the basis of Netty, Tomcat NIO, etc.)
 *
 *   I/O vs NIO
 *     java.io  : stream-oriented, blocking, one thread per connection
 *     java.nio : buffer-oriented, can be non-blocking, selectors multiplex
 *                many connections on few threads
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class IONioDemo {

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("io-demo");   // sandbox for this demo
        try {
            byteStreams(dir);
            characterStreams(dir);
            nioFiles(dir);
            directoryOperations(dir);
            buffersAndChannels(dir);
            readingConsoleInput();
        } finally {
            deleteRecursively(dir);
            System.out.println("\n(temp directory cleaned up)");
        }
    }

    // =================================================================
    // 1. Byte streams - copy a binary file
    // =================================================================
    static void byteStreams(Path dir) throws IOException {
        System.out.println("===== 1. BYTE STREAMS =====");
        File src = dir.resolve("data.bin").toFile();
        File dst = dir.resolve("copy.bin").toFile();

        // Write raw bytes
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(src))) {
            for (int i = 0; i < 1000; i++) out.write(i % 256);   // write() takes an int, writes the low byte
        }

        // Copy with a manual buffer (shows how it works under the hood)
        try (InputStream in = new BufferedInputStream(new FileInputStream(src));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            byte[] buffer = new byte[256];
            int bytesRead;
            // read() returns the number of bytes read, or -1 at END OF FILE
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);                 // write only what was read
            }
        }
        System.out.println("copied " + dst.length() + " bytes");

        // Shortcut (Java 9): in.transferTo(out)
    }

    // =================================================================
    // 2. Character streams - read/write text line by line
    //    ALWAYS specify the charset (UTF-8) to avoid platform-dependent bugs.
    // =================================================================
    static void characterStreams(Path dir) throws IOException {
        System.out.println("\n===== 2. CHARACTER STREAMS =====");
        File file = dir.resolve("notes.txt").toFile();

        // PrintWriter gives println/printf on top of a Writer
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)))) {
            pw.println("line 1: hello");
            pw.printf("line 2: %d + %d = %d%n", 2, 3, 5);
            pw.println("line 3: नमस्ते (unicode works with UTF-8)");
        }

        // BufferedReader.readLine() returns null at end of file
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int n = 0;
            while ((line = br.readLine()) != null) {
                System.out.println("read -> " + line);
                n++;
            }
            System.out.println("total lines = " + n);
        }

        // Append mode: second constructor arg 'true'
        try (FileWriter fw = new FileWriter(file, StandardCharsets.UTF_8, true)) {
            fw.write("line 4: appended\n");
        }
        System.out.println("after append, lines = " + Files.readAllLines(file.toPath()).size());
    }

    // =================================================================
    // 3. NIO.2 Files & Path - the modern, concise way (prefer this!)
    // =================================================================
    static void nioFiles(Path dir) throws IOException {
        System.out.println("\n===== 3. NIO.2 Files & Path =====");
        Path file = dir.resolve("modern.txt");               // join paths safely
        System.out.println("fileName = " + file.getFileName() + ", parent exists = " + Files.exists(file.getParent()));

        // Write: whole content in one call
        Files.writeString(file, "alpha\nbeta\ngamma\n");     // Java 11
        Files.write(file, List.of("delta"), StandardOpenOption.APPEND);

        // Read: whole file / all lines / lazily as a stream
        String content = Files.readString(file);
        List<String> lines = Files.readAllLines(file);
        System.out.println("readString length = " + content.length() + ", lines = " + lines);

        // Files.lines is LAZY (good for huge files) and MUST be closed -> try-with-resources
        try (Stream<String> stream = Files.lines(file)) {
            long count = stream.filter(l -> l.contains("a")).count();
            System.out.println("lines containing 'a' = " + count);
        }

        // Metadata
        System.out.println("size = " + Files.size(file) + " bytes, isRegularFile = " + Files.isRegularFile(file));

        // Copy / move / delete
        Path copy = dir.resolve("modern-copy.txt");
        Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
        Path moved = Files.move(copy, dir.resolve("moved.txt"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("moved exists = " + Files.exists(moved) + ", old copy exists = " + Files.exists(copy));
        Files.deleteIfExists(moved);

        // BufferedReader from Files (handy for large files line by line)
        try (BufferedReader br = Files.newBufferedReader(file)) {
            System.out.println("first line via newBufferedReader = " + br.readLine());
        }
    }

    // =================================================================
    // 4. Directories: create, list, walk
    // =================================================================
    static void directoryOperations(Path dir) throws IOException {
        System.out.println("\n===== 4. DIRECTORIES =====");
        Path nested = dir.resolve("a/b/c");
        Files.createDirectories(nested);                     // like mkdir -p
        Files.writeString(nested.resolve("deep.txt"), "deep");
        Files.writeString(dir.resolve("a/top.java"), "class X {}");

        // list: one level only
        try (Stream<Path> s = Files.list(dir)) {
            System.out.println("top level = " + s.map(p -> p.getFileName().toString()).sorted().toList());
        }
        // walk: recursive
        try (Stream<Path> s = Files.walk(dir)) {
            List<String> files = s.filter(Files::isRegularFile)
                    .map(p -> dir.relativize(p).toString())
                    .sorted()
                    .toList();
            System.out.println("all files = " + files);
        }
        // find by pattern
        try (Stream<Path> s = Files.find(dir, 10, (p, attrs) -> p.toString().endsWith(".java"))) {
            System.out.println("java files = " + s.map(p -> p.getFileName().toString()).toList());
        }
    }

    // =================================================================
    // 5. Buffers & Channels (core NIO)
    //    ByteBuffer has: capacity (fixed size), position (next index),
    //    limit (how far you may read/write).
    //      write mode: put() advances position
    //      flip():     limit = position, position = 0   -> switch to READ mode
    //      clear():    position = 0, limit = capacity   -> ready to write again
    // =================================================================
    static void buffersAndChannels(Path dir) throws IOException {
        System.out.println("\n===== 5. BUFFERS & CHANNELS =====");
        ByteBuffer buf = ByteBuffer.allocate(16);
        printBuffer("new", buf);
        buf.put("hi!".getBytes(StandardCharsets.UTF_8));
        printBuffer("after put", buf);
        buf.flip();
        printBuffer("after flip", buf);
        byte[] out = new byte[buf.remaining()];
        buf.get(out);
        System.out.println("read back = " + new String(out, StandardCharsets.UTF_8));
        buf.clear();
        printBuffer("after clear", buf);

        // FileChannel: write and read via buffers
        Path file = dir.resolve("channel.txt");
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            ch.write(ByteBuffer.wrap("written through a FileChannel".getBytes(StandardCharsets.UTF_8)));
        }
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            ByteBuffer readBuf = ByteBuffer.allocate((int) ch.size());
            ch.read(readBuf);
            readBuf.flip();
            System.out.println("channel read = " + StandardCharsets.UTF_8.decode(readBuf));
        }
        // allocateDirect() = off-heap memory: faster for I/O, costlier to create.
        // FileChannel.transferTo() can use zero-copy (OS sends file bytes directly).
    }

    static void printBuffer(String label, ByteBuffer b) {
        System.out.printf("%-11s position=%d limit=%d capacity=%d%n", label, b.position(), b.limit(), b.capacity());
    }

    // =================================================================
    // 6. Reading input from the console (for coding problems)
    //    Scanner: easy but SLOW for big inputs (regex parsing).
    //    BufferedReader + StringTokenizer / split: FAST (competitive programming).
    //    Here we simulate System.in with a string so the demo runs on its own.
    // =================================================================
    static void readingConsoleInput() throws IOException {
        System.out.println("\n===== 6. FAST INPUT =====");
        String fakeInput = "3\n10 20 30\n";

        Scanner sc = new Scanner(new ByteArrayInputStream(fakeInput.getBytes()));
        int n = sc.nextInt();
        int sum = 0;
        for (int i = 0; i < n; i++) sum += sc.nextInt();
        System.out.println("Scanner sum = " + sum);

        BufferedReader br = new BufferedReader(new StringReader(fakeInput));
        int m = Integer.parseInt(br.readLine().trim());
        StringTokenizer st = new StringTokenizer(br.readLine());
        int total = 0;
        for (int i = 0; i < m; i++) total += Integer.parseInt(st.nextToken());
        System.out.println("BufferedReader sum = " + total);
        // Real usage: new BufferedReader(new InputStreamReader(System.in))
    }

    // Delete children before parents: sort paths in reverse (deepest first)
    static void deleteRecursively(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
