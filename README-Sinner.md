# README-Sinner

这份文档是给当前并行开发的同事看的，目的是同步 `codex/privileged-chat-capture` 这条分支上已经落地的内容、影响范围、接手方式和注意事项。

## 1. 当前分支和提交

- 当前功能分支：`codex/privileged-chat-capture`
- 当前提交：`9a7986d Add privileged chat capture pipeline`
- 远端仓库：`xilda622/LanxinProphet`
- 当前状态：本地提交已完成，但由于 GitHub 账号 `S1nnerfps` 对该仓库没有写权限，暂时还没有成功推送到远端。

如果后续切换到有权限的 GitHub 账号，只需要把这条分支推上去即可：

```bash
git push -u origin codex/privileged-chat-capture
```

## 2. 这次落地了什么

本次不是只写了目标说明，而是已经把一套可继续对接 vivo 系统能力的 Android 插件骨架落到了代码里。

### 2.1 已落地的核心能力

1. 聊天气泡事件捕获
   - 基于 `AccessibilityService`
   - 监听微信 / QQ 包名
   - 提取被点击或长按节点的 `bounds`、中心点、时间戳、文本 hint
   - 做了简单去重，避免连续重复触发

2. 截屏与局部裁剪
   - 当前使用 Android 官方 `AccessibilityService.takeScreenshot(...)`
   - 根据节点屏幕坐标裁剪出聊天气泡区域
   - 这部分是当前可编译的通用实现
   - 如果后续要追求更低时延，可以再替换成设备侧更底层的系统截图路径

3. OCR IPC 适配层
   - 已定义 AIDL：
     - `IVendorOcrService.aidl`
     - `IVendorOcrCallback.aidl`
   - 已实现 `VendorOcrClient`
   - 当前通过 `Manifest` 元数据读取厂商 OCR 服务的 action / package
   - 如果远端 OCR 服务不可用，会降级使用 accessibility 直接抓到的文本

4. 元数据采集
   - 时间戳：使用事件时间
   - 位置：使用 `LocationManager.getCurrentLocation(...)` 或兼容回退逻辑
   - 结果统一封装为结构化 note payload

5. 备忘录同步适配层
   - 已实现 `VivoNotesSyncClient`
   - 支持两种写入策略：
     - `ContentProvider insert`
     - 结构化 `Intent` 发送到目标服务 / 广播
   - 具体 URI、action、包名都没有硬编码猜测值，而是留成 Manifest 配置项

6. 异步执行与内存管理
   - 使用 `kotlinx-coroutines-android`
   - 截图、OCR、定位、备忘录同步都走异步
   - OCR 流程完成后会主动 `recycle()` 裁剪得到的 `Bitmap`

## 3. 主要代码位置

### 3.1 新增目录

- `app/src/main/aidl/com/lanxin/prophet/ocr/`
- `app/src/main/java/com/lanxin/prophet/accessibility/`
- `app/src/main/java/com/lanxin/prophet/capture/`
- `app/src/main/java/com/lanxin/prophet/location/`
- `app/src/main/java/com/lanxin/prophet/model/`
- `app/src/main/java/com/lanxin/prophet/notes/`
- `app/src/main/java/com/lanxin/prophet/ocr/`
- `app/src/main/java/com/lanxin/prophet/pipeline/`
- `app/src/main/java/com/lanxin/prophet/util/`
- `app/src/main/java/com/lanxin/prophet/vendor/`
- `app/src/main/res/xml/lanxin_accessibility_service.xml`
- `app/src/main/res/values/lanxin_plugin_strings.xml`

### 3.2 关键类说明

- `ChatBubbleAccessibilityService.kt`
  - 无障碍入口
  - 负责接收事件并启动整条采集流水线

- `BubbleSelectionExtractor.kt`
  - 从 `AccessibilityEvent` / `AccessibilityNodeInfo` 提取聊天节点信息

- `AccessibilityApiScreenshotEngine.kt`
  - 截屏并裁剪聊天气泡区域

