/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.swebench;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.taobao.profile.Manager;
import com.taobao.profile.swebench.task.SWEBenchTask;
import com.taobao.profile.swebench.task.TaskResult;
import com.taobao.profile.swebench.evaluator.ModelEvaluator;
import com.taobao.profile.swebench.reporter.BenchmarkReporter;
import com.taobao.profile.swebench.loader.TaskLoader;

/**
 * SWE-bench评测管理器
 * 负责协调AI模型在软件工程任务上的性能评测
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class SWEBenchManager {
    
    private static SWEBenchManager instance = new SWEBenchManager();
    
    /**
     * 线程池用于并行执行评测任务
     */
    private ExecutorService executorService;
    
    /**
     * 任务列表
     */
    private List<SWEBenchTask> tasks;
    
    /**
     * 模型评估器
     */
    private ModelEvaluator evaluator;
    
    /**
     * 报告生成器
     */
    private BenchmarkReporter reporter;
    
    /**
     * 是否正在运行
     */
    private volatile boolean isRunning = false;
    
    /**
     * 评测配置
     */
    private SWEBenchConfig config;
    
    private SWEBenchManager() {
        this.tasks = new ArrayList<>();
        this.config = new SWEBenchConfig();
    }
    
    /**
     * 获取单例实例
     */
    public static SWEBenchManager getInstance() {
        return instance;
    }
    
    /**
     * 初始化评测环境
     */
    public void initialize() {
        if (Manager.instance().isDebugMode()) {
            System.out.println("初始化SWE-bench评测环境...");
        }
        
        // 创建线程池
        int threadCount = config.getParallelTaskCount();
        executorService = Executors.newFixedThreadPool(threadCount);
        
        // 初始化评估器和报告器
        evaluator = new ModelEvaluator(config);
        reporter = new BenchmarkReporter(config);
        
        // 加载任务
        loadTasks();
    }
    
    /**
     * 加载评测任务
     */
    private void loadTasks() {
        tasks.clear();
        
        try {
            // 根据配置的数据集类型加载任务
            String datasetType = config.getDatasetType();
            
            if ("sample".equals(datasetType)) {
                // 加载示例任务
                tasks.addAll(TaskLoader.loadSampleTasks());
            } else if ("csv".equals(datasetType)) {
                // 从CSV文件加载
                String csvPath = config.getTaskDataPath() + "/swebench_tasks.csv";
                tasks.addAll(TaskLoader.loadFromCsv(csvPath));
            } else if ("json".equals(datasetType)) {
                // 从JSON文件加载
                String jsonPath = config.getTaskDataPath() + "/swebench_tasks.json";
                tasks.addAll(TaskLoader.loadFromJson(jsonPath));
            } else {
                // 默认加载示例任务
                tasks.addAll(TaskLoader.loadSampleTasks());
            }
            
            if (Manager.instance().isDebugMode()) {
                System.out.println("成功加载SWE-bench任务，任务数: " + tasks.size());
                for (SWEBenchTask task : tasks) {
                    System.out.println("  - " + task.getTaskId() + ": " + task.getIssueTitle());
                }
            }
        } catch (Exception e) {
            System.err.println("加载任务失败: " + e.getMessage());
            e.printStackTrace();
            // 加载失败时使用示例任务
            tasks.addAll(TaskLoader.loadSampleTasks());
        }
    }
    
    /**
     * 开始评测
     * 
     * @param modelName 要评测的模型名称
     * @return 是否成功开始
     */
    public boolean startBenchmark(String modelName) {
        if (isRunning) {
            System.err.println("评测已在运行中");
            return false;
        }
        
        isRunning = true;
        System.out.println("开始SWE-bench评测，模型: " + modelName);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        List<TaskResult> results = new ArrayList<>();
        
        try {
            // 执行所有任务
            for (SWEBenchTask task : tasks) {
                TaskResult result = evaluator.evaluateTask(task, modelName);
                results.add(result);
                
                // 实时输出进度
                if (Manager.instance().isDebugMode()) {
                    System.out.println("完成任务: " + task.getTaskId() + 
                                     ", 成功: " + result.isSuccess());
                }
            }
            
            // 生成报告
            reporter.generateReport(modelName, results, startTime);
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            isRunning = false;
        }
        
        return true;
    }
    
    /**
     * 停止评测
     */
    public void stopBenchmark() {
        if (!isRunning) {
            return;
        }
        
        System.out.println("停止SWE-bench评测...");
        isRunning = false;
        
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 获取评测状态
     */
    public String getStatus() {
        return isRunning ? "运行中" : "已停止";
    }
    
    /**
     * 添加自定义任务
     */
    public void addTask(SWEBenchTask task) {
        tasks.add(task);
    }
    
    /**
     * 获取配置
     */
    public SWEBenchConfig getConfig() {
        return config;
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        stopBenchmark();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}