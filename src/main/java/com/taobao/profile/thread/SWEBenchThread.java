/**
 * (C) 2011-2012 Alibaba Group Holding Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 */
package com.taobao.profile.thread;

import com.taobao.profile.Manager;
import com.taobao.profile.swebench.SWEBenchManager;

/**
 * SWE-bench集成线程
 * 负责处理来自InnerSocketThread的SWE-bench相关命令
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class SWEBenchThread {
    
    private static SWEBenchThread instance = new SWEBenchThread();
    
    private SWEBenchThread() {
    }
    
    public static SWEBenchThread getInstance() {
        return instance;
    }
    
    /**
     * 处理SWE-bench命令
     * 
     * @param command 命令
     * @return 响应结果
     */
    public String handleCommand(String command) {
        if (command == null) {
            return "ERROR: 命令为空";
        }
        
        String[] parts = command.split(":");
        String action = parts[0];
        
        try {
            if (Manager.SWEBENCH_START.equals(action)) {
                if (parts.length < 2) {
                    return "ERROR: 缺少模型名称参数";
                }
                return startSWEBench(parts[1]);
                
            } else if (Manager.SWEBENCH_STOP.equals(action)) {
                return stopSWEBench();
                
            } else if (Manager.SWEBENCH_STATUS.equals(action)) {
                return getSWEBenchStatus();
                
            } else {
                return "ERROR: 未知的SWE-bench命令: " + action;
            }
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    /**
     * 启动SWE-bench评测
     */
    private String startSWEBench(String modelName) {
        try {
            SWEBenchManager manager = SWEBenchManager.getInstance();
            manager.initialize();
            
            // 在新线程中启动评测，避免阻塞
            Thread benchmarkThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    manager.startBenchmark(modelName);
                }
            });
            benchmarkThread.setName("TProfiler-SWEBench-" + modelName);
            benchmarkThread.setDaemon(true);
            benchmarkThread.start();
            
            return "OK: SWE-bench评测已启动，模型: " + modelName;
            
        } catch (Exception e) {
            return "ERROR: 启动失败 - " + e.getMessage();
        }
    }
    
    /**
     * 停止SWE-bench评测
     */
    private String stopSWEBench() {
        try {
            SWEBenchManager manager = SWEBenchManager.getInstance();
            manager.stopBenchmark();
            return "OK: SWE-bench评测已停止";
            
        } catch (Exception e) {
            return "ERROR: 停止失败 - " + e.getMessage();
        }
    }
    
    /**
     * 获取SWE-bench状态
     */
    private String getSWEBenchStatus() {
        try {
            SWEBenchManager manager = SWEBenchManager.getInstance();
            String status = manager.getStatus();
            return "OK: SWE-bench状态 - " + status;
            
        } catch (Exception e) {
            return "ERROR: 获取状态失败 - " + e.getMessage();
        }
    }
}