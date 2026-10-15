package com.offline_First.data.staging

/**
 * Offline-first staging data for EduNova Java Developer Roadmap.
 * Strictly independent of Python roadmap data.
 */
object JavaRoadmapStagingData {

    // --- Major Spines & Fundamentals ---
    val NODE_JAVA = VisualRoadmapNode(
        id = "java_root",
        title = "Java",
        section = "Starting Point",
        isMajor = true,
        objectives = listOf("Understand Java platform & JVM architecture", "Install JDK & configure PATH"),
        explanation = "Java is a class-based, object-oriented programming language designed for portability and enterprise scale.",
        codeSnippet = "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, Java!\");\n    }\n}"
    )

    val NODE_LEARN_BASICS = VisualRoadmapNode(
        id = "java_learn_basics",
        title = "Learn the Basics",
        section = "Fundamentals",
        isMajor = true,
        objectives = listOf("Master basic Java syntax", "Understand data types and flow control"),
        explanation = "Foundational syntax and structure required for writing any Java application.",
        codeSnippet = "int age = 25;\nString name = \"EduNova\";\nSystem.out.printf(\"%s is %d years old\\n\", name, age);"
    )

    // Left branch under Learn the Basics
    val NODE_BASIC_SYNTAX = VisualRoadmapNode(
        id = "java_basic_syntax",
        title = "Basic Syntax",
        section = "Fundamentals",
        objectives = listOf("Identifiers, keywords, statements", "Main method signature"),
        explanation = "Structure of Java programs including class declaration, main method, and semicolons.",
        codeSnippet = "public class Greeting {\n    public static void main(String[] args) {\n        // Statement here\n    }\n}"
    )

    val NODE_LIFECYCLE = VisualRoadmapNode(
        id = "java_lifecycle",
        title = "Lifecycle of a Program",
        section = "Fundamentals",
        objectives = listOf("Java compilation javac -> .class bytecode", "JVM ClassLoader, execution engine, JIT compiler"),
        explanation = "Java source code is compiled into bytecode executed on the Java Virtual Machine (JVM).",
        codeSnippet = "// 1. javac App.java -> App.class\n// 2. java App (JVM executes bytecode)"
    )

    val NODE_DATA_TYPES = VisualRoadmapNode(
        id = "java_data_types",
        title = "Data Types",
        section = "Fundamentals",
        objectives = listOf("8 primitives: byte, short, int, long, float, double, char, boolean", "Reference types"),
        explanation = "Java is strongly typed with primitive types for performance and reference types for objects.",
        codeSnippet = "int count = 42;\ndouble price = 19.99;\nboolean isActive = true;\nchar grade = 'A';"
    )

    val NODE_VARIABLES_SCOPES = VisualRoadmapNode(
        id = "java_variables_scopes",
        title = "Variables and Scopes",
        section = "Fundamentals",
        objectives = listOf("Local, instance, and class variables", "Block scope and shadowing"),
        explanation = "Rules defining variable declaration, memory lifetime, and visibility scope.",
        codeSnippet = "public class ScopeDemo {\n    static int globalCount; // Class var\n    int instanceId;         // Instance var\n    void run() { int local = 10; }\n}"
    )

    val NODE_TYPE_CASTING = VisualRoadmapNode(
        id = "java_type_casting",
        title = "Type Casting",
        section = "Fundamentals",
        objectives = listOf("Widening (implicit) casting", "Narrowing (explicit) casting & overflow risks"),
        explanation = "Converting a value from one primitive data type to another.",
        codeSnippet = "double d = 100.5;\nint i = (int) d; // Explicit cast: 100"
    )

    // Right branch under Learn the Basics
    val NODE_STRINGS_METHODS = VisualRoadmapNode(
        id = "java_strings_methods",
        title = "Strings and Methods",
        section = "Fundamentals",
        objectives = listOf("String immutability & String Pool", "StringBuilder vs StringBuffer"),
        explanation = "Handling text efficiently with Java's immutable String class and mutable builders.",
        codeSnippet = "String s = \"Java\";\nStringBuilder sb = new StringBuilder(s);\nsb.append(\" 21\");\nString res = sb.toString();"
    )

    val NODE_MATH_OPERATIONS = VisualRoadmapNode(
        id = "java_math_operations",
        title = "Math Operations",
        section = "Fundamentals",
        objectives = listOf("Arithmetic, bitwise, comparison operators", "java.lang.Math utility methods"),
        explanation = "Built-in mathematical operations and helper utilities in java.lang.Math.",
        codeSnippet = "double root = Math.sqrt(25.0);\nint max = Math.max(10, 20);\ndouble pow = Math.pow(2, 8);"
    )

    val NODE_ARRAYS = VisualRoadmapNode(
        id = "java_arrays",
        title = "Arrays",
        section = "Fundamentals",
        objectives = listOf("1D and multidimensional arrays", "Arrays utility class (sort, binarySearch)"),
        explanation = "Fixed-length indexed collections storing elements of the same type.",
        codeSnippet = "int[] nums = new int[]{3, 1, 4, 1, 5};\njava.util.Arrays.sort(nums);\nint len = nums.length;"
    )

    val NODE_CONDITIONALS = VisualRoadmapNode(
        id = "java_conditionals",
        title = "Conditionals",
        section = "Fundamentals",
        objectives = listOf("if, else if, else branching", "Switch statements & pattern matching (Java 17+)"),
        explanation = "Branching execution flow based on boolean expressions and patterns.",
        codeSnippet = "String status = switch(code) {\n    case 200 -> \"OK\";\n    case 404 -> \"Not Found\";\n    default -> \"Unknown\";\n};"
    )

    val NODE_LOOPS = VisualRoadmapNode(
        id = "java_loops",
        title = "Loops",
        section = "Fundamentals",
        objectives = listOf("for, enhanced for-each", "while, do-while, break, and continue"),
        explanation = "Repetitive execution control structures for iterating over sequences.",
        codeSnippet = "for (int n : List.of(1, 2, 3)) {\n    System.out.println(n);\n}"
    )

    val NODE_BASICS_OF_OOP_START = VisualRoadmapNode(
        id = "java_basics_oop_start",
        title = "Basics of OOP",
        section = "Fundamentals",
        objectives = listOf("Objects and blueprints", "4 core pillars: Encapsulation, Inheritance, Polymorphism, Abstraction"),
        explanation = "Introduction to the object-oriented paradigm forming the foundation of Java.",
        codeSnippet = "Car myCar = new Car(\"Model S\");\nmyCar.drive();"
    )

    // --- Object-Oriented Programming Section ---
    val NODE_OOP = VisualRoadmapNode(
        id = "java_oop",
        title = "Object Oriented Programming",
        section = "OOP",
        isMajor = true,
        objectives = listOf("Design reusable domain models", "Implement abstraction and encapsulation"),
        explanation = "Java's core design philosophy: modeling systems through interacting classes and objects.",
        codeSnippet = "public class BankAccount {\n    private double balance;\n    public void deposit(double amt) { balance += amt; }\n}"
    )

