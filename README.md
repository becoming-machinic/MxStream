# **Welcome to MxStream**
MxStream is a Java Stream library that provides enhanced functionality beyond the standard Java streams. It includes methods for parallel processing, asynchronous mapping, batching, filtering, sorting, and more.
## **Key Features**
- **Parallel Processing**: Control over parallelism level for improved performance.
- **Asynchronous Mapping**: Process data on multiple threads without losing order using asyncMap.
- **Exception Handling**: Handle exceptions within streams using exceptionHandler.
- **Batching**: Batch elements with optional timeouts for efficient processing.
- **Fan-Out**: Convert single-threaded streams to parallel stream using fanOut.
- **Sorting**: Sort items in a sliding window to control memory usage.

## **Using MxStream**

Get it from [Maven Central](https://mvnrepository.com/artifact/io.machinic/mxstreams/latest)

### Example: Using asyncMap
The asyncMap function runs the map operation asynchronously using the function and ExecutorService provided. The asyncMap operation will maintain stream order.
``` java
List<String> result = MxStream.of("a", "b", "c")
    .asyncMap(2, s -> {
        // Perform some expensive operation on the elements. These operations will be split over multiple threads.
        return s.toUpperCase();
    }).toList();
System.out.println(result);  // Output: [A, B, C]
```

### Example: Using fanOut
Splits a single threaded stream into a parallel stream. The stream primary thread will focus on processing upstream steps. Downstream processing will be done by additional threads. Like standard java parallel streams the resulting order is non-deterministic.
``` java
List<String> result = MxStream.of("a", "b", "c");
    .fanOut(10, 50);
    .map(value -> {
        // Perform some expensive operation
        return s.toUpperCase();
    }).toList();
```

### Example: Using batching
Batches the elements of the stream into lists of the given size. This operation is the logical opposite of flatMap.
``` java
List<List<String>> result = MxStream.of("a", "b", "c");
    .batch(2);
    .toList();
System.out.println(result);  // Output: [[a, b], [c]]
```

### Example: Stream from BufferedReader
```java
try (BufferedReader bufferedReader = new BufferedReader(new FileReader("example.ndjson"))) {
    // Create a pipeline source from the BufferedReader
    List<Person> people = MxStream.of(bufferedReader)
        .filter(line -> !line.trim().isEmpty())
        .asyncMap(10, line -> objectMapper.readValue(line, Person.class))
        .peek(person -> System.out.println(person.toString()))
        .toList();
} catch (Exception e) {
    // Handle any exceptions that may occur while reading from the BufferedReader
}
```

By utilizing these features and examples, developers can harness the power of MxStream to improve their stream-based applications.