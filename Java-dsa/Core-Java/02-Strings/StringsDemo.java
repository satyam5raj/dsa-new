/*
 * ============================================================================
 *  TOPIC 2 : STRINGS - String Pool, Immutability, StringBuilder vs StringBuffer
 * ============================================================================
 *
 *  Run:  java StringsDemo.java
 *
 *  KEY FACTS
 *  ---------
 *  1. String is an OBJECT (class java.lang.String), not a primitive.
 *  2. String is IMMUTABLE: once created, its content can never change.
 *     Every "modifying" method (toUpperCase, replace, concat, substring...)
 *     returns a NEW String.
 *  3. Since Java 9, a String internally stores a byte[] plus a "coder" flag
 *     (LATIN1 = 1 byte/char, UTF16 = 2 bytes/char) -> "Compact Strings".
 *
 *  STRING POOL (String Constant Pool)
 *  ----------------------------------
 *  A special area in the HEAP where the JVM keeps ONE copy of each string
 *  LITERAL. When you write "hello" twice, both variables point to the SAME
 *  pooled object. This saves memory.
 *
 *      String a = "hello";            // goes to / reuses the pool
 *      String b = "hello";            // reuses the SAME pooled object -> a == b
 *      String c = new String("hello");// forces a NEW object on the heap -> a != c
 *      String d = c.intern();         // returns the pooled copy -> a == d
 *
 *  WHY IMMUTABLE? (classic interview question)
 *  -------------------------------------------
 *   - Pooling is only safe if no one can change a shared string.
 *   - Security: class names, file paths, URLs, DB credentials can't be
 *     altered after validation.
 *   - Thread-safe for free: no synchronization needed to share.
 *   - hashCode can be cached -> great as HashMap keys.
 *
 *  STRINGBUILDER vs STRINGBUFFER
 *  -----------------------------
 *                     String        StringBuilder       StringBuffer
 *   Mutable?          No            Yes                 Yes
 *   Thread-safe?      Yes (immut.)  No                  Yes (synchronized)
 *   Speed             slow concat   FASTEST             slower (locking)
 *   Use when          fixed text    building strings    legacy multi-thread code
 *                                   in one thread
 *  In practice: use StringBuilder. StringBuffer is rarely needed today.
 */

import java.util.Arrays;

public class StringsDemo {

    public static void main(String[] args) {
        stringPool();
        immutability();
        commonMethods();
        comparison();
        stringBuilder();
        concatenationPerformance();
        charManipulation();
        classicProblems();
    }

    // =================================================================
    // 1. String pool & == vs equals
    // =================================================================
    static void stringPool() {
        System.out.println("===== 1. STRING POOL =====");
        String a = "hello";
        String b = "hello";
        String c = new String("hello");
        String d = c.intern();

        // == compares REFERENCES (are they the same object?)
        // equals() compares CONTENT (same characters?)
        System.out.println("a == b      : " + (a == b));       // true  (same pool object)
        System.out.println("a == c      : " + (a == c));       // false (c is a separate heap object)
        System.out.println("a.equals(c) : " + a.equals(c));    // true  (same characters)
        System.out.println("a == d      : " + (a == d));       // true  (intern() returns pooled copy)

        // Compile-time constants are joined by the compiler and pooled
        String e = "hel" + "lo";
        System.out.println("a == \"hel\"+\"lo\" : " + (a == e));   // true

        // Runtime concatenation creates a NEW object (not pooled)
        String part = "hel";
        String f = part + "lo";
        System.out.println("a == part+\"lo\"   : " + (a == f));    // false

        // Interview Q: how many objects does  new String("hello")  create?
        // Answer: up to 2 -> one in the pool (if "hello" wasn't already there)
        //         + one regular heap object.

        // RULE: ALWAYS compare strings with equals(), never with ==
    }

    // =================================================================
    // 2. Immutability in action
    // =================================================================
    static void immutability() {
        System.out.println("\n===== 2. IMMUTABILITY =====");
        String s = "java";
        s.toUpperCase();                 // creates "JAVA" and throws it away
        System.out.println("after s.toUpperCase(): " + s);   // still "java"

        s = s.toUpperCase();             // reassign the variable to the new object
        System.out.println("after s = s.toUpperCase(): " + s);
        // The original "java" object was NOT changed; 's' now points to a new object.
    }

