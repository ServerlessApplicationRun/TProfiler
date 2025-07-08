/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.swebench.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import com.taobao.profile.swebench.SWEBenchManager;
import com.taobao.profile.swebench.task.SWEBenchTask;

/**
 * SWE-bench客户端
 * 用于启动和管理SWE-bench评测
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class SWEBenchClient {
    
    private static final String VERSION = "1.0.0";
    
    public static void main(String[] args) {
        SWEBenchClient client = new SWEBenchClient();
        
        if (args.length == 0) {
            client.interactiveMode();
        } else {
            client.commandMode(args);
        }
    }
    
    /**
     * 命令行模式
     */
    private void commandMode(String[] args) {
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "start":
                if (args.length < 2) {
                    System.err.println("用法: swebench-client start <model-name>");
                    System.exit(1);
                }
                startBenchmark(args[1]);
                break;
                
            case "stop":
                stopBenchmark();
                break;
                
            case "status":
                getStatus();
                break;
                
            case "help":
            case "-h":
            case "--help":
                printHelp();
                break;
                
            case "version":
            case "-v":
            case "--version":
                System.out.println("SWE-bench Client " + VERSION);
                break;
                
            case "list":
                listModels();
                break;
                
            case "config":
                showConfig();
                break;
                
            default:
                System.err.println("未知命令: " + command);
                System.err.println("使用 'swebench-client help' 查看帮助");
                System.exit(1);
        }
    }
    
    /**
     * 交互模式
     */
    private void interactiveMode() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=====================================");
        System.out.println("SWE-bench 评测客户端 v" + VERSION);
        System.out.println("=====================================");
        System.out.println();
        
        printMenu();
        
        while (true) {
            System.out.print("\n请选择操作: ");
            String input = scanner.nextLine().trim();
            
            switch (input) {
                case "1":
                    System.out.print("请输入模型名称: ");
                    String modelName = scanner.nextLine().trim();
                    startBenchmark(modelName);
                    break;
                    
                case "2":
                    stopBenchmark();
                    break;
                    
                case "3":
                    getStatus();
                    break;
                    
                case "4":
                    listModels();
                    break;
                    
                case "5":
                    showConfig();
                    break;
                    
                case "6":
                    addCustomTask(scanner);
                    break;
                    
                case "0":
                case "q":
                case "quit":
                case "exit":
                    System.out.println("退出程序");
                    System.exit(0);
                    break;
                    
                default:
                    System.out.println("无效的选择，请重试");
            }
            
            printMenu();
        }
    }
    
    /**
     * 打印菜单
     */
    private void printMenu() {
        System.out.println("\n----- 菜单 -----");
        System.out.println("1. 开始评测");
        System.out.println("2. 停止评测");
        System.out.println("3. 查看状态");
        System.out.println("4. 列出支持的模型");
        System.out.println("5. 查看配置");
        System.out.println("6. 添加自定义任务");
        System.out.println("0. 退出");
        System.out.println("----------------");
    }
    
    /**
     * 开始评测
     */
    private void startBenchmark(String modelName) {
        try {
            System.out.println("正在启动SWE-bench评测...");
            System.out.println("模型: " + modelName);
            
            // 初始化评测管理器
            SWEBenchManager manager = SWEBenchManager.getInstance();
            manager.initialize();
            
            // 启动评测
            boolean success = manager.startBenchmark(modelName);
            
            if (success) {
                System.out.println("评测已完成");
            } else {
                System.err.println("评测失败");
            }
            
        } catch (Exception e) {
            System.err.println("启动评测时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 停止评测
     */
    private void stopBenchmark() {
        try {
            System.out.println("正在停止评测...");
            
            SWEBenchManager manager = SWEBenchManager.getInstance();
            manager.stopBenchmark();
            
            System.out.println("评测已停止");
            
        } catch (Exception e) {
            System.err.println("停止评测时出错: " + e.getMessage());
        }
    }
    
    /**
     * 获取状态
     */
    private void getStatus() {
        try {
            SWEBenchManager manager = SWEBenchManager.getInstance();
            String status = manager.getStatus();
            
            System.out.println("当前状态: " + status);
            
        } catch (Exception e) {
            System.err.println("获取状态时出错: " + e.getMessage());
        }
    }
    
    /**
     * 列出支持的模型
     */
    private void listModels() {
        System.out.println("\n支持的模型:");
        System.out.println("- GPT-4");
        System.out.println("- GPT-3.5-turbo");
        System.out.println("- Claude-2");
        System.out.println("- Claude-instant");
        System.out.println("- Llama-2-70b");
        System.out.println("- CodeLlama-34b");
        System.out.println("- StarCoder");
        System.out.println("- Custom (需要配置API)");
    }
    
    /**
     * 显示配置
     */
    private void showConfig() {
        try {
            SWEBenchManager manager = SWEBenchManager.getInstance();
            manager.initialize();
            
            System.out.println("\n当前配置:");
            System.out.println("并行任务数: " + manager.getConfig().getParallelTaskCount());
            System.out.println("任务超时: " + manager.getConfig().getTaskTimeoutMinutes() + " 分钟");
            System.out.println("最大重试: " + manager.getConfig().getMaxRetryCount() + " 次");
            System.out.println("报告路径: " + manager.getConfig().getReportPath());
            System.out.println("数据集类型: " + manager.getConfig().getDatasetType());
            System.out.println("Docker镜像: " + manager.getConfig().getDockerImage());
            System.out.println("启用性能分析: " + manager.getConfig().isEnableProfiling());
            
        } catch (Exception e) {
            System.err.println("显示配置时出错: " + e.getMessage());
        }
    }
    
    /**
     * 添加自定义任务
     */
    private void addCustomTask(Scanner scanner) {
        System.out.println("\n添加自定义任务:");
        
        try {
            System.out.print("任务ID: ");
            String taskId = scanner.nextLine().trim();
            
            System.out.print("仓库所有者: ");
            String repoOwner = scanner.nextLine().trim();
            
            System.out.print("仓库名称: ");
            String repoName = scanner.nextLine().trim();
            
            System.out.print("Issue编号: ");
            String issueNumber = scanner.nextLine().trim();
            
            System.out.print("Issue标题: ");
            String issueTitle = scanner.nextLine().trim();
            
            System.out.print("Issue描述: ");
            String issueDescription = scanner.nextLine().trim();
            
            // 创建任务
            SWEBenchTask task = new SWEBenchTask(taskId, repoOwner, repoName);
            task.setIssueNumber(issueNumber);
            task.setIssueTitle(issueTitle);
            task.setIssueDescription(issueDescription);
            
            // 添加到管理器
            SWEBenchManager manager = SWEBenchManager.getInstance();
            manager.addTask(task);
            
            System.out.println("任务已添加: " + taskId);
            
        } catch (Exception e) {
            System.err.println("添加任务时出错: " + e.getMessage());
        }
    }
    
    /**
     * 打印帮助信息
     */
    private void printHelp() {
        System.out.println("用法: swebench-client [命令] [参数]");
        System.out.println();
        System.out.println("命令:");
        System.out.println("  start <model>  开始评测指定模型");
        System.out.println("  stop           停止当前评测");
        System.out.println("  status         查看评测状态");
        System.out.println("  list           列出支持的模型");
        System.out.println("  config         显示当前配置");
        System.out.println("  help           显示此帮助信息");
        System.out.println("  version        显示版本信息");
        System.out.println();
        System.out.println("如果不提供命令，将进入交互模式");
    }
}