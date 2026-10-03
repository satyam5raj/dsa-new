/*
 * ============================================================================
 *  TOPIC 6.1 : LAMBDAS, FUNCTIONAL INTERFACES, METHOD REFERENCES
 * ============================================================================
 *
 *  Run:  java LambdasAndFunctionalInterfaces.java
 *
 *  FUNCTIONAL INTERFACE
 *  --------------------
 *  An interface with EXACTLY ONE abstract method (SAM = Single Abstract Method).
 *  It may have any number of default/static methods.
 *  @FunctionalInterface is optional but makes the compiler enforce the rule.
 *
 *  LAMBDA
 *  ------
 *  A short way to provide the implementation of that ONE method.
 *  It's "a function you can pass around as a value".
 *
 *      (parameters) -> expression
 *      (parameters) -> { statements; return x; }
 *
 *  Before Java 8 you'd write an anonymous class:
 *      Comparator<String> c = new Comparator<String>() {
 *          public int compare(String a, String b) { return a.length() - b.length(); }
 *      };
 *  With a lambda:
 *      Comparator<String> c = (a, b) -> a.length() - b.length();
 *
 *  BUILT-IN FUNCTIONAL INTERFACES (java.util.function) - memorize these 4 + 2:
 *  ------------------------------------------------------------------------
 *   Interface            Method              Shape           Example
 *  ------------------------------------------------------------------------
 *   Predicate<T>         boolean test(T)     T -> boolean    filtering
 *   Function<T,R>        R apply(T)          T -> R          mapping
 *   Consumer<T>          void accept(T)      T -> void       printing
 *   Supplier<T>          T get()             () -> T         lazy creation
 *   UnaryOperator<T>     T apply(T)          T -> T          transform same type
 *   BinaryOperator<T>    T apply(T,T)        (T,T) -> T      reduce / sum
 *   BiFunction<T,U,R>    R apply(T,U)        (T,U) -> R
 *  ------------------------------------------------------------------------
 *  Primitive versions avoid boxing: IntPredicate, IntFunction, ToIntFunction,
 *  IntUnaryOperator, IntBinaryOperator, ...
 *
 *  METHOD REFERENCES (shorthand for a lambda that only calls a method)
 *  ------------------------------------------------------------------------
 *   Kind                          Syntax               Lambda equivalent
 *   static method                 Integer::parseInt    s -> Integer.parseInt(s)
 *   instance method of an object  System.out::println  x -> System.out.println(x)
 *   instance method of a type     String::length       s -> s.length()
 *   constructor                   ArrayList::new       () -> new ArrayList<>()
 */

import java.util.*;
import java.util.function.*;

public class LambdasAndFunctionalInterfaces {

    // Our own functional interface
    @FunctionalInterface
    interface Calculator {
        int calculate(int a, int b);          // the single abstract method

        // default & static methods don't break the "functional" rule
        default Calculator andThenDouble() {
            return (a, b) -> calculate(a, b) * 2;
        }
    }

    public static void main(String[] args) {
        lambdaSyntax();
        builtInInterfaces();
        composingFunctions();
        methodReferences();
        effectivelyFinal();
        lambdasVsAnonymousClasses();
    }

    // =================================================================
    // 1. Lambda syntax variations
    // =================================================================
    static void lambdaSyntax() {
        System.out.println("===== 1. LAMBDA SYNTAX =====");

        Calculator add = (a, b) -> a + b;              // expression body, implicit return
        Calculator multiply = (int a, int b) -> a * b; // explicit param types
        Calculator safeDivide = (a, b) -> {            // block body needs explicit return
            if (b == 0) return 0;
            return a / b;
        };
        Runnable noArgs = () -> System.out.println("no-arg lambda ran");
        Function<String, Integer> oneArg = s -> s.length(); // parentheses optional for 1 param

        System.out.println("add(3,4)        = " + add.calculate(3, 4));
        System.out.println("multiply(3,4)   = " + multiply.calculate(3, 4));
        System.out.println("safeDivide(8,0) = " + safeDivide.calculate(8, 0));
        System.out.println("add.andThenDouble(3,4) = " + add.andThenDouble().calculate(3, 4));
        noArgs.run();
        System.out.println("length of 'lambda' = " + oneArg.apply("lambda"));

        // Passing behaviour as an argument: the strategy is decided by the caller
        System.out.println("operate(10, 5, subtract) = " + operate(10, 5, (a, b) -> a - b));
    }

