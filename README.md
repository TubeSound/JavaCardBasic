# JavaCardBasic

Java Cardを使ったシステムの基本設計に向けて、**コードを動かしながら、アプレットと実行環境の役割を学ぶ**リポジトリです。

第1回は、PC上のシミュレータにコマンドを送り、`Hello, Java Card!`を受け取ります。
カードやリーダーを用意する前に、APDU通信とアプレットの呼び出しを確かめます。

## 最初に読むコード

| ファイル | 役割 |
| --- | --- |
| [HelloApplet.java](src/main/java/io/github/tubesound/javacardbasic/card/HelloApplet.java) | カード側。受信コマンドの確認と応答 |
| [HelloDemo.java](src/main/java/io/github/tubesound/javacardbasic/host/HelloDemo.java) | PC側。シミュレータの準備、コマンド送信、結果表示 |
| [HelloAppletTest.java](src/test/java/io/github/tubesound/javacardbasic/HelloAppletTest.java) | 正常応答、エラー応答、長さの境界を確認 |
| [第1回の解説](docs/01-apdu-and-runtime.md) | OS・実行環境・アプレットの関係と、基本設計への反映 |

## 実行する

必要なものは**JDK 17**とGitです。Mavenは同梱のWrapperが取得するため、別途インストールする必要はありません。
初回はMavenと依存ライブラリをダウンロードするため、インターネット接続が必要です。

JDKは[Adoptium](https://adoptium.net/temurin/releases/?version=17)などから用意できます。
`java -version`と`javac -version`で17系が表示されることを確認してください。
`JAVA_HOME`を設定する場合は、JDKのインストール先を指定します（末尾の`bin`は含めません）。

### Windows / PowerShell

```powershell
git clone https://github.com/TubeSound/JavaCardBasic.git
cd JavaCardBasic
git switch learning/lesson-01-apdu
.\mvnw.cmd test
.\mvnw.cmd -q compile exec:java
```

### WSL / Linux / macOS

```bash
git clone https://github.com/TubeSound/JavaCardBasic.git
cd JavaCardBasic
git switch learning/lesson-01-apdu
./mvnw test
./mvnw -q compile exec:java
```

既にcloneしている場合は、そのフォルダで`git fetch origin`してから学習用ブランチに切り替えてください。

デモのHELLO部分では、次の内容が表示されます。

```text
[HELLO]
C: 80 10 00 00 11
R: 48 65 6C 6C 6F 2C 20 4A 61 76 61 20 43 61 72 64 21 90 00
SW: 9000
Text: Hello, Java Card!
```

`C`は送信したコマンド、`R`は返ってきた応答です。
最後の`90 00`は正常終了を表すステータスワード（SW）で、文字列には含まれません。
続いて、未対応命令の`6D00`と、指定した応答長が足りない場合の`6C11`も表示します。

## 何を使って動かしているか

- ビルド・実行：JDK 17、Maven 3.9.9（Maven Wrapper経由）
- シミュレータ：`com.klinec:jcardsim:3.0.6.0`（[ph4r05によるjCardSim配布版](https://github.com/ph4r05/jcardsim#readme)）
- テスト：JUnit Jupiter 5.10.3

jCardSimはPCのJVM上でアプレットの`.class`ファイルを動かします。
このリポジトリのMaven設定はシミュレータ用のビルドで、実カードに入れるCAPファイルは生成しません。
`maven.compiler.release=17`もPC用のクラス形式の指定であり、Java Cardのバージョンを表す値ではありません。
依存ライブラリの`3.0.6.0`は配布物のバージョンです。対象製品のJava Card仕様の版は、別に確認します。

**ここで作るのは学習用アプレットです。OS内部の実装は、まず実行環境との接点から理解していきます。**
実カードでのCAP変換・搭載、通信プロトコル、電源断、永続化、トランザクション、アプレット間の分離は、この第1回では検証していません。

## 次に行うこと

まず[第1回の解説](docs/01-apdu-and-runtime.md)を読み、`80 10 00 00 11`の意味と、誰が`process()`を呼ぶかをコードで確認します。
その後、HELLOの応答や命令番号を変更して、デモとテストに結果が表れることを確かめます。
次の題材は、コマンドのデータ受信と、RAM・永続データの扱いです。
