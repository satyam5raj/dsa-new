/*
 * ============================================================================
 *  TOPIC 9 : SERIALIZATION BASICS
 * ============================================================================
 *
 *  Run:  java SerializationDemo.java
 *
 *  SERIALIZATION   = converting an object's state into a BYTE STREAM
 *                    (to save to a file, send over a network, cache it).
 *  DESERIALIZATION = rebuilding the object from those bytes.
 *
 *  HOW (built-in Java serialization)
 *   1. The class implements java.io.Serializable  (a MARKER interface:
 *      no methods, it just "tags" the class as allowed).
 *   2. ObjectOutputStream.writeObject(obj)   -> bytes
 *      ObjectInputStream.readObject()        -> object (needs a cast)
 *
 *  RULES TO KNOW
 *   - All non-transient fields must themselves be Serializable, otherwise
 *     NotSerializableException.
 *   - 'transient' fields are SKIPPED -> restored as default (null/0/false).
 *     Use for passwords, caches, derived values, non-serializable things.
 *   - 'static' fields are NOT serialized (they belong to the class, not the object).
 *   - The CONSTRUCTOR of a Serializable class is NOT called during
 *     deserialization. The first NON-serializable parent's no-arg
 *     constructor IS called (so that parent must have one).
 *   - The whole object GRAPH is written; shared references and cycles
 *     are preserved (each object written once).
 *
 *  serialVersionUID
 *   - A version number for the class. On readObject(), the stored UID must
 *     match the current class's UID, else InvalidClassException.
 *   - If you don't declare it, the JVM computes one from the class structure,
 *     so even adding a method can break old saved data. ALWAYS declare it:
 *        private static final long serialVersionUID = 1L;
 *
 *  CUSTOMIZING
 *   - private void writeObject(ObjectOutputStream) / readObject(ObjectInputStream)
 *     hooks: add encryption, validation, write transient fields manually.
 *   - readResolve(): replace the deserialized object (keeps Singletons single).
 *   - Externalizable: you write EVERYTHING yourself (writeExternal/readExternal),
 *     needs a public no-arg constructor. Faster/smaller but more work.
 *
 *  SECURITY WARNING (important for senior roles)
 *   Deserializing UNTRUSTED bytes can execute attacker-chosen code paths
 *   ("deserialization gadget" attacks). Never readObject() from untrusted
 *   sources. Use ObjectInputFilter to allow-list classes, or prefer JSON
 *   (Jackson), Protobuf, Avro for data exchange. Modern advice: avoid native
 *   Java serialization for new designs.
 */

import java.io.*;
import java.util.*;

public class SerializationDemo {

    public static void main(String[] args) throws Exception {
        basicSerialization();
        transientAndStatic();
        constructorBehaviour();
        customHooks();
        singletonProblem();
        externalizable();
        deepCopyViaSerialization();
        filterUntrustedInput();
    }

