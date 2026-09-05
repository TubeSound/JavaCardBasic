# Oracle ConverterでJava Card 3.0.5向けCAPを作る

## 1. Tools 26.0を配置

[Oracle Java Card Downloads](https://www.oracle.com/java/technologies/javacard-downloads.html)から **Java Card Development Kit Tools 26.0** を取得します。
基準ファイル名は `java_card_devkit_tools-bin-v26.0-b_705-04-MAY-2026.zip` です。
Oracleのダウンロード画面の案内に従って取得し、ZIPを展開します。

この手順ではSimulatorやEclipse Plug-inは不要です。対象仕様が3.0.5でも、ツール本体を旧SDK 3.0.5にする必要はありません。
[現行Converterの対象バージョン](https://docs.oracle.com/en/java/javacard/3.2/jctug/using-converter-target-java-card-version.html)

環境変数 `JC_HOME_TOOLS` を、次の2ファイルがあるディレクトリーに設定します。
例えば展開内容を `C:\tools\javacard-tools-26.0` に置いた場合:

```text
C:\tools\javacard-tools-26.0\bin\converter.bat
C:\tools\javacard-tools-26.0\lib\api_classic-3.0.5.jar
```

| 環境変数 | 設定例 |
| --- | --- |
| `JAVA_HOME` | `C:\Program Files\Java\jdk-25` |
| `JC_HOME_TOOLS` | `C:\tools\javacard-tools-26.0` |

`bin` と `lib` が直下にある場所を指定してください。ユーザー環境変数でも利用できます。設定後はVS Codeを再起動します。
このスクリプトは `JC_HOME` や旧SDK用の `JC_CLASSIC_HOME` は使いません。

```powershell
Test-Path "$env:JC_HOME_TOOLS\bin\converter.bat"
Test-Path "$env:JC_HOME_TOOLS\lib\api_classic-3.0.5.jar"
& "$env:JAVA_HOME\bin\javac.exe" -version
& "$env:JC_HOME_TOOLS\bin\converter.bat" -version
```

ファイルがない場合はToolsの種類、バージョン、展開先の階層を確認します。OracleのJDK／Tools本体はリポジトリに含めていません。

## 2. 実行

`Ctrl+Shift+B` → **Java Card: Build & Convert CAP (3.0.5)** を実行します。
ターミナルからの実行方法:

```powershell
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File .\scripts\build-cap.ps1
```

タスクは `powershell.exe` と引数を個別に指定するため、VS Codeの既定シェルに依存しません。
パスはPowerShellの呼び出し演算子と引数配列で渡します。

## 3. スクリプトの処理

1. `JAVA_HOME`、JDK 25、`JC_HOME_TOOLS` と必要なファイルを確認します。
2. Wrapperで `clean test` を実行し、失敗したら停止します。
3. AppletをOracleの **3.0.5用API** だけをクラスパスにして `--release 8 -g` で再コンパイルします。
4. Converterの `-target 3.0.5 -out CAP EXP JCA` で変換します。
5. 終了コードと、CAP／EXP／JCAが作成されたことを確認します。

Maven用クラスは `target/classes`、CAP変換用クラスは `target/cap-classes` です。
`src/main/java` にはカード側コードだけを置き、テストやホスト側の試作は `src/test/java` に置きます。
複数パッケージのCAPへ拡張する場合はConverter設定も追加してください。

Java 8形式は中間ファイルの指定です。ラムダ、Stream、通常のJava SEライブラリがカードで使える保証にはなりません。
ConverterによるJava Cardのサブセット確認と参照検証が必要です。

Tools 26.0では `-target` に応じた標準APIのExportファイルが自動選択されるため、このサンプルは標準API用の `-exportpath` を指定しません。
独自の別パッケージを参照する場合は、そのEXPファイルのパスを追加します。
`-noverify` は指定せず、入力・出力検証を有効にしています。[OracleのConverterオプション](https://docs.oracle.com/en/java/javacard/3.2/jctug/running-converter.html)

## 4. 出力先と識別子

```text
target/cap/io/github/tubesound/javacardbasic/card/javacard/card.cap
target/cap/io/github/tubesound/javacardbasic/card/javacard/card.exp
target/cap/io/github/tubesound/javacardbasic/card/javacard/card.jca
```

| 設定 | 値 |
| --- | --- |
| パッケージ | `io.github.tubesound.javacardbasic.card` |
| Appletクラス | `io.github.tubesound.javacardbasic.card.SampleApplet` |
| パッケージAID | `F05455424501` |
| Applet AID | `F0545542450101` |
| パッケージバージョン | `1.0` |
| 対象API / CAP形式 | Java Card 3.0.5 / compact 2.2 |

AIDは学習用の独自値です。実製品では発行者の割り当てに合わせます。
Applet AIDを変える場合はCAPスクリプトと `SampleAppletTest` の両方を変更します。
このサンプルはデフォルトのApplet AIDで登録するため、別のインスタンスAIDを使う設計は追加実装が必要です。

## 5. CAPができた後

CAP生成は実機へのインストール完了を意味しません。
カードのJava Card／GlobalPlatformバージョン、API、ロード権限、セキュアチャネルと鍵を確認し、そのカード用の手順でロードします。
Oracle Simulatorへロードする場合も、別途Simulatorとインストール手順が必要です。

CIはjCardSimテストを行います。Oracle Tools本体によるCAP変換とWindows 11の画面操作は、利用PCでこの手順を実行して確認してください。

## 公式資料

- [Tools 26.0のユーザーガイド](https://docs.oracle.com/en/java/javacard/3.2/jctug/index.html)
- [コンパイラー設定（Java 25、release 8、デバッグ情報）](https://docs.oracle.com/en/java/javacard/3.2/jctug/setting-java-compiler-options.html)
- [対象バージョンとCAP形式](https://docs.oracle.com/en/java/javacard/3.2/jctug/using-converter-target-java-card-version.html)
