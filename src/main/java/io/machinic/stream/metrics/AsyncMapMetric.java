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

package io.machinic.stream.metrics;

import java.util.concurrent.atomic.AtomicLong;

public class AsyncMapMetric{
	
	private volatile long startTimestamp = 0L;
	private volatile long endTimestamp = 0L;
	private final AtomicLong atomicCounter = new AtomicLong();
	private final AtomicLong taskDuration = new AtomicLong();
	
	public void onStart() {
		this.startTimestamp = System.currentTimeMillis();
	}
	
	public void onEvent(long taskDuration) {
		atomicCounter.incrementAndGet();
		this.taskDuration.addAndGet(taskDuration);
	}
	
	public void onStop() {
		endTimestamp = System.currentTimeMillis();
	}
	
	public long getCount() {
		return atomicCounter.get();
	}
	
	public long getDuration() {
		long endTime = endTimestamp;
		if (endTime == 0L) {
			return System.currentTimeMillis() - startTimestamp;
		}
		return endTime - startTimestamp;
	}
	
	public long getTaskDuration() {
		return taskDuration.get();
	}
	
}
