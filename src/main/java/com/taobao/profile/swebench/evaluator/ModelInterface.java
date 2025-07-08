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
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import com.taobao.profile.swebench.SWEBenchConfig;
import com.taobao.profile.swebench.task.SWEBenchTask;

/**
 * 模型接口
 * 负责与AI模型进行交互，生成解决方案
 * 
 * @author TProfiler Team
 * @since 2025-1
 */
public class ModelInterface {
    
    private SWEBenchConfig config;
    private int lastApiCallCount = 0;
    private int lastTokenCount = 0;
    
    public ModelInterface(SWEBenchConfig config) {
        this.config = config;
    }
    
    /**
     * 调用模型生成解决方案
     */
    public String generateSolution(SWEBenchTask task, String modelName) throws IOException {
        // 重置计数器
        lastApiCallCount = 0;
        lastTokenCount = 0;
        
        String prompt = buildPrompt(task);
        String response = callModel(modelName, prompt);
        
        // 从响应中提取补丁
        return extractPatch(response);
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(SWEBenchTask task) {
        StringBuilder prompt = new StringBuilder();
        
        // 系统提示
        prompt.append("You are an expert software engineer. ");
        prompt.append("Your task is to solve the following GitHub issue by generating a patch.\n\n");
        
        // 任务描述
        prompt.append(task.generateTaskPrompt());
        
        // 指导说明
        prompt.append("\nInstructions:\n");
        prompt.append("1. Analyze the issue carefully\n");
        prompt.append("2. Identify the root cause\n");
        prompt.append("3. Generate a minimal patch that fixes the issue\n");
        prompt.append("4. Make sure the patch follows the project's coding style\n");
        prompt.append("5. The patch should be in unified diff format\n\n");
        
        prompt.append("Please provide your solution as a patch:\n");
        
        return prompt.toString();
    }
    
    /**
     * 调用模型API
     */
    private String callModel(String modelName, String prompt) throws IOException {
        lastApiCallCount++;
        
        // 这里是一个简化的实现，实际应该根据不同的模型调用相应的API
        if (config.getModelApiUrl() == null || config.getModelApiUrl().isEmpty()) {
            // 如果没有配置API，返回模拟响应
            return generateMockResponse(modelName, prompt);
        }
        
        // 调用真实API
        return callRealAPI(modelName, prompt);
    }
    
    /**
     * 调用真实的模型API
     */
    private String callRealAPI(String modelName, String prompt) throws IOException {
        URL url = new URL(config.getModelApiUrl());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + config.getModelApiKey());
            conn.setDoOutput(true);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("prompt", prompt);
            requestBody.put("max_tokens", config.getModelMaxTokens());
            
            // 发送请求
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                writer.write(toJson(requestBody));
            }
            
            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
            
            // 解析响应并更新token计数
            Map<String, Object> responseData = parseJson(response.toString());
            if (responseData.containsKey("usage")) {
                Map<String, Object> usage = (Map<String, Object>) responseData.get("usage");
                lastTokenCount = ((Number) usage.get("total_tokens")).intValue();
            }
            
            return (String) responseData.get("content");
            
        } finally {
            conn.disconnect();
        }
    }
    
    /**
     * 生成模拟响应（用于测试）
     */
    private String generateMockResponse(String modelName, String prompt) {
        lastTokenCount = prompt.length() / 4; // 粗略估算token数
        
        StringBuilder response = new StringBuilder();
        response.append("Based on the issue description, here is the patch:\n\n");
        response.append("```diff\n");
        response.append("--- a/example.py\n");
        response.append("+++ b/example.py\n");
        response.append("@@ -10,7 +10,7 @@\n");
        response.append(" def example_function():\n");
        response.append("-    return \"old value\"\n");
        response.append("+    return \"new value\"\n");
        response.append(" \n");
        response.append("```\n");
        
        return response.toString();
    }
    
    /**
     * 从响应中提取补丁
     */
    private String extractPatch(String response) {
        // 查找diff代码块
        int startIndex = response.indexOf("```diff");
        if (startIndex == -1) {
            startIndex = response.indexOf("```patch");
        }
        
        if (startIndex != -1) {
            startIndex = response.indexOf('\n', startIndex) + 1;
            int endIndex = response.indexOf("```", startIndex);
            if (endIndex != -1) {
                return response.substring(startIndex, endIndex).trim();
            }
        }
        
        // 如果没有找到代码块，尝试查找diff格式
        if (response.contains("--- ") && response.contains("+++ ")) {
            return extractDiffFormat(response);
        }
        
        // 返回整个响应作为补丁
        return response;
    }
    
    /**
     * 提取diff格式的补丁
     */
    private String extractDiffFormat(String response) {
        StringBuilder patch = new StringBuilder();
        String[] lines = response.split("\n");
        boolean inDiff = false;
        
        for (String line : lines) {
            if (line.startsWith("--- ") || line.startsWith("+++ ") || 
                line.startsWith("@@ ") || line.startsWith("+") || 
                line.startsWith("-") || line.startsWith(" ")) {
                inDiff = true;
                patch.append(line).append("\n");
            } else if (inDiff && !line.trim().isEmpty() && 
                      !line.startsWith("+") && !line.startsWith("-")) {
                // 结束diff部分
                break;
            }
        }
        
        return patch.toString().trim();
    }
    
    /**
     * 简单的JSON序列化
     */
    private String toJson(Map<String, Object> map) {
        // 这里应该使用真正的JSON库，这只是一个简化示例
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            
            if (entry.getValue() instanceof String) {
                json.append("\"").append(escapeJson((String) entry.getValue())).append("\"");
            } else {
                json.append(entry.getValue());
            }
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * 简单的JSON解析
     */
    private Map<String, Object> parseJson(String json) {
        // 这里应该使用真正的JSON库，这只是一个简化示例
        Map<String, Object> result = new HashMap<>();
        // TODO: 实现JSON解析
        return result;
    }
    
    // Getters
    
    public int getLastApiCallCount() {
        return lastApiCallCount;
    }
    
    public int getLastTokenCount() {
        return lastTokenCount;
    }
}