    // Basics of OOP box (left side of OOP)
    val NODE_CLASSES_OBJECTS = VisualRoadmapNode(
        id = "java_classes_objects",
        title = "Classes and Objects",
        section = "OOP",
        objectives = listOf("Class blueprints vs object instances", "Constructor overloading"),
        explanation = "Classes define structure and state; objects are concrete heap instances.",
        codeSnippet = "public class User {\n    String name;\n    User(String n) { this.name = n; }\n}"
    )

    val NODE_ATTRIBUTES_METHODS = VisualRoadmapNode(
        id = "java_attributes_methods",
        title = "Attributes and Methods",
        section = "OOP",
        objectives = listOf("Field declarations and defaults", "Method signatures and return types"),
        explanation = "Attributes represent internal state; methods define behaviors and actions.",
        codeSnippet = "public void setName(String name) {\n    this.name = name;\n}"
    )

    val NODE_ACCESS_SPECIFIERS = VisualRoadmapNode(
        id = "java_access_specifiers",
        title = "Access Specifiers",
        section = "OOP",
        objectives = listOf("public, protected, package-private (default), private", "Information hiding"),
        explanation = "Encapsulation modifiers controlling member visibility across packages.",
        codeSnippet = "private int secretKey;\npublic int getPublicKey() { return 123; }"
    )

    val NODE_STATIC_KEYWORD = VisualRoadmapNode(
        id = "java_static_keyword",
        title = "Static Keyword",
        section = "OOP",
        objectives = listOf("Static variables and class methods", "Static initialization blocks"),
        explanation = "Members belonging to the class itself rather than individual instances.",
        codeSnippet = "public static final double PI = 3.14159;\npublic static int add(int a, int b) { return a + b; }"
    )

    val NODE_FINAL_KEYWORD = VisualRoadmapNode(
        id = "java_final_keyword",
        title = "Final Keyword",
        section = "OOP",
        objectives = listOf("Final variables (constants)", "Final methods (prevent override) & classes (prevent inheritance)"),
        explanation = "Immutability and inheritance prevention modifier in Java.",
        codeSnippet = "final int MAX_RETRIES = 3;\npublic final class ImmutableValue { }"
    )

    val NODE_NESTED_CLASSES = VisualRoadmapNode(
        id = "java_nested_classes",
        title = "Nested Classes",
        section = "OOP",
        objectives = listOf("Static nested classes", "Inner member classes, local classes, anonymous classes"),
        explanation = "Defining classes logically grouped within another outer class.",
        codeSnippet = "public class Outer {\n    static class StaticNested { }\n    class Inner { }\n}"
    )

    val NODE_PACKAGES = VisualRoadmapNode(
        id = "java_packages",
        title = "Packages",
        section = "OOP",
        objectives = listOf("Package naming conventions", "Import statements & static imports"),
        explanation = "Namespaces for grouping related classes and interfaces avoiding naming conflicts.",
        codeSnippet = "package com.edunova.service;\nimport java.util.List;"
    )

    // More about OOP box (center under OOP)
    val NODE_OBJECT_LIFECYCLE = VisualRoadmapNode(
        id = "java_object_lifecycle",
        title = "Object Lifecycle",
        section = "OOP",
        objectives = listOf("Allocation on heap", "Garbage collection and finalize/Cleaner"),
        explanation = "How Java objects are created in memory, referenced, and collected by the GC.",
        codeSnippet = "User u = new User(); // Allocation\nu = null; // Eligible for Garbage Collection"
    )

    val NODE_INHERITANCE = VisualRoadmapNode(
        id = "java_inheritance",
        title = "Inheritance",
        section = "OOP",
        objectives = listOf("extends keyword", "super constructor & method invocation"),
        explanation = "Mechanism where a child class acquires fields and methods of a parent class.",
        codeSnippet = "public class Dog extends Animal {\n    @Override\n    public void speak() { System.out.println(\"Woof!\"); }\n}"
    )

    val NODE_ABSTRACTION = VisualRoadmapNode(
        id = "java_abstraction",
        title = "Abstraction",
        section = "OOP",
        objectives = listOf("abstract classes vs interfaces", "Hiding implementation details"),
        explanation = "Exposing only essential features while hiding complex internal mechanics.",
        codeSnippet = "public abstract class Shape {\n    abstract double area();\n}"
    )

    val NODE_METHOD_CHAINING = VisualRoadmapNode(
        id = "java_method_chaining",
        title = "Method Chaining",
        section = "OOP",
        objectives = listOf("Fluent APIs and Builder pattern", "Returning this"),
        explanation = "Calling multiple methods consecutively on the same object reference.",
        codeSnippet = "sb.append(\"Hello\").append(\" \").append(\"World\");"
    )

    val NODE_ENCAPSULATION = VisualRoadmapNode(
        id = "java_encapsulation",
        title = "Encapsulation",
        section = "OOP",
        objectives = listOf("Private fields and public accessors", "Maintaining internal invariants"),
        explanation = "Wrapping data and code into a single unit, protecting state from unauthorized modification.",
        codeSnippet = "public class Account {\n    private double bal;\n    public double getBal() { return bal; }\n}"
    )

    val NODE_INTERFACES = VisualRoadmapNode(
        id = "java_interfaces",
        title = "Interfaces",
        section = "OOP",
        objectives = listOf("Contracts with implements", "Default and static methods (Java 8+)"),
        explanation = "Abstract contracts specifying behaviors that implementing classes must provide.",
        codeSnippet = "public interface PaymentProcessor {\n    void processPayment(double amount);\n}"
    )

    val NODE_ENUMS = VisualRoadmapNode(
        id = "java_enums",
        title = "Enums",
        section = "OOP",
        objectives = listOf("Type-safe constants", "Enums with fields, constructors, and methods"),
        explanation = "Special class representing a fixed set of predefined constants.",
        codeSnippet = "public enum Status {\n    PENDING, PROCESSING, COMPLETED;\n}"
    )

    val NODE_RECORD = VisualRoadmapNode(
        id = "java_record",
        title = "Record",
        section = "OOP",
        objectives = listOf("Compact immutable data carriers (Java 16+)", "Automatic equals, hashCode, toString"),
        explanation = "Concise syntax for declaring immutable nominal data carrier classes.",
        codeSnippet = "public record Point(int x, int y) { }"
    )

    val NODE_METHOD_OVERLOAD_OVERRIDE = VisualRoadmapNode(
        id = "java_method_overload_override",
        title = "Method Overloading / Overriding",
        section = "OOP",
        objectives = listOf("Compile-time polymorphism (overload)", "Runtime polymorphism with @Override"),
        explanation = "Same method name with different parameters vs redefining inherited parent behavior.",
        codeSnippet = "int sum(int a, int b) { return a+b; }\ndouble sum(double a, double b) { return a+b; }"
    )

    val NODE_INITIALIZER_BLOCK = VisualRoadmapNode(
        id = "java_initializer_block",
        title = "Initializer Block",
        section = "OOP",
        objectives = listOf("Instance initializer blocks", "Static initializer blocks"),
        explanation = "Blocks of code executed when class is loaded (static) or object is instantiated.",
        codeSnippet = "static {\n    // Runs once when class is loaded\n}"
    )

