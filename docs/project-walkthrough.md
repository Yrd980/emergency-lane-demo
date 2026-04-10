# 项目运行流程说明

## 主流程

### 1. 启动后端
后端负责统一提供分析、案件、复核与举报接口。

### 2. 选择视频源并触发分析
通过 Web 端 source selector 选择一个本地视频源，然后触发一次 analysis run。

### 3. 后端处理链
后端会依次执行：

- 读取 source video
- 抽样关键帧
- 后端视觉识别服务判断是否疑似占用应急车道
- 尽量提取车牌号（默认走 mock 演示识别，不以第三方视觉服务为前提）
- 通过时序规则融合为事件
- 按车牌归档为案件
- 生成证据图
- 生成 15 秒证据片段
- 生成正式举报文书
- 将结果归入对应 run 历史

### 4. Web 端操作路径
Web 端可完成：

- 查看 latest run 与 run 历史
- 按 `status / plate / run_id / review_status` 筛选事件
- 按 `status / plate / run_id / review_status` 筛选案件
- 进入案件详情查看证据图、片段、文书
- 编辑：
  - corrected plate
  - review status
  - operator note
- 在案件进入 `待举报` 后发起模拟举报
- 以与 Android 端一致的浅紫协同主题展示 overview、列表、详情、证据链与反馈状态，适合现场连续演示双端联动

### 5. Android 端协同路径
Android 端连接同一套后端接口，完成：

- 查看 latest run 摘要
- 按状态筛选案件
- 查看案件详情、证据图、片段与文书
- 查看和更新复核字段
- 在案件进入 `待举报` 后查看并同步举报结果

## 典型演示路径

1. 选择 source A 并分析；
2. 再选择 source B 并分析；
3. 查看 `/api/runs` 或 Web run panel，确认两个 run 都保留；
4. 用 `run_id` 筛选事件/案件；
5. 打开一个案件，修正车牌、填写备注、将复核状态改为“复核通过”；
6. 提交案件模拟举报；
7. 在 Android 端刷新后确认同一案件状态已经同步为“已举报”。

## 结果特征

这种方式保证三部分围绕同一套数据模型运行，而不是各自维护独立业务链，并且让毕设演示可以清楚展示：

- 多次任务历史
- 可选数据源
- 案件人工复核
- 双端状态同步
- 完整证据链输出

## 真机联调补充路径（2026-04-08）

1. 后端使用 `0.0.0.0` 启动，并确认 `http://<开发机局域网IP>:8000/api/health` 可从手机访问；
2. Android 构建时可通过 `-PemergencyLaneApiBaseUrl=http://<开发机局域网IP>:8000/api/` 注入默认地址；
3. 若现场网络变化，可在 App 顶部“连接诊断”卡里直接切换到新的局域网地址；
4. 切换后先看“最近一次总览拉取结果”，确认 overview 拉通，再进入案件复核与举报演示；
5. 若需要回到模拟器或构建默认地址，可直接在同一张卡片里恢复默认配置。

6. 若要降低真机覆盖安装摩擦，优先使用 `android/scripts/install-debug.sh`（内部会先构建 APK，再尝试 `adb install -r -t`，必要时回退到 `adb push + pm install`，并在成功后自动拉起 App）；
7. 当前本机 Android 构建建议使用 JDK 17，例如 `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew --no-daemon assembleDebug`，可避免 JDK 21 下的 Dex/构建异常。
