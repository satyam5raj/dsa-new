/*
 * ============================================================================
 *  TOPIC 6.2 : STREAMS API
 * ============================================================================
 *
 *  Run:  java StreamsDemo.java
 *
 *  A STREAM is a pipeline that processes a sequence of elements DECLARATIVELY
 *  ("what" you want, not "how" to loop).
 *
 *      source  ->  intermediate ops (0 or more)  ->  terminal op (exactly 1)
 *      list.stream().filter(...).map(...).collect(...)
 *
 *  KEY PROPERTIES
 *   1. Does NOT store data and does NOT modify the source.
 *   2. LAZY: intermediate ops do nothing until a terminal op is called.
 *   3. SINGLE USE: once a terminal op runs, the stream is consumed.
 *      Reusing it -> IllegalStateException.
 *   4. Elements flow ONE AT A TIME through the whole pipeline (vertical
 *      processing), which allows short-circuiting (findFirst, limit, anyMatch).
 *
 *  INTERMEDIATE (return a Stream, lazy)
 *    filter, map, flatMap, distinct, sorted, peek, limit, skip,
 *    mapToInt/mapToObj, takeWhile/dropWhile (Java 9)
 *    stateless: filter, map  |  stateful: distinct, sorted (need to see many elements)
 *
 *  TERMINAL (produce a result / side effect, trigger execution)
 *    forEach, collect, reduce, count, min, max, sum, average,
 *    anyMatch, allMatch, noneMatch, findFirst, findAny, toList (Java 16)
 *
 *  STREAM vs COLLECTION
 *    Collection = data in memory you can iterate many times.
 *    Stream     = a one-shot computation over data.
 */

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

public class StreamsDemo {

    // Sample domain used throughout
    record Employee(String name, String dept, double salary, int age) {}

    static final List<Employee> EMPLOYEES = List.of(
            new Employee("Aarav", "Engineering", 120_000, 29),
            new Employee("Diya", "Engineering", 150_000, 35),
            new Employee("Kabir", "Sales", 70_000, 41),
            new Employee("Meera", "Sales", 85_000, 26),
            new Employee("Rohan", "HR", 60_000, 38),
            new Employee("Sara", "Engineering", 95_000, 24));

    public static void main(String[] args) {
        creatingStreams();
        filterMapReduce();
        laziness();
        sortingAndLimiting();
        matchingAndFinding();
        flatMap();
        primitiveStreams();
        collectors();
        interviewQuestions();
        parallelStreams();
    }

    // =================================================================
    // 1. Creating streams
    // =================================================================
    static void creatingStreams() {
        System.out.println("===== 1. CREATING STREAMS =====");
        Stream<String> fromList = List.of("a", "b").stream();
        Stream<Integer> fromValues = Stream.of(1, 2, 3);
        IntStream fromArray = Arrays.stream(new int[]{4, 5, 6});
        IntStream range = IntStream.range(0, 5);              // 0..4
        IntStream rangeClosed = IntStream.rangeClosed(1, 5);  // 1..5
        Stream<Integer> iterate = Stream.iterate(1, x -> x * 2).limit(5); // infinite, so limit!
        Stream<Double> generate = Stream.generate(Math::random).limit(2);
        IntStream chars = "hello".chars();                    // stream of char codes

        System.out.println("list    : " + fromList.toList());
        System.out.println("of      : " + fromValues.toList());
        System.out.println("array   : " + Arrays.toString(fromArray.toArray()));
        System.out.println("range   : " + Arrays.toString(range.toArray()));
        System.out.println("closed  : " + Arrays.toString(rangeClosed.toArray()));
        System.out.println("iterate : " + iterate.toList());
        System.out.println("generate: " + generate.count() + " randoms");
        System.out.println("chars   : " + chars.mapToObj(c -> String.valueOf((char) c)).collect(Collectors.joining("-")));

        // Single use!
        Stream<String> once = Stream.of("x");
        once.count();
        try {
            once.count();
        } catch (IllegalStateException e) {
            System.out.println("reusing a stream -> IllegalStateException");
        }
    }

