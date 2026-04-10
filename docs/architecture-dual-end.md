# 双端项目架构说明

## 1. 系统定位

本仓库不是单一网页，也不是单一 Android 检测 App，而是一个围绕 **analysis run -> event -> case -> evidence -> report** 构建的双端协同系统：

- **后端**：统一分析与案件编排中枢
- **网页端**：run 总览、筛选、复核、证据链展示端
- **Android 端**：移动协同查看与复核辅助端

## 2. 三层分工

### 后端
- 管理多个 analysis run 历史
- 枚举本地 `data/` 视频源
- 视频抽样分析
- 后端视觉识别服务（默认 mock 演示识别，可选接第三方视觉服务）
- 时序融合生成事件
- 按案件归档
- 生成 15 秒证据片段与正式举报文书
- 提供 run / event / case / report API
- 维护案件复核状态流转

### 网页端
- 展示 overview 与 latest run
- 展示 run 历史面板
- 选择 source 并触发分析
- 按 `status / plate / run_id / review_status` 筛选事件与案件
- 编辑案件复核字段
- 展示证据图、片段、文书与关联事件
- 作为统一操作入口
- 当前 Web 视觉已对齐 Android 协同端的浅紫主题：统一背景层、卡片层、主按钮与状态色，便于双端演示时保持同一套产品语言

### Android 端
- 连接同一 FastAPI 后端
- 展示 run 摘要
- 按状态筛选案件
- 查看案件详情、片段与证据图
- 查看与编辑 `corrected_plate_number / operator_note / review_status`
- 展示并同步举报状态

## 3. 数据主线

当前系统的数据主线是：

1. source video
2. analysis run
3. captures
4. events
5. cases
6. evidence / clip / report

其中：
- `analysis_runs` 是任务历史主表
- `events` 记录一次 run 的违规事件
- `cases` 记录按车牌聚合后的案件与人工复核状态
- Web 与 Android 共用这一套后端数据，不各自维护独立案件模型

## 4. 与参考 Android 原型的关系

`newnew` 里的文档和旧 Android 原型说明了另一条路线：

- CameraX 实时采集
- JNI + ncnn / YOLO 端侧检测
- HyperLPR 本地识别
- Room 案件管理

本仓库**没有直接复刻**那条高耦合端侧检测链，而是吸收其“案件化闭环”和“模块化表达”，将其沉淀为当前双端协同结构。

## 5. 当前架构价值

这种结构能够同时体现：

1. 多 source、多 run 的演示能力；
2. event -> case 的数据组织；
3. 人工复核与举报状态联动；
4. 证据图、片段、文书的完整证据链；
5. Web 与 Android 的角色分工；
6. 从参考原型到当前主系统的演进过程。

## 6. Android 当前代码边界（2026-04-08）

Android 端已完成一轮“联调优先”的结构化拆分：

- `MainActivity.kt`：只负责 Compose 入口挂载；
- `model/MobileModels.kt`：承载 overview / run / case / connection / ui state 模型；
- `network/`：承载 Retrofit 接口与 client 创建；
- `viewmodel/MainViewModel.kt`：承载列表加载、案件复核、举报、API 地址切换；
- `ui/`：承载连接诊断、总览、筛选、案件列表、案件详情等 Compose 组件；
- `ApiBaseUrlStore.kt`：承载真机调试地址覆盖与恢复默认地址逻辑。

这样处理后，Android 端仍然保持“移动协同查看 / 复核辅助”的定位，但不再由一个超大 Activity 同时承担 API、状态和 UI。
