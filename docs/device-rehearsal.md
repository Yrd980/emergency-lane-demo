# 真机安装与全链路彩排说明

## 目标

本轮补齐两件事：

1. 让 Android 真机安装/覆盖安装路径更稳定；
2. 让真机在弱网环境下也能继续承担“移动协同查看 / 复核辅助端”角色。

当前仍坚持 **FastAPI 是案件、证据链、举报状态的最终事实源**。  
但 Android 现在已经形成两条并存的能力线：

- **云侧协同线**：继续消费 FastAPI 的 `overview / runs / cases / reports`；
- **端侧本地线**：Android 项目内保留一条端侧本地检测链，包含 CameraX、JNI、YOLOv8+ncnn、HyperLPR3、Room 与本地案例流。

因此 Android 新增的本地能力现在包括：

- **本地缓存层**：缓存最近一次成功同步的 `overview / runs / cases / selected case`；
- **端侧草稿位**：缓存候选车牌、现场补充说明与本地复核状态；
- **端侧检测模式**：本地相机检测、抓拍、Room 记录、案例归档、本地裁剪与本地报告生成。

## 真机安装

> 说明：本轮仓库收口时，**没有要求把 APK 下载/安装到本地设备重新测试**。  
> 当前文档保留真机安装路径，是为了后续答辩彩排时可直接使用；本轮实际验收以 `assembleDebug` 编译通过为准。

### 推荐命令

```bash
cd android
ANDROID_SERIAL=<adb-device-id> ./scripts/install-debug.sh
```

其中：

- `<adb-device-id>` 表示当前 adb 会话里真实在线的设备标识；
- 局域网 IP、无线调试地址、配对端口、配对码都会随 Wi‑Fi / 无线调试会话变化，不应把某次现场值写死成长期文档配置。

### 脚本行为

- 优先读取 `ANDROID_SERIAL`
- 未指定时自动挑选一个在线 adb 设备
- 执行 `./gradlew --no-daemon assembleDebug`，随后尝试 `adb install -r -t`；若流式安装失败，再回退到 `adb push + pm install`
- 安装成功后自动拉起 `com.yrd.emergencylanemobile/.MainActivity`

### 覆盖安装失败时

若设备 ROM 拒绝覆盖安装、厂商系统要求额外确认，或 `installDebug` 因 `--no-streaming` 失败：

```bash
cd android
ANDROID_SERIAL=<adb-device-id> ./scripts/install-debug.sh --clean
```

`--clean` 会在安装失败后先卸载旧调试包，再重新安装。默认安装路径不会在失败时自动卸载旧包；只有显式传入 `--clean` 才会执行 remove-and-reinstall。

### 为什么优先用脚本

相比直接运行 `installDebug`，该脚本更适合当前仓库当前的真机场景：

- 优先锁定单一设备，避免多 adb 连接时把安装目标打歪；
- 不走 Gradle/ddmlib 的安装通路，可绕开部分设备上 `pm install --no-streaming` 不兼容的问题；
- 若设备只是弹出厂商确认框，脚本会先保住现有安装，不会未经显式参数就自动卸载；
- 覆盖安装被厂商 ROM 拒绝时，可显式切到 `--clean` 路径完成卸载后重装。

### 当前已验证到的安装障碍

2026-04-08 的真机联调中，已确认两类障碍：

1. `./gradlew installDebug` 在当前 Android 16 / 厂商 ROM 设备上会失败，报错指向 `Unknown option --no-streaming`；
2. 即使改走 `adb install -r -t` 或 `pm install -r -t`，若手机端未确认安装/更新授权，仍会出现 `INSTALL_FAILED_ABORTED: User rejected permissions`。

因此当前仓库侧的结论是：

- **仓库内已解决/规避的问题**：提供脚本绕开 `installDebug` 的不兼容路径，并支持失败后 `--clean` 重装；
- **仍需人工配合的问题**：手机端首次覆盖安装时，必须手动确认厂商系统弹窗。

## 端侧缓存层

### 缓存内容

缓存入口：`android/app/src/main/java/com/yrd/emergencylanemobile/DeviceAssistStore.kt`

当前缓存：

- 总览 `overview`
- run 列表 `runs`
- 案件列表 `cases`
- 当前选中的案件详情 `selectedCase`
- 当前状态筛选条件
- 端侧草稿 `localDraft`

它服务的是 **云侧协同线**，不是替代本地 Room 数据库。

### 使用方式

- 在线刷新成功时，自动覆盖本地快照
- 在线拉取失败但本地已有快照时，自动回退到本地缓存
- UI 会在“连接诊断”卡片中明确显示：
  - 当前内容源是“实时后端”还是“本地缓存”
  - 最近缓存时间
  - 已缓存的 run / case 数量

## 端侧草稿位

案件详情中新增“端侧增强链路（本机缓存）”区域，可保存：

- 端侧候选车牌
- 现场补充说明
- 本地复核状态

它的作用是：

- 真机彩排时先记录现场补充信息
- 弱网时先落本地草稿
- 网络恢复后，再通过正常 case review 接口提交到 FastAPI

当前这是 **云侧协同增强位**，不是新的事实源。

## 端侧本地检测模式

当前 Android 首页已新增“进入端侧本地检测模式”按钮。跳转后进入从 `newnew` 合并而来的本地链路，包含：

- `PendingFragment`：待处理抓拍记录
- `HomeFragment`：CameraX + JNI/ncnn 相机检测页
- `CasesFragment`：本地历史案例库
- `Room`：`ViolationRecord` / `CaseInfo`
- `HyperLPR3`：本地车牌识别
- `VideoTrimmer`：本地 15 秒片段裁剪

这条链路当前的定位不是替代后端，而是：

1. 证明项目具备端侧本地检测能力；
2. 在毕设检查时展示“云侧闭环 + 端侧能力”双轨叙事；
3. 为后续需要时，再通过后端 device 接口做导入/同步兼容。

## 后端 device 兼容接口

为兼容端侧本地链路，后端已新增：

- `POST /api/device/cases/import`
- `GET /api/device/sync-snapshot`
- `GET /api/device/cases/{id}`

当前用途是：

- 接收端侧本地案件快照/证据元数据；
- 输出端侧案件同步视图；
- 在答辩时说明本地链路与云侧后端存在兼容路径。

这不是自动双向同步系统，而是当前阶段最小可解释、最小可演示的兼容层。

## 推荐彩排顺序

1. 启动后端
   ```bash
   cd backend
   uv run uvicorn src.app.main:app --host 0.0.0.0 --port 8000 --reload
   ```
2. 启动前端
   ```bash
   cd frontend
   npm run dev
   ```
3. 安装并启动 Android 真机
   ```bash
   cd android
   ANDROID_SERIAL=<adb-device-id> ./scripts/install-debug.sh
   ```
4. Android 端确认连接诊断卡显示局域网 API 正常
5. Web 发起一次 demo analysis
6. Web 查看 run / 事件 / 案件
7. Android 打开同一案件，确认详情同步
8. Android 可选保存一条端侧草稿
9. Web 或 Android 完成正式复核并提交模拟举报
10. 双端刷新确认状态一致

## 现场弱网应对

若彩排现场网络抖动：

1. 先不要切主链；
2. 让 Android 自动回退到本地缓存继续讲解；
3. 必要时在案件详情里先保存“端侧草稿”；
4. 网络恢复后点击“重新检测”；
5. 再把最终复核结果提交到 FastAPI。

这样可以保证答辩展示不中断，同时不破坏当前双端协同闭环。