    // =================================================================
    // 2. filter / map / reduce - the core trio
    // =================================================================
    static void filterMapReduce() {
        System.out.println("\n===== 2. FILTER / MAP / REDUCE =====");
        List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<Integer> evenSquares = nums.stream()
                .filter(n -> n % 2 == 0)      // keep evens     -> 2,4,6,8,10
                .map(n -> n * n)              // transform      -> 4,16,36,64,100
                .toList();                    // Java 16 (unmodifiable). Older: collect(Collectors.toList())
        System.out.println("even squares = " + evenSquares);

        // reduce(identity, accumulator): fold all elements into one value
        int sum = nums.stream().reduce(0, Integer::sum);
        int product = nums.stream().limit(5).reduce(1, (a, b) -> a * b);
        Optional<Integer> max = nums.stream().reduce(Integer::max);   // no identity -> Optional
        System.out.println("sum = " + sum + ", product(1..5) = " + product + ", max = " + max.get());

        List<String> names = EMPLOYEES.stream()
                .filter(e -> e.salary() > 90_000)
                .map(Employee::name)
                .toList();
        System.out.println("high earners = " + names);
    }

    // =================================================================
    // 3. Laziness & vertical processing - watch the order of prints
    // =================================================================
    static void laziness() {
        System.out.println("\n===== 3. LAZINESS =====");
        Stream<String> pipeline = Stream.of("one", "two", "three", "four")
                .filter(s -> {
                    System.out.println("   filter: " + s);
                    return s.length() > 3;
                })
                .map(s -> {
                    System.out.println("   map:    " + s);
                    return s.toUpperCase();
                });
        System.out.println("pipeline built, nothing printed yet (lazy)");

        Optional<String> first = pipeline.findFirst();   // stops after the FIRST match
        System.out.println("findFirst = " + first.get() + "  ('four' was never processed)");
    }

    // =================================================================
    // 4. sorted, distinct, limit, skip, takeWhile, dropWhile
    // =================================================================
    static void sortingAndLimiting() {
        System.out.println("\n===== 4. SORT / DISTINCT / LIMIT / SKIP =====");
        List<Integer> nums = List.of(5, 3, 8, 3, 1, 8, 9);
        System.out.println("distinct sorted  = " + nums.stream().distinct().sorted().toList());
        System.out.println("sorted desc      = " + nums.stream().sorted(Comparator.reverseOrder()).toList());
        System.out.println("skip 2, limit 3  = " + nums.stream().skip(2).limit(3).toList());
        System.out.println("takeWhile < 8    = " + nums.stream().takeWhile(n -> n < 8).toList());  // stops at first fail
        System.out.println("dropWhile < 8    = " + nums.stream().dropWhile(n -> n < 8).toList());

        // Top 3 salaries (pagination pattern: skip(page*size).limit(size))
        List<String> top3 = EMPLOYEES.stream()
                .sorted(Comparator.comparingDouble(Employee::salary).reversed())
                .limit(3)
                .map(e -> e.name() + "(" + (int) e.salary() + ")")
                .toList();
        System.out.println("top 3 salaries   = " + top3);
    }

    // =================================================================
    // 5. Matching & finding (all short-circuit)
    // =================================================================
    static void matchingAndFinding() {
        System.out.println("\n===== 5. MATCH / FIND =====");
        System.out.println("anyMatch HR       = " + EMPLOYEES.stream().anyMatch(e -> e.dept().equals("HR")));
        System.out.println("allMatch age > 20 = " + EMPLOYEES.stream().allMatch(e -> e.age() > 20));
        System.out.println("noneMatch salary<0= " + EMPLOYEES.stream().noneMatch(e -> e.salary() < 0));
        System.out.println("findFirst Sales   = " + EMPLOYEES.stream().filter(e -> e.dept().equals("Sales"))
                .findFirst().map(Employee::name).orElse("none"));
        System.out.println("oldest            = " + EMPLOYEES.stream()
                .max(Comparator.comparingInt(Employee::age)).map(Employee::name).orElse("none"));
        System.out.println("count Engineering = " + EMPLOYEES.stream().filter(e -> e.dept().equals("Engineering")).count());
    }

