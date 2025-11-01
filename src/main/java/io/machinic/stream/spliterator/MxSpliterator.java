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

package io.machinic.stream.spliterator;

import java.util.function.Consumer;

public interface MxSpliterator<T> {
	
	/**
	 * If a remaining element exists: performs the given action on it, returning {@code true}; else returns {@code false}.  If this Spliterator is {@link #ORDERED} the action is performed on the next element in encounter order.  Exceptions thrown by the
	 * action are relayed to the caller.
	 * <p>
	 * Subsequent behavior of a spliterator is unspecified if the action throws an exception.
	 * @param action The action whose operation is performed at-most once
	 * @return {@code false} if no remaining elements existed upon entry to this method, else {@code true}.
	 * @throws NullPointerException if the specified action is null
	 */
	boolean tryAdvance(Consumer<? super T> action);
	
	/**
	 * Performs the given action for each remaining element, sequentially in the current thread, until all elements have been processed or the action throws an exception.  If this Spliterator is {@link #ORDERED}, actions are performed in encounter order.
	 * Exceptions thrown by the action are relayed to the caller.
	 * <p>
	 * Subsequent behavior of a spliterator is unspecified if the action throws an exception.
	 * @param action The action
	 * @throws NullPointerException if the specified action is null
	 * @implSpec The default implementation repeatedly invokes {@link #tryAdvance} until it returns {@code false}.  It should be overridden whenever possible.
	 */
	default void forEachRemaining(Consumer<? super T> action) {
		do {
		} while (tryAdvance(action));
	}
	
	/**
	 * If this spliterator can be partitioned, returns a Spliterator covering elements, that will, upon return from this method, not be covered by this Spliterator.
	 *
	 * <p>If this Spliterator is {@link #ORDERED}, the returned Spliterator
	 * must cover a strict prefix of the elements.
	 *
	 * <p>Unless this Spliterator covers an infinite number of elements,
	 * repeated calls to {@code trySplit()} must eventually return {@code null}. Upon non-null return:
	 * <ul>
	 * <li>the value reported for {@code estimateSize()} before splitting,
	 * must, after splitting, be greater than or equal to {@code estimateSize()}
	 * for this and the returned Spliterator; and</li>
	 * <li>if this Spliterator is {@code SUBSIZED}, then {@code estimateSize()}
	 * for this spliterator before splitting must be equal to the sum of
	 * {@code estimateSize()} for this and the returned Spliterator after
	 * splitting.</li>
	 * </ul>
	 *
	 * <p>This method may return {@code null} for any reason,
	 * including emptiness, inability to split after traversal has
	 * commenced, data structure constraints, and efficiency
	 * considerations.
	 * @return a {@code Spliterator} covering some portion of the elements, or {@code null} if this spliterator cannot be split
	 * @apiNote An ideal {@code trySplit} method efficiently (without traversal) divides its elements exactly in half, allowing balanced parallel computation.  Many departures from this ideal remain highly effective; for example, only approximately
	 * 		splitting an approximately balanced tree, or for a tree in which leaf nodes may contain either one or two elements, failing to further split these nodes.  However, large deviations in balance and/or overly inefficient {@code trySplit} mechanics
	 * 		typically result in poor parallel performance.
	 */
	MxSpliterator<T> trySplit();
	
	void close();
	
}
