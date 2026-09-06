# JavaCardBasic

Windows 11・VS Code・Maven・jCardSimで、Java Card Appletを動かしながら学ぶプロジェクトです。
最初の課題は、APDUコマンド `00 10 00 00 04` に対して `PING` と成功ステータス `9000` を返すことです。

**まずSDKなしでJUnitテストとステップ実行を始め、必要になったらOracleのConverterでJava Card 3.0.5向けCAPを作成します。**

## 採用する構成

導入手順の見直し基準日: 2026-09-05。Mavenとライブラリのバージョンは再現性のため固定しています。

| 役割 | 採用するもの |
| --- | --- |
| 開発PC | Windows 11 x64 |
| PCでMaven・テスト・Converterを実行するJDK | **Oracle JDK 25 x64** |
| エディター | VS Code + Extension Pack for Java |
| ビルド | Apache Maven 3.9.16（Maven Wrapper同梱） |
| シミュレーター | `com.klinec:jcardsim:3.0.6.0`（Java Card 3.0.5 APIを扱うfork） |
| テスト | JUnit Jupiter 5.14.4 + Maven Surefire 3.5.5 |
| CAP変換ツール（追加導入） | **Oracle Java Card Development Kit Tools 26.0** |
| カード側の対象 | **Java Card Classic 3.0.5**（`-target 3.0.5`） |
| Appletの中間クラス形式 | Java 8形式（`--release 8 -g`、major version 52） |

JDKの25、Toolsの26.0、Java Cardの3.0.5、クラス形式の8は、それぞれ別の番号です。
Java SE 25の機能がカード上で使える、という意味ではありません。

提案されていたJDK 11＋旧SDK 3.0.5＋source/target 11は、公式の確認環境と一致しません。
旧SDKの資料はJDK 7／8を記載しています。現行ToolsはJDK 25を推奨し、3.0.5向けCAPも生成できます。
採用理由と提案から修正した箇所は[導入案の検証結果](docs/03-validation.md)を参照してください。

## 最初にテストを動かす

1. [Windowsの導入手順](docs/00-windows-setup.md)に従い、Oracle JDK 25とVS Codeを準備します。
2. このリポジトリを取得し、`pom.xml` があるフォルダーをVS Codeで開きます。
3. VS CodeのPowerShellターミナルで実行します。

```powershell
.\mvnw.cmd --version
.\mvnw.cmd clean test
```

初回はMavenと依存ライブラリをダウンロードします。手動導入済みのMavenでは `mvn clean test` でも実行できます。
テスト結果の目安は `Failures: 0, Errors: 0, Skipped: 0` と `BUILD SUCCESS` です。
Oracle Java Card SDK、カード、カードリーダーは、このテストには不要です。

## VS Codeでステップ実行

1. [SampleApplet.java](src/main/java/io/github/tubesound/javacardbasic/card/SampleApplet.java) の `process()` 内、CLA判定行にブレークポイントを置きます。
2. [SampleAppletTest.java](src/test/java/io/github/tubesound/javacardbasic/card/SampleAppletTest.java) の `pingReturnsFourBytesAndSuccess()` を開きます。
3. テストの上の **Debug Test**、またはTestingビューのデバッグボタンを選びます。
4. F10／F11で進め、`buffer` の先頭5バイトと `le` を確認します。

テストはjCardSimの `Simulator` にAPDUの生バイト列を渡します。VS CodeのJava言語サーバーが `java.smartcardio` モジュールをビルドパスへ追加できない既知の問題を避けるため、`CardSimulator`、`CommandAPDU`、`ResponseAPDU` は使用しません。

以前の版で `Cannot find the class file for javax.smartcardio.CommandAPDU` が表示された場合は、最新のmainを取得し、コマンドパレットから **Java: Clean Java Language Server Workspace** → **Reload and delete** を実行してください。

ここで停止するのはPC上のjCardSimが呼び出したJavaクラスです。実機やOracle Simulator内のCAPに接続するデバッグではありません。

## COD・IEF・WEFのサンプル

2つ目の課題として、論理ファイルとメモリ寿命を分離した
[FileSystemApplet](src/main/java/io/github/tubesound/javacardbasic/card/FileSystemApplet.java)を追加しています。
このプロジェクトでは **CODをtransientメモリ領域** と定義します。

- `CodMemory`: `CLEAR_ON_DESELECT`と`CLEAR_ON_RESET`の一時配列
- `WefFile`: 通常データを保持する永続オブジェクト
- `IefFile`: 外部読出しを許さない内部EF
- `KeyObject`: `OwnerPIN`を隠蔽する鍵オブジェクト

個別テストは次のコマンドで実行できます。

```powershell
.\\mvnw.cmd -Dtest=FileSystemAppletTest test
```

コマンド、クラス構成、選択解除とリセットによる状態変化は
[COD・IEF・WEF・鍵オブジェクト](docs/04-cod-ief-wef.md)で解説します。
JPKI固有のAID、証明書、鍵、APDUはまだ扱いません。

## CAPを生成する

[Oracle Toolsの追加導入](docs/02-build-cap.md)を済ませて `JC_HOME_TOOLS` を設定し、`Ctrl+Shift+B` を押します。
ターミナルからも同じ処理を実行できます。

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\build-cap.ps1
```

テスト、Oracle APIでの再コンパイル、Converterの変換・検証が順に実行されます。
出力は `target/cap/io/github/tubesound/javacardbasic/card/javacard/` 内の `card.cap`、`card.exp`、`card.jca` です。

`mvn package` が生成するJARはCAPではありません。実機へのロードは、カード製品、GlobalPlatform設定、発行者の鍵に合わせて別途設計します。

## 学習用の資料

| 資料 | 内容 |
| --- | --- |
| [Windowsの導入手順](docs/00-windows-setup.md) | JDK、Maven、VS Code、デバッグ、トラブル解決 |
| [APDUと実行環境](docs/01-apdu-and-runtime.md) | コマンド仕様、JCREの役割、基本設計で決めること |
| [CAP生成](docs/02-build-cap.md) | Toolsの配置、出力先、AID、変換の流れ |
| [導入案の検証結果](docs/03-validation.md) | 修正理由、公式資料、検証範囲 |
| [COD・IEF・WEF・鍵オブジェクト](docs/04-cod-ief-wef.md) | 論理EF、transientメモリ、鍵クラス、ライフサイクル |

GitHub ActionsではWindowsとLinux上のOracle JDK 25でシミュレーションテストを実行します。
WindowsのCIランナーはWindows Server系であり、Windows 11のVS Code画面操作を検証するものではありません。
CAP変換と実機での動作は別途確認します。

プロジェクトのコードは[MIT License](LICENSE)、同梱Maven WrapperはApache License 2.0です。[第三者ソフトウェア](THIRD_PARTY_NOTICES.md)も参照してください。