    // =================================================================
    // 6. flatMap - one element -> MANY elements, then flatten
    //    map:     Stream<List<X>>   (a stream of lists)
    //    flatMap: Stream<X>         (one flat stream)
    // =================================================================
    static void flatMap() {
        System.out.println("\n===== 6. FLATMAP =====");
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3, 4), List.of(5));
        List<Integer> flat = nested.stream().flatMap(List::stream).toList();
        System.out.println("flatten = " + flat);

        List<String> sentences = List.of("hello world", "hello java streams");
        List<String> uniqueWords = sentences.stream()
                .flatMap(s -> Arrays.stream(s.split(" ")))   // each sentence -> many words
                .distinct()
                .toList();
        System.out.println("unique words = " + uniqueWords);
    }

    // =================================================================
    // 7. Primitive streams: IntStream, LongStream, DoubleStream
    //    Avoid boxing overhead and give sum/average/summaryStatistics.
    // =================================================================
    static void primitiveStreams() {
        System.out.println("\n===== 7. PRIMITIVE STREAMS =====");
        int[] arr = {4, 8, 15, 16, 23, 42};
        System.out.println("sum     = " + Arrays.stream(arr).sum());
        System.out.println("average = " + Arrays.stream(arr).average().orElse(0));
        System.out.println("max     = " + Arrays.stream(arr).max().getAsInt());

        IntSummaryStatistics stats = Arrays.stream(arr).summaryStatistics();
        System.out.println("stats   = " + stats);

        double totalSalary = EMPLOYEES.stream().mapToDouble(Employee::salary).sum();  // obj -> primitive
        System.out.println("total salary = " + totalSalary);

        List<Integer> boxed = IntStream.rangeClosed(1, 5).boxed().toList();           // primitive -> obj
        System.out.println("boxed   = " + boxed);

        // int[] <-> List<Integer> conversions (very common in DSA)
        int[] back = boxed.stream().mapToInt(Integer::intValue).toArray();
        System.out.println("back to int[] = " + Arrays.toString(back));
    }

    // =================================================================
    // 8. Collectors - the power tool
    // =================================================================
    static void collectors() {
        System.out.println("\n===== 8. COLLECTORS =====");

        // toList / toSet / toCollection
        Set<String> depts = EMPLOYEES.stream().map(Employee::dept).collect(Collectors.toSet());
        TreeSet<String> sortedDepts = EMPLOYEES.stream().map(Employee::dept)
                .collect(Collectors.toCollection(TreeSet::new));
        System.out.println("depts (set)      = " + depts);
        System.out.println("depts (TreeSet)  = " + sortedDepts);

        // joining
        String csv = EMPLOYEES.stream().map(Employee::name).collect(Collectors.joining(", ", "[", "]"));
        System.out.println("joining          = " + csv);

        // toMap (throws IllegalStateException on duplicate keys unless you pass a merge fn)
        Map<String, Double> nameToSalary = EMPLOYEES.stream()
                .collect(Collectors.toMap(Employee::name, Employee::salary));
        System.out.println("toMap            = " + nameToSalary);
        Map<String, Double> maxSalaryByDept = EMPLOYEES.stream()
                .collect(Collectors.toMap(Employee::dept, Employee::salary, Math::max)); // merge on clash
        System.out.println("toMap with merge = " + maxSalaryByDept);

        // groupingBy - like SQL GROUP BY
        Map<String, List<String>> namesByDept = EMPLOYEES.stream()
                .collect(Collectors.groupingBy(Employee::dept,
                        Collectors.mapping(Employee::name, Collectors.toList())));
        System.out.println("groupingBy names = " + namesByDept);

        Map<String, Long> countByDept = EMPLOYEES.stream()
                .collect(Collectors.groupingBy(Employee::dept, Collectors.counting()));
        System.out.println("count by dept    = " + countByDept);

        Map<String, Double> avgSalaryByDept = EMPLOYEES.stream()
                .collect(Collectors.groupingBy(Employee::dept, TreeMap::new,       // sorted keys
                        Collectors.averagingDouble(Employee::salary)));
        System.out.println("avg salary/dept  = " + avgSalaryByDept);

        Map<String, Optional<Employee>> topPaidByDept = EMPLOYEES.stream()
                .collect(Collectors.groupingBy(Employee::dept,
                        Collectors.maxBy(Comparator.comparingDouble(Employee::salary))));
        topPaidByDept.forEach((d, e) -> System.out.println("   top in " + d + " = " + e.get().name()));

        // partitioningBy - split into exactly two groups: true / false
        Map<Boolean, List<String>> under30 = EMPLOYEES.stream()
                .collect(Collectors.partitioningBy(e -> e.age() < 30,
                        Collectors.mapping(Employee::name, Collectors.toList())));
        System.out.println("partition age<30 = " + under30);

        // summarizing
        DoubleSummaryStatistics salaryStats = EMPLOYEES.stream()
                .collect(Collectors.summarizingDouble(Employee::salary));
        System.out.printf("salary min=%.0f max=%.0f avg=%.0f%n",
                salaryStats.getMin(), salaryStats.getMax(), salaryStats.getAverage());
    }

    // =================================================================
    // 9. Frequently asked stream interview questions
    // =================================================================
    static void interviewQuestions() {
        System.out.println("\n===== 9. INTERVIEW QUESTIONS =====");

        // Q1: frequency of each character
        String word = "programming";
        Map<Character, Long> freq = word.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));
        System.out.println("char frequency      = " + freq);

        // Q2: first non-repeating character
        Character firstUnique = freq.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        System.out.println("first non-repeating = " + firstUnique);

        // Q3: duplicates in a list
        List<Integer> nums = List.of(1, 2, 3, 2, 4, 3, 5);
        Set<Integer> seen = new HashSet<>();
        Set<Integer> dups = nums.stream().filter(n -> !seen.add(n)).collect(Collectors.toSet());
        System.out.println("duplicates          = " + dups);

        // Q4: second highest salary
        Optional<Double> second = EMPLOYEES.stream().map(Employee::salary).distinct()
                .sorted(Comparator.reverseOrder()).skip(1).findFirst();
        System.out.println("2nd highest salary  = " + second.orElse(0.0));

        // Q5: longest string
        String longest = Stream.of("java", "kotlin", "go", "typescript")
                .max(Comparator.comparingInt(String::length)).orElse("");
        System.out.println("longest             = " + longest);

        // Q6: reverse each word
        String reversed = Arrays.stream("hello stream world".split(" "))
                .map(w -> new StringBuilder(w).reverse().toString())
                .collect(Collectors.joining(" "));
        System.out.println("reverse each word   = " + reversed);

        // Q7: sum of squares of odd numbers
        int sumSq = IntStream.rangeClosed(1, 10).filter(n -> n % 2 == 1).map(n -> n * n).sum();
        System.out.println("sum sq of odds 1-10 = " + sumSq);
    }

    // =================================================================
    // 10. Parallel streams
    //    .parallelStream() / .parallel() splits work across the common
    //    ForkJoinPool. Helpful ONLY for large, CPU-heavy, independent work.
    //    Pitfalls: overhead on small data, shared mutable state = race
    //    conditions, ordering not guaranteed with forEach (use forEachOrdered).
    //    Also: blocking I/O inside parallel streams starves the common pool.
    // =================================================================
    static void parallelStreams() {
        System.out.println("\n===== 10. PARALLEL STREAMS =====");
        long sum = LongStream.rangeClosed(1, 10_000_000).parallel().sum();
        System.out.println("parallel sum 1..10M = " + sum);

        List<Integer> unsafe = new ArrayList<>();
        String unsafeResult;
        try {
            IntStream.range(0, 10_000).parallel().forEach(unsafe::add); // WRONG: ArrayList is not thread-safe
            unsafeResult = String.valueOf(unsafe.size());
        } catch (RuntimeException e) {
            unsafeResult = "crashed with " + e.getClass().getSimpleName();
        }
        List<Integer> safe = IntStream.range(0, 10_000).parallel().boxed().toList();  // RIGHT: let the stream collect
        System.out.println("unsafe size = " + unsafeResult + " (expected 10000), safe size = " + safe.size());
    }
}