    static int operate(int a, int b, Calculator op) {
        return op.calculate(a, b);
    }

    // =================================================================
    // 2. Built-in functional interfaces
    // =================================================================
    static void builtInInterfaces() {
        System.out.println("\n===== 2. BUILT-IN INTERFACES =====");

        Predicate<Integer> isEven = n -> n % 2 == 0;
        System.out.println("Predicate isEven(4)    = " + isEven.test(4));

        Function<String, String> shout = s -> s.toUpperCase() + "!";
        System.out.println("Function shout(hi)     = " + shout.apply("hi"));

        Consumer<String> greeter = name -> System.out.println("Consumer says hello, " + name);
        greeter.accept("Satyam");

        Supplier<List<String>> listFactory = ArrayList::new;   // creates a new list each call
        List<String> fresh = listFactory.get();
        System.out.println("Supplier gave          = " + fresh);

        UnaryOperator<Integer> square = x -> x * x;
        System.out.println("UnaryOperator square(5)= " + square.apply(5));

        BinaryOperator<Integer> max = BinaryOperator.maxBy(Comparator.naturalOrder());
        System.out.println("BinaryOperator max(3,9)= " + max.apply(3, 9));

        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        System.out.println("BiFunction repeat(ab,3)= " + repeat.apply("ab", 3));

        IntBinaryOperator addInts = (a, b) -> a + b;            // no boxing
        System.out.println("IntBinaryOperator      = " + addInts.applyAsInt(2, 3));

        // Real usage in collections APIs
        List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        nums.removeIf(isEven);                                  // takes a Predicate
        nums.replaceAll(square);                                // takes a UnaryOperator
        nums.forEach(n -> System.out.print(n + " "));           // takes a Consumer
        System.out.println(" <- odd numbers squared");
    }

    // =================================================================
    // 3. Composing functions - build bigger behaviour from small pieces
    // =================================================================
    static void composingFunctions() {
        System.out.println("\n===== 3. COMPOSITION =====");

        Predicate<String> notEmpty = s -> !s.isEmpty();
        Predicate<String> shortWord = s -> s.length() < 5;
        Predicate<String> combined = notEmpty.and(shortWord);              // AND
        Predicate<String> either = notEmpty.negate().or(shortWord);        // NOT ... OR ...
        System.out.println("'cat' notEmpty AND short = " + combined.test("cat"));
        System.out.println("'elephant'               = " + combined.test("elephant"));
        System.out.println("'' empty OR short        = " + either.test(""));

        Function<Integer, Integer> plus2 = x -> x + 2;
        Function<Integer, Integer> times3 = x -> x * 3;
        // andThen: first this, then other.   compose: first other, then this.
        System.out.println("plus2.andThen(times3)(1) = " + plus2.andThen(times3).apply(1)); // (1+2)*3 = 9
        System.out.println("plus2.compose(times3)(1) = " + plus2.compose(times3).apply(1)); // 1*3+2 = 5

        Consumer<String> log = s -> System.out.print("[log] " + s);
        Consumer<String> newline = s -> System.out.println(" (done)");
        log.andThen(newline).accept("chained consumers");

        // Comparator composition
        List<String> words = new ArrayList<>(List.of("banana", "fig", "apple", "kiwi"));
        words.sort(Comparator.comparing(String::length).thenComparing(Comparator.reverseOrder()));
        System.out.println("by length, then reverse alpha = " + words);
    }