    val NODE_STATIC_DYNAMIC_BINDING = VisualRoadmapNode(
        id = "java_static_dynamic_binding",
        title = "Static vs Dynamic Binding",
        section = "OOP",
        objectives = listOf("Early binding for static/private/final", "Late/virtual method lookup at runtime"),
        explanation = "Connecting a method call to its method body during compilation vs execution.",
        codeSnippet = "Animal a = new Dog();\na.speak(); // Dynamic dispatch calls Dog.speak()"
    )

    val NODE_PASS_VALUE_REFERENCE = VisualRoadmapNode(
        id = "java_pass_value_reference",
        title = "Pass by Value / Pass by Reference",
        section = "OOP",
        objectives = listOf("Java is strictly pass-by-value", "Passing object reference copies"),
        explanation = "Clarifying how arguments are passed into Java methods: values of primitives and reference copies.",
        codeSnippet = "void modify(List<String> list) {\n    list.add(\"Item\"); // Mutates caller object\n    list = new ArrayList<>(); // Reassignment doesn't affect caller\n}"
    )

    // Right side of OOP: Advanced Features
    val NODE_EXCEPTION_HANDLING = VisualRoadmapNode(
        id = "java_exception_handling",
        title = "Exception Handling",
        section = "Language Features",
        isMajor = true,
        objectives = listOf("Checked vs unchecked exceptions", "try-with-resources and custom exceptions"),
        explanation = "Structured runtime error handling preventing unexpected application termination.",
        codeSnippet = "try (var br = new BufferedReader(new FileReader(\"file.txt\"))) {\n    return br.readLine();\n} catch (IOException e) {\n    log.error(\"Read failed\", e);\n}"
    )

    val NODE_LAMBDAS = VisualRoadmapNode(
        id = "java_lambdas",
        title = "Lambda Expressions",
        section = "Language Features",
        isMajor = true,
        objectives = listOf("Anonymous inline functions", "Method references :: and Functional Interfaces"),
        explanation = "Concise functional programming syntax introduced in Java 8.",
        codeSnippet = "List<String> names = List.of(\"Alice\", \"Bob\");\nnames.forEach(System.out::println);"
    )

    val NODE_ANNOTATIONS = VisualRoadmapNode(
        id = "java_annotations",
        title = "Annotations",
        section = "Language Features",
        isMajor = true,
        objectives = listOf("Built-in annotations (@Override, @Deprecated)", "Custom annotations and reflection retention"),
        explanation = "Metadata added to Java source code interpreted at compile-time or runtime.",
        codeSnippet = "@Target(ElementType.METHOD)\n@Retention(RetentionPolicy.RUNTIME)\npublic @interface LogExecutionTime { }"
    )

    val NODE_MODULES = VisualRoadmapNode(
        id = "java_modules",
        title = "Modules",
        section = "Language Features",
        isMajor = true,
        objectives = listOf("JPMS (Java Platform Module System)", "module-info.java with requires and exports"),
        explanation = "Higher-level aggregation of packages introducing strong encapsulation.",
        codeSnippet = "module com.edunova.core {\n    exports com.edunova.core.api;\n    requires java.sql;\n}"
    )

    val NODE_OPTIONALS = VisualRoadmapNode(
        id = "java_optionals",
        title = "Optionals",
        section = "Language Features",
        isMajor = true,
        objectives = listOf("Avoiding NullPointerException", "map, flatMap, orElseThrow idioms"),
        explanation = "Container object representing presence or absence of a non-null value.",
        codeSnippet = "Optional<User> user = findById(id);\nString name = user.map(User::name).orElse(\"Guest\");"
    )

    val NODE_GENERICS = VisualRoadmapNode(
        id = "java_generics",
        title = "Generics",
        section = "Language Features",
        isMajor = true,
        objectives = listOf("Type parameters and type safety", "Wildcards (? extends / ? super) and type erasure"),
        explanation = "Enables types (classes and interfaces) to be parameters when defining classes, interfaces and methods.",
        codeSnippet = "public class Box<T> {\n    private T item;\n    public void set(T item) { this.item = item; }\n}"
    )

    val NODE_REFLECTION = VisualRoadmapNode(
        id = "java_reflection",
        title = "Reflection",
        section = "Language Features",
        isMajor = true,
        objectives = listOf("Class<T> inspection at runtime", "Inspecting and invoking methods/constructors dynamically"),
        explanation = "API providing runtime inspection and modification of classes, fields, methods, and annotations.",
        codeSnippet = "Class<?> clazz = User.class;\nMethod method = clazz.getMethod(\"getName\");"
    )

    // --- Concurrency, Collections & Architecture ---
    val NODE_CONCURRENCY = VisualRoadmapNode(
        id = "java_concurrency",
        title = "Concurrency",
        section = "Concurrency",
        isMajor = true,
        objectives = listOf("Multithreading principles", "Thread safety, race conditions, and deadlocks"),
        explanation = "Simultaneous execution of multiple threads sharing memory and CPU resources.",
        codeSnippet = "ExecutorService executor = Executors.newFixedThreadPool(4);\nexecutor.submit(() -> doWork());"
    )

    // Concurrency left branch
    val NODE_CRYPTOGRAPHY = VisualRoadmapNode(
        id = "java_cryptography",
        title = "Cryptography",
        section = "Security",
        isMajor = true,
        objectives = listOf("Java Cryptography Architecture (JCA)", "Hashing (SHA-256), AES encryption, digital signatures"),
        explanation = "Securing application data and communications using standard Java security APIs.",
        codeSnippet = "MessageDigest digest = MessageDigest.getInstance(\"SHA-256\");\nbyte[] hash = digest.digest(text.getBytes());"
    )

    val NODE_DATE_TIME = VisualRoadmapNode(
        id = "java_date_time",
        title = "Date and Time",
        section = "Standard Library",
        isMajor = true,
        objectives = listOf("java.time (LocalDate, ZonedDateTime, Instant)", "Duration and Period calculations"),
        explanation = "Modern immutable and thread-safe date-time API introduced in Java 8.",
        codeSnippet = "LocalDate today = LocalDate.now();\nLocalDate nextWeek = today.plusDays(7);"
    )

    val NODE_NETWORKING = VisualRoadmapNode(
        id = "java_networking",
        title = "Networking",
        section = "Standard Library",
        isMajor = true,
        objectives = listOf("java.net.http.HttpClient", "TCP Sockets and URL connections"),
        explanation = "Building network clients, REST requests, and socket servers in Java.",
        codeSnippet = "HttpClient client = HttpClient.newHttpClient();\nHttpRequest req = HttpRequest.newBuilder(uri).build();\nHttpResponse<String> res = client.send(req, BodyHandlers.ofString());"
    )

    val NODE_REGEX = VisualRoadmapNode(
        id = "java_regex",
        title = "Regular Expressions",
        section = "Standard Library",
        isMajor = true,
        objectives = listOf("java.util.regex (Pattern and Matcher)", "Pattern compilation and group extraction"),
        explanation = "High-performance text pattern search, validation, and manipulation.",
        codeSnippet = "Pattern pattern = Pattern.compile(\"\\\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Z]{2,}\\\\b\", Pattern.CASE_INSENSITIVE);\nMatcher m = pattern.matcher(email);"
    )

