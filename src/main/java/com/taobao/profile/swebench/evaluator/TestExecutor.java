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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.taobao.profile.swebench.task.SWEBenchTask;
import com.taobao.profile.swebench.task.TaskResult;

/**
 * 测试执行器
 * 负责应用补丁并执行测试
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class TestExecutor {
    
    private DockerEnvironment dockerEnv;
    
    public TestExecutor(DockerEnvironment dockerEnv) {
        this.dockerEnv = dockerEnv;
    }
    
    /**
     * 运行测试
     */
    public TaskResult.TestResult runTests(SWEBenchTask task, String patch) throws IOException {
        TaskResult.TestResult result = new TaskResult.TestResult();
        String containerName = dockerEnv.getContainerName(task);
        
        try {
            // 1. 应用补丁
            applyPatch(containerName, patch);
            
            // 2. 运行测试命令
            List<String> testOutputs = new ArrayList<>();
            
            if (task.getTestCommands() != null && !task.getTestCommands().isEmpty()) {
                for (String testCommand : task.getTestCommands()) {
                    String output = dockerEnv.executeInContainer(containerName, testCommand);
                    testOutputs.add(output);
                }
            } else {
                // 使用默认测试命令
                String output = runDefaultTests(containerName, task);
                testOutputs.add(output);
            }
            
            // 3. 解析测试结果
            parseTestResults(testOutputs, result);
            
            // 4. 检查失败的测试
            if (task.getFailingTests() != null) {
                checkFailingTests(containerName, task.getFailingTests(), result);
            }
            
        } catch (Exception e) {
            result.setTestOutput("测试执行失败: " + e.getMessage());
            result.setTotalTests(1);
            result.setFailedTests(1);
        }
        
        return result;
    }
    
    /**
     * 应用补丁
     */
    private void applyPatch(String containerName, String patch) throws IOException {
        // 将补丁保存到临时文件
        File patchFile = File.createTempFile("patch", ".diff");
        try (FileWriter writer = new FileWriter(patchFile)) {
            writer.write(patch);
        }
        
        // 复制补丁到容器
        dockerEnv.copyToContainer(containerName, patchFile.getAbsolutePath(), "/tmp/patch.diff");
        
        // 应用补丁
        String applyCommand = "cd /workspace && git apply /tmp/patch.diff";
        String output = dockerEnv.executeInContainer(containerName, applyCommand);
        
        // 清理临时文件
        patchFile.delete();
        
        // 检查补丁是否应用成功
        if (output.contains("error") || output.contains("failed")) {
            throw new IOException("补丁应用失败: " + output);
        }
    }
    
    /**
     * 运行默认测试
     */
    private String runDefaultTests(String containerName, SWEBenchTask task) throws IOException {
        // 尝试常见的测试命令
        String[] testCommands = {
            "python -m pytest",
            "python -m unittest discover",
            "npm test",
            "mvn test",
            "gradle test",
            "make test"
        };
        
        for (String command : testCommands) {
            try {
                String output = dockerEnv.executeInContainer(containerName, 
                    "cd /workspace && " + command + " 2>&1 || true");
                if (!output.contains("command not found")) {
                    return output;
                }
            } catch (Exception e) {
                // 忽略错误，尝试下一个命令
            }
        }
        
        return "No test command found";
    }
    
    /**
     * 解析测试结果
     */
    private void parseTestResults(List<String> outputs, TaskResult.TestResult result) {
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        List<String> failedTestNames = new ArrayList<>();
        StringBuilder fullOutput = new StringBuilder();
        
        for (String output : outputs) {
            fullOutput.append(output).append("\n");
            
            // 解析pytest输出
            if (output.contains("passed") || output.contains("failed")) {
                Pattern pytestPattern = Pattern.compile("(\\d+) passed.*?(\\d+) failed");
                Matcher matcher = pytestPattern.matcher(output);
                if (matcher.find()) {
                    passedTests += Integer.parseInt(matcher.group(1));
                    failedTests += Integer.parseInt(matcher.group(2));
                }
            }
            
            // 解析unittest输出
            if (output.contains("Ran") && output.contains("tests")) {
                Pattern unittestPattern = Pattern.compile("Ran (\\d+) tests?");
                Matcher matcher = unittestPattern.matcher(output);
                if (matcher.find()) {
                    totalTests = Integer.parseInt(matcher.group(1));
                }
                
                if (output.contains("OK")) {
                    passedTests = totalTests;
                } else if (output.contains("FAILED")) {
                    Pattern failPattern = Pattern.compile("failures=(\\d+)");
                    matcher = failPattern.matcher(output);
                    if (matcher.find()) {
                        failedTests = Integer.parseInt(matcher.group(1));
                        passedTests = totalTests - failedTests;
                    }
                }
            }
            
            // 提取失败的测试名称
            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.contains("FAILED") || line.contains("FAIL:")) {
                    failedTestNames.add(line.trim());
                }
            }
        }
        
        // 如果没有解析到总测试数，根据已知数据计算
        if (totalTests == 0) {
            totalTests = passedTests + failedTests;
        }
        
        result.setTotalTests(totalTests);
        result.setPassedTests(passedTests);
        result.setFailedTests(failedTests);
        result.setFailedTestNames(failedTestNames);
        result.setTestOutput(fullOutput.toString());
    }
    
    /**
     * 检查特定的失败测试
     */
    private void checkFailingTests(String containerName, List<String> failingTests, 
                                  TaskResult.TestResult result) throws IOException {
        List<String> stillFailing = new ArrayList<>();
        
        for (String testName : failingTests) {
            // 运行单个测试
            String command = String.format("cd /workspace && python -m pytest %s -v 2>&1 || true", testName);
            String output = dockerEnv.executeInContainer(containerName, command);
            
            if (output.contains("FAILED") || output.contains("ERROR")) {
                stillFailing.add(testName);
            }
        }
        
        // 更新失败的测试列表
        if (!stillFailing.isEmpty()) {
            result.setFailedTestNames(stillFailing);
            result.setFailedTests(stillFailing.size());
            
            // 调整通过的测试数
            if (result.getTotalTests() > 0) {
                result.setPassedTests(result.getTotalTests() - stillFailing.size());
            }
        }
    }
}