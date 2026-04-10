# 高速公路应急车道违章辅助举报双端项目

这是一个面向毕设演示的“双端协同”项目：

- `backend/`：`uv + FastAPI`，负责视频分析、事件生成、案件归档、证据链与任务历史。
- `frontend/`：Vite + TypeScript，负责 run 总览、事件/案件筛选、证据链展示、案件人工复核编辑。
- `android/`：Kotlin + Compose，包含面向 FastAPI 的移动协同界面，以及位于 `com.example.emergencylaneguard` 下的端侧本地检测链（CameraX / JNI / YOLOv8+ncnn / HyperLPR3 / Room）。
- `data/`：演示视频、封面图与 manifest。
- `storage/`：运行时生成的关键帧、证据图、证据片段、报告文本与 SQLite 数据库。

## 当前项目能力

当前仓库已经具备完整的可演示闭环：

1. 选择本地演示视频 source；
2. 发起一次 analysis run；
3. 后端按时间间隔抽取关键帧；
4. 后端统一调用视觉识别服务判断是否疑似占用应急车道，并尽量读取车牌；默认可走 mock 演示识别，不以第三方视觉服务为前提；
5. 通过时序融合生成事件；
6. 按车牌归档为案件；
7. 生成证据图、15 秒证据片段与正式举报文书；
8. Web 与 Android 共享同一套 run / event / case 数据；
9. Android 内可切换到端侧本地检测模式，展示 CameraX、JNI、YOLOv8+ncnn、HyperLPR3 与 Room 组成的本地处理链；
10. 支持人工复核案件并联动举报状态流转。

## 毕设交付定位

当前仓库建议作为毕设主系统：

- **主线**：`backend + frontend + android` 的双端协同闭环；
- **Android 端侧补充**：已将一条来自 `newnew` 的本地检测链纳入 Android 项目，相关代码当前位于 `com.example.emergencylaneguard` 命名空间下，覆盖 CameraX、JNI、YOLOv8+ncnn、HyperLPR3 与 Room；
- **当前重点**：用“云侧主链 + 端侧演示链”双轨叙事支撑毕设检查。

因此当前阶段的实际落地是：
- **云侧主链**：FastAPI 负责视频分析、事件/案件归档、证据链与举报文书；
- **端侧演示链**：Android 内保留本地检测、抓拍、YOLOv8+ncnn 推理、HyperLPR3 识别、Room 案例库、视频裁剪与本地报告；
- 两条线互补，不互相替代。

## 这次补齐后的重点功能

### 1. Analysis run 管理
- 支持多个 analysis run 历史保留
- 每个 run 记录 `running / done / failed`
- 每个 run 关联 source video
- 默认不清空历史结果

### 2. 数据源选择
- `GET /api/sources` 返回 `data/` 下可用视频源
- `POST /api/tasks/analyze-demo` 支持 `source_name`
- 当前仓库已内置两个 demo source：
  - `demo_highway.mp4`
  - `demo_highway_alt.mp4`

### 3. 案件人工复核工作流
- case 支持字段：
  - `corrected_plate_number`
  - `operator_note`
  - `review_status`
- case 状态流转：
  - `待复核`
  - `待举报`
  - `已举报`
- 报告/举报动作与 case 状态联动

### 4. 事件 / 案件筛选
- `/api/events`
- `/api/cases`

支持按以下字段过滤：
- `status`
- `plate`
- `run_id`
- `review_status`

### 5. Web / Android 协同
- Web：source selector、run panel、事件/案件筛选、case 编辑表单
- Android：run 摘要、案件状态筛选、case review 信息展示与更新

## 当前交付状态（2026-04-08）

- **Android 真机联调底座已完成**：支持通过 `emergencyLaneApiBaseUrl`、`emergencyLaneDebugLanBaseUrl` 和 App 内 debug 诊断卡切换 API 地址；
- **Android 结构已完成第一轮拆分**：`MainActivity` 仅保留入口，状态、API、模型、UI 组件拆分到独立目录；
- **Android 已增量并入端侧缓存层**：最近一次成功同步的 `overview / runs / cases / selected case` 与端侧草稿会缓存在手机本地，弱网彩排时仍可继续展示主链；
- **Android 已纳入端侧本地检测链**：`com.example.emergencylaneguard.*` 下包含 CameraX、JNI、YOLOv8+ncnn、HyperLPR3 与 Room 相关代码，并通过 App 内按钮跳转进入该链路；
- **Android 真机覆盖安装脚本已补齐**：可通过 `android/scripts/install-debug.sh` 指定 `ANDROID_SERIAL` 完成单设备安装、失败后按需 `--clean` 重装并自动拉起 App；
- **Web 大屏已完成第一轮重构**：当前已对齐 Android 协同端的浅紫主题，保留 source 选择、run 历史、案件筛选、证据链与举报闭环；
- **后端已新增 device 兼容接口**：支持导入端侧案件快照并输出 `/api/device/sync-snapshot`，用于说明本地链路与云侧后端的兼容路径。

### Android 当前源码分层

- `android/app/src/main/java/com/yrd/emergencylanemobile/model/`：DTO / UI state
- `android/app/src/main/java/com/yrd/emergencylanemobile/network/`：Retrofit API 与 client
- `android/app/src/main/java/com/yrd/emergencylanemobile/viewmodel/`：页面状态编排与联调逻辑
- `android/app/src/main/java/com/yrd/emergencylanemobile/ui/`：Compose 页面与组件
- `android/app/src/main/java/com/yrd/emergencylanemobile/ApiBaseUrlStore.kt`：运行期 API 地址覆盖存储
- `android/app/src/main/java/com/yrd/emergencylanemobile/DeviceAssistStore.kt`：真机缓存快照与端侧草稿存储
- `android/app/src/main/java/com/example/emergencylaneguard/`：Android 端侧本地检测链代码（CameraX / JNI / YOLOv8+ncnn / HyperLPR3 / Room）

## 启动后端

```bash
cd backend
cp .env.example .env
```

默认演示模式下，可保留：

```env
USE_MOCK_ZHIPU=true
```

启动：

```bash
cd backend
uv run uvicorn src.app.main:app --host 127.0.0.1 --port 8000 --reload
```

## 启动网页端

```bash
cd frontend
npm install
npm run dev
```

前端默认访问 `http://127.0.0.1:8000/api`。

## 项目级 Playwright MCP

当前仓库已支持项目级 Playwright MCP。

如果你用 Codex 在这个仓库里做页面调试或联调，直接从仓库根目录启动即可读取项目里的 `.codex/config.toml`：

```bash
cd /home/yrd/documents/git_clone_code/etc/emergency-lane-demo
codex
```

项目级配置默认会：

- 启用 `@playwright/mcp@latest`
- 打开 `vision,devtools` 能力
- 把调试输出写入 `.playwright-mcp/`
- 保存 trace，默认视口 `1440x960`

当前仓库已在全局 Codex 配置中标记为 trusted。详细说明见 `PLAYWRIGHT_MCP.md`。

## 启动 Android 端

```bash
cd android
./gradlew assembleDebug
```

说明：
- 默认优先读取 `emergencyLaneApiBaseUrl`（支持 `android/local.properties`、项目/用户 `gradle.properties`、或命令行 `-PemergencyLaneApiBaseUrl=...`），未设置时再回退到模拟器地址 `http://10.0.2.2:8000/api/`；
- 可选 `emergencyLaneDebugLanBaseUrl` 作为 debug 局域网默认值，用于真机联调构建；
- App 内置 debug 连接诊断卡，可查看当前 API 地址、最近一次 overview 拉取结果，并临时切换到局域网地址而无需改源码；
- App 会把最近一次成功同步的总览 / run / 案件 / 当前案件详情缓存到真机本地，并保留端侧草稿区，供本地检测链与协同链并行展示；
- App 首页新增“进入端侧本地检测模式”按钮，可跳转到 CameraX / JNI / YOLOv8+ncnn / HyperLPR3 / Room 演示链；
- 若要编译端侧本地检测模式，需要本机具备 Android SDK、NDK 与 CMake 3.22.1，并允许 Gradle 拉取 HyperLPR3 依赖；
- 真机联调建议后端使用 `uv run uvicorn src.app.main:app --host 0.0.0.0 --port 8000 --reload` 启动，并确保手机与开发机在同一网段；
- 本地构建时可创建 `android/local.properties` 指向 SDK，例如 `sdk.dir=/home/yrd/Android/Sdk`，也可在其中加入 `emergencyLaneApiBaseUrl=http://<你的局域网IP>:8000/api/`。

真机安装推荐：

```bash
cd android
ANDROID_SERIAL=<adb-device-id> ./scripts/install-debug.sh
```

- `ANDROID_SERIAL` / `ADB_SERIAL`、局域网 IP、无线调试地址都会随当前 Wi‑Fi / adb 会话变化，不要把某次现场值当成长期固定配置；
- 若厂商 ROM 拒绝覆盖安装，可在设备确认安装授权后改用 `./scripts/install-debug.sh --clean`；
- 默认安装路径不会在失败时自动卸载旧包，只有显式传入 `--clean` 才会执行“卸载后重装”；
- 脚本会优先使用 `ANDROID_SERIAL`，未指定时自动挑选一个在线设备，并在安装成功后拉起 App；
- 相比直接 `installDebug`，该脚本更适合当前仓库的真机联调：能固定单一设备并在必要时回退到“卸载后重装”。

## 关键接口

### 基础与总览
- `GET /api/health`
- `GET /api/overview`
- `GET /api/sources`

### run
- `GET /api/runs`
- `GET /api/runs/{run_id}`

### events
- `GET /api/events`
- `GET /api/events/{id}`
- `POST /api/events/{id}/report`

### cases
- `GET /api/cases`
- `GET /api/cases/{id}`
- `PATCH /api/cases/{id}`
- `POST /api/cases/{id}/report`

### tasks
- `POST /api/tasks/analyze-demo`

### device compatibility
- `POST /api/device/cases/import`
- `GET /api/device/sync-snapshot`
- `GET /api/device/cases/{id}`

## 验证

### 后端
```bash
cd backend
uv run python -m unittest discover -s tests -v
```

### 前端
```bash
cd frontend
npm run build
```

### Android
```bash
cd android
./gradlew --no-daemon assembleDebug
```

说明：
- 本轮验收以 **编译通过** 为主，不要求把 APK 安装到本地设备；
- 端侧本地检测模式是否存在，以源码、资源、JNI 构建链和 `assembleDebug` 通过作为当前阶段证据；
- 若后续需要真机彩排，再使用 `android/scripts/install-debug.sh` 进行安装验证。

### Android（真机局域网联调构建）
```bash
cd android
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew --no-daemon assembleDebug -PemergencyLaneApiBaseUrl=http://<你的局域网IP>:8000/api/
```

### Android（真机一键安装 + 启动）
```bash
ANDROID_SERIAL=<adb-device-id> android/scripts/install-debug.sh
```

说明：
- 当前环境下 Android 构建与 Dex 打包应优先使用 JDK 17；若系统默认是 JDK 21，可临时加上 `JAVA_HOME=/usr/lib/jvm/java-17-openjdk`；
- 安装脚本会先执行 `assembleDebug`，再尝试 `adb install -r -t`，必要时回退到 `adb push + pm install`；
- 当前 Android 16 / 厂商 ROM 环境下，`./gradlew installDebug` 仍可能因 ddmlib 追加 `--no-streaming` 而失败，因此优先使用该脚本；
- 若设备弹出安装/更新确认框，需先在手机端授权一次；否则会出现 `INSTALL_FAILED_ABORTED: User rejected permissions`；
- 不传 `ANDROID_SERIAL` / `ADB_SERIAL` 时，会自动选择第一台已授权 adb 设备；
- 文档中的 `http://<你的局域网IP>:8000/api/`、`<adb-device-id>` 都是占位符，使用时请替换成你当前网络/无线调试会话里的实时值。

## 文档

- `docs/architecture-dual-end.md`：双端项目架构与职责分工
- `docs/device-rehearsal.md`：真机安装、端侧缓存层与全链路彩排说明
- `docs/project-walkthrough.md`：项目运行流程与演示路径
- `docs/source-newnew-digest.md`：参考工程 `newnew` 文档提炼
- `docs/implementation-scope.md`：当前项目实现范围与模块拆解
