/*
 * Copyright 2026 Becoming Machinic Inc.
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

package io.machinic.stream.concurrent;

import io.machinic.stream.StreamInterruptedException;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class MapFutureTask<IN, OUT> implements RunnableFuture<OUT> {
	
	// VarHandle mechanics
	private static final VarHandle STATE;
	private static final VarHandle RUNNER;
	
	static {
		try {
			MethodHandles.Lookup l = MethodHandles.lookup();
			STATE = l.findVarHandle(MapFutureTask.class, "state", int.class);
			RUNNER = l.findVarHandle(MapFutureTask.class, "runner", Thread.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	
	private static final int PENDING = 4;
	private static final int CANCELED = 3;
	private static final int RUNNING = 2;
	private static final int FAILED = 1;
	private static final int DONE = 0;
	
	private final CountDownLatch taskLatch = new CountDownLatch(1);
	private volatile Thread runner;
	private volatile int state = PENDING;
	
	private final Function<? super IN, ? extends OUT> mapper;
	private final IN input;
	
	private final long scheduleNanoTime = System.nanoTime();
	private volatile long startNanoTime;
	private volatile long endNanoTime;
	
	private volatile OUT output;
	private volatile Exception exception;
	
	public MapFutureTask(Function<? super IN, ? extends OUT> mapper, IN input) {
		this.mapper = mapper;
		this.input = input;
	}
	
	@Override
	public void run() {
		// Clear interrupted status
		//noinspection ResultOfMethodCallIgnored
		Thread.interrupted();
		// No status protection
		if (!RUNNER.compareAndSet(this, null, Thread.currentThread())) {
			return;
		}
		
		try {
			if (!STATE.compareAndSet(this, PENDING, RUNNING)) {
				return;
			}
			startNanoTime = System.nanoTime();
			
			try {
				this.output = this.mapper.apply(this.input);
				STATE.set(this, DONE);
			} catch (Exception e) {
				this.exception = e;
				if (e instanceof StreamInterruptedException) {
					STATE.set(this, CANCELED);
				} else {
					STATE.set(this, FAILED);
				}
			} finally {
				endNanoTime = System.nanoTime();
			}
		} finally {
			RUNNER.compareAndSet(this, Thread.currentThread(), null);
			taskLatch.countDown();
			// Clear interrupted status
			//noinspection ResultOfMethodCallIgnored
			Thread.interrupted();
		}
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (this.state == PENDING && STATE.compareAndSet(this, PENDING, CANCELED)) {
			return true;
		}
		
		if (mayInterruptIfRunning) {
			Thread runner = this.runner;
			if (runner != null) {
				runner.interrupt();
				STATE.set(this, CANCELED);
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean isCancelled() {
		return this.state == CANCELED;
	}
	
	@Override
	public boolean isDone() {
		return taskLatch.getCount() == 0;
	}
	
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return this.taskLatch.await(timeout, unit);
	}
	
	public IN getInput() {
		return this.input;
	}
	
	public OUT getOutput() {
		return this.output;
	}
	
	public Exception getException() {
		return this.exception;
	}
	
	public long getDuration() {
		if (this.startNanoTime != 0 && this.endNanoTime != 0) {
			return this.endNanoTime - this.startNanoTime;
		}
		return 0L;
	}
	
	@Override
	public OUT get() throws InterruptedException, ExecutionException {
		this.taskLatch.await();
		if (exception != null) {
			if (exception instanceof InterruptedException) {
				throw (InterruptedException) exception;
			} else {
				throw new ExecutionException(exception);
			}
		}
		return this.output;
	}
	
	@Override
	public OUT get(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!this.taskLatch.await(timeout, timeUnit)) {
			throw new TimeoutException("Task timed out after " + timeout + " " + timeUnit);
		}
		if (exception != null) {
			if (exception instanceof InterruptedException) {
				throw (InterruptedException) exception;
			} else {
				throw new ExecutionException(exception);
			}
		}
		return this.output;
	}
}
