/*
 * ============================================================================
 *  TOPIC 1.3 : CONTROL FLOW
 * ============================================================================
 *
 *  Run:  java ControlFlow.java
 *
 *  Control flow decides WHICH statements run and HOW MANY times.
 *    - Decisions : if / else if / else, switch (classic + modern)
 *    - Loops     : for, enhanced for (for-each), while, do-while
 *    - Jumps     : break, continue, labeled break/continue, return
 */

public class ControlFlow {

    public static void main(String[] args) {
        ifElse();
        classicSwitch();
        modernSwitch();
        loops();
        breakAndContinue();
        patternPrinting();
    }

    // =================================================================
    // 1. if / else if / else
    // =================================================================
    static void ifElse() {
        System.out.println("===== 1. IF / ELSE =====");
        int marks = 82;

        // Conditions are checked TOP to BOTTOM; the first true one wins.
        if (marks >= 90) {
            System.out.println("Grade A");
        } else if (marks >= 75) {
            System.out.println("Grade B");   // this runs
        } else if (marks >= 50) {
            System.out.println("Grade C");   // skipped even though it's also true
        } else {
            System.out.println("Fail");
        }

        // Tip: always use braces {} even for one line - avoids bugs when adding lines later.

        // Guard clause / early return style - reduces nesting (preferred in real code)
        System.out.println("canVote(15) = " + canVote(15) + ", canVote(30) = " + canVote(30));
    }

    static String canVote(int age) {
        if (age < 0) return "invalid";   // guard 1
        if (age < 18) return "no";       // guard 2
        return "yes";                    // happy path, not nested
    }

    // =================================================================
    // 2. Classic switch (statement)
    //    Works with: byte, short, char, int (+ wrappers), String, enum.
    //    FALL-THROUGH: without 'break', execution continues into the next case!
    // =================================================================
    static void classicSwitch() {
        System.out.println("\n===== 2. CLASSIC SWITCH =====");
        int day = 3;

        switch (day) {
            case 1:
                System.out.println("Monday");
                break;
            case 2:
                System.out.println("Tuesday");
                break;
            case 3:
                System.out.println("Wednesday");
                // missing break -> FALLS THROUGH into case 4 (usually a bug)
            case 4:
                System.out.println("Thursday (fell through!)");
                break;
            default:
                System.out.println("Other day");
        }

        // Fall-through used ON PURPOSE to group cases
        char ch = 'e';
        switch (ch) {
            case 'a': case 'e': case 'i': case 'o': case 'u':
                System.out.println(ch + " is a vowel");
                break;
            default:
                System.out.println(ch + " is a consonant");
        }
    }

    // =================================================================
    // 3. Modern switch (Java 14+) - arrow syntax, NO fall-through,
    //    and can be used as an EXPRESSION that returns a value.
    // =================================================================
    static void modernSwitch() {
        System.out.println("\n===== 3. MODERN SWITCH =====");
        int day = 6;

        // As an expression: the whole switch produces a value
        String type = switch (day) {
            case 1, 2, 3, 4, 5 -> "Weekday";       // multiple labels with commas
            case 6, 7 -> "Weekend";
            default -> "Invalid";
        };
        System.out.println("day " + day + " is " + type);

        // 'yield' returns a value from a block in a switch expression
        String month = "FEB";
        int days = switch (month) {
            case "FEB" -> {
                boolean leap = false;
                yield leap ? 29 : 28;
            }
            case "APR", "JUN", "SEP", "NOV" -> 30;
            default -> 31;
        };
        System.out.println(month + " has " + days + " days");
    }

    // =================================================================
    // 4. Loops
    // =================================================================
    static void loops() {
        System.out.println("\n===== 4. LOOPS =====");

        // ---- for loop: use when you know the number of iterations ----
        //      for (init; condition; update)
        System.out.print("for:       ");
        for (int i = 0; i < 5; i++) {
            System.out.print(i + " ");
        }
        System.out.println();

        // Two pointers in one for loop (very common in DSA)
        int[] arr = {1, 2, 3, 4, 5};
        for (int left = 0, right = arr.length - 1; left < right; left++, right--) {
            int temp = arr[left];
            arr[left] = arr[right];
            arr[right] = temp;
        }
        System.out.println("reversed:  " + java.util.Arrays.toString(arr));

        // ---- enhanced for (for-each): read every element, no index needed ----
        //      You CANNOT modify the array slot through the loop variable.
        System.out.print("for-each:  ");
        for (int value : arr) {
            System.out.print(value + " ");
        }
        System.out.println();

        // ---- while: use when you DON'T know iterations in advance ----
        //      condition checked BEFORE each iteration (may run 0 times)
        int n = 12345, digits = 0;
        while (n > 0) {
            n /= 10;
            digits++;
        }
        System.out.println("while:     12345 has " + digits + " digits");

        // ---- do-while: body runs AT LEAST once, condition checked AFTER ----
        int k = 100;
        do {
            System.out.println("do-while:  runs once even though k=" + k + " fails k < 10");
        } while (k < 10);
    }

    // =================================================================
    // 5. break, continue, labels
    //    break    -> exit the innermost loop (or switch)
    //    continue -> skip the rest of this iteration, go to the next
    //    label:   -> lets break/continue target an OUTER loop
    // =================================================================
    static void breakAndContinue() {
        System.out.println("\n===== 5. BREAK / CONTINUE / LABELS =====");

        System.out.print("skip evens, stop at 9: ");
        for (int i = 1; i <= 20; i++) {
            if (i % 2 == 0) continue;   // skip even numbers
            if (i > 9) break;           // stop the loop entirely
            System.out.print(i + " ");
        }
        System.out.println();

        // Labeled break: find a target in a 2D matrix and exit BOTH loops
        int[][] matrix = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        int target = 5;
        outer:                                   // the label
        for (int r = 0; r < matrix.length; r++) {
            for (int c = 0; c < matrix[r].length; c++) {
                if (matrix[r][c] == target) {
                    System.out.println("found " + target + " at (" + r + "," + c + ")");
                    break outer;                 // exits the OUTER loop, not just inner
                }
            }
        }
    }

    // =================================================================
    // 6. Nested loops - pattern printing (builds intuition for loops)
    //    Outer loop = rows, inner loop = columns.
    // =================================================================
    static void patternPrinting() {
        System.out.println("\n===== 6. PATTERNS =====");
        int rows = 4;

        // Right triangle
        for (int i = 1; i <= rows; i++) {
            for (int j = 1; j <= i; j++) System.out.print("* ");
            System.out.println();
        }

        // Pyramid: spaces first, then stars
        for (int i = 1; i <= rows; i++) {
            System.out.print(" ".repeat(rows - i));     // leading spaces
            System.out.println("* ".repeat(i));          // stars
        }
    }
}
