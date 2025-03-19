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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RateAsyncMapMetricSupplier implements AsyncMapMetricSupplier {
	private final CopyOnWriteArrayList<AsyncMapMetric> asyncMapMetrics = new CopyOnWriteArrayList<>();
	
	@Override
	public AsyncMapMetric get() {
		AsyncMapMetric metric = new AsyncMapMetric();
		asyncMapMetrics.add(metric);
		return metric;
	}
	
	public long getCount() {
		return asyncMapMetrics.stream().mapToLong(AsyncMapMetric::getCount).sum();
	}
	
	public long getDuration() {
		return asyncMapMetrics.stream().mapToLong(AsyncMapMetric::getDuration).sum();
	}
	
	/**
	 * Calculates the average rate of events per second
	 * @return events per second
	 */
	public double getAverageRate() {
		List<Double> sum = asyncMapMetrics.stream()
				.map(metric -> metric.getCount() / Math.max(Long.valueOf(metric.getDuration()).doubleValue(), 1D))
				.toList();
		return (sum.stream().reduce(0D, Double::sum) / sum.size()) * 1000D;
	}
	
	public long getTotalDuration() {
		return asyncMapMetrics.stream()
				.mapToLong(AsyncMapMetric::getTaskDuration)
				.sum();
	}
	
	public double getAverageDuration() {
		List<Double> sum = asyncMapMetrics.stream()
				.map(metric -> metric.getCount() / Math.max(Long.valueOf(metric.getTaskDuration()).doubleValue(), 1D))
				.toList();
		return sum.stream().reduce(0D, Double::sum) / sum.size();
	}
	

}
