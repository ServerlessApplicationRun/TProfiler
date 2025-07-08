/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.swebench.evaluator;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.taobao.profile.Manager;
import com.taobao.profile.Profiler;
import com.taobao.profile.swebench.SWEBenchConfig;
import com.taobao.profile.swebench.task.SWEBenchTask;
import com.taobao.profile.swebench.task.TaskResult;

/**
 * 模型评估器
 * 负责调用AI模型解决任务并评估结果
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class ModelEvaluator {
    
    private SWEBenchConfig config;
    private DockerEnvironment dockerEnv;
    private ModelInterface modelInterface;
    
    public ModelEvaluator(SWEBenchConfig config) {
        this.config = config;
        this.dockerEnv = new DockerEnvironment(config);
        this.modelInterface = new ModelInterface(config);
    }
    
    /**
     * 评估单个任务
     */
    public TaskResult evaluateTask(SWEBenchTask task, String modelName) {
        TaskResult result = new TaskResult(task.getTaskId(), modelName);
        result.getPerformanceMetrics().setStartTime(System.currentTimeMillis());
        
        // 如果启用了性能分析，开始记录
        int profileMethodId = -1;
        if (config.isEnableProfiling() && Manager.instance().canProfile()) {
            profileMethodId = task.getTaskId().hashCode();
            Profiler.Start(profileMethodId);
        }
        
        try {
            // 1. 准备执行环境
            if (Manager.instance().isDebugMode()) {
                System.out.println("准备任务环境: " + task.getTaskId());
            }
            
            prepareEnvironment(task);
            
            // 2. 调用模型生成解决方案
            long startCpuTime = getCpuTime();
            String generatedPatch = modelInterface.generateSolution(task, modelName);
            long cpuTime = getCpuTime() - startCpuTime;
            
            result.setGeneratedPatch(generatedPatch);
            result.getPerformanceMetrics().setCpuTimeMillis(cpuTime);
            result.getPerformanceMetrics().setApiCallCount(modelInterface.getLastApiCallCount());
            result.getPerformanceMetrics().setTokenCount(modelInterface.getLastTokenCount());
            
            // 3. 应用补丁并运行测试
            TestExecutor testExecutor = new TestExecutor(dockerEnv);
            TaskResult.TestResult testResult = testExecutor.runTests(task, generatedPatch);
            result.setTestResult(testResult);
            
            // 4. 判断是否成功
            result.setSuccess(testResult.getFailedTests() == 0 && testResult.getTotalTests() > 0);
            
            // 5. 收集性能数据
            collectPerformanceData(result);
            
        } catch (TimeoutException e) {
            result.setSuccess(false);
            result.setErrorMessage("任务执行超时: " + e.getMessage());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("任务执行失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 记录结束时间
            result.getPerformanceMetrics().recordEnd();
            
            // 结束性能分析
            if (profileMethodId != -1 && Manager.instance().canProfile()) {
                Profiler.End(profileMethodId);
            }
            
            // 清理环境
            cleanupEnvironment(task);
        }
        
        return result;
    }
    
    /**
     * 准备执行环境
     */
    private void prepareEnvironment(SWEBenchTask task) throws IOException {
        // 创建工作目录
        File workDir = new File(config.getTaskDataPath(), task.getTaskId());
        if (!workDir.exists()) {
            workDir.mkdirs();
        }
        
        // 克隆或更新仓库
        String repoPath = cloneRepository(task, workDir);
        
        // 准备Docker容器
        dockerEnv.prepareContainer(task, repoPath);
    }
    
    /**
     * 克隆仓库
     */
    private String cloneRepository(SWEBenchTask task, File workDir) throws IOException {
        File repoDir = new File(workDir, task.getRepoName());
        
        if (!repoDir.exists()) {
            // 克隆仓库
            String cloneCmd = String.format("git clone %s %s", 
                task.getRepoUrl(), repoDir.getAbsolutePath());
            executeCommand(cloneCmd, workDir);
        }
        
        // 切换到指定分支
        if (task.getRepoBranch() != null) {
            String checkoutCmd = "git checkout " + task.getRepoBranch();
            executeCommand(checkoutCmd, repoDir);
        }
        
        return repoDir.getAbsolutePath();
    }
    
    /**
     * 执行命令
     */
    private void executeCommand(String command, File workDir) throws IOException {
        Process process = Runtime.getRuntime().exec(command, null, workDir);
        try {
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("命令执行超时: " + command);
            }
            
            if (process.exitValue() != 0) {
                throw new IOException("命令执行失败: " + command);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("命令执行被中断: " + command);
        }
    }
    
    /**
     * 收集性能数据
     */
    private void collectPerformanceData(TaskResult result) {
        // 收集内存使用
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        result.getPerformanceMetrics().setMemoryUsedBytes(memoryUsed);
        
        // 估算成本（基于token数量）
        double costPerToken = 0.00002; // 示例成本
        double cost = result.getPerformanceMetrics().getTokenCount() * costPerToken;
        result.getPerformanceMetrics().setCostEstimate(cost);
    }
    
    /**
     * 清理环境
     */
    private void cleanupEnvironment(SWEBenchTask task) {
        try {
            dockerEnv.cleanupContainer(task);
            
            // 如果不保存中间结果，删除工作目录
            if (!config.isSaveIntermediateResults()) {
                File workDir = new File(config.getTaskDataPath(), task.getTaskId());
                deleteDirectory(workDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
    
    /**
     * 获取CPU时间
     */
    private long getCpuTime() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? 
            bean.getCurrentThreadCpuTime() / 1000000L : 0L;
    }
}