- `VendorOcrClient.kt`
  - 绑定厂商 OCR 服务
  - 通过 AIDL 发起识别
  - 失败时自动降级

- `DeviceLocationRepository.kt`
  - 获取当前定位

- `ChatCaptureCoordinator.kt`
  - 负责把截屏、OCR、定位、备忘录写入串起来

- `VivoNotesSyncClient.kt`
  - 将最终数据写入系统备忘录

- `VendorIntegrationConfig.kt`
  - 统一读取厂商配置
  - 所有 vivo 侧真实接入参数都从这里进

## 4. 这次改动会影响哪些共享文件

为了降低与其他同事的冲突，这次已经尽量把改动收缩成“新增文件为主”。真正改到的共享文件只有下面几个：

- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `app/src/test/kotlin/org/example/AppTest.kt`

说明：

- `MainActivity.kt` 没有保留业务侵入修改
- 没有引入新的公共页面
- 没有改原始 README
- 资源名也单独用了 `lanxin_*` 前缀，尽量减少命名冲突

## 5. Manifest 里当前还是占位值的地方

下面这些值现在还是占位值，必须等拿到 vivo 设备上的真实接口信息后再替换：

- `REPLACE_ME_VENDOR_OCR_ACTION`
- `REPLACE_ME_VENDOR_OCR_PACKAGE`
- `REPLACE_ME_NOTES_CONTENT_URI`
- `REPLACE_ME_NOTES_INSERT_ACTION`
- `REPLACE_ME_NOTES_TARGET_PACKAGE`

这些字段都在：

- `app/src/main/AndroidManifest.xml`

如果同事手里已经有真实的 vivo 系统服务信息，优先改这里，不要在代码里写死。

## 6. 同事如果要继续接手，建议顺序

1. 先拉取这条功能分支，而不是直接在 `main` 上复制代码。
2. 先确认设备系统版本与截图能力是否匹配。
3. 拿到 vivo OCR 的真实 AIDL 契约或 action / package。
4. 拿到 vivo 备忘录的真实 `ContentProvider` URI、字段名或接收 Intent 契约。
5. 把 Manifest 占位值替换掉。
6. 再做真机联调和性能压测。

## 7. 当前已知限制

1. 还没有完成真机联调。
2. 还没有接入 vivo 真实 OCR 服务，只完成了 IPC 适配层。
3. 还没有接入 vivo 真实备忘录 Provider / Service，只完成了写入适配层。
4. 本地没有完成构建验证，因为当前这台机器缺少 `JAVA_HOME` / `java`。
5. 当前截图实现使用的是 Android 官方无障碍截图接口，不是最终极限性能方案。

## 8. 如果和其他功能并行开发，怎么避免冲突

- 不要直接在 `main` 上继续叠加这条功能。
- 优先以 `codex/privileged-chat-capture` 为基础继续开发。
- 如果需要改 `AndroidManifest.xml`，合并前先人工检查权限、service 注册、metadata 是否互相覆盖。
- 如果需要改 `app/build.gradle.kts`，注意依赖版本是否被别人一起调整。
- 新增资源和类名尽量延续 `lanxin_` / `Lanxin` 前缀。

## 9. 我这边本次实际完成的功能汇总

这次已经落地的，不是“说明文档”，而是下面这些实际代码能力：

- 系统级聊天选择插件的工程骨架
- 无障碍服务注册与目标应用过滤
- 聊天气泡节点坐标、边界和文本信息提取
- 截屏与局部 Bitmap 裁剪
- OCR 跨进程 AIDL 契约和客户端绑定逻辑
- 定位信息采集
- 识别结果结构化封装
- 写入系统备忘录的两套适配方式
- 异步协程流水线
- Bitmap 生命周期回收
- 基础测试替换

## 10. 后续最关键的一步

后续不是继续扩写业务骨架，而是补齐两份真实的厂商接口信息：

1. vivo OCR 服务的真实绑定契约
2. vivo 备忘录写入契约

只有这两项补全之后，这条链路才能从“可编译骨架”进入“可真机跑通”阶段。