    // =================================================================
    // 4. Method references
    // =================================================================
    static void methodReferences() {
        System.out.println("\n===== 4. METHOD REFERENCES =====");

        // static method
        Function<String, Integer> parse = Integer::parseInt;
        System.out.println("Integer::parseInt        = " + (parse.apply("41") + 1));

        // instance method of a PARTICULAR object
        String prefix = "Hello, ";
        Function<String, String> greet = prefix::concat;
        System.out.println("prefix::concat           = " + greet.apply("World"));

        // instance method of an ARBITRARY object of a type (first param becomes 'this')
        Function<String, String> upper = String::toUpperCase;           // s -> s.toUpperCase()
        BiFunction<String, String, Boolean> eq = String::equalsIgnoreCase; // (a,b) -> a.equalsIgnoreCase(b)
        System.out.println("String::toUpperCase      = " + upper.apply("java"));
        System.out.println("String::equalsIgnoreCase = " + eq.apply("A", "a"));

        // constructor reference
        Function<String, StringBuilder> sbMaker = StringBuilder::new;   // s -> new StringBuilder(s)
        Supplier<Map<String, Integer>> mapMaker = HashMap::new;
        IntFunction<int[]> arrayMaker = int[]::new;                     // n -> new int[n]
        System.out.println("StringBuilder::new       = " + sbMaker.apply("abc").reverse());
        System.out.println("HashMap::new             = " + mapMaker.get());
        System.out.println("int[]::new length        = " + arrayMaker.apply(5).length);

        List.of("x", "y", "z").forEach(System.out::print);              // x -> System.out.print(x)
        System.out.println(" <- System.out::print");
    }

    // =================================================================
    // 5. Captured variables must be "effectively final"
    //    A lambda can READ local variables from the enclosing method only if
    //    they're never reassigned. Why? The lambda gets a COPY of the value
    //    (it may run later, on another thread, after the method returned).
    //    Fields and array/object contents CAN be mutated (not recommended).
    // =================================================================
    static int counterField = 0;

    static void effectivelyFinal() {
        System.out.println("\n===== 5. EFFECTIVELY FINAL =====");
        int base = 10;                      // never reassigned -> effectively final
        Function<Integer, Integer> addBase = x -> x + base;
        // base = 20;                       // would make the lambda above a COMPILE ERROR
        System.out.println("addBase(5) = " + addBase.apply(5));

        int[] holder = {0};                 // workaround: mutate contents, not the variable
        List.of(1, 2, 3).forEach(n -> holder[0] += n);
        List.of(1, 2, 3).forEach(n -> counterField += n);   // fields are allowed
        System.out.println("sum via array holder = " + holder[0] + ", via field = " + counterField);
        // Better: use streams -> List.of(1,2,3).stream().mapToInt(i -> i).sum()
    }

    // =================================================================
    // 6. Lambda vs anonymous class - difference in 'this'
    //    - Anonymous class: 'this' = the anonymous object itself; it creates a
    //      new class file and a new scope (can shadow variables).
    //    - Lambda: 'this' = the ENCLOSING instance; no new scope; compiled via
    //      invokedynamic (no extra class file), usually lighter.
    //    - Anonymous classes can implement interfaces with MANY methods,
    //      or extend abstract classes; lambdas only target functional interfaces.
    // =================================================================
    String name = "outer instance";

    void showThis() {
        Runnable anon = new Runnable() {
            String name = "anonymous object";
            @Override
            public void run() { System.out.println("anonymous class this.name = " + this.name); }
        };
        Runnable lambda = () -> System.out.println("lambda this.name          = " + this.name);
        anon.run();
        lambda.run();
    }

    static void lambdasVsAnonymousClasses() {
        System.out.println("\n===== 6. LAMBDA vs ANONYMOUS CLASS =====");
        new LambdasAndFunctionalInterfaces().showThis();
    }
}
