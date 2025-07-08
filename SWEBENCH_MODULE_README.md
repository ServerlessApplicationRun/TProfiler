# TProfiler SWE-bench 性能分析和评测模块

## 概述

SWE-bench模块是TProfiler的一个扩展功能，用于评测AI模型在软件工程任务上的性能表现。该模块实现了对AI模型解决真实GitHub issues能力的自动化评测。

## 主要功能

1. **任务管理**
   - 支持从GitHub加载真实的软件工程任务
   - 支持自定义任务创建
   - 任务难度分级和分类

2. **模型评测**
   - 支持多种主流AI模型（GPT-4、Claude、Llama等）
   - 自动调用模型API生成解决方案
   - 在Docker容器中安全执行代码

3. **性能分析**
   - 执行时间统计
   - 资源使用监控（CPU、内存）
   - API调用和Token使用统计
   - 成本估算

4. **测试验证**
   - 自动应用生成的补丁
   - 运行项目测试套件
   - 解析测试结果

5. **报告生成**
   - 多格式报告（文本、HTML、JSON、CSV）
   - 详细的性能指标
   - 可视化结果展示

## 使用方法

### 1. 配置

编辑 `swebench.properties` 文件：

```properties
# 基本配置
swebench.parallel.tasks=4
swebench.task.timeout=30
swebench.max.retry=3

# 模型API配置
swebench.model.api.url=https://api.openai.com/v1/completions
swebench.model.api.key=your-api-key-here
swebench.model.max.tokens=4096

# Docker配置
swebench.docker.image=swebench/eval:latest

# 数据集类型：full, lite, verified
swebench.dataset.type=lite
```

### 2. 启动评测

#### 命令行模式

```bash
# 开始评测
./swebench-client start GPT-4

# 停止评测
./swebench-client stop

# 查看状态
./swebench-client status

# 列出支持的模型
./swebench-client list

# 查看帮助
./swebench-client help
```

#### 交互模式

直接运行 `./swebench-client` 进入交互式菜单。

### 3. 查看结果

评测完成后，报告会保存在配置的报告路径下（默认为 `~/swebench-reports`）：

- `swebench_<model>_<timestamp>.txt` - 文本报告
- `swebench_<model>_<timestamp>.html` - HTML报告（可在浏览器中查看）
- `swebench_<model>_<timestamp>.json` - JSON格式（便于程序处理）
- `swebench_<model>_<timestamp>.csv` - CSV格式（可导入Excel）
- `swebench_summary.txt` - 汇总报告

## 架构设计

```
com.taobao.profile.swebench/
├── SWEBenchManager.java       # 核心管理器
├── SWEBenchConfig.java        # 配置管理
├── task/
│   ├── SWEBenchTask.java      # 任务定义
│   └── TaskResult.java        # 任务结果
├── evaluator/
│   ├── ModelEvaluator.java    # 模型评估器
│   ├── DockerEnvironment.java # Docker环境管理
│   ├── ModelInterface.java    # 模型接口
│   └── TestExecutor.java      # 测试执行器
├── reporter/
│   └── BenchmarkReporter.java # 报告生成器
└── client/
    └── SWEBenchClient.java    # 客户端程序
```

## 性能指标

评测报告包含以下关键指标：

1. **成功率**：成功解决的任务占比
2. **执行时间**：每个任务的执行耗时
3. **测试通过率**：生成代码的测试覆盖度
4. **资源使用**：CPU、内存使用情况
5. **API调用**：模型API调用次数
6. **Token使用**：总Token消耗量
7. **成本估算**：基于Token使用的成本

## 集成TProfiler

SWE-bench模块与TProfiler深度集成：

1. 使用TProfiler的性能分析功能监控评测过程
2. 利用TProfiler的线程分析追踪并发任务执行
3. 通过TProfiler的慢查询分析优化Docker操作

## 扩展性

该模块设计为易于扩展：

1. **添加新模型**：实现 `ModelInterface` 接口
2. **自定义任务源**：扩展任务加载逻辑
3. **新的报告格式**：在 `BenchmarkReporter` 中添加新方法
4. **测试框架支持**：扩展 `TestExecutor` 的解析逻辑

## 依赖要求

- Java 6+
- Docker
- 网络连接（用于调用模型API和下载GitHub仓库）

## 注意事项

1. 确保Docker已正确安装和配置
2. 模型API密钥请妥善保管
3. 评测过程可能耗时较长，建议在服务器上运行
4. 注意API调用成本，合理设置并行任务数

## 未来计划

1. 支持更多编程语言（目前主要支持Python）
2. 增加更多模型支持
3. 实现分布式评测
4. 添加实时监控界面
5. 支持自定义评测指标

## 贡献

欢迎提交Issue和Pull Request来改进这个模块！