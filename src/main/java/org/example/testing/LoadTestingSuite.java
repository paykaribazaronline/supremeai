package org.example.testing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Phase 4.2: Load Testing Suite
 * Comprehensive load testing for monitoring system and code generation services
 */
@Service
public class LoadTestingSuite {

    @Autowired(required = false)
    private org.example.service.MetricsService metricsService;

    public static class LoadTestResult {
        public String testName;
        public int totalRequests;
        public int successfulRequests = 0;
        public int failedRequests = 0;
        public double avgResponseTime = 0;
        public double minResponseTime = Double.MAX_VALUE;
        public double maxResponseTime = 0;
        public double p95ResponseTime = 0;
        public double p99ResponseTime = 0;
        public int throughput = 0; // requests per second
        public double errorRate = 0;
        public long startTime;
        public long endTime;
        public String status = "RUNNING";

        LoadTestResult(String name, int total) {
            this.testName = name;
            this.totalRequests = total;
            this.startTime = System.currentTimeMillis();
        }
    }

    private final Map<String, LoadTestResult> testResults = new ConcurrentHashMap<>();
    private final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

    /**
     * Load test: Endpoint throughput testing
     * Simulates N concurrent requests to an endpoint
     */
    public LoadTestResult testEndpointThroughput(String endpoint, int numRequests, int concurrency) {
        LoadTestResult result = new LoadTestResult(endpoint + " - Throughput Test", numRequests);
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);
        
        try {
            long testStart = System.currentTimeMillis();

            for (int i = 0; i < numRequests; i++) {
                executor.submit(() -> {
                    try {
                        long reqStart = System.currentTimeMillis();
                        // Simulate request
                        simulateRequest(endpoint);
                        long reqDuration = System.currentTimeMillis() - reqStart;
                        
                        success.incrementAndGet();
                        totalTime.addAndGet(reqDuration);
                        responseTimes.add(reqDuration);
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.MINUTES);

            long testEnd = System.currentTimeMillis();
            long totalDuration = testEnd - testStart;

            result.successfulRequests = success.get();
            result.failedRequests = failed.get();
            result.avgResponseTime = success.get() == 0 ? 0 : (double) totalTime.get() / success.get();
            result.errorRate = (double) failed.get() / numRequests * 100;
            result.throughput = (int) ((numRequests * 1000) / totalDuration);
            calculatePercentiles(result);
            result.endTime = System.currentTimeMillis();
            result.status = "COMPLETED";

        } catch (Exception e) {
            result.status = "FAILED";
            result.endTime = System.currentTimeMillis();
        } finally {
            executor.shutdownNow();
        }

        testResults.put(result.testName, result);
        return result;
    }

    /**
     * Load test: Sustained load testing
     * Maintains constant load for duration
     */
    public LoadTestResult testSustainedLoad(String endpoint, int requestsPerSecond, int durationSeconds) {
        LoadTestResult result = new LoadTestResult(endpoint + " - Sustained Load Test", 
            requestsPerSecond * durationSeconds);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);

        try {
            long testStart = System.currentTimeMillis();

            // Schedule requests at fixed rate
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                for (int i = 0; i < requestsPerSecond; i++) {
                    new Thread(() -> {
                        try {
                            long reqStart = System.currentTimeMillis();
                            simulateRequest(endpoint);
                            long reqDuration = System.currentTimeMillis() - reqStart;
                            
                            success.incrementAndGet();
                            totalTime.addAndGet(reqDuration);
                            responseTimes.add(reqDuration);
                        } catch (Exception e) {
                            failed.incrementAndGet();
                        }
                    }).start();
                }
            }, 0, 1, TimeUnit.SECONDS);

            // Wait for test duration
            Thread.sleep(durationSeconds * 1000L);
            future.cancel(true);
            scheduler.shutdown();
            scheduler.awaitTermination(2, TimeUnit.MINUTES);

            long testEnd = System.currentTimeMillis();

            result.successfulRequests = success.get();
            result.failedRequests = failed.get();
            result.avgResponseTime = success.get() == 0 ? 0 : (double) totalTime.get() / success.get();
            result.errorRate = result.totalRequests == 0 ? 0 : (double) failed.get() / result.totalRequests * 100;
            result.throughput = (success.get() + failed.get()) / durationSeconds;
            calculatePercentiles(result);
            result.endTime = System.currentTimeMillis();
            result.status = "COMPLETED";

        } catch (Exception e) {
            result.status = "FAILED";
            result.endTime = System.currentTimeMillis();
        } finally {
            scheduler.shutdownNow();
        }

