/*
 * ============================================================================
 *  STRINGS 1.1 : STRING FUNDAMENTALS FOR DSA
 * ============================================================================
 *
 *  Run:  java StringFundamentals.java
 *  (Java-specific internals, like the string pool and immutability, are covered in
 *   Core-Java/02-Strings/StringsDemo.java. This file focuses on what PROBLEM SOLVING needs.)
 *
 *  A STRING IS AN ARRAY OF CHARACTERS
 *    "hello" -> ['h','e','l','l','o'], index 0..4
 *    So EVERY array pattern (two pointers, sliding window, prefix sums, hashing)
 *    works on strings too. What's new:
 *      1. characters are SMALL INTEGERS -> count them in an int[26] / int[128] array
 *      2. Java Strings are IMMUTABLE -> "modifying" creates a new string. Use char[]
 *         or StringBuilder when you build or change text.
 *      3. new algorithms for SEARCHING text (KMP, Rabin-Karp) -> StringMatchingAlgorithms.java
 *
 *  COST OF COMMON OPERATIONS (n = length)
 *  ---------------------------------------------------------------
 *   s.charAt(i)                     O(1)
 *   s.length()                      O(1)
 *   s.substring(i, j)               O(j - i)   (copies the characters!)
 *   s + t                           O(n + m)   (new string)
 *   s.equals(t), s.compareTo(t)     O(n)
 *   s.indexOf(t)                    O(n * m) worst case
 *   s.toCharArray()                 O(n)       (a copy)
 *   sb.append(c)                    O(1) amortized
 *   sb.insert(0, c), sb.deleteCharAt(0)   O(n)  (shifts)
 *   sb.reverse(), sb.toString()     O(n)
 *  ---------------------------------------------------------------
 *  CLASSIC TRAP: result = result + c inside a loop -> O(n^2). Use StringBuilder -> O(n).
 */

import java.util.*;

public class StringFundamentals {

    public static void main(String[] args) {
        charactersAreNumbers();
        frequencyCounting();
        characterClassification();
        traversalAndBuilding();
        splittingAndJoining();
        comparingStrings();
        concatenationTrap();
    }

    // =================================================================
    // 1. Characters are numbers (ASCII / Unicode code units)
    //    'A'..'Z' = 65..90     'a'..'z' = 97..122     '0'..'9' = 48..57
    //    You rarely need the numbers themselves: SUBTRACT A BASE CHARACTER instead.
    // =================================================================
    static void charactersAreNumbers() {
        System.out.println("===== 1. CHARACTERS ARE NUMBERS =====");
        char c = 'd';
        System.out.println("   (int) 'd'       = " + (int) c);
        System.out.println("   'd' - 'a'       = " + (c - 'a') + "      <- index into an int[26]");
        System.out.println("   '7' - '0'       = " + ('7' - '0') + "      <- digit char -> int value");
        System.out.println("   (char)('a' + 2) = " + (char) ('a' + 2) + "      <- int -> char (cast needed)");
        System.out.println("   'a' - 'A'       = " + ('a' - 'A') + "     <- upper/lower case distance");
        System.out.println("   toUpperCase('q')= " + Character.toUpperCase('q'));

        // Caesar shift by 3 using modular arithmetic: wraps 'z' around to 'c'
        StringBuilder shifted = new StringBuilder();
        for (char ch : "xyz".toCharArray()) shifted.append((char) ('a' + (ch - 'a' + 3) % 26));
        System.out.println("   shift \"xyz\" by 3 = " + shifted);
    }

    // =================================================================
    // 2. Frequency counting: THE most used string technique
    //    - only lowercase letters -> int[26],  index = c - 'a'
    //    - any ASCII              -> int[128], index = c
    //    - any Unicode            -> HashMap<Character, Integer>
    //    Arrays are faster than HashMap and use O(1) space (fixed size 26/128).
    // =================================================================
    static void frequencyCounting() {
        System.out.println("\n===== 2. FREQUENCY COUNTING =====");
        String s = "programming";

        int[] freq = new int[26];
        for (char c : s.toCharArray()) freq[c - 'a']++;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 26; i++) {
            if (freq[i] > 0) sb.append((char) ('a' + i)).append('=').append(freq[i]).append(' ');
        }
        System.out.println("   int[26] counts : " + sb.toString().trim());

        Map<Character, Integer> map = new TreeMap<>();
        for (char c : "Héllo, Wörld!".toCharArray()) map.merge(c, 1, Integer::sum);
        System.out.println("   HashMap counts : " + map + "  (works for any character)");

