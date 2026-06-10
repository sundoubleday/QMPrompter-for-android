# 乔木提词器

自用提词器，支持 iOS 和 Android。

## 功能

- 文稿列表，新建、编辑、保存、删除、搜索。
- 本地 JSON 存储，首次启动内置一篇试用文稿。
- 前置摄像头实时预览背景。
- 全屏提词页，语音跟随自动高亮当前行。
- 手动匀速滚动，点击屏幕播放/暂停。
- 左侧上下滑动调速度，右侧上下滑动调进度。
- 字号、速度、文字颜色、背景透明度设置。
- **屏幕内录**：录制提词过程（含麦克风音频），保存为 MP4 到手机 Movies/QMPrompter 目录。
- AI 生成口播稿（DeepSeek API）。
- 语音输入文稿。
- 玻璃质感 UI。

## 下载安装

### Android

前往 [Releases](https://github.com/sundoubleday/QMPrompter-for-android/releases) 下载最新 APK，传输到手机安装即可。

> 首次安装需要在手机「设置 → 安全」中允许安装未知来源应用。首次打开 App 会请求摄像头和麦克风权限。支持 Android 10+。

### iOS

需要通过 Xcode 构建安装，见下方「iOS 工程配置」。

## Android 工程

- 项目目录：`android/`
- 语言：Kotlin + Jetpack Compose
- 最低版本：Android 10（API 29）
- 目标版本：Android 14（API 36）
- 构建：`cd android && ./gradlew assembleDebug`
- APK 输出：`android/app/build/outputs/apk/debug/app-debug.apk`

### 用 Android Studio 打开

1. 打开 Android Studio。
2. File → Open → 选择 `android` 目录。
3. 等待 Gradle sync 完成。
4. 连接手机，点击 Run（▶）。

### 屏幕录制

首次使用录制功能时，系统会弹出屏幕录制授权弹窗，点击「立即开始」即可。录制完成后视频自动保存到手机 **Movies/QMPrompter** 目录，可通过文件管理器查看。

## iOS 工程配置

- Xcode project: `QMPrompter.xcodeproj`
- Target: `QMPrompter`
- Bundle ID: `com.qiaomu.Prompter`
- iOS deployment target: `17.0`

### iOS 真机测试

1. 打开 `QMPrompter.xcodeproj`。
2. Xcode > Settings > Accounts 里确认 Personal Team 可用。
3. 连接 iPhone，选择 `QMPrompter` scheme 和真机。
4. 在 target 的 Signing & Capabilities 确认 Team 可用。
5. 点击 Run。

## 技术栈对照

| 模块 | iOS | Android |
|------|-----|---------|
| UI 框架 | SwiftUI | Jetpack Compose |
| 滚动引擎 | CADisplayLink | Coroutine + 16ms tick |
| 语音识别 | SFSpeechRecognizer | Android SpeechRecognizer |
| 摄像头 | AVCaptureSession | CameraX |
| 加密存储 | Keychain | EncryptedSharedPreferences |
| 本地存储 | FileManager JSON | SharedPreferences + Gson |
| 网络请求 | URLSession | OkHttp |
| 序列化 | Codable | kotlinx.serialization |
| 屏幕录制 | — | MediaProjection + MediaRecorder |

## 更新日志

### v1.1.0 (Android)

- minSdk 升级到 29（Android 10+）。
- 启动时自动请求摄像头和麦克风权限。
- 录制视频保存到公共目录 Movies/QMPrompter（可通过文件管理器查看）。
- 录制完成后 Toast 提示保存路径。

### v1.0.0 (Android)

- 首个 Android 版本，完整移植 iOS 功能。
- Jetpack Compose UI，摄像头背景，自动滚动，语音跟随。
- 屏幕内录，AI 生成口播稿。

## License

MIT