        testResults.put(result.testName, result);
        return result;
    }

    /**
     * Load test: Spike testing
     * Sudden increase in load to test recovery
     */
    public LoadTestResult testSpikeLoad(String endpoint, int normalLoad, int spikeLoad, int spikeDurationSeconds) {
        LoadTestResult result = new LoadTestResult(endpoint + " - Spike Test", 
            normalLoad * 10 + spikeLoad * spikeDurationSeconds);

        ExecutorService executor = Executors.newFixedThreadPool(Math.max(normalLoad, spikeLoad) / 10 + 1);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);

        try {
            long testStart = System.currentTimeMillis();

            // Normal load
            submitLoad(executor, normalLoad, success, failed, totalTime, endpoint);
            Thread.sleep(5000); // 5 seconds at normal load

            // Spike load
            submitLoad(executor, spikeLoad, success, failed, totalTime, endpoint);
            Thread.sleep(spikeDurationSeconds * 1000L);

            // Back to normal
            submitLoad(executor, normalLoad, success, failed, totalTime, endpoint);
            Thread.sleep(5000);

            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.MINUTES);

            long testEnd = System.currentTimeMillis();

            result.successfulRequests = success.get();
            result.failedRequests = failed.get();
            result.avgResponseTime = success.get() == 0 ? 0 : (double) totalTime.get() / success.get();
            result.errorRate = result.totalRequests == 0 ? 0 : (double) failed.get() / result.totalRequests * 100;
            result.throughput = (success.get() + failed.get()) / 20; // ~20 seconds total
            calculatePercentiles(result);
            result.endTime = System.currentTimeMillis();
            result.status = "COMPLETED";

        } catch (Exception e) {
            result.status = "FAILED";
            result.endTime = System.currentTimeMillis();
        } finally {
            executor.shutdownNow();
        }

        testResults.put(result.testName, result);
        return result;
    }

    /**
     * Load test: WebSocket connection stress test
     * Tests WebSocket concurrent connections
     */
    public LoadTestResult testWebSocketConnections(int numConnections) {
        LoadTestResult result = new LoadTestResult("WebSocket Stress Test", numConnections);

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(numConnections, 100));
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);

        try {
            long testStart = System.currentTimeMillis();

            for (int i = 0; i < numConnections; i++) {
                executor.submit(() -> {
                    try {
                        long connStart = System.currentTimeMillis();
                        // Simulate WebSocket connection
                        Thread.sleep(new Random().nextInt(100, 500)); // Connection time
                        long connDuration = System.currentTimeMillis() - connStart;
                        
                        success.incrementAndGet();
                        totalTime.addAndGet(connDuration);
                        responseTimes.add(connDuration);
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    }
                });
            }

            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.MINUTES);

            long testEnd = System.currentTimeMillis();

            result.successfulRequests = success.get();
            result.failedRequests = failed.get();
            result.avgResponseTime = success.get() == 0 ? 0 : (double) totalTime.get() / success.get();
            result.errorRate = (double) failed.get() / numConnections * 100;
            result.throughput = (int) (success.get() / ((testEnd - testStart) / 1000 + 1));
            calculatePercentiles(result);
            result.endTime = System.currentTimeMillis();
            result.status = "COMPLETED";

        } catch (Exception e) {
            result.status = "FAILED";
            result.endTime = System.currentTimeMillis();
        } finally {
            executor.shutdownNow();
        }

        testResults.put(result.testName, result);
        return result;
    }

    /**
     * Helper: Submit load to executor
     */
    private void submitLoad(ExecutorService executor, int numRequests, AtomicInteger success, 
                          AtomicInteger failed, AtomicLong totalTime, String endpoint) {
        for (int i = 0; i < numRequests; i++) {
            executor.submit(() -> {
                try {
                    long reqStart = System.currentTimeMillis();
                    simulateRequest(endpoint);
                    long reqDuration = System.currentTimeMillis() - reqStart;
                    
                    success.incrementAndGet();
                    totalTime.addAndGet(reqDuration);
                    responseTimes.add(reqDuration);
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
            });
        }
    }

    /**
     * Helper: Calculate P95 and P99 response times
     */
    private void calculatePercentiles(LoadTestResult result) {
        if (responseTimes.isEmpty()) return;

        List<Long> sorted = new ArrayList<>(responseTimes);
        Collections.sort(sorted);

        result.minResponseTime = sorted.get(0);
        result.maxResponseTime = sorted.get(sorted.size() - 1);

        int p95Index = (int) (sorted.size() * 0.95);
        int p99Index = (int) (sorted.size() * 0.99);

        result.p95ResponseTime = sorted.get(Math.min(p95Index, sorted.size() - 1));
        result.p99ResponseTime = sorted.get(Math.min(p99Index, sorted.size() - 1));
    }

    /**
     * Helper: Simulate HTTP request
     */
    private void simulateRequest(String endpoint) throws Exception {
        // Simulate request processing time (10-500ms)
        Thread.sleep(new Random().nextInt(10, 500));
    }

    /**
     * Get test result
     */
    public Map<String, Object> getTestResult(String testName) {
        LoadTestResult result = testResults.get(testName);
        if (result == null) return null;

        Map<String, Object> map = new HashMap<>();
        map.put("testName", result.testName);
        map.put("status", result.status);
        map.put("totalRequests", result.totalRequests);
        map.put("successfulRequests", result.successfulRequests);
        map.put("failedRequests", result.failedRequests);
        map.put("successRate", result.successfulRequests + result.failedRequests == 0 ? 0 :
            ((double) result.successfulRequests / (result.successfulRequests + result.failedRequests)) * 100);
        map.put("avgResponseTime", String.format("%.2f ms", result.avgResponseTime));
        map.put("minResponseTime", String.format("%.2f ms", result.minResponseTime));
        map.put("maxResponseTime", String.format("%.2f ms", result.maxResponseTime));
        map.put("p95ResponseTime", String.format("%.2f ms", result.p95ResponseTime));
        map.put("p99ResponseTime", String.format("%.2f ms", result.p99ResponseTime));
        map.put("errorRate", String.format("%.2f%%", result.errorRate));
        map.put("throughput", result.throughput + " req/sec");
        map.put("duration", (result.endTime - result.startTime) + " ms");

        return map;
    }

    /**
     * Get all test results
     */
    public List<Map<String, Object>> getAllTestResults() {
        return testResults.values().stream()
                .map(result -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("testName", result.testName);
                    map.put("status", result.status);
                    map.put("successRate", result.successfulRequests + result.failedRequests == 0 ? 0 :
                        ((double) result.successfulRequests / (result.successfulRequests + result.failedRequests)) * 100);
                    map.put("throughput", result.throughput + " req/sec");
                    map.put("avgResponseTime", String.format("%.2f ms", result.avgResponseTime));
                    map.put("errorRate", String.format("%.2f%%", result.errorRate));
                    return map;
                })
                .toList();
    }

    /**
     * Clear test results
     */
    public void clearResults() {
        testResults.clear();
        responseTimes.clear();
    }
}
