# Oracle環境のセットアップ

## 1. 公式配布物を入手する

次の3点をOracle公式サイトから入手し、それぞれ別のディレクトリへ展開します。

1. [Oracle JDK 25 (64 bit)](https://www.oracle.com/java/technologies/downloads/#java25)
2. [Java Card Development Kit Tools 26.0](https://www.oracle.com/java/technologies/javacard-downloads.html)
3. 同じページにあるJava Card Development Kit Simulator 26.0のWindows版またはLinux版

Oracleの配布条件を画面で確認してダウンロードしてください。配布物はリポジトリへコピーしません。
OracleがSimulator 26.0の対象として挙げるOSはWindows 11、Ubuntu 24.04 LTS、Oracle Linux 9です。

## 2. 環境変数を設定する

PowerShellの例です。実際の展開先に置き換えます。

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
$env:JC_HOME_TOOLS = 'C:\dev\oracle-javacard\tools-26.0'
$env:JC_HOME_SIMULATOR = 'C:\dev\oracle-javacard\simulator-26.0'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Linuxの例です。

```bash
export JAVA_HOME="$HOME/opt/oracle-jdk-25"
export JC_HOME_TOOLS="$HOME/opt/oracle-javacard/tools-26.0"
export JC_HOME_SIMULATOR="$HOME/opt/oracle-javacard/simulator-26.0"
export PATH="$JAVA_HOME/bin:$PATH"
```

ビルドスクリプトは`java.vendor`が`Oracle Corporation`であり、Java仕様バージョンが25であることを検査します。

## 3. Simulatorを初期構成する

展開直後のSimulatorは、最初にSCP03鍵とGlobal PINを設定する必要があります。
この教材では公開された学習専用値を使います。

```powershell
.\scripts\provision-simulator.ps1
```

またはLinuxで次を実行します。

```bash
./scripts/provision-simulator.sh
```

スクリプトは変更前のSimulator実行ファイルを`.unprovisioned`という名前で保存し、二重実行を防ぎます。
`config/client.config.properties`のISD AID・鍵と、Simulatorへ埋め込む鍵は一致しています。
これらの固定値はローカル学習専用で、製品や共有環境では使用しません。

## 4. ビルドする

```powershell
.\scripts\build.ps1
```

またはLinuxで次を実行します。

```bash
./scripts/build.sh
```

ビルドでは次を実行します。

1. Oracle JDK 25の`javac`でアプレットをコンパイルする。
2. Oracle JCDK Tools 26.0の`converter`でCAP、EXP、JCAを生成・検証する。
3. Oracle JDK 25とSimulator同梱のAMService／Socket ProviderでPC側クライアントをコンパイルする。

生成物は`build/`へ置かれます。

## 5. Simulatorとテストを実行する

1つ目のターミナルでSimulatorを起動します。

```powershell
.\scripts\start-simulator.ps1
```

2つ目のターミナルでテストクライアントを実行します。

```powershell
.\scripts\run.ps1
```

Linuxではそれぞれ`./scripts/start-simulator.sh`と`./scripts/run.sh`です。
既定ではOracle Socket Providerを使い`localhost:9025`へ接続します。
WindowsにOracle Java Card PCSC Driverを設定した場合は、次の形式でも指定できます。

```powershell
.\scripts\run.ps1 'pcsc:Oracle Java Card PCSC Reader 0'
```

## 6. 公式の根拠

- [Simulatorの前提条件](https://docs.oracle.com/en/java/javacard/3.2/jcdksu/before-installing-java-card-development-simulator.html)
- [Simulatorの初期構成](https://docs.oracle.com/en/java/javacard/3.2/jcdksu/configuring-java-card-development-kit-simulator.html)
- [Simulatorのコマンドライン](https://docs.oracle.com/en/java/javacard/3.2/jcdksu/java-card-development-kit-simulator-command-line.html)
- [Converterの実行方法](https://docs.oracle.com/en/java/javacard/3.2/jctug/running-converter.html)
- [AMServiceクライアントのコンパイルと実行](https://docs.oracle.com/en/java/javacard/3.2/jcdksu/running-client-application-applet-management.html)