    // Concurrency subtopics
    val NODE_VOLATILE = VisualRoadmapNode(
        id = "java_volatile",
        title = "volatile keyword",
        section = "Concurrency",
        objectives = listOf("Visibility guarantee across CPU caches", "Instruction reordering barrier"),
        explanation = "Ensures reads and writes to a variable are direct to main memory without cache delay.",
        codeSnippet = "private volatile boolean running = true;\npublic void stop() { running = false; }"
    )

    val NODE_JVM_MEMORY = VisualRoadmapNode(
        id = "java_jvm_memory",
        title = "Java Memory Model",
        section = "Concurrency",
        objectives = listOf("Heap vs Stack memory", "Happens-before relationship"),
        explanation = "Specification defining how threads interact through memory and visible state changes.",
        codeSnippet = "// JMM guarantees thread visibility through happens-before rules"
    )

    val NODE_VIRTUAL_THREADS = VisualRoadmapNode(
        id = "java_virtual_threads",
        title = "Virtual Threads",
        section = "Concurrency",
        objectives = listOf("Project Loom lightweight threads (Java 21+)", "High-throughput I/O bound concurrency"),
        explanation = "User-mode threads managed by the JVM that scale to millions of concurrent tasks.",
        codeSnippet = "try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {\n    executor.submit(() -> fetchHttp());\n}"
    )

    val NODE_THREADS = VisualRoadmapNode(
        id = "java_threads",
        title = "Threads",
        section = "Concurrency",
        objectives = listOf("OS platform threads", "Thread lifecycle and synchronization primitives"),
        explanation = "Underlying OS threads running concurrent instructions in Java.",
        codeSnippet = "Thread t = new Thread(() -> System.out.println(\"Running\"));\nt.start();"
    )

    val NODE_SYNCHRONIZATION = VisualRoadmapNode(
        id = "java_synchronization",
        title = "Synchronization",
        section = "Concurrency",
        objectives = listOf("synchronized methods and blocks", "Intrinsic object locks (monitor) and deadlock prevention"),
        explanation = "Coordination of concurrent thread access to shared mutable resources to prevent race conditions.",
        codeSnippet = "public synchronized void increment() {\n    count++;\n}"
    )

    val NODE_EXECUTORS = VisualRoadmapNode(
        id = "java_executors",
        title = "Executors",
        section = "Concurrency",
        objectives = listOf("Executor and ExecutorService interfaces", "Decoupling task submission from execution mechanics"),
        explanation = "High-level thread management framework replacing manual thread instantiation and management.",
        codeSnippet = "ExecutorService executor = Executors.newFixedThreadPool(2);\nFuture<String> future = executor.submit(() -> \"Done\");"
    )

    val NODE_THREAD_POOLS = VisualRoadmapNode(
        id = "java_thread_pools",
        title = "Thread Pools",
        section = "Concurrency",
        objectives = listOf("ThreadPoolExecutor configuration", "Core pool size, maximum pool size, work queue and rejection policies"),
        explanation = "Reusing worker threads to execute multiple tasks efficiently and prevent thread allocation overhead.",
        codeSnippet = "ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());"
    )

    // Collections container
    val NODE_COLLECTIONS_FRAMEWORK = VisualRoadmapNode(
        id = "java_collections_framework",
        title = "Collections Framework",
        section = "Collections",
        isMajor = true,
        objectives = listOf("Iterable and Collection root hierarchies", "Choosing the right data structure for algorithmic efficiency"),
        explanation = "Unified architecture for representing and manipulating collections of objects in Java.",
        codeSnippet = "Collection<String> coll = new ArrayList<>();"
    )

    val NODE_LINKED_LIST = VisualRoadmapNode(
        id = "java_linked_list",
        title = "LinkedList",
        section = "Collections",
        objectives = listOf("Doubly-linked list implementation", "O(1) insertion/deletion vs O(n) random access"),
        explanation = "Sequential access list implementing both List and Deque interfaces.",
        codeSnippet = "LinkedList<String> list = new LinkedList<>();\nlist.addFirst(\"Head\");"
    )

    // Collections container
    val NODE_ARRAY_VS_ARRAYLIST = VisualRoadmapNode(
        id = "java_array_vs_arraylist",
        title = "Array vs ArrayList",
        section = "Collections",
        objectives = listOf("Fixed-size primitive arrays vs dynamic resizing", "Generic type support"),
        explanation = "Comparing fixed primitive arrays with the dynamically resizable ArrayList.",
        codeSnippet = "List<String> list = new ArrayList<>();\nlist.add(\"Item 1\");"
    )

    val NODE_SET = VisualRoadmapNode(
        id = "java_set",
        title = "Set",
        section = "Collections",
        objectives = listOf("HashSet, LinkedHashSet, TreeSet", "Uniqueness and equals/hashCode contract"),
        explanation = "Collection storing unique elements with no duplicates.",
        codeSnippet = "Set<String> uniqueTags = new HashSet<>(List.of(\"java\", \"cloud\", \"java\"));"
    )

    val NODE_MAP = VisualRoadmapNode(
        id = "java_map",
        title = "Map",
        section = "Collections",
        objectives = listOf("HashMap, ConcurrentHashMap, TreeMap", "O(1) average lookup and key collisions"),
        explanation = "Key-value pair store mapping unique keys to values.",
        codeSnippet = "Map<String, Integer> scores = new HashMap<>();\nscores.put(\"Alice\", 95);"
    )

    val NODE_QUEUE = VisualRoadmapNode(
        id = "java_queue",
        title = "Queue",
        section = "Collections",
        objectives = listOf("FIFO (First In First Out)", "PriorityQueue and LinkedList"),
        explanation = "Collection designed for holding elements prior to processing.",
        codeSnippet = "Queue<Task> taskQueue = new LinkedList<>();\ntaskQueue.offer(new Task());"
    )

    val NODE_DEQUE = VisualRoadmapNode(
        id = "java_deque",
        title = "Deque",
        section = "Collections",
        objectives = listOf("Double-ended queue (FIFO and LIFO)", "ArrayDeque performance benefits"),
        explanation = "Linear collection supporting element insertion and removal at both endpoints.",
        codeSnippet = "Deque<String> stack = new ArrayDeque<>();\nstack.push(\"Page 1\");"
    )

    val NODE_STACK = VisualRoadmapNode(
        id = "java_stack",
        title = "Stack",
        section = "Collections",
        objectives = listOf("LIFO operations", "Why ArrayDeque is preferred over legacy Vector-based Stack"),
        explanation = "Last-In-First-Out data structure for unwinding operations and algorithms.",
        codeSnippet = "Deque<Integer> stack = new ArrayDeque<>();\nstack.push(10);\nint top = stack.pop();"
    )

    val NODE_ITERATOR = VisualRoadmapNode(
        id = "java_iterator",
        title = "Iterator",
        section = "Collections",
        objectives = listOf("hasNext, next, and safe remove", "Fail-fast vs fail-safe iterators"),
        explanation = "Standard interface for traversing any collection sequentially while removing elements safely.",
        codeSnippet = "Iterator<String> it = list.iterator();\nwhile(it.hasNext()) { if(it.next().isEmpty()) it.remove(); }"
    )

