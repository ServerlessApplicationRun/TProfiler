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
 * SWE-bench任务定义
 * 代表一个需要AI模型解决的软件工程问题
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class SWEBenchTask {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * GitHub仓库信息
     */
    private String repoOwner;
    private String repoName;
    private String repoBranch;
    
    /**
     * Issue信息
     */
    private String issueNumber;
    private String issueTitle;
    private String issueDescription;
    
    /**
     * 测试相关
     */
    private List<String> testCommands;
    private List<String> failingTests;
    
    /**
     * 预期的代码变更文件
     */
    private List<String> expectedFiles;
    
    /**
     * 任务难度等级 (1-5)
     */
    private int difficultyLevel;
    
    /**
     * 任务类型
     */
    private TaskType taskType;
    
    /**
     * 额外的元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 基准解决方案（用于对比）
     */
    private String baselinePatch;
    
    /**
     * 任务创建时间
     */
    private long createTime;
    
    /**
     * 预期完成时间（分钟）
     */
    private int expectedTimeMinutes;
    
    public enum TaskType {
        BUG_FIX("bug_fix"),
        FEATURE("feature"),
        REFACTOR("refactor"),
        TEST("test"),
        DOCUMENTATION("documentation");
        
        private String value;
        
        TaskType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
    }
    
    public SWEBenchTask() {
        this.createTime = System.currentTimeMillis();
        this.taskType = TaskType.BUG_FIX;
        this.difficultyLevel = 3;
    }
    
    public SWEBenchTask(String taskId, String repoOwner, String repoName) {
        this();
        this.taskId = taskId;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }
    
    /**
     * 生成任务的完整描述
     */
    public String generateTaskPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Repository: ").append(repoOwner).append("/").append(repoName).append("\n");
        prompt.append("Branch: ").append(repoBranch).append("\n");
        prompt.append("Issue #").append(issueNumber).append(": ").append(issueTitle).append("\n\n");
        prompt.append("Description:\n").append(issueDescription).append("\n\n");
        
        if (failingTests != null && !failingTests.isEmpty()) {
            prompt.append("Failing tests:\n");
            for (String test : failingTests) {
                prompt.append("- ").append(test).append("\n");
            }
        }
        
        return prompt.toString();
    }
    
    /**
     * 获取GitHub仓库URL
     */
    public String getRepoUrl() {
        return String.format("https://github.com/%s/%s", repoOwner, repoName);
    }
    
    /**
     * 获取Issue URL
     */
    public String getIssueUrl() {
        return String.format("%s/issues/%s", getRepoUrl(), issueNumber);
    }
    
    // Getters and setters
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getRepoOwner() {
        return repoOwner;
    }
    
    public void setRepoOwner(String repoOwner) {
        this.repoOwner = repoOwner;
    }
    
    public String getRepoName() {
        return repoName;
    }
    
    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }
    
    public String getRepoBranch() {
        return repoBranch;
    }
    
    public void setRepoBranch(String repoBranch) {
        this.repoBranch = repoBranch;
    }
    
    public String getIssueNumber() {
        return issueNumber;
    }
    
    public void setIssueNumber(String issueNumber) {
        this.issueNumber = issueNumber;
    }
    
    public String getIssueTitle() {
        return issueTitle;
    }
    
    public void setIssueTitle(String issueTitle) {
        this.issueTitle = issueTitle;
    }
    
    public String getIssueDescription() {
        return issueDescription;
    }
    
    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }
    
    public List<String> getTestCommands() {
        return testCommands;
    }
    
    public void setTestCommands(List<String> testCommands) {
        this.testCommands = testCommands;
    }
    
    public List<String> getFailingTests() {
        return failingTests;
    }
    
    public void setFailingTests(List<String> failingTests) {
        this.failingTests = failingTests;
    }
    
    public List<String> getExpectedFiles() {
        return expectedFiles;
    }
    
    public void setExpectedFiles(List<String> expectedFiles) {
        this.expectedFiles = expectedFiles;
    }
    
    public int getDifficultyLevel() {
        return difficultyLevel;
    }
    
    public void setDifficultyLevel(int difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getBaselinePatch() {
        return baselinePatch;
    }
    
    public void setBaselinePatch(String baselinePatch) {
        this.baselinePatch = baselinePatch;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    public int getExpectedTimeMinutes() {
        return expectedTimeMinutes;
    }
    
    public void setExpectedTimeMinutes(int expectedTimeMinutes) {
        this.expectedTimeMinutes = expectedTimeMinutes;
    }
}