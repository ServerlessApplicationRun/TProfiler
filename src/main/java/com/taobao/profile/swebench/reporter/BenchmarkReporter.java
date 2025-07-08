/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.swebench.reporter;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import com.taobao.profile.swebench.SWEBenchConfig;
import com.taobao.profile.swebench.task.TaskResult;
import com.taobao.profile.utils.DailyRollingFileWriter;

/**
 * 基准测试报告生成器
 * 负责生成评测结果报告
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class BenchmarkReporter {
    
    private SWEBenchConfig config;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat fileFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    public BenchmarkReporter(SWEBenchConfig config) {
        this.config = config;
    }
    
    /**
     * 生成报告
     */
    public void generateReport(String modelName, List<TaskResult> results, long startTime) {
        try {
            // 创建报告目录
            File reportDir = new File(config.getReportPath());
            if (!reportDir.exists()) {
                reportDir.mkdirs();
            }
            
            // 生成多种格式的报告
            generateTextReport(modelName, results, startTime);
            generateHtmlReport(modelName, results, startTime);
            generateJsonReport(modelName, results, startTime);
            generateCsvReport(modelName, results, startTime);
            
            // 生成汇总报告
            generateSummaryReport(modelName, results, startTime);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 生成文本报告
     */
    private void generateTextReport(String modelName, List<TaskResult> results, long startTime) 
            throws IOException {
        String fileName = String.format("swebench_%s_%s.txt", 
            modelName.replaceAll("[^a-zA-Z0-9]", "_"), fileFormat.format(new Date()));
        File reportFile = new File(config.getReportPath(), fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("=====================================");
            writer.println("SWE-bench 评测报告");
            writer.println("=====================================");
            writer.println();
            writer.println("模型: " + modelName);
            writer.println("开始时间: " + dateFormat.format(new Date(startTime)));
            writer.println("结束时间: " + dateFormat.format(new Date()));
            writer.println("总耗时: " + formatDuration(System.currentTimeMillis() - startTime));
            writer.println();
            
            // 统计信息
            generateStatistics(writer, results);
            
            // 详细结果
            writer.println("\n详细结果:");
            writer.println("-------------------------------------");
            
            for (TaskResult result : results) {
                writer.println("\n任务ID: " + result.getTaskId());
                writer.println("状态: " + (result.isSuccess() ? "成功" : "失败"));
                writer.println("执行时间: " + result.getPerformanceMetrics().getExecutionTimeMillis() + "ms");
                writer.println("测试通过率: " + String.format("%.2f%%", result.getTestResult().getPassRate()));
                writer.println("测试结果: " + result.getTestResult().getPassedTests() + "/" + 
                              result.getTestResult().getTotalTests());
                
                if (!result.isSuccess() && result.getErrorMessage() != null) {
                    writer.println("错误信息: " + result.getErrorMessage());
                }
                
                writer.println("-------------------------------------");
            }
        }
        
        System.out.println("文本报告已生成: " + reportFile.getAbsolutePath());
    }
    
    /**
     * 生成HTML报告
     */
    private void generateHtmlReport(String modelName, List<TaskResult> results, long startTime) 
            throws IOException {
        String fileName = String.format("swebench_%s_%s.html", 
            modelName.replaceAll("[^a-zA-Z0-9]", "_"), fileFormat.format(new Date()));
        File reportFile = new File(config.getReportPath(), fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<meta charset=\"UTF-8\">");
            writer.println("<title>SWE-bench 评测报告 - " + modelName + "</title>");
            writer.println("<style>");
            writer.println("body { font-family: Arial, sans-serif; margin: 20px; }");
            writer.println("h1 { color: #333; }");
            writer.println("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
            writer.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
            writer.println("th { background-color: #f2f2f2; }");
            writer.println(".success { color: green; }");
            writer.println(".failure { color: red; }");
            writer.println(".stats { background-color: #f9f9f9; padding: 15px; margin: 20px 0; }");
            writer.println("</style>");
            writer.println("</head>");
            writer.println("<body>");
            
            writer.println("<h1>SWE-bench 评测报告</h1>");
            writer.println("<div class=\"stats\">");
            writer.println("<p><strong>模型:</strong> " + modelName + "</p>");
            writer.println("<p><strong>开始时间:</strong> " + dateFormat.format(new Date(startTime)) + "</p>");
            writer.println("<p><strong>结束时间:</strong> " + dateFormat.format(new Date()) + "</p>");
            writer.println("<p><strong>总耗时:</strong> " + formatDuration(System.currentTimeMillis() - startTime) + "</p>");
            
            // 统计信息
            int totalTasks = results.size();
            int successTasks = 0;
            double totalCost = 0;
            long totalTokens = 0;
            
            for (TaskResult result : results) {
                if (result.isSuccess()) successTasks++;
                totalCost += result.getPerformanceMetrics().getCostEstimate();
                totalTokens += result.getPerformanceMetrics().getTokenCount();
            }
            
            writer.println("<p><strong>总任务数:</strong> " + totalTasks + "</p>");
            writer.println("<p><strong>成功数:</strong> " + successTasks + "</p>");
            writer.println("<p><strong>成功率:</strong> " + String.format("%.2f%%", (double)successTasks/totalTasks*100) + "</p>");
            writer.println("<p><strong>总成本:</strong> $" + String.format("%.4f", totalCost) + "</p>");
            writer.println("<p><strong>总Token数:</strong> " + totalTokens + "</p>");
            writer.println("</div>");
            
            // 结果表格
            writer.println("<h2>详细结果</h2>");
            writer.println("<table>");
            writer.println("<tr>");
            writer.println("<th>任务ID</th>");
            writer.println("<th>状态</th>");
            writer.println("<th>执行时间(ms)</th>");
            writer.println("<th>测试通过率</th>");
            writer.println("<th>API调用</th>");
            writer.println("<th>Token数</th>");
            writer.println("<th>成本</th>");
            writer.println("</tr>");
            
            for (TaskResult result : results) {
                writer.println("<tr>");
                writer.println("<td>" + result.getTaskId() + "</td>");
                writer.println("<td class=\"" + (result.isSuccess() ? "success" : "failure") + "\">" + 
                              (result.isSuccess() ? "成功" : "失败") + "</td>");
                writer.println("<td>" + result.getPerformanceMetrics().getExecutionTimeMillis() + "</td>");
                writer.println("<td>" + String.format("%.2f%%", result.getTestResult().getPassRate()) + "</td>");
                writer.println("<td>" + result.getPerformanceMetrics().getApiCallCount() + "</td>");
                writer.println("<td>" + result.getPerformanceMetrics().getTokenCount() + "</td>");
                writer.println("<td>$" + String.format("%.4f", result.getPerformanceMetrics().getCostEstimate()) + "</td>");
                writer.println("</tr>");
            }
            
            writer.println("</table>");
            writer.println("</body>");
            writer.println("</html>");
        }
        
        System.out.println("HTML报告已生成: " + reportFile.getAbsolutePath());
    }
    
    /**
     * 生成JSON报告
     */
    private void generateJsonReport(String modelName, List<TaskResult> results, long startTime) 
            throws IOException {
        String fileName = String.format("swebench_%s_%s.json", 
            modelName.replaceAll("[^a-zA-Z0-9]", "_"), fileFormat.format(new Date()));
        File reportFile = new File(config.getReportPath(), fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("{");
            writer.println("  \"model\": \"" + modelName + "\",");
            writer.println("  \"startTime\": \"" + dateFormat.format(new Date(startTime)) + "\",");
            writer.println("  \"endTime\": \"" + dateFormat.format(new Date()) + "\",");
            writer.println("  \"duration\": " + (System.currentTimeMillis() - startTime) + ",");
            writer.println("  \"results\": [");
            
            for (int i = 0; i < results.size(); i++) {
                TaskResult result = results.get(i);
                writer.println("    {");
                writer.println("      \"taskId\": \"" + result.getTaskId() + "\",");
                writer.println("      \"success\": " + result.isSuccess() + ",");
                writer.println("      \"executionTime\": " + result.getPerformanceMetrics().getExecutionTimeMillis() + ",");
                writer.println("      \"testPassRate\": " + result.getTestResult().getPassRate() + ",");
                writer.println("      \"apiCalls\": " + result.getPerformanceMetrics().getApiCallCount() + ",");
                writer.println("      \"tokens\": " + result.getPerformanceMetrics().getTokenCount() + ",");
                writer.println("      \"cost\": " + result.getPerformanceMetrics().getCostEstimate());
                writer.print("    }");
                if (i < results.size() - 1) writer.print(",");
                writer.println();
            }
            
            writer.println("  ]");
            writer.println("}");
        }
        
        System.out.println("JSON报告已生成: " + reportFile.getAbsolutePath());
    }
    
    /**
     * 生成CSV报告
     */
    private void generateCsvReport(String modelName, List<TaskResult> results, long startTime) 
            throws IOException {
        String fileName = String.format("swebench_%s_%s.csv", 
            modelName.replaceAll("[^a-zA-Z0-9]", "_"), fileFormat.format(new Date()));
        File reportFile = new File(config.getReportPath(), fileName);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            // CSV头
            writer.println("TaskID,Model,Success,ExecutionTime(ms),TestPassRate(%),PassedTests,TotalTests,APIcalls,Tokens,Cost($)");
            
            // 数据行
            for (TaskResult result : results) {
                writer.printf("%s,%s,%s,%d,%.2f,%d,%d,%d,%d,%.4f\n",
                    result.getTaskId(),
                    modelName,
                    result.isSuccess(),
                    result.getPerformanceMetrics().getExecutionTimeMillis(),
                    result.getTestResult().getPassRate(),
                    result.getTestResult().getPassedTests(),
                    result.getTestResult().getTotalTests(),
                    result.getPerformanceMetrics().getApiCallCount(),
                    result.getPerformanceMetrics().getTokenCount(),
                    result.getPerformanceMetrics().getCostEstimate()
                );
            }
        }
        
        System.out.println("CSV报告已生成: " + reportFile.getAbsolutePath());
    }
    
    /**
     * 生成汇总报告
     */
    private void generateSummaryReport(String modelName, List<TaskResult> results, long startTime) 
            throws IOException {
        File summaryFile = new File(config.getReportPath(), "swebench_summary.txt");
        
        // 追加模式写入
        try (PrintWriter writer = new PrintWriter(new FileWriter(summaryFile, true))) {
            int successCount = 0;
            double totalCost = 0;
            long totalTime = 0;
            
            for (TaskResult result : results) {
                if (result.isSuccess()) successCount++;
                totalCost += result.getPerformanceMetrics().getCostEstimate();
                totalTime += result.getPerformanceMetrics().getExecutionTimeMillis();
            }
            
            writer.printf("%s | %s | 任务数: %d | 成功: %d (%.2f%%) | 总耗时: %s | 总成本: $%.4f\n",
                dateFormat.format(new Date()),
                modelName,
                results.size(),
                successCount,
                (double)successCount/results.size()*100,
                formatDuration(totalTime),
                totalCost
            );
        }
    }
    
    /**
     * 生成统计信息
     */
    private void generateStatistics(PrintWriter writer, List<TaskResult> results) {
        int totalTasks = results.size();
        int successTasks = 0;
        int failedTasks = 0;
        long totalExecutionTime = 0;
        long totalCpuTime = 0;
        long totalMemory = 0;
        int totalApiCalls = 0;
        int totalTokens = 0;
        double totalCost = 0;
        
        Map<Integer, Integer> difficultyDistribution = new HashMap<>();
        
        for (TaskResult result : results) {
            if (result.isSuccess()) {
                successTasks++;
            } else {
                failedTasks++;
            }
            
            totalExecutionTime += result.getPerformanceMetrics().getExecutionTimeMillis();
            totalCpuTime += result.getPerformanceMetrics().getCpuTimeMillis();
            totalMemory += result.getPerformanceMetrics().getMemoryUsedBytes();
            totalApiCalls += result.getPerformanceMetrics().getApiCallCount();
            totalTokens += result.getPerformanceMetrics().getTokenCount();
            totalCost += result.getPerformanceMetrics().getCostEstimate();
        }
        
        writer.println("统计信息:");
        writer.println("-------------------------------------");
        writer.println("总任务数: " + totalTasks);
        writer.println("成功数: " + successTasks);
        writer.println("失败数: " + failedTasks);
        writer.println("成功率: " + String.format("%.2f%%", (double)successTasks/totalTasks*100));
        writer.println();
        writer.println("性能指标:");
        writer.println("平均执行时间: " + (totalTasks > 0 ? totalExecutionTime/totalTasks : 0) + "ms");
        writer.println("平均CPU时间: " + (totalTasks > 0 ? totalCpuTime/totalTasks : 0) + "ms");
        writer.println("平均内存使用: " + formatBytes(totalTasks > 0 ? totalMemory/totalTasks : 0));
        writer.println();
        writer.println("API使用:");
        writer.println("总API调用: " + totalApiCalls);
        writer.println("总Token数: " + totalTokens);
        writer.println("总成本: $" + String.format("%.4f", totalCost));
        writer.println("平均成本: $" + String.format("%.4f", totalTasks > 0 ? totalCost/totalTasks : 0));
    }
    
    /**
     * 格式化时长
     */
    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d小时%d分钟%d秒", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}