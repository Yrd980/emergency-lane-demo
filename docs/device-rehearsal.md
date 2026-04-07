# 真机安装与全链路彩排说明

## 目标

本轮补齐两件事：

1. 让 Android 真机安装/覆盖安装路径更稳定；
2. 让真机在弱网环境下也能继续承担“移动协同查看 / 复核辅助端”角色。

当前仍坚持 **FastAPI 是案件、证据链、举报状态的最终事实源**。  
Android 新增的本地能力只承担：

- **本地缓存层**：缓存最近一次成功同步的 `overview / runs / cases / selected case`；
- **端侧草稿位**：缓存候选车牌、现场补充说明与本地复核状态，作为后续 CameraX / JNI / ncnn/YOLO / HyperLPR 接入前的增强链占位。

## 真机安装

### 推荐命令

```bash
cd android
ANDROID_SERIAL=192.168.120.13:39929 ./scripts/install-debug.sh
```

### 脚本行为

- 优先读取 `ANDROID_SERIAL`
- 未指定时自动挑选一个在线 adb 设备
- 执行 `./gradlew --no-daemon assembleDebug`，随后尝试 `adb install -r -t`；若流式安装失败，再回退到 `adb push + pm install`
- 安装成功后自动拉起 `com.yrd.emergencylanemobile/.MainActivity`

### 覆盖安装失败时

若设备 ROM 拒绝覆盖安装、厂商系统要求额外确认，或 `installDebug` 因 `--no-streaming` 失败：

```bash
cd android
ANDROID_SERIAL=192.168.120.13:39929 ./scripts/install-debug.sh --clean
```

`--clean` 会在安装失败后先卸载旧调试包，再重新安装。

### 为什么优先用脚本

相比直接运行 `installDebug`，该脚本更适合当前仓库当前的真机场景：

- 优先锁定单一设备，避免多 adb 连接时把安装目标打歪；
- 不走 Gradle/ddmlib 的安装通路，可绕开部分设备上 `pm install --no-streaming` 不兼容的问题；
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

当前这是 **增强链占位**，不是新的事实源。

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
   ANDROID_SERIAL=192.168.120.13:39929 ./scripts/install-debug.sh
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