    // ---------- helpers: object <-> byte[] (in memory, no files needed) ----------
    static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }
        return bos.toByteArray();
    }

    static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ois.readObject();
        }
    }

    // =================================================================
    // 1. Basic round trip
    // =================================================================
    static class Address implements Serializable {
        private static final long serialVersionUID = 1L;
        String city;
        Address(String city) { this.city = city; }
    }

    static class Employee implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        int age;
        Address address;               // must also be Serializable
        List<String> skills;           // ArrayList is Serializable

        Employee(String name, int age, Address address, List<String> skills) {
            this.name = name;
            this.age = age;
            this.address = address;
            this.skills = skills;
        }

        @Override
        public String toString() {
            return name + "(" + age + ", " + address.city + ", " + skills + ")";
        }
    }

    static void basicSerialization() throws Exception {
        System.out.println("===== 1. BASIC ROUND TRIP =====");
        Employee original = new Employee("Ravi", 30, new Address("Pune"), new ArrayList<>(List.of("Java", "SQL")));
        byte[] bytes = serialize(original);
        Employee copy = (Employee) deserialize(bytes);

        System.out.println("serialized size = " + bytes.length + " bytes");
        System.out.println("original = " + original);
        System.out.println("copy     = " + copy);
        System.out.println("same object? " + (original == copy) + " (a brand new object was built)");
    }

    // =================================================================
    // 2. transient & static are not saved
    // =================================================================
    static class UserAccount implements Serializable {
        private static final long serialVersionUID = 1L;
        static String appVersion = "v1";   // class-level, not part of object state
        String username;
        transient String password;         // skipped on purpose
        transient int loginCount;

        UserAccount(String username, String password) {
            this.username = username;
            this.password = password;
            this.loginCount = 5;
        }
    }

    static void transientAndStatic() throws Exception {
        System.out.println("\n===== 2. TRANSIENT & STATIC =====");
        UserAccount acc = new UserAccount("satyam", "s3cret");
        byte[] bytes = serialize(acc);
        UserAccount.appVersion = "v2";      // change static AFTER serializing

        UserAccount back = (UserAccount) deserialize(bytes);
        System.out.println("username   = " + back.username);
        System.out.println("password   = " + back.password + "   (transient -> null)");
        System.out.println("loginCount = " + back.loginCount + "      (transient -> 0)");
        System.out.println("appVersion = " + UserAccount.appVersion + "     (static: current class value, not saved)");
    }

    // =================================================================
    // 3. Which constructors run during deserialization?
    // =================================================================
    static class Base {                               // NOT Serializable
        int baseValue;
        Base() {                                      // REQUIRED no-arg constructor
            System.out.println("   Base() no-arg constructor called");
            baseValue = -1;
        }
    }

    static class Child extends Base implements Serializable {
        private static final long serialVersionUID = 1L;
        int childValue;
        Child(int b, int c) {
            System.out.println("   Child(int,int) constructor called");
            baseValue = b;
            childValue = c;
        }
    }

    static void constructorBehaviour() throws Exception {
        System.out.println("\n===== 3. CONSTRUCTORS =====");
        System.out.println("creating:");
        Child c = new Child(10, 20);
        byte[] bytes = serialize(c);
        System.out.println("deserializing:");
        Child back = (Child) deserialize(bytes);
        System.out.println("childValue = " + back.childValue + " (restored from bytes)");
        System.out.println("baseValue  = " + back.baseValue + " (parent not Serializable -> its ctor re-ran)");
    }

    // =================================================================
    // 4. Custom writeObject / readObject hooks
    //    Must be PRIVATE with these exact signatures; the JVM finds them
    //    via reflection.
    // =================================================================
    static class SecureCard implements Serializable {
        private static final long serialVersionUID = 1L;
        String holder;
        transient String cardNumber;   // we'll write it ourselves, "encrypted"

        SecureCard(String holder, String cardNumber) {
            this.holder = holder;
            this.cardNumber = cardNumber;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();                                  // writes non-transient fields
            out.writeObject(new StringBuilder(cardNumber).reverse().toString()); // toy "encryption"
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();                                    // reads non-transient fields
            String enc = (String) in.readObject();
            cardNumber = new StringBuilder(enc).reverse().toString();  // "decrypt"
            if (holder == null || holder.isBlank()) {                  // validate on read
                throw new InvalidObjectException("holder required");
            }
        }
    }

    static void customHooks() throws Exception {
        System.out.println("\n===== 4. CUSTOM HOOKS =====");
        SecureCard card = new SecureCard("Asha", "1234-5678");
        SecureCard back = (SecureCard) deserialize(serialize(card));
        System.out.println("restored holder = " + back.holder + ", card = " + back.cardNumber);
    }

    // =================================================================
    // 5. Serialization breaks Singletons -> fix with readResolve()
    //    (Enum singletons are immune: enums serialize by name.)
    // =================================================================
    static class AppConfig implements Serializable {
        private static final long serialVersionUID = 1L;
        static final AppConfig INSTANCE = new AppConfig();
        private AppConfig() {}

        // Called after deserialization; the returned object replaces the new one
        private Object readResolve() {
            return INSTANCE;
        }
    }

    static void singletonProblem() throws Exception {
        System.out.println("\n===== 5. SINGLETON & readResolve =====");
        AppConfig back = (AppConfig) deserialize(serialize(AppConfig.INSTANCE));
        System.out.println("same singleton after deserialization? " + (back == AppConfig.INSTANCE));
    }

    // =================================================================
    // 6. Externalizable - full manual control
    // =================================================================
    static class Product implements Externalizable {
        String name;
        double price;

        public Product() {}            // REQUIRED: public no-arg ctor (called on read)

        Product(String name, double price) { this.name = name; this.price = price; }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(name);
            out.writeDouble(price);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            name = in.readUTF();        // read in the SAME order as written
            price = in.readDouble();
        }
    }

    static void externalizable() throws Exception {
        System.out.println("\n===== 6. EXTERNALIZABLE =====");
        Product back = (Product) deserialize(serialize(new Product("Laptop", 75_000)));
        System.out.println("product = " + back.name + " @ " + back.price);
    }

    // =================================================================
    // 7. Deep copy trick: serialize then deserialize = independent clone
    //    (simple but slow; copy constructors are usually better)
    // =================================================================
    static void deepCopyViaSerialization() throws Exception {
        System.out.println("\n===== 7. DEEP COPY =====");
        Employee e = new Employee("Kiran", 28, new Address("Delhi"), new ArrayList<>(List.of("Go")));
        Employee clone = (Employee) deserialize(serialize(e));
        clone.address.city = "Mumbai";
        clone.skills.add("Rust");
        System.out.println("original = " + e + "  (unchanged)");
        System.out.println("clone    = " + clone);
    }

    // =================================================================
    // 8. ObjectInputFilter (Java 9) - allow-list classes when reading
    // =================================================================
    static void filterUntrustedInput() throws Exception {
        System.out.println("\n===== 8. DESERIALIZATION FILTER =====");
        byte[] bytes = serialize(new Employee("X", 1, new Address("Y"), new ArrayList<>()));

        // Only allow java.lang.* / java.util.* classes; reject everything else
        ObjectInputFilter filter = ObjectInputFilter.Config.createFilter("java.lang.*;java.util.*;!*");
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            ois.setObjectInputFilter(filter);
            ois.readObject();
        } catch (InvalidClassException e) {
            System.out.println("rejected by filter: " + e.getMessage());
        }
    }
}
