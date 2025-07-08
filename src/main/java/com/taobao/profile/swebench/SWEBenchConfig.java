/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.swebench;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * SWE-bench评测配置
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class SWEBenchConfig {
    
    /**
     * 默认配置文件名
     */
    private static final String CONFIG_FILE = "swebench.properties";
    
    /**
     * 并行任务数
     */
    private int parallelTaskCount = 4;
    
    /**
     * 单个任务超时时间（分钟）
     */
    private int taskTimeoutMinutes = 30;
    
    /**
     * 最大重试次数
     */
    private int maxRetryCount = 3;
    
    /**
     * 报告输出路径
     */
    private String reportPath = System.getProperty("user.home") + "/swebench-reports";
    
    /**
     * 任务数据路径
     */
    private String taskDataPath = System.getProperty("user.home") + "/swebench-tasks";
    
    /**
     * 是否启用性能分析
     */
    private boolean enableProfiling = true;
    
    /**
     * 是否保存中间结果
     */
    private boolean saveIntermediateResults = true;
    
    /**
     * Docker镜像名称
     */
    private String dockerImage = "swebench/eval:latest";
    
    /**
     * 评测数据集类型
     */
    private String datasetType = "lite"; // full, lite, verified
    
    /**
     * 模型API配置
     */
    private String modelApiUrl;
    private String modelApiKey;
    private int modelMaxTokens = 4096;
    
    public SWEBenchConfig() {
        loadConfig();
    }
    
    /**
     * 从配置文件加载配置
     */
    private void loadConfig() {
        Properties props = new Properties();
        
        // 尝试从多个位置加载配置文件
        File[] configLocations = {
            new File(CONFIG_FILE),
            new File(System.getProperty("user.home") + "/.tprofiler/" + CONFIG_FILE),
            new File("conf/" + CONFIG_FILE)
        };
        
        for (File configFile : configLocations) {
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    props.load(reader);
                    parseProperties(props);
                    System.out.println("加载SWE-bench配置文件: " + configFile.getAbsolutePath());
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        // 使用默认配置
        System.out.println("未找到SWE-bench配置文件，使用默认配置");
    }
    
    /**
     * 解析配置属性
     */
    private void parseProperties(Properties props) {
        // 基本配置
        parallelTaskCount = Integer.parseInt(props.getProperty("swebench.parallel.tasks", "4"));
        taskTimeoutMinutes = Integer.parseInt(props.getProperty("swebench.task.timeout", "30"));
        maxRetryCount = Integer.parseInt(props.getProperty("swebench.max.retry", "3"));
        
        // 路径配置
        reportPath = props.getProperty("swebench.report.path", reportPath);
        taskDataPath = props.getProperty("swebench.task.path", taskDataPath);
        
        // 功能开关
        enableProfiling = Boolean.parseBoolean(props.getProperty("swebench.enable.profiling", "true"));
        saveIntermediateResults = Boolean.parseBoolean(props.getProperty("swebench.save.intermediate", "true"));
        
        // Docker配置
        dockerImage = props.getProperty("swebench.docker.image", dockerImage);
        
        // 数据集配置
        datasetType = props.getProperty("swebench.dataset.type", "lite");
        
        // 模型API配置
        modelApiUrl = props.getProperty("swebench.model.api.url");
        modelApiKey = props.getProperty("swebench.model.api.key");
        modelMaxTokens = Integer.parseInt(props.getProperty("swebench.model.max.tokens", "4096"));
    }
    
    // Getters and setters
    
    public int getParallelTaskCount() {
        return parallelTaskCount;
    }
    
    public void setParallelTaskCount(int parallelTaskCount) {
        this.parallelTaskCount = parallelTaskCount;
    }
    
    public int getTaskTimeoutMinutes() {
        return taskTimeoutMinutes;
    }
    
    public void setTaskTimeoutMinutes(int taskTimeoutMinutes) {
        this.taskTimeoutMinutes = taskTimeoutMinutes;
    }
    
    public int getMaxRetryCount() {
        return maxRetryCount;
    }
    
    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
    
    public String getReportPath() {
        return reportPath;
    }
    
    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }
    
    public String getTaskDataPath() {
        return taskDataPath;
    }
    
    public void setTaskDataPath(String taskDataPath) {
        this.taskDataPath = taskDataPath;
    }
    
    public boolean isEnableProfiling() {
        return enableProfiling;
    }
    
    public void setEnableProfiling(boolean enableProfiling) {
        this.enableProfiling = enableProfiling;
    }
    
    public boolean isSaveIntermediateResults() {
        return saveIntermediateResults;
    }
    
    public void setSaveIntermediateResults(boolean saveIntermediateResults) {
        this.saveIntermediateResults = saveIntermediateResults;
    }
    
    public String getDockerImage() {
        return dockerImage;
    }
    
    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }
    
    public String getDatasetType() {
        return datasetType;
    }
    
    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }
    
    public String getModelApiUrl() {
        return modelApiUrl;
    }
    
    public void setModelApiUrl(String modelApiUrl) {
        this.modelApiUrl = modelApiUrl;
    }
    
    public String getModelApiKey() {
        return modelApiKey;
    }
    
    public void setModelApiKey(String modelApiKey) {
        this.modelApiKey = modelApiKey;
    }
    
    public int getModelMaxTokens() {
        return modelMaxTokens;
    }
    
    public void setModelMaxTokens(int modelMaxTokens) {
        this.modelMaxTokens = modelMaxTokens;
    }
}