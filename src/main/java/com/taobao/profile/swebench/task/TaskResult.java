/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.swebench.task;

import java.util.List;
import java.util.Map;

/**
 * SWE-bench任务执行结果
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class TaskResult {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 模型名称
     */
    private String modelName;
    
    /**
     * 是否成功解决
     */
    private boolean success;
    
    /**
     * 生成的补丁内容
     */
    private String generatedPatch;
    
    /**
     * 测试结果
     */
    private TestResult testResult;
    
    /**
     * 性能指标
     */
    private PerformanceMetrics performanceMetrics;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 执行日志
     */
    private List<String> executionLogs;
    
    /**
     * 额外的结果数据
     */
    private Map<String, Object> additionalData;
    
    /**
     * 测试结果内部类
     */
    public static class TestResult {
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private List<String> failedTestNames;
        private String testOutput;
        
        public TestResult() {
            this.totalTests = 0;
            this.passedTests = 0;
            this.failedTests = 0;
        }
        
        public double getPassRate() {
            return totalTests > 0 ? (double) passedTests / totalTests * 100 : 0;
        }
        
        // Getters and setters
        public int getTotalTests() {
            return totalTests;
        }
        
        public void setTotalTests(int totalTests) {
            this.totalTests = totalTests;
        }
        
        public int getPassedTests() {
            return passedTests;
        }
        
        public void setPassedTests(int passedTests) {
            this.passedTests = passedTests;
        }
        
        public int getFailedTests() {
            return failedTests;
        }
        
        public void setFailedTests(int failedTests) {
            this.failedTests = failedTests;
        }
        
        public List<String> getFailedTestNames() {
            return failedTestNames;
        }
        
        public void setFailedTestNames(List<String> failedTestNames) {
            this.failedTestNames = failedTestNames;
        }
        
        public String getTestOutput() {
            return testOutput;
        }
        
        public void setTestOutput(String testOutput) {
            this.testOutput = testOutput;
        }
    }
    
    /**
     * 性能指标内部类
     */
    public static class PerformanceMetrics {
        private long startTime;
        private long endTime;
        private long executionTimeMillis;
        private long cpuTimeMillis;
        private long memoryUsedBytes;
        private int apiCallCount;
        private int tokenCount;
        private double costEstimate;
        
        public PerformanceMetrics() {
            this.startTime = System.currentTimeMillis();
        }
        
        public void recordEnd() {
            this.endTime = System.currentTimeMillis();
            this.executionTimeMillis = endTime - startTime;
        }
        
        // Getters and setters
        public long getStartTime() {
            return startTime;
        }
        
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
        
        public long getExecutionTimeMillis() {
            return executionTimeMillis;
        }
        
        public void setExecutionTimeMillis(long executionTimeMillis) {
            this.executionTimeMillis = executionTimeMillis;
        }
        
        public long getCpuTimeMillis() {
            return cpuTimeMillis;
        }
        
        public void setCpuTimeMillis(long cpuTimeMillis) {
            this.cpuTimeMillis = cpuTimeMillis;
        }
        
        public long getMemoryUsedBytes() {
            return memoryUsedBytes;
        }
        
        public void setMemoryUsedBytes(long memoryUsedBytes) {
            this.memoryUsedBytes = memoryUsedBytes;
        }
        
        public int getApiCallCount() {
            return apiCallCount;
        }
        
        public void setApiCallCount(int apiCallCount) {
            this.apiCallCount = apiCallCount;
        }
        
        public int getTokenCount() {
            return tokenCount;
        }
        
        public void setTokenCount(int tokenCount) {
            this.tokenCount = tokenCount;
        }
        
        public double getCostEstimate() {
            return costEstimate;
        }
        
        public void setCostEstimate(double costEstimate) {
            this.costEstimate = costEstimate;
        }
    }
    
    public TaskResult() {
        this.testResult = new TestResult();
        this.performanceMetrics = new PerformanceMetrics();
    }
    
    public TaskResult(String taskId, String modelName) {
        this();
        this.taskId = taskId;
        this.modelName = modelName;
    }
    
    /**
     * 生成结果摘要
     */
    public String generateSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Task: ").append(taskId).append("\n");
        summary.append("Model: ").append(modelName).append("\n");
        summary.append("Success: ").append(success).append("\n");
        summary.append("Execution Time: ").append(performanceMetrics.getExecutionTimeMillis()).append("ms\n");
        
        if (testResult != null) {
            summary.append("Test Pass Rate: ").append(String.format("%.2f%%", testResult.getPassRate())).append("\n");
            summary.append("Tests: ").append(testResult.getPassedTests()).append("/").append(testResult.getTotalTests()).append("\n");
        }
        
        if (!success && errorMessage != null) {
            summary.append("Error: ").append(errorMessage).append("\n");
        }
        
        return summary.toString();
    }
    
    // Getters and setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getGeneratedPatch() {
        return generatedPatch;
    }
    
    public void setGeneratedPatch(String generatedPatch) {
        this.generatedPatch = generatedPatch;
    }
    
    public TestResult getTestResult() {
        return testResult;
    }
    
    public void setTestResult(TestResult testResult) {
        this.testResult = testResult;
    }
    
    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }
    
    public void setPerformanceMetrics(PerformanceMetrics performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<String> getExecutionLogs() {
        return executionLogs;
    }
    
    public void setExecutionLogs(List<String> executionLogs) {
        this.executionLogs = executionLogs;
    }
    
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }
    
    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }
}