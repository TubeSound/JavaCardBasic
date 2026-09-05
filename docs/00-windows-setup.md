# Windows 11で導入する

## 1. Oracle JDK 25 x64

[Oracleのダウンロード](https://www.oracle.com/java/technologies/downloads/#java25)で、JDK 25のWindows x64版を取得してインストールします。
JREではなく `javac.exe` を含むJDKを選びます。

Windowsの「環境変数」で次を設定します。パスは実際のインストール先に合わせてください。

| 変数 | 設定例 |
| --- | --- |
| `JAVA_HOME` | `C:\Program Files\Java\jdk-25` |
| `Path` に追加 | `%JAVA_HOME%\bin` |

`JAVA_HOME` に `bin` は含めず、値の前後に引用符を入れません。
設定後はVS Codeとターミナルを開き直し、PowerShellで確認します。

```powershell
$env:JAVA_HOME
Test-Path "$env:JAVA_HOME\bin\javac.exe"
& "$env:JAVA_HOME\bin\java.exe" -version
& "$env:JAVA_HOME\bin\javac.exe" -version
where.exe java
```

`Test-Path` が `True`、`javac` が25系であることを確認します。
古いJavaが先に見つかる場合はPath内の順序を確認します。

**JDK 11をすでに使っている場合:** MavenとjCardSimの学習部分にJDK 11を使うこと自体は可能ですが、JDK 11と旧SDK 3.0.5の組み合わせを公式推奨とは扱いません。
この手順の基準はOracle JDK 25で、CAPスクリプトもJDK 25を確認します。[採用理由](03-validation.md)

## 2. VS Codeと拡張機能

1. [Visual Studio Code](https://code.visualstudio.com/) のWindows版をインストールします。
2. **Extension Pack for Java**（ID: `vscjava.vscode-java-pack`）をインストールします。
3. Javaファイルを開き、Javaプロジェクトの読み込み完了を待ちます。

このパックにはMicrosoftのテスト／デバッグ拡張と、Red HatのJava言語サポートなどが含まれます。
現行のWindows x64向け言語サポートには実行用JREが同梱されています。
Universal版などで自分で指定する場合は言語サーバーにJava 21以上が必要です。JDK 11を `java.jdt.ls.java.home` に指定しないでください。
[Java拡張の公式説明](https://github.com/redhat-developer/vscode-java#java-tooling-jdk)

`Ctrl+Shift+P` → **Java: Configure Java Runtime** でOracle JDK 25が利用可能か確認します。
必要ならユーザー設定に次を指定します。パスは自分のPCのものに変更します。

```json
{
    "java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-25"
}
```

これは言語サーバーの実行設定です。Appletのクラス形式は `pom.xml` の `release=8` で指定します。
Java互換レベルとして8が表示されても、JDK 8の新規インストールが必要という意味ではありません。

## 3. リポジトリを取得

Gitを使う場合は[Git for Windows](https://git-scm.com/install/windows)を用意し、PowerShellで実行します。

```powershell
git clone https://github.com/TubeSound/JavaCardBasic.git
Set-Location JavaCardBasic
code .
```

Gitを使わない場合はGitHubの **Code → Download ZIP** から取得して展開し、VS Codeで `pom.xml` のあるフォルダーを開きます。

## 4. Maven Wrapperで確認

```powershell
.\mvnw.cmd --version
.\mvnw.cmd clean test
```

WrapperがApache Maven 3.9.16を取得するため、Mavenの手動導入やPathへの追加は不要です。
初回はMaven Centralへの接続が必要です。バージョン表示でMaven 3.9.16、Java 25、vendorがOracle Corporationとなることを確認します。

期待するテスト集計は次のとおりです。テスト数が0の場合は導入完了と判断しません。

```text
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Mavenを手動導入する場合

[Apache Mavenの公式配布](https://maven.apache.org/download.cgi)から3.9.16のBinary ZIPを取得します。
例えば `C:\tools\apache-maven-3.9.16` に展開し、その直下に `bin\mvn.cmd` があることを確認します。
Pathに `C:\tools\apache-maven-3.9.16\bin` を追加し、新しいターミナルで実行します。

```powershell
mvn --version
mvn clean test
```

同梱タスクとCAPスクリプトは、バージョンを揃えるためWrapperを使います。[Mavenの公式導入手順](https://maven.apache.org/install.html)

## 5. テストとデバッグ

1. `SampleAppletTest` の `pingReturnsFourBytesAndSuccess()` を開きます。
2. `SampleApplet.process()` のCLA判定行にF9でブレークポイントを置きます。
3. **Debug Test** を実行します。
4. `buffer[0..4]` が `00 10 00 00 04`、`le` が4となることを確認します。
5. F10で進め、送信直前の `buffer[0..3]` が `50 49 4E 47` になることを確認します。

Mavenから特定のテストだけを実行する場合:

```powershell
.\mvnw.cmd '-Dtest=SampleAppletTest#pingReturnsFourBytesAndSuccess' test
```

JUnitテストには `main()` や独自の `launch.json` は不要です。
CAPが必要になったら[追加導入](02-build-cap.md)へ進みます。

## よくある問題

| 症状 | 確認・対処 |
| --- | --- |
| 環境変数を変更しても古いJavaになる | VS Codeを開き直す。`where.exe java` とWrapperの `--version` を確認する |
| `javac.exe` が見つからない | JDKのルートを `JAVA_HOME` に設定したか確認する |
| 依存関係を取得できない | Centralへの接続と社内プロキシを確認。社内設定はユーザーの `.m2/settings.xml` に置く |
| `com.licel:jcardsim:3.0.5` が見つからない | POMは `com.klinec:jcardsim:3.0.6.0` を使う。Javaのimportは `com.licel...` のままでよい |
| Testingにテストが出ない | `pom.xml` のフォルダーを開き、JavaのStandard Modeで読み込む。必要なら `Java: Clean Java Language Server Workspace` を実行 |
| テストは成功するがCAPタスクで止まる | Toolsと `JC_HOME_TOOLS` を[設定する](02-build-cap.md) |
| PowerShellスクリプトの実行が拒否される | 組織の実行ポリシーを確認。ファイルのブロックや署名の問題は管理方針に従って解消する |
| ブレークポイントが無効になる | Javaプロジェクトの読み込み完了を待ち、デバッグを再実行する |