    val NODE_GENERIC_COLLECTIONS = VisualRoadmapNode(
        id = "java_generic_collections",
        title = "Generic Collections",
        section = "Collections",
        objectives = listOf("Compile-time type safety", "Wildcards (? extends T, ? super T)"),
        explanation = "Parameterized types ensuring type safety without runtime casting exceptions.",
        codeSnippet = "public void printList(List<? extends Number> numbers) {\n    numbers.forEach(System.out::println);\n}"
    )

    // Right of collections: Architecture
    val NODE_DEPENDENCY_INJECTION = VisualRoadmapNode(
        id = "java_dependency_injection",
        title = "Dependency Injection",
        section = "Architecture",
        isMajor = true,
        objectives = listOf("Inversion of Control (IoC)", "Constructor injection vs Field injection"),
        explanation = "Design pattern where an object receives its dependencies from an external container.",
        codeSnippet = "@Service\npublic class UserService {\n    private final UserRepository repo;\n    public UserService(UserRepository r) { this.repo = r; }\n}"
    )

    val NODE_IO_OPERATIONS = VisualRoadmapNode(
        id = "java_io_operations",
        title = "I/O Operations",
        section = "I/O",
        isMajor = true,
        objectives = listOf("InputStream and OutputStream", "Reader and Writer character streams"),
        explanation = "Handling sequential input and output data streams in Java.",
        codeSnippet = "InputStream in = new FileInputStream(\"data.bin\");\nint byteRead = in.read();"
    )

    val NODE_FILE_OPERATIONS = VisualRoadmapNode(
        id = "java_file_operations",
        title = "File Operations",
        section = "I/O",
        isMajor = true,
        objectives = listOf("java.nio.file.Files and Path", "Reading, writing, and copying files efficiently"),
        explanation = "Modern NIO.2 file handling with non-blocking support and path operations.",
        codeSnippet = "Path path = Path.of(\"app.log\");\nFiles.writeString(path, \"System booted\\n\");"
    )

    // --- Functional Programming, Build Tools, Frameworks, Database ---
    val NODE_HIGH_ORDER_FUNCTIONS = VisualRoadmapNode(
        id = "java_high_order_functions",
        title = "High Order Functions",
        section = "Functional",
        objectives = listOf("Functions accepting or returning functions", "Function, Consumer, Supplier, Predicate"),
        explanation = "Functions that take other functions as arguments or produce functions as results.",
        codeSnippet = "Function<Integer, Integer> timesTwo = x -> x * 2;\nFunction<Integer, Integer> timesFour = timesTwo.andThen(timesTwo);"
    )

    val NODE_FUNCTIONAL_INTERFACES = VisualRoadmapNode(
        id = "java_functional_interfaces",
        title = "Functional Interfaces",
        section = "Functional",
        objectives = listOf("@FunctionalInterface contract", "Single Abstract Method (SAM)"),
        explanation = "Interfaces with exactly one abstract method eligible for lambda expressions.",
        codeSnippet = "@FunctionalInterface\npublic interface Transformer<T, R> {\n    R transform(T input);\n}"
    )

    val NODE_FUNCTIONAL_COMPOSITION = VisualRoadmapNode(
        id = "java_functional_composition",
        title = "Functional Composition",
        section = "Functional",
        objectives = listOf("andThen, compose, and, or", "Building complex logic from small functions"),
        explanation = "Combining two or more simpler functions into a single compound function pipeline.",
        codeSnippet = "Predicate<String> notNull = Objects::nonNull;\nPredicate<String> notEmpty = s -> !s.isEmpty();\nPredicate<String> valid = notNull.and(notEmpty);"
    )

    val NODE_STREAM_API = VisualRoadmapNode(
        id = "java_stream_api",
        title = "Stream API",
        section = "Functional",
        objectives = listOf("Intermediate operations (filter, map, sorted)", "Terminal operations (collect, reduce, count)"),
        explanation = "Declarative data processing pipelines over collections with lazy evaluation.",
        codeSnippet = "List<String> result = users.stream()\n    .filter(User::isActive)\n    .map(User::name)\n    .toList();"
    )

    val NODE_BUILD_TOOLS = VisualRoadmapNode(
        id = "java_build_tools",
        title = "Build Tools",
        section = "Build Tools",
        isMajor = true,
        objectives = listOf("Automating compilation, testing, and packaging", "Managing external dependencies"),
        explanation = "Tools that automate transforming Java source code into production deployable JARs and WARs.",
        codeSnippet = "./gradlew clean build\n./mvnw clean package"
    )

    val NODE_MAVEN = VisualRoadmapNode(
        id = "java_maven",
        title = "Maven",
        section = "Build Tools",
        objectives = listOf("pom.xml architecture", "Maven lifecycle: compile, test, package, install, deploy"),
        explanation = "Declarative XML-based build and dependency management standard for Java.",
        codeSnippet = "<dependency>\n    <groupId>org.springframework.boot</groupId>\n    <artifactId>spring-boot-starter-web</artifactId>\n</dependency>"
    )

    val NODE_GRADLE = VisualRoadmapNode(
        id = "java_gradle",
        title = "Gradle",
        section = "Build Tools",
        objectives = listOf("Kotlin DSL & Groovy DSL", "Incremental builds and task dependency graphs"),
        explanation = "Modern high-performance build automation tool adopted across Android and Spring ecosystems.",
        codeSnippet = "dependencies {\n    implementation(\"org.springframework.boot:spring-boot-starter-web\")\n}"
    )

    val NODE_BAZEL = VisualRoadmapNode(
        id = "java_bazel",
        title = "Bazel",
        section = "Build Tools",
        objectives = listOf("Hermetic builds and distributed caching", "Monorepo multi-language support"),
        explanation = "Google's open-source build tool designed for large-scale monorepos.",
        codeSnippet = "java_binary(\n    name = \"server\",\n    srcs = glob([\"*.java\"]),\n)"
    )

    val NODE_WEB_FRAMEWORKS = VisualRoadmapNode(
        id = "java_web_frameworks",
        title = "Web Frameworks",
        section = "Web",
        isMajor = true,
        objectives = listOf("Building RESTful APIs and web services", "Selecting the right enterprise framework"),
        explanation = "Frameworks providing routing, security, dependency injection, and HTTP server capabilities.",
        codeSnippet = "@RestController\n@RequestMapping(\"/api\")\npublic class ApiController { }"
    )

    val NODE_SPRING_BOOT = VisualRoadmapNode(
        id = "java_spring_boot",
        title = "Spring (Spring Boot)",
        section = "Web",
        badge = "Recommended",
        objectives = listOf("Auto-configuration and starters", "Production-ready Actuator and embedded Tomcat"),
        explanation = "The dominant enterprise framework powering microservices, APIs, and cloud backends.",
        codeSnippet = "@SpringBootApplication\npublic class App {\n    public static void main(String[] args) {\n        SpringApplication.run(App.class, args);\n    }\n}"
    )

