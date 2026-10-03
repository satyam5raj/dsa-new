# OOP — Object-Oriented Programming

Study the folders in order. Every file is self-contained and runs with `java FileName.java`.
Most files show a **BAD** version first and then the **GOOD** version, so you can see *why* each principle exists.

| # | Folder | Files | What you'll learn |
|---|--------|-------|-------------------|
| 1 | [01-Classes-And-Objects](01-Classes-And-Objects/) | `ClassesAndObjects` | Fields, constructors, `this`, constructor chaining, static vs instance, initialization order, access modifiers, nested/inner/local/anonymous classes, enums, the `Object` class |
| 2 | [02-Four-Pillars](02-Four-Pillars/) | `Encapsulation`, `Abstraction`, `Inheritance`, `Polymorphism` | Each pillar with its **why**: protecting invariants, leaking internals, encapsulation vs abstraction, overriding rules, field hiding, casting, compile-time vs runtime polymorphism, overload traps, replacing conditionals with polymorphism |
| 3 | [03-Composition-vs-Inheritance](03-Composition-vs-Inheritance/) | `CompositionVsInheritance` | The fragile base class (the `InstrumentedHashSet` bug), class explosion, swapping behaviour at runtime, and why `java.util.Stack` is a design mistake |
| 4 | [04-Interfaces-vs-Abstract-Classes](04-Interfaces-vs-Abstract-Classes/) | `InterfacesVsAbstractClasses` | Comparison table, capabilities vs shared state, default/static/private interface methods, the diamond problem, skeletal implementations (`AbstractList`) |
| 5 | [05-SOLID](05-SOLID/) | `S_…`, `O_…`, `L_…`, `I_…`, `D_…` | One file per principle with a realistic violation and its fix. Also covers Square/Rectangle, Bird/Penguin, DIP vs DI vs IoC, and a mini IoC container |
| 6 | [06-Design-Patterns](06-Design-Patterns/) | `P1_Singleton` … `P9_State` | Singleton (5 ways, and why it's often an anti-pattern), Factory (simple, registry, factory method, abstract factory), Builder (with validation and a step builder), Strategy, Observer (sync and async event bus), Decorator, Adapter, Template Method, and State (vending machine) |
| 7 | [07-Equality-And-Immutability](07-Equality-And-Immutability/) | `EqualsHashCodeContract`, `ImmutableClassDesign` | The equals/hashCode contract, what breaks when you override only equals, the inheritance symmetry trap, mutable keys, compareTo consistency, the rules for immutable classes, defensive copies, shallow immutability of records |

## Choosing a pattern

| Problem | Pattern |
|---|---|
| Exactly one shared instance | Singleton (prefer DI or an enum) |
| Type chosen at runtime, hide `new` | Factory |
| Many optional parameters, immutable result | Builder |
| Interchangeable algorithms | Strategy |
| Notify many dependents of a change | Observer |
| Add features by wrapping, combined at runtime | Decorator |
| Make an incompatible API fit your interface | Adapter |
| Same steps in a fixed order with varying details | Template Method |
| Behaviour depends on the current state, with transitions | State |

## Interview questions to test yourself

1. What is the difference between encapsulation and abstraction? Give a real example. *(Encapsulation, Abstraction)*
2. Why can getters and setters for every field still break encapsulation? *(Encapsulation)*
3. Overloading vs overriding: which one uses the runtime type? What does `greet(animalRefToDog)` call? *(Polymorphism)*
4. Why does Java not allow multiple inheritance of classes? How do default methods handle the diamond problem? *(Inheritance, InterfacesVsAbstractClasses)*
5. Explain the fragile base class problem with a concrete example. *(CompositionVsInheritance)*
6. When would you choose an abstract class over an interface after Java 8? *(InterfacesVsAbstractClasses)*
7. Explain each SOLID principle with a violation you have seen in real code. *(05-SOLID)*
8. Why does Square extending Rectangle violate LSP? *(L_LiskovSubstitution)*
9. What is the difference between DIP, DI and IoC? *(D_DependencyInversion)*
10. Write a thread-safe lazy Singleton. Why is `volatile` needed in double-checked locking? Why is an enum best? *(P1_Singleton)*
11. Factory Method vs Abstract Factory? *(P2_Factory)*
12. Builder vs telescoping constructors vs JavaBeans? *(P3_Builder)*
13. Strategy vs State vs Template Method? *(P4, P8, P9)*
14. What are the pitfalls of Observer in production? *(P5_Observer)*
15. Decorator vs Proxy vs Adapter? Where does the JDK use Decorator? *(P6, P7)*
16. What happens if you override `equals()` but not `hashCode()`? *(EqualsHashCodeContract)*
17. How do you make a class immutable? Is a record fully immutable? *(ImmutableClassDesign)*

Next topic: **Multithreading & Concurrency**.
