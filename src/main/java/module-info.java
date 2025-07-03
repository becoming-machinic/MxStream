module io.machinic.stream {
	requires org.slf4j;
	requires static com.fasterxml.jackson.annotation;
	exports io.machinic.stream;
	exports io.machinic.stream.spliterator;
	exports io.machinic.stream.sink;
	exports io.machinic.stream.metrics;
}