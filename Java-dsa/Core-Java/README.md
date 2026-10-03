# Core Java

Study the folders in order. Each file is self-contained and runnable with `java FileName.java` (Java 21 is needed for `08-Modern-Java`).

| # | Folder | Files | What you'll learn |
|---|--------|-------|-------------------|
| 1 | [01-Fundamentals](01-Fundamentals/) | `DataTypesAndVariables`, `Operators`, `ControlFlow`, `MethodsAndArrays` | Primitives vs references, casting, overflow, Integer cache, stack vs heap, pass-by-value, bit tricks, switch expressions, loops and labels, recursion, arrays and 2D grids |
| 2 | [02-Strings](02-Strings/) | `StringsDemo` | String pool, `==` vs `equals`, immutability (and why), StringBuilder vs StringBuffer, performance, char tricks, classic string problems |
| 3 | [03-Collections](03-Collections/) | `ListDemo`, `SetDemo`, `MapDemo`, `QueueDequeDemo` | Hierarchy, ArrayList vs LinkedList, fail-fast iterators, Comparable vs Comparator, HashSet/TreeSet, **HashMap internals** (hashing, buckets, load factor, resize, treeification) plus a hand-built HashMap, LinkedHashMap LRU cache, TreeMap, ConcurrentHashMap, Deque as a stack or queue, PriorityQueue/heaps, monotonic stack |
| 4 | [04-Generics](04-Generics/) | `GenericsDemo` | Generic classes and methods, bounded types, invariance, wildcards and **PECS**, type erasure and its limits, generic stack |
| 5 | [05-Exceptions](05-Exceptions/) | `ExceptionsDemo` | Hierarchy, checked vs unchecked, finally traps, custom exceptions, try-with-resources, suppressed exceptions, chaining, best practices |
| 6 | [06-Java8-Features](06-Java8-Features/) | `LambdasAndFunctionalInterfaces`, `StreamsDemo`, `OptionalAndOtherJava8` | Lambdas, built-in functional interfaces, composition, method references, effectively final, Streams (laziness, flatMap, Collectors, groupingBy, partitioningBy, parallel), stream interview questions, Optional, default/static interface methods, java.time |
| 7 | [07-IO-NIO](07-IO-NIO/) | `IONioDemo` | Byte vs character streams, buffering, decorator pattern, NIO.2 `Files`/`Path`, directory walking, ByteBuffer and FileChannel, fast input reading |
| 8 | [08-Modern-Java](08-Modern-Java/) | `ModernJavaDemo` | Records, sealed classes, pattern matching (`instanceof` and `switch`), record patterns, text blocks, sequenced collections, virtual threads |
| 9 | [09-Serialization](09-Serialization/) | `SerializationDemo` | Serializable, `transient` and `static`, `serialVersionUID`, constructor behaviour, custom hooks, the Singleton problem with `readResolve`, Externalizable, deserialization security filters |

## Interview questions to test yourself

Answer each question out loud. If you get stuck, look at the file in brackets.

1. Why is `Integer a = 128, b = 128; a == b` false while 127 gives true? *(DataTypesAndVariables)*
2. Is Java pass-by-value or pass-by-reference? *(DataTypesAndVariables)*
3. Why is String immutable? How many objects does `new String("hi")` create? *(StringsDemo)*
4. StringBuilder vs StringBuffer: when would you use each? *(StringsDemo)*
5. Walk through `HashMap.put()` step by step. What are the load factor, threshold and treeification? *(MapDemo)*
6. Why must you override `hashCode()` when you override `equals()`? *(SetDemo)*
7. How does ConcurrentHashMap (Java 8) achieve thread safety? Why are null keys not allowed? *(MapDemo)*
8. ArrayList vs LinkedList: which is faster in practice, and why? *(ListDemo)*
9. What is fail-fast vs weakly consistent iteration? *(ListDemo, MapDemo)*
10. Explain PECS with `Collections.copy`. *(GenericsDemo)*
11. What is type erasure? Why can't you do `new T()`? *(GenericsDemo)*
12. Checked vs unchecked exceptions: which should a custom business exception be? *(ExceptionsDemo)*
13. What does a `return` in `finally` do? *(ExceptionsDemo)*
14. Are streams lazy? Show it. Why can't a stream be reused? *(StreamsDemo)*
15. `map` vs `flatMap`? `orElse` vs `orElseGet`? *(StreamsDemo, OptionalAndOtherJava8)*
16. When do parallel streams hurt performance? *(StreamsDemo)*
17. What is the difference between java.io and java.nio? What does `flip()` do? *(IONioDemo)*
18. Record vs class vs Lombok `@Data`? Why use sealed classes? *(ModernJavaDemo)*
19. What is `serialVersionUID` for? How does deserialization break a Singleton? *(SerializationDemo)*

Next topic: **OOP**.
