# 導入案の検証結果

確認基準日: 2026-09-05。Windows 11・VS Code・Maven・jCardSimで学習する方針を採用し、バージョンとコードの不整合を修正しました。

## 提案から修正した点

| 提案 | 確認結果 | 対応 |
| --- | --- | --- |
| SDK 3.0.5にはJDK 11が高互換 | 旧SDKの公式確認環境はJDK 7／8。独自アプリ開発にはJDK 7と記載 | Oracle JDK 25＋現行Tools 26.0を採用。対象APIは3.0.5を維持 [1][2][3] |
| 旧SDKをZIP展開し `JC_HOME` を設定 | 旧SDKはMSIと `JC_CLASSIC_HOME` を使用 | 現行ToolsのZIPと `JC_HOME_TOOLS` を使用 [1][4] |
| `com.licel:jcardsim:3.0.5` | Maven Centralにそのバージョンはない。Licel側のGitHub Packagesは別の配布先 | Centralのfork `com.klinec:jcardsim:3.0.6.0` を固定。「Java Card仕様3.0.6」という意味ではない [5][6] |
| `source/target=11` をCAPへ変換 | PCのJDKとConverter入力形式を混同している | Appletは `--release 8 -g`、テストはrelease 11。CAP用はOracle APIで再コンパイル [2] |
| JUnit 5の依存関係だけ追加 | Surefire未指定ではMavenの既定プラグインに実行可否が左右される | Surefire 3.5.5と `failIfNoTests=true`。JUnit 5系の5.14.4を使用 [7] |
| 空のインストールデータからAIDを読む | 2引数 `installApplet()` は空の配列を渡す | デフォルトAIDを使う `register()` に修正 [8] |
| 4引数 `CommandAPDU` でPING応答を要求 | LeなしのCase 1になっている | 5引数版でLe=4を指定。応答の文字コードはUS-ASCIIに固定 [9] |
| `&& %JC_HOME%...` を実行 | `&&` はWindows PowerShell 5.1非対応。`%変数%` はcmdの書式 | process形式のタスク、`$env:`、引数配列、終了コード判定に変更 [10] |
| 出力先はルートの `com/example/javacard` | `-d` 未指定なら `-classdir` が出力ルート | `-d target/cap` を明示し、成果物を確認 [3] |
| JDK 11だけで言語サーバーも動く | 現行Universal版はJava 21以上。Windows x64版には実行用JRE同梱 | 言語サーバー用JDKとプロジェクトの形式を区別して説明 [11] |

JDK 11がMavenやjCardSimに使えない、という結論ではありません。
新規導入からCAP生成まで1つのOracle JDKで揃えるため、現行Toolsが推奨する25を基準にします。
旧SDK 3.0.5を必須とする既存製品では、その製品で確認されたJDK・SDK・Exportファイルを別に管理してください。

## 検証範囲

| 段階 | 確認すること | 方法 |
| --- | --- | --- |
| 依存解決・コンパイル | 固定した依存関係でビルドできる | Wrapperで `clean test` |
| Applet動作 | インストール、選択・再選択、PING、CLA／INS／P1／P2不正、Le不足、エラー後の正常応答 | 9件のJUnitテスト |
| Windowsコマンド実行 | WrapperとJUnitをWindows上で実行できる | ActionsのWindowsジョブ、Oracle JDK 25 |
| APIとCAP | 対象APIで再コンパイルし、CAP／EXP／JCAを生成・検証できる | 利用PCで `scripts/build-cap.ps1` を実行 |
| VS Codeの画面 | Testing表示、Appletのブレークポイントで停止する | Windows 11でDebug Test |
| 実機 | ロード、APDU、通信、メモリー、暗号処理 | 製品のカードとリーダーで確認 |

Actionsの成功はCAP変換、Windows 11のGUI、実機の成功を保証しません。
実行結果はPRとActionsのログに記録し、実施できていない項目は未検証として扱います。

## 一次資料

1. [Oracle旧SDK 3.0.5の前提条件・MSI・環境変数](https://docs.oracle.com/cd/E59935_01/guide/install_and_setup_the_development_kit.htm)
2. [Oracle現行Toolsのコンパイラー設定](https://docs.oracle.com/en/java/javacard/3.2/jctug/setting-java-compiler-options.html)
3. [Oracle Converterの引数、検証、出力先](https://docs.oracle.com/en/java/javacard/3.2/jctug/running-converter.html)
4. [Oracle ToolsとSimulatorの環境変数](https://docs.oracle.com/en/java/javacard/3.2/jctug/installed-files-directories.html)
5. [Maven Centralのcom.licel:jcardsim一覧](https://repo.maven.apache.org/maven2/com/licel/jcardsim/)
6. [jCardSim forkのREADMEと公開座標](https://github.com/ph4r05/jcardsim)
7. [Maven SurefireのJUnit Platform実行](https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html)
8. [jCardSimのSimulator実装](https://github.com/ph4r05/jcardsim/blob/master/src/main/java/com/licel/jcardsim/base/Simulator.java)
9. [Oracle Java SE CommandAPDU API](https://docs.oracle.com/en/java/javase/25/docs/api/java.smartcardio/javax/smartcardio/CommandAPDU.html)
10. [Microsoft PowerShellのパイプラインチェーン演算子](https://learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_pipeline_chain_operators)
11. [Java拡張のJDK要件と同梱JRE](https://github.com/redhat-developer/vscode-java#java-tooling-jdk)
