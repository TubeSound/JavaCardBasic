# JavaCardBasic

Java Card OSを、アプレットを作って動かしながら学ぶためのリポジトリです。
第1回では、PCからAPDUを送り、カード側の`HelloApplet`が応答するまでを確認します。

## Oracle製ツールチェーン

Javaの開発・実行に使う製品を、次のOracle公式配布物に統一しています。

| 用途 | 製品 | バージョン |
| --- | --- | --- |
| Javaコンパイル・PC側実行 | Oracle JDK | 25 (64 bit) |
| アプレットのCAP変換・検証 | Oracle Java Card Development Kit Tools | 26.0 |
| Java Card実行環境 | Oracle Java Card Development Kit Simulator | 26.0 |
| Simulatorへの配備 | Oracle AMService | Simulator 26.0同梱 |
| Simulatorとの通信 | Oracle Socket Provider | Simulator 26.0同梱 |

ビルド依存として外部パッケージ管理ツールや別実装のSimulatorを使いません。
GitHubはソース管理、WindowsまたはLinuxのシェルは起動操作にだけ使います。
Oracle公式ではJCDK 26.0をOracle JDK 25で検証しています。

## 最初に読む順番

1. [Oracle環境のセットアップ](docs/00-oracle-toolchain.md)
2. [第1回: Java Card OSとAPDU](docs/01-apdu-and-runtime.md)
3. [カード側コード](src/applet/java/io/github/tubesound/javacardbasic/card/HelloApplet.java)
4. [PC側コード](src/client/java/io/github/tubesound/javacardbasic/client/HelloClient.java)

## 実行の流れ

Windows PowerShellでは次の順に実行します。

```powershell
# 初回のみ。展開直後のSimulatorを学習用SCP03鍵で構成する
.\scripts\provision-simulator.ps1

# アプレットをコンパイルしてCAPへ変換し、PC側クライアントもコンパイルする
.\scripts\build.ps1

# ターミナル1
.\scripts\start-simulator.ps1

# ターミナル2
.\scripts\run.ps1
```

Ubuntu 24.04またはOracle Linux 9では、対応する`.sh`を使います。

```bash
./scripts/provision-simulator.sh
./scripts/build.sh
# ターミナル1
./scripts/start-simulator.sh
# ターミナル2
./scripts/run.sh
```

`run`はOracle AMServiceでCAPをロード・インストールし、正常系と異常系のAPDUを検証してからアンインストールします。
成功時は最後に`All APDU checks passed.`と表示されます。

## AID

| 対象 | AID |
| --- | --- |
| CAPパッケージ | `F0 54 55 42 45 01` |
| アプレットクラス／インスタンス | `F0 54 55 42 45 01 01` |

これらは学習用の値です。製品用AIDを決定したものではありません。

## 公式資料

- [Oracle Java Card Downloads](https://www.oracle.com/java/technologies/javacard-downloads.html)
- [Oracle JDK Downloads](https://www.oracle.com/java/technologies/downloads/)
- [Java Card Development Kit Simulator User Guide 26.0](https://docs.oracle.com/en/java/javacard/3.2/jcdksu/index.html)
- [Java Card Development Kit Tools User Guide 26.0](https://docs.oracle.com/en/java/javacard/3.2/jctug/index.html)
- [Java Card API 3.2](https://docs.oracle.com/en/java/javacard/3.2/jcapi/api_classic/index.html)