    val NODE_QUARKUS = VisualRoadmapNode(
        id = "java_quarkus",
        title = "Quarkus",
        section = "Web",
        objectives = listOf("Supersonic Subatomic Java", "GraalVM native compilation for low memory and instant start"),
        explanation = "Kubernetes-native Java framework optimized for serverless containers.",
        codeSnippet = "@Path(\"/hello\")\npublic class HelloResource {\n    @GET\n    public String hello() { return \"Hello Quarkus!\"; }\n}"
    )

    val NODE_JAVALIN = VisualRoadmapNode(
        id = "java_javalin",
        title = "Javalin",
        section = "Web",
        objectives = listOf("Lightweight micro-framework", "Simple routing without reflection"),
        explanation = "Minimalist web framework for Java and Kotlin prioritizing simplicity.",
        codeSnippet = "var app = Javalin.create().start(7070);\napp.get(\"/\", ctx -> ctx.result(\"Hello World\"));"
    )

    val NODE_PLAY_FRAMEWORK = VisualRoadmapNode(
        id = "java_play_framework",
        title = "Play Framework",
        section = "Web",
        objectives = listOf("Asynchronous reactive architecture", "Akka / Pekko actor model integration"),
        explanation = "High-velocity reactive web framework built for scale.",
        codeSnippet = "public class HomeController extends Controller {\n    public Result index() { return ok(\"Welcome!\"); }\n}"
    )

    val NODE_DATABASE_ACCESS = VisualRoadmapNode(
        id = "java_database_access",
        title = "Database Access",
        section = "Database",
        isMajor = true,
        objectives = listOf("Relational databases and SQL integration", "Object-Relational Mapping (ORM)"),
        explanation = "Persisting application domain state reliably in SQL and NoSQL databases.",
        codeSnippet = "userRepo.findById(userId).ifPresent(u -> u.setActive(true));"
    )

    val NODE_JDBC = VisualRoadmapNode(
        id = "java_jdbc",
        title = "JDBC",
        section = "Database",
        objectives = listOf("Connection, PreparedStatement, ResultSet", "Low-level SQL execution without overhead"),
        explanation = "Standard low-level Java API for executing SQL queries directly.",
        codeSnippet = "try (PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\")) {\n    ps.setInt(1, 10);\n    ResultSet rs = ps.executeQuery();\n}"
    )

    val NODE_JPA = VisualRoadmapNode(
        id = "java_jpa",
        title = "JPA",
        section = "Database",
        objectives = listOf("Jakarta Persistence API specification", "EntityManager, EntityTransaction, JPQL queries"),
        explanation = "Standard Java specification for object-relational mapping and database persistence.",
        codeSnippet = "EntityManager em = emf.createEntityManager();\nUser u = em.find(User.class, 1L);"
    )

    val NODE_EBEAN = VisualRoadmapNode(
        id = "java_ebean",
        title = "EBean",
        section = "Database",
        objectives = listOf("Query bean generation", "Partial object queries and performance"),
        explanation = "Alternative ORM providing fast query-bean type-safe database queries.",
        codeSnippet = "Customer c = DB.find(Customer.class).where().eq(\"name\", \"Rob\").findOne();"
    )

    val NODE_HIBERNATE = VisualRoadmapNode(
        id = "java_hibernate",
        title = "Hibernate",
        section = "Database",
        objectives = listOf("JPA implementation & SessionFactory", "Caching (L1, L2) and lazy loading"),
        explanation = "Industry-leading ORM mapping Java objects to relational database tables.",
        codeSnippet = "@Entity\n@Table(name = \"users\")\npublic class User {\n    @Id @GeneratedValue\n    private Long id;\n}"
    )

    val NODE_SPRING_DATA_JPA = VisualRoadmapNode(
        id = "java_spring_data_jpa",
        title = "Spring Data JPA",
        section = "Database",
        objectives = listOf("Repository interfaces without boilerplate", "Derived query methods & @Query"),
        explanation = "Reduces boilerplate database access code to declarative repository interfaces.",
        codeSnippet = "public interface UserRepository extends JpaRepository<User, Long> {\n    List<User> findByEmailEndingWith(String domain);\n}"
    )

    // --- Documentation, Logging, Testing ---
    val NODE_DOCUMENTATION = VisualRoadmapNode(
        id = "java_documentation",
        title = "Documentation",
        section = "Documentation",
        isMajor = true,
        objectives = listOf("API specification and developer guidance", "Clear doc generation"),
        explanation = "Standard practices for documenting Java classes, methods, and public API interfaces.",
        codeSnippet = "/**\n * Calculates discount for order.\n * @param order the user order\n * @return discounted price\n */"
    )

    val NODE_JAVADOC = VisualRoadmapNode(
        id = "java_javadoc",
        title = "Javadoc",
        section = "Documentation",
        objectives = listOf("Tags: @param, @return, @throws, @see", "Generating HTML API documentation"),
        explanation = "Standard Java documentation generator producing clean HTML API reference docs.",
        codeSnippet = "javadoc -d docs src/com/edunova/*.java"
    )

    val NODE_LOGGING_FRAMEWORKS = VisualRoadmapNode(
        id = "java_logging_frameworks",
        title = "Logging Frameworks",
        section = "Logging",
        isMajor = true,
        objectives = listOf("Log levels: TRACE, DEBUG, INFO, WARN, ERROR", "Structured JSON logging"),
        explanation = "Capturing diagnostic execution logs essential for production monitoring and debugging.",
        codeSnippet = "private static final Logger log = LoggerFactory.getLogger(App.class);\nlog.info(\"User {} logged in\", username);"
    )

    val NODE_LOGBACK = VisualRoadmapNode(
        id = "java_logback",
        title = "Logback",
        section = "Logging",
        objectives = listOf("Default logging engine for Spring Boot", "logback.xml configuration and appenders"),
        explanation = "Successor to Log4j with high performance and native SLF4J support.",
        codeSnippet = "<appender name=\"STDOUT\" class=\"ch.qos.logback.core.ConsoleAppender\">\n</appender>"
    )

    val NODE_LOG4J2 = VisualRoadmapNode(
        id = "java_log4j2",
        title = "Log4j2",
        section = "Logging",
        objectives = listOf("Asynchronous loggers with LMAX Disruptor", "Low latency high-throughput logging"),
        explanation = "Next-generation logging engine engineered for extreme enterprise throughput.",
        codeSnippet = "<Configuration status=\"WARN\">\n  <Appenders><Console name=\"Log\"/></Appenders>\n</Configuration>"
    )

    val NODE_SLF4J = VisualRoadmapNode(
        id = "java_slf4j",
        title = "SLF4J",
        section = "Logging",
        objectives = listOf("Simple Logging Facade for Java", "Decoupling application code from logging implementations"),
        explanation = "Logging abstraction layer allowing switching underlying logging engines with zero code changes.",
        codeSnippet = "import org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;"
    )

    val NODE_TINYLOG = VisualRoadmapNode(
        id = "java_tinylog",
        title = "TinyLog",
        section = "Logging",
        objectives = listOf("Zero-configuration static logger", "Ultra-lightweight footprint"),
        explanation = "Simple, lightweight logging framework for small utilities and microservices.",
        codeSnippet = "import org.tinylog.Logger;\nLogger.info(\"System initialized\");"
    )

