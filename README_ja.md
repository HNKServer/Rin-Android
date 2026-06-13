# AndroidEw

軽量なAndroidサーバーアプリケーションで、モバイルクライアントインターフェースを備えています。

![AndroidEw](image.jpg)

## 概要

AndroidEwは、KotlinベースのAndroidアプリケーションで、軽量なサーバーソリューションと、それに対応するモバイルインターフェースを提供します。このアプリは、サーバーをフォアグラウンドサービスとして実行するため、Androidデバイスで継続的にバックグラウンドで実行する必要があるアプリケーションに最適です。

このプロジェクトは、**ew**のローカルバージョンを提供し、Androidデバイスで実行できます。**ew**は「Love Live! School idol festival 2 MIRACLE LIVE!」のための（ほとんど機能する）サーバーです。このアプリを使用すると、ゲームサーバーを自分のAndroidフォンやタブレットでホストでき、ゲーム環境を完全に制御できます。バックエンドサーバーロジックは**Rustで記述**されており、高いパフォーマンスとメモリの安全性を実現しています。

## インストール

AndroidEwをインストールするには、[リリースページ](https://git.ethanthesleepy.one/ethanaobrien/ew-android/releases)から最新のリリースAPKをダウンロードしてください。

### 手順：

1. **リリースページからAPKをダウンロード**します。
2. デバイスで**不明なソースからのインストールを有効にします**（設定→セキュリティまたは設定→アプリ→特別なアプリアクセス）。
3. ダウンロードした**APKを開き、「インストール」をタップ**します。
4. **最初の起動時にセットアップを完了**します。
5. **必要な権限を付与**します（通知、ストレージ、インターネット）。
6. メイン画面から**サーバーを開始**します。

> 注：開発ビルドには、エミュレーターまたはテストデバイスを設定する必要があります。プロダクションリリースは直接インストール用に構成されています。

## 機能

- **バックグラウンドサービスサーバー**: 永続的な通知付きのフォアグラウンドAndroidサービスとして実行されます。
- **モバイルクライアントインターフェース**: サーバー制御のためのクリーンなマテリアルデザインUI。
- **多言語サポート**: 英語と日本語をサポート（`values/strings.xml`および`values-ja/strings.xml`によるローカリゼーション）。
- **自動起動**: デバイス起動時にサーバーを自動的に起動するように構成できます。
- **イースターモード**: アプリ設定を通じて切り替え可能なオプションモード。
- **バージョン管理**: ビルトインのアップデートチェックとダウンロード機能。
- **安全なファイルインストール**: FileProviderを使用して安全なAPKインストールを行います。
- **進行状況通知**: キャンセル機能付きのリアルタイムダウンロードの進行状況更新。
- **エッジ・トゥ・エッジ表示**: ウィンドウインセット処理によるモダンなAndroid UI。

## アーキテクチャ

### アプリケーション構造

```
AndroidEw/
├── app/
│   ├── src/main/
│   │   ├── java/one/ethanthesleepy/androidew/
│   │   │   ├── MainActivity.kt          # メインUIとサーバー制御
│   │   │   ├── StartupActivity.kt       # 最初のセットアップウィザード
│   │   │   ├── BackgroundService.kt     # フォアグラウンドサーバーサービス
│   │   │   ├── AppBroadcastReceiver.kt  # サービス制御のためのブロードキャストレシーバー
│   │   │   ├── DownloadCancelReceiver.kt# ダウンロードキャンセル処理
│   │   │   └── Utilities.kt             # ユーティリティ関数
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml    # メイン画面レイアウト
│   │   │   │   ├── settings.xml         # 設定画面レイアウト
│   │   │   │   ├── startup_main.xml     # スタートアップレイアウト
│   │   │   │   ├── install_game.xml     # ゲームインストールウィザード
│   │   │   │   ├── permission_request.xml
│   │   │   │   └── setup_done.xml
│   │   │   ├── values/                  # 英語の文字列
│   │   │   └── values-ja/               # 日本語の文字列
│   │   ├── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── gradle/libs.versions.toml            # バージョンカタログ
├── LICENSE                              # GNU GPL v3
└── README.md
```

### 主要コンポーネント

#### BackgroundService

フォアグラウンドサービスとしてコアサーバー機能を実行します（ネイティブライブラリ`libew.so`を使用）。

- **目的**: 信頼性のために、サーバーをフォアグラウンドサービスとして実行し続ける。
- **特徴**:
    - 青いライトインジケーター付きのフォアグラウンド通知
    - 自動イースターモードサポート
    - ネイティブRust実装によるサーバーの開始/停止
    - `START_STICKY`によるサービス持続性

#### MainActivity

サーバー制御と設定管理のためのメインUI。

- **特徴**:
    - サーバーの開始/停止の切り替え
    - サーバーの状態の表示
    - バージョン情報の表示（アプリバージョンとサーバー内部バージョン）
    - 設定ページと設定
    - アップデートチェッカー
    - イースターモードの切り替え
    - 自動起動設定
    - Gitリポジトリへのリンク

#### StartupActivity

新規ユーザー向けの最初のセットアップウィザード。

- **セットアップフロー**:
    1. 権限リクエスト（Android 13+）
    2. マニュアルとオプションのダウンロード
    3. ゲームのインストール（該当する場合）
    4. 完了

#### Utilities

アプリケーション全体で共有されるユーティリティメソッド。

- **機能**:
    - リモートサーバーからのバージョン更新チェック
    - Kotlinx Serializationによる設定のシリアライズ
    - 進捗通知によるファイルダウンロード
    - FileProviderによるAPKインストール
    - キャッシュ管理
    - 通知ブロードキャスト

## 技術スタック

### コアテクノロジー

- **言語**: Kotlin (Androidアプリ), Rust (サーバーバックエンド)
- **対象プラットフォーム**: Android 16 (API 36)
- **最小要件**: Android 8.0+ (API 26)
- **ビルドシステム**: Gradle with Kotlin DSL
- **アーキテクチャ**: MVVM-lite with View Binding

### ネイティブ統合

- `System.loadLibrary("ew")`を介してネイティブライブラリを使用
- サーバーロジックのための外部Rust実装
- ABIサポート: `arm64-v8a`のみ
- **アップストリームソース**: サーバーライブラリ`libew.so`は、Rustベースのバックエンドリポジトリである[ew](https://git.ethanthesleepy.one/ethanaobrien/ew)からビルドされます。ソースは、次のコマンドでビルド中にクローンされます。
  ```bash
  git clone --recurse-submodules https://git.ethanthesleepy.one/ethanaobrien/ew.git
  ```

## ユーザーインターフェース

### メイン画面

プライマリインターフェースには、次の機能があります。

- **サーバー状態表示**: サーバーの状態を表示します（実行中/停止中/起動中/停止中）。
- **制御ボタン**: サーバーの開始/停止を切り替えます。
- **バージョン情報**: アプリバージョンとサーバー内部バージョンを表示します。
- **設定へのアクセス**: 設定ページを開いて構成します。
- **ダウンロードセクション**: 追加のダウンロードへのリンク。

### 設定ページ

構成可能なオプションが含まれています。

- **起動時の自動起動**: デバイス起動時にサーバーを自動的に起動するオプション。
- **イースターモード**: 特殊機能を切り替えます。
- **アップデートチェッカー**: アプリケーションのアップデートを手動で確認します。
- **Gitリポジトリ**: ソースコードを表示するためのリンク。

### セットアップウィザード

新規ユーザーのセットアップフロー：

1. 権限処理（Android 13+通知権限）。
2. ダウンロード手順。
3. ゲームのインストールオプション。
4. 完了画面。

## 権限

`AndroidManifest.xml`に必要な権限：

```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

## データストレージ

- **設定**: `filesDir/settings.json`に保存されます（JSON形式、Kotlinx Serializationを使用）。
- **一時ファイル**: `filesDir/temp/`ディレクトリに保存されます。
- **サーバーデータ**: `externalFilesDir/ew_data/`に保存されます。

## アップデートとリリース

### バージョンチェック

アプリは、次の場所に接続してアップデートを自動的にチェックします。
```
https://git.ethanthesleepy.one/ethanaobrien/ew-android/releases/download/latest/version.json
```

### アップデートフロー

1. 現在インストールされているバージョンを取得します。
2. リリースサーバーから最新バージョンを取得します。
3. バージョンを比較します。
4. アップデートがある場合はAPKをダウンロードします。
5. FileProviderを使用して安全にインストールします。

### リリースプロセス

ビルドでは、署名付きAPKが生成されます。
- キーストア：`lovelive.keystore`
- セキュリティのために環境変数を使用して署名します。

## ローカリゼーション

アプリは複数の言語をサポートしています。

- **英語**: 主言語（デフォルト）。
- **日本語**: `values-ja/strings.xml`

### サポートされている機能

- すべてのUI文字列
- 通知
- エラーメッセージ
- セットアップウィザードの内容

## セキュリティに関する考慮事項

### FileProvider構成

安全なファイル共有のために`FileProvider`を使用します。
- `authorities = "${applicationId}.provider"`
- `file_paths.xml`構成を尊重します。
- パス・トラバーサル脆弱性を防ぎます。

### ネイティブライブラリ

アプリは、サーバー機能のためにネイティブライブラリ (`libew.so`) をロードします。
- 別プロセスのスペースで実行されます。
- より高いパフォーマンスを提供します。
- クロスプラットフォーム互換を有効にします。
- メモリの安全性と高いパフォーマンスのために**Rust**で記述されています。
- [git.ethanthesleepy.one/ethanaobrien/ew](https://git.ethanthesleepy.one/ethanaobrien/ew)にあるアップストリームソースからビルドされます。

## 開発

### アプリのビルド

1. **前提条件**:
    - Android Studio Hedgehog以降
    - JDK 17以降
    - Android SDK 36以降
    - AArch64ターゲットを使用したRustツールチェーン
    - Android NDK r29以降

2. **ビルドコマンド**:
   ```bash
   ./gradlew assembleDebug
   ./gradlew assembleRelease
   ```

3. **開発のための設定**:
    - アップストリームの `ew` リポジトリをクローンします: `git clone --recurse-submodules git.ethanthesleepy.one/ethanaobrien/ew.git`
    - cargo-ndkをインストールします: `cargo install cargo-ndk`
    - AArch64ターゲットを追加します: `rustup target add aarch64-linux-android`

4. **ネイティブライブラリのビルド**:
   ```bash
   cd ew
   cargo ndk -t arm64-v8a -o ../app/src/main/jniLibs/ build --release --features library
   ```

5. **リリースビルドの署名**:
    - `app/build.gradle.kts`でキーストアを構成します。
    - 機密データに環境変数を使用します。
        - `RELEASE_KEYSTORE_PASSWORD`
        - `RELEASE_KEYSTORE_ALIAS`
        - `RELEASE_KEY_PASSWORD`

## ライセンス

このプロジェクトは、**GNU General Public License v3.0 (GPLv3)**のもとでライセンスされています。

完全なライセンステキストについては、[LICENSE](LICENSE)ファイルを参照してください。

### その意味

- ✓ ソフトウェアを自由に利用、変更、配布できます。
- ✓ 変更を同じライセンスの下で利用可能にする必要があります。
- ✓ 保証は提供されません。
- ✓ バイナリを配布する際には、ソースコードを提供する必要があります。

## 謝辞

- **作成者**: Ethan O'Brien [@ethanaobrien](https://git.ethanthesleepy.one/ethanaobrien)
- **リポジトリ**: [Git - ethanaobrien/ew-android](https://git.ethanthesleepy.one/ethanaobrien/ew-android)
