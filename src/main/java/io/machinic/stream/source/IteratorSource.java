/*
 * Copyright 2025 Becoming Machinic Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.machinic.stream.source;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;

public class IteratorSource<IN> extends PipelineSource<IN> {
	
	public IteratorSource(Iterator<IN> iterator) {
		super(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}
	
	public IteratorSource(Spliterator<IN> spliterator, boolean parallel) {
		super(spliterator, parallel);
	}
	
	@Override
	public void close() throws Exception {
		closedReference.getAndSet(true);
		// Nothing to close
	}
}