    val NODE_TESTING = VisualRoadmapNode(
        id = "java_testing",
        title = "Testing",
        section = "Testing",
        isMajor = true,
        objectives = listOf("Unit, integration, and end-to-end testing", "Continuous verification and code coverage"),
        explanation = "Systematic automated verification ensuring code correctness, regression prevention, and reliability.",
        codeSnippet = "@Test\nvoid testAddition() {\n    assertEquals(4, Calculator.add(2, 2));\n}"
    )

    val NODE_UNIT_TESTING = VisualRoadmapNode(
        id = "java_unit_testing",
        title = "Unit Testing",
        section = "Testing",
        objectives = listOf("Testing isolated units of code", "Fast feedback in CI/CD pipelines"),
        explanation = "Testing individual classes and methods in complete isolation.",
        codeSnippet = "@Test\nvoid testEmptyList() { assertTrue(list.isEmpty()); }"
    )

    val NODE_JUNIT = VisualRoadmapNode(
        id = "java_junit",
        title = "JUnit",
        section = "Testing",
        objectives = listOf("JUnit 5 Jupiter architecture", "@ParameterizedTest, @BeforeEach, assertAll"),
        explanation = "The standard unit testing framework across the entire Java programming language.",
        codeSnippet = "@ParameterizedTest\n@ValueSource(strings = {\"a\", \"b\"})\nvoid testInputs(String val) { assertNotNull(val); }"
    )

    val NODE_TESTNG = VisualRoadmapNode(
        id = "java_testng",
        title = "TestNG",
        section = "Testing",
        objectives = listOf("Flexible test configuration", "Data providers and parallel test execution"),
        explanation = "Testing framework covering end-to-end integration and complex test suites.",
        codeSnippet = "@Test(dataProvider = \"testData\")\npublic void verifyData(int n) { assert n > 0; }"
    )

    val NODE_INTEGRATION_TESTING = VisualRoadmapNode(
        id = "java_integration_testing",
        title = "Integration Testing",
        section = "Testing",
        objectives = listOf("Testing interconnected components", "Database and HTTP server lifecycle"),
        explanation = "Verifying that multiple modules, databases, and network services operate correctly together.",
        codeSnippet = "@SpringBootTest(webEnvironment = RANDOM_PORT)\nclass ApiIntegrationTest { }"
    )

    val NODE_REST_ASSURED = VisualRoadmapNode(
        id = "java_rest_assured",
        title = "REST Assured",
        section = "Testing",
        objectives = listOf("BDD HTTP API testing", "given(), when(), then() assertions"),
        explanation = "Domain-specific language for testing and validating HTTP REST services.",
        codeSnippet = "given().pathParam(\"id\", 1)\n.when().get(\"/users/{id}\")\n.then().statusCode(200);"
    )

    val NODE_JMETER = VisualRoadmapNode(
        id = "java_jmeter",
        title = "JMeter",
        section = "Testing",
        objectives = listOf("Performance and load testing", "Simulating concurrent user traffic"),
        explanation = "Apache performance testing tool measuring application latency and throughput under heavy load.",
        codeSnippet = "// JMeter test plan measuring latency under 5,000 requests/sec"
    )

    val NODE_BEHAVIOR_TESTING = VisualRoadmapNode(
        id = "java_behavior_testing",
        title = "Behavior Testing",
        section = "Testing",
        objectives = listOf("Behavior-Driven Development (BDD)", "Cucumber-JVM Gherkin syntax (Given, When, Then)"),
        explanation = "Writing executable user-story specifications in human-readable plain language.",
        codeSnippet = "Feature: User Login\n  Scenario: Successful login\n    Given user is on login page\n    When user enters valid credentials\n    Then user sees dashboard"
    )

    val NODE_MOCKING_MOCKITO = VisualRoadmapNode(
        id = "java_mocking_mockito",
        title = "Mocking > Mockito",
        section = "Testing",
        objectives = listOf("when(...).thenReturn(...)", "verify(mock).methodCall() assertions"),
        explanation = "Simulating external collaborators, databases, and HTTP clients during unit testing.",
        codeSnippet = "UserRepository mockRepo = mock(UserRepository.class);\nwhen(mockRepo.findById(1L)).thenReturn(Optional.of(testUser));"
    )

    // Complete list of all interactive visual nodes
    val ALL_VISUAL_NODES: List<VisualRoadmapNode> = listOf(
        NODE_JAVA,
        NODE_LEARN_BASICS,
        // Fundamentals left branch
        NODE_BASIC_SYNTAX,
        NODE_LIFECYCLE,
        NODE_DATA_TYPES,
        NODE_VARIABLES_SCOPES,
        NODE_TYPE_CASTING,
        // Fundamentals right branch
        NODE_STRINGS_METHODS,
        NODE_MATH_OPERATIONS,
        NODE_ARRAYS,
        NODE_CONDITIONALS,
        NODE_LOOPS,
        NODE_BASICS_OF_OOP_START,
        // OOP
        NODE_OOP,
        NODE_CLASSES_OBJECTS,
        NODE_ATTRIBUTES_METHODS,
        NODE_ACCESS_SPECIFIERS,
        NODE_STATIC_KEYWORD,
        NODE_FINAL_KEYWORD,
        NODE_NESTED_CLASSES,
        NODE_PACKAGES,
        NODE_OBJECT_LIFECYCLE,
        NODE_INHERITANCE,
        NODE_ABSTRACTION,
        NODE_METHOD_CHAINING,
        NODE_ENCAPSULATION,
        NODE_INTERFACES,
        NODE_ENUMS,
        NODE_RECORD,
        NODE_METHOD_OVERLOAD_OVERRIDE,
        NODE_INITIALIZER_BLOCK,
        NODE_STATIC_DYNAMIC_BINDING,
        NODE_PASS_VALUE_REFERENCE,
        // OOP right branch
        NODE_EXCEPTION_HANDLING,
        NODE_LAMBDAS,
        NODE_ANNOTATIONS,
        NODE_MODULES,
        NODE_OPTIONALS,
        NODE_GENERICS,
        NODE_REFLECTION,
        // Concurrency
        NODE_CONCURRENCY,
        NODE_CRYPTOGRAPHY,
        NODE_DATE_TIME,
        NODE_NETWORKING,
        NODE_REGEX,
        NODE_VOLATILE,
        NODE_JVM_MEMORY,
        NODE_VIRTUAL_THREADS,
        NODE_THREADS,
        NODE_SYNCHRONIZATION,
        NODE_EXECUTORS,
        NODE_THREAD_POOLS,
        // Collections
        NODE_COLLECTIONS_FRAMEWORK,
        NODE_ARRAY_VS_ARRAYLIST,
        NODE_LINKED_LIST,
        NODE_SET,
        NODE_MAP,
        NODE_QUEUE,
        NODE_DEQUE,
        NODE_STACK,
        NODE_ITERATOR,
        NODE_GENERIC_COLLECTIONS,
        NODE_DEPENDENCY_INJECTION,
        NODE_IO_OPERATIONS,
        NODE_FILE_OPERATIONS,
        // Functional & Build Tools
        NODE_HIGH_ORDER_FUNCTIONS,
        NODE_FUNCTIONAL_INTERFACES,
        NODE_FUNCTIONAL_COMPOSITION,
        NODE_STREAM_API,
        NODE_BUILD_TOOLS,
        NODE_MAVEN,
        NODE_GRADLE,
        NODE_BAZEL,
        // Web Frameworks
        NODE_WEB_FRAMEWORKS,
        NODE_SPRING_BOOT,
        NODE_QUARKUS,
        NODE_JAVALIN,
        NODE_PLAY_FRAMEWORK,
        // Database Access
        NODE_DATABASE_ACCESS,
        NODE_JDBC,
        NODE_JPA,
        NODE_EBEAN,
        NODE_HIBERNATE,
        NODE_SPRING_DATA_JPA,
        // Documentation & Logging
        NODE_DOCUMENTATION,
        NODE_JAVADOC,
        NODE_LOGGING_FRAMEWORKS,
        NODE_LOGBACK,
        NODE_LOG4J2,
        NODE_SLF4J,
        NODE_TINYLOG,
        // Testing
        NODE_TESTING,
        NODE_UNIT_TESTING,
        NODE_JUNIT,
        NODE_TESTNG,
        NODE_INTEGRATION_TESTING,
        NODE_REST_ASSURED,
        NODE_JMETER,
        NODE_BEHAVIOR_TESTING,
        NODE_MOCKING_MOCKITO
    )