    // =================================================================
    // 3. Most used String methods
    // =================================================================
    static void commonMethods() {
        System.out.println("\n===== 3. COMMON METHODS =====");
        String s = "  Hello, World  ";

        System.out.println("length()        = " + s.length());
        System.out.println("trim()          = [" + s.trim() + "]");      // removes ASCII spaces
        System.out.println("strip()         = [" + s.strip() + "]");     // Unicode-aware (Java 11)
        String t = s.trim();
        System.out.println("charAt(0)       = " + t.charAt(0));
        System.out.println("indexOf('o')    = " + t.indexOf('o'));
        System.out.println("lastIndexOf('o')= " + t.lastIndexOf('o'));
        System.out.println("substring(7)    = " + t.substring(7));       // from 7 to end
        System.out.println("substring(0,5)  = " + t.substring(0, 5));    // [0,5) end exclusive
        System.out.println("contains(\"World\")= " + t.contains("World"));
        System.out.println("startsWith(\"He\")= " + t.startsWith("He"));
        System.out.println("replace('l','L')= " + t.replace('l', 'L'));
        System.out.println("toLowerCase()   = " + t.toLowerCase());
        System.out.println("isEmpty(\"\")     = " + "".isEmpty());
        System.out.println("isBlank(\"  \")   = " + "  ".isBlank());       // Java 11
        System.out.println("repeat(3)       = " + "ab".repeat(3));        // Java 11

        // split -> String[] (argument is a REGEX)
        String csv = "apple,banana,,cherry";
        System.out.println("split(\",\")      = " + Arrays.toString(csv.split(",")));
        System.out.println("split(\"\\\\s+\")   = " + Arrays.toString("a   b  c".split("\\s+")));

        // join
        System.out.println("String.join     = " + String.join("-", "a", "b", "c"));

        // Conversions
        int n = Integer.parseInt("42");
        String back = String.valueOf(n);
        char[] chars = "abc".toCharArray();
        String fromChars = new String(chars);
        System.out.println("parse/valueOf   = " + n + " / " + back + " / " + fromChars);

        // Formatting
        System.out.println(String.format("format          = %s is %d years, %.2f m", "Ram", 25, 1.756));
    }

    // =================================================================
    // 4. Comparing strings
    // =================================================================
    static void comparison() {
        System.out.println("\n===== 4. COMPARISON =====");
        System.out.println("\"Java\".equalsIgnoreCase(\"JAVA\") = " + "Java".equalsIgnoreCase("JAVA"));

        // compareTo: lexicographic (dictionary) order
        //   negative -> first < second, 0 -> equal, positive -> first > second
        System.out.println("\"apple\".compareTo(\"banana\") = " + "apple".compareTo("banana")); // negative
        System.out.println("\"b\".compareTo(\"a\")         = " + "b".compareTo("a"));          // positive
        System.out.println("\"Z\".compareTo(\"a\")         = " + "Z".compareTo("a"));          // negative! uppercase < lowercase

        // Null-safe equality: put the literal FIRST, or use Objects.equals
        String maybeNull = null;
        System.out.println("\"x\".equals(null)      = " + "x".equals(maybeNull));               // false, no NPE
        System.out.println("Objects.equals(null,null) = " + java.util.Objects.equals(null, null));
    }

    // =================================================================
    // 5. StringBuilder - mutable sequence of characters
    //    Internally a resizable byte array. Default capacity 16; grows to
    //    roughly (old * 2 + 2) when full, copying the old content.
    // =================================================================
    static void stringBuilder() {
        System.out.println("\n===== 5. STRINGBUILDER =====");
        StringBuilder sb = new StringBuilder();      // capacity 16

        sb.append("Hello")                            // methods return 'this' -> chaining
          .append(' ')
          .append("World")
          .append(123);
        System.out.println("append  : " + sb);

        sb.insert(0, ">> ");
        System.out.println("insert  : " + sb);

        sb.delete(0, 3);                              // remove [0,3)
        System.out.println("delete  : " + sb);

        sb.setCharAt(0, 'J');
        System.out.println("setCharAt: " + sb);

        sb.deleteCharAt(sb.length() - 1);             // remove last char (common in backtracking!)
        System.out.println("deleteCharAt last: " + sb);

        sb.reverse();
        System.out.println("reverse : " + sb);

        System.out.println("length=" + sb.length() + " capacity=" + sb.capacity());

        // Pre-size if you know the final size -> avoids repeated resizing
        StringBuilder big = new StringBuilder(1000);
        System.out.println("pre-sized capacity = " + big.capacity());

        // StringBuffer has the SAME API, but every method is synchronized
        StringBuffer buffer = new StringBuffer("thread-safe");
        buffer.append("!");
        System.out.println("StringBuffer: " + buffer);
    }

    // =================================================================
    // 6. Why '+' in a loop is slow
    //    s = s + i  creates a NEW String every iteration and copies all
    //    previous characters -> O(n^2) total. StringBuilder is O(n).
    // =================================================================
    static void concatenationPerformance() {
        System.out.println("\n===== 6. PERFORMANCE =====");
        int n = 20_000;

        long start = System.nanoTime();
        String s = "";
        for (int i = 0; i < n; i++) s += i;            // BAD in loops
        long plusTime = System.nanoTime() - start;

        start = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(i);     // GOOD
        long sbTime = System.nanoTime() - start;

        System.out.printf("String +=       : %d ms%n", plusTime / 1_000_000);
        System.out.printf("StringBuilder   : %d ms%n", sbTime / 1_000_000);
        System.out.println("same result? " + s.equals(sb.toString()));
        // Note: a single  "a" + b + "c"  outside a loop is fine; the compiler optimizes it.
    }

    // =================================================================
    // 7. Character helpers (used heavily in string DSA problems)
    // =================================================================
    static void charManipulation() {
        System.out.println("\n===== 7. CHARACTERS =====");
        char c = 'g';
        System.out.println("isLetter=" + Character.isLetter(c)
                + " isDigit=" + Character.isDigit('7')
                + " isLetterOrDigit=" + Character.isLetterOrDigit('#')
                + " toUpperCase=" + Character.toUpperCase(c));

        // char arithmetic tricks
        int index = c - 'a';               // 'g' - 'a' = 6 -> index into int[26]
        int digit = '7' - '0';             // char digit to int value
        char shifted = (char) ('a' + 2);   // 'c'
        System.out.println("c-'a'=" + index + " '7'-'0'=" + digit + " 'a'+2=" + shifted);

        // Frequency array: classic O(n) counting for lowercase letters
        String word = "banana";
        int[] freq = new int[26];
        for (char ch : word.toCharArray()) freq[ch - 'a']++;
        System.out.println("freq of 'a' in banana = " + freq['a' - 'a'] + ", 'n' = " + freq['n' - 'a']);
    }

    // =================================================================
    // 8. Classic interview problems
    // =================================================================
    static void classicProblems() {
        System.out.println("\n===== 8. CLASSIC PROBLEMS =====");
        System.out.println("reverse(\"hello\")         = " + reverse("hello"));
        System.out.println("isPalindrome(\"A man, a plan, a canal: Panama\") = "
                + isPalindrome("A man, a plan, a canal: Panama"));
        System.out.println("isAnagram(listen, silent) = " + isAnagram("listen", "silent"));
        System.out.println("reverseWords(\"  the sky  is blue \") = [" + reverseWords("  the sky  is blue ") + "]");
    }

    // Two pointers on a char array. O(n) time, O(n) space (char array).
    static String reverse(String s) {
        char[] arr = s.toCharArray();
        int i = 0, j = arr.length - 1;
        while (i < j) {
            char tmp = arr[i];
            arr[i++] = arr[j];
            arr[j--] = tmp;
        }
        return new String(arr);
    }

    // Skip non-alphanumeric chars, compare case-insensitively. O(n) time, O(1) space.
    static boolean isPalindrome(String s) {
        int i = 0, j = s.length() - 1;
        while (i < j) {
            while (i < j && !Character.isLetterOrDigit(s.charAt(i))) i++;
            while (i < j && !Character.isLetterOrDigit(s.charAt(j))) j--;
            if (Character.toLowerCase(s.charAt(i)) != Character.toLowerCase(s.charAt(j))) return false;
            i++;
            j--;
        }
        return true;
    }

    // Count with +1 for s, -1 for t; all zeros means anagram. O(n).
    static boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) return false;
        int[] count = new int[26];
        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i) - 'a']++;
            count[t.charAt(i) - 'a']--;
        }
        for (int c : count) if (c != 0) return false;
        return true;
    }

    // Split on whitespace, then join in reverse order.
    static String reverseWords(String s) {
        String[] words = s.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = words.length - 1; i >= 0; i--) {
            sb.append(words[i]);
            if (i > 0) sb.append(' ');
        }
        return sb.toString();
    }
}