        // Two strings have the same letters <=> their frequency arrays are equal
        System.out.println("   anagram(listen, silent) = " + Arrays.equals(counts("listen"), counts("silent")));
    }

    static int[] counts(String s) {
        int[] f = new int[26];
        for (int i = 0; i < s.length(); i++) f[s.charAt(i) - 'a']++;
        return f;
    }

    // =================================================================
    // 3. Character classification (used when inputs contain spaces/punctuation)
    // =================================================================
    static void characterClassification() {
        System.out.println("\n===== 3. CHARACTER CLASSIFICATION =====");
        String s = "A1 b!";
        for (char c : s.toCharArray()) {
            System.out.printf("   '%c' letter=%-5b digit=%-5b letterOrDigit=%-5b whitespace=%-5b upper=%b%n",
                    c, Character.isLetter(c), Character.isDigit(c), Character.isLetterOrDigit(c),
                    Character.isWhitespace(c), Character.isUpperCase(c));
        }
    }

    // =================================================================
    // 4. Traversal & building
    //    - read with charAt(i) or a char[] (toCharArray copies once, then indexing is fast)
    //    - modify in place with a char[] (Strings can't be changed)
    //    - build with StringBuilder
    // =================================================================
    static void traversalAndBuilding() {
        System.out.println("\n===== 4. TRAVERSAL & BUILDING =====");
        String s = "hello world";

        char[] arr = s.toCharArray();
        for (int i = 0, j = arr.length - 1; i < j; i++, j--) {   // two pointers on a char[]
            char t = arr[i]; arr[i] = arr[j]; arr[j] = t;
        }
        System.out.println("   reversed via char[]      : " + new String(arr));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ("aeiou".indexOf(c) == -1) sb.append(c);           // keep non-vowels
        }
        System.out.println("   vowels removed (builder) : " + sb);

        // Building backwards: deleteCharAt(last) is O(1) -> the backtracking "undo" step
        StringBuilder path = new StringBuilder("abc");
        path.append('d');
        path.deleteCharAt(path.length() - 1);
        System.out.println("   append + undo            : " + path);

        // Run-length encoding: a typical "build while scanning" loop
        System.out.println("   RLE(\"aaabccdddd\")        : " + runLength("aaabccdddd"));
    }

    static String runLength(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int j = i;
            while (j < s.length() && s.charAt(j) == s.charAt(i)) j++;    // extend the group
            sb.append(s.charAt(i)).append(j - i);
            i = j;                                                      // jump to the next group
        }
        return sb.toString();
    }

    // =================================================================
    // 5. Splitting & joining words
    // =================================================================
    static void splittingAndJoining() {
        System.out.println("\n===== 5. SPLITTING & JOINING =====");
        String sentence = "  the   sky is  blue ";
        String[] words = sentence.trim().split("\\s+");                 // regex: one or more spaces
        System.out.println("   words      = " + Arrays.toString(words));
        System.out.println("   joined '-' = " + String.join("-", words));
        System.out.println("   split(\" \") on raw input gives empty strings: " + Arrays.toString("a  b".split(" ")));
    }

    // =================================================================
    // 6. Comparing
    //    ==         compares REFERENCES -> never use it for content
    //    equals     compares content, O(n)
    //    compareTo  lexicographic (dictionary) order: negative / 0 / positive
    // =================================================================
    static void comparingStrings() {
        System.out.println("\n===== 6. COMPARING =====");
        String a = "apple", b = new String("apple");
        System.out.println("   a == b       : " + (a == b) + "   a.equals(b): " + a.equals(b));
        System.out.println("   \"app\".compareTo(\"apple\") = " + "app".compareTo("apple") + "  (a prefix comes first)");
        String[] fruits = {"banana", "Apple", "cherry", "apple"};
        Arrays.sort(fruits);
        System.out.println("   sorted (uppercase first!) : " + Arrays.toString(fruits));
        Arrays.sort(fruits, String.CASE_INSENSITIVE_ORDER);
        System.out.println("   case-insensitive          : " + Arrays.toString(fruits));
    }

    // =================================================================
    // 7. Why + in a loop is quadratic
    // =================================================================
    static void concatenationTrap() {
        System.out.println("\n===== 7. THE CONCATENATION TRAP =====");
        int n = 30_000;
        long t0 = System.nanoTime();
        String s = "";
        for (int i = 0; i < n; i++) s += 'x';                  // copies the whole string every time
        long t1 = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('x');
        long t2 = System.nanoTime();
        System.out.printf("   %,d appends: String += took %d ms, StringBuilder took %.2f ms (same result: %b)%n",
                n, (t1 - t0) / 1_000_000, (t2 - t1) / 1e6, s.equals(sb.toString()));
    }
}