    // --- Frequently Asked Questions (Strictly 5 Questions from Section 7) ---
    val JAVA_FAQS: List<RoadmapFaqItem> = listOf(
        RoadmapFaqItem(
            id = "faq_java_1",
            question = "What is a Java developer?",
            answer = "A Java developer designs, builds, and maintains robust, scalable backend systems, web applications, Android applications, and enterprise microservices. They leverage Java's platform independence, rich ecosystem, and modern frameworks like Spring Boot to build high-performance software."
        ),
        RoadmapFaqItem(
            id = "faq_java_2",
            question = "What should I learn before starting Java?",
            answer = "While no mandatory prerequisites exist, a basic grasp of general programming principles—such as variables, conditional branching, loops, and problem solving—makes learning Java faster. Java's strong typing also makes it an excellent first language for mastering Object-Oriented Programming (OOP) fundamentals."
        ),
        RoadmapFaqItem(
            id = "faq_java_3",
            question = "How long does it take to learn Java?",
            answer = "Learning core Java fundamentals, syntax, and object-oriented principles usually takes 2 to 3 months of consistent hands-on practice. Becoming job-ready with the enterprise ecosystem—including Spring Boot, REST APIs, relational databases, Docker, and automated testing—typically takes 6 to 9 months."
        ),
        RoadmapFaqItem(
            id = "faq_java_4",
            question = "What projects can I build with Java?",
            answer = "You can build diverse applications ranging from command-line utilities and multi-threaded data processors to production-grade Spring Boot REST APIs, real-time banking transaction engines, distributed chat services, Android mobile apps, and microservice architectures deployed on Kubernetes."
        ),
        RoadmapFaqItem(
            id = "faq_java_5",
            question = "Should I learn Spring Boot after Java fundamentals?",
            answer = "Yes! Once you are comfortable with core Java, OOP, collections, exception handling, and build tools like Maven or Gradle, Spring Boot is the most critical and widely adopted framework to learn for professional enterprise backend development and modern REST API development."
        )
    )

    // --- Java Practice Projects ---
    val JAVA_PROJECTS: List<RoadmapProjectItem> = listOf(
        RoadmapProjectItem(
            id = "proj_java_1",
            title = "CLI Student Management System",
            difficulty = "Beginner",
            estimatedHours = 10,
            description = "Build a console-based management application to record, search, and calculate grades for students.",
            relevantSkills = listOf("Java Syntax", "OOP", "Collections (ArrayList/Map)", "File I/O"),
            deliverables = listOf("Menu-driven CLI interface", "Enrollment and grade calculation logic", "JSON or CSV persistence"),
            instructions = "Create domain classes (Student, Course, Grade). Implement methods for enrolling students, recording scores, calculating GPAs, and saving records to disk using java.nio.file."
        ),
        RoadmapProjectItem(
            id = "proj_java_2",
            title = "Multi-threaded Log File Analyzer",
            difficulty = "Intermediate",
            estimatedHours = 16,
            description = "High-speed log parsing tool utilizing ExecutorService and regular expressions to extract metrics from gigabyte-sized log files.",
            relevantSkills = listOf("Concurrency", "ExecutorService", "Regex (Pattern/Matcher)", "Stream API"),
            deliverables = listOf("Concurrent file reader chunks", "IP address and HTTP status aggregation", "Performance benchmark report"),
            instructions = "Read large web server logs in parallel chunks using Virtual Threads or fixed thread pools. Aggregate 4xx/5xx error frequencies, top requesting IPs, and write summary reports."
        ),
        RoadmapProjectItem(
            id = "proj_java_3",
            title = "Spring Boot RESTful API with JPA",
            difficulty = "Intermediate",
            estimatedHours = 24,
            description = "Full production-ready REST service with CRUD operations, database persistence, validation, and automated tests.",
            relevantSkills = listOf("Spring Boot", "Spring Data JPA", "PostgreSQL / H2", "JUnit 5 & Mockito"),
            deliverables = listOf("REST endpoints with validation", "Relational database mapping", "Swagger/OpenAPI documentation", "Integration tests"),
            instructions = "Design an E-Commerce product catalog API with pagination, filtering, custom JPA repositories, global exception handling, and JUnit test coverage."
        ),
        RoadmapProjectItem(
            id = "proj_java_4",
            title = "Real-time Chat Server with Sockets",
            difficulty = "Advanced",
            estimatedHours = 28,
            description = "Distributed multi-client chat server using TCP Sockets, thread synchronization, and message broadcasting.",
            relevantSkills = listOf("Java Networking", "TCP Sockets", "Thread Synchronization", "Protocols"),
            deliverables = listOf("Multi-client socket server", "Broadcast and direct messaging protocols", "Graceful disconnect handling"),
            instructions = "Implement a ServerSocket accepting concurrent client connections. Use worker threads or virtual threads to listen for incoming messages and broadcast to connected client sockets."
        ),
        RoadmapProjectItem(
            id = "proj_java_5",
            title = "Microservices Order & Payment Engine",
            difficulty = "Advanced",
            estimatedHours = 40,
            description = "Event-driven enterprise microservices architecture with Spring Cloud, Docker, and Kafka.",
            relevantSkills = listOf("Spring Boot", "Spring Cloud", "Kafka / Messaging", "Docker", "Circuit Breakers"),
            deliverables = listOf("Order service and Payment service", "Kafka event streaming", "Docker Compose deployment setup", "Resilience4j fault tolerance"),
            instructions = "Architect two decoupled microservices communicating through Kafka topics for asynchronous payment confirmation, featuring health checks, API gateways, and containerized deployment."
        )
    )
}
