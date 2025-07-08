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

import com.taobao.profile.swebench.SWEBenchConfig;
import com.taobao.profile.swebench.task.SWEBenchTask;

/**
 * Docker环境管理
 * 负责创建和管理任务执行的Docker容器
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class DockerEnvironment {
    
    private SWEBenchConfig config;
    private static final String CONTAINER_PREFIX = "swebench-";
    
    public DockerEnvironment(SWEBenchConfig config) {
        this.config = config;
    }
    
    /**
     * 准备Docker容器
     */
    public void prepareContainer(SWEBenchTask task, String repoPath) throws IOException {
        String containerName = getContainerName(task);
        
        // 检查容器是否已存在
        if (containerExists(containerName)) {
            // 停止并删除旧容器
            stopContainer(containerName);
            removeContainer(containerName);
        }
        
        // 创建新容器
        createContainer(task, containerName, repoPath);
    }
    
    /**
     * 创建容器
     */
    private void createContainer(SWEBenchTask task, String containerName, String repoPath) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("-d");
        command.add("--name");
        command.add(containerName);
        command.add("-v");
        command.add(repoPath + ":/workspace");
        command.add("-w");
        command.add("/workspace");
        
        // 设置资源限制
        command.add("--memory=4g");
        command.add("--cpus=2");
        
        // 使用配置的镜像
        command.add(config.getDockerImage());
        command.add("sleep");
        command.add("infinity");
        
        executeDockerCommand(command);
    }
    
    /**
     * 在容器中执行命令
     */
    public String executeInContainer(String containerName, String cmd) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("exec");
        command.add(containerName);
        command.add("bash");
        command.add("-c");
        command.add(cmd);
        
        return executeDockerCommand(command);
    }
    
    /**
     * 复制文件到容器
     */
    public void copyToContainer(String containerName, String sourcePath, String destPath) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("cp");
        command.add(sourcePath);
        command.add(containerName + ":" + destPath);
        
        executeDockerCommand(command);
    }
    
    /**
     * 从容器复制文件
     */
    public void copyFromContainer(String containerName, String sourcePath, String destPath) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("cp");
        command.add(containerName + ":" + sourcePath);
        command.add(destPath);
        
        executeDockerCommand(command);
    }
    
    /**
     * 清理容器
     */
    public void cleanupContainer(SWEBenchTask task) {
        String containerName = getContainerName(task);
        try {
            stopContainer(containerName);
            removeContainer(containerName);
        } catch (Exception e) {
            // 忽略清理错误
        }
    }
    
    /**
     * 检查容器是否存在
     */
    private boolean containerExists(String containerName) {
        try {
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("ps");
            command.add("-a");
            command.add("--format");
            command.add("{{.Names}}");
            
            String output = executeDockerCommand(command);
            return output.contains(containerName);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 停止容器
     */
    private void stopContainer(String containerName) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("stop");
        command.add(containerName);
        
        executeDockerCommand(command);
    }
    
    /**
     * 删除容器
     */
    private void removeContainer(String containerName) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("rm");
        command.add(containerName);
        
        executeDockerCommand(command);
    }
    
    /**
     * 执行Docker命令
     */
    private String executeDockerCommand(List<String> command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Docker命令执行失败: " + String.join(" ", command) + 
                                    "\n输出: " + output.toString());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Docker命令被中断");
        }
        
        return output.toString();
    }
    
    /**
     * 获取容器名称
     */
    public String getContainerName(SWEBenchTask task) {
        return CONTAINER_PREFIX + task.getTaskId().toLowerCase().replaceAll("[^a-z0-9-]", "-");
    }
}