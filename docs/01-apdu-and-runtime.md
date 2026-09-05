# APDUとJava Card実行環境を学ぶ

## この課題で作るもの

カード上で実行されるプログラムが **Applet** です。`SampleApplet` はPINGコマンドを受けると固定の4バイト `PING` を返します。受信データをそのまま返すEchoではありません。

Java Card VMがカード用バイトコードを実行し、JCRE（Java Card Runtime Environment）がAppletのインストール、選択、APDUの配送などを管理します。
カードOS全体には通信やメモリー管理、製品固有の実装も含まれます。
jCardSimはPCのJVM上でAPIの振る舞いをシミュレーションし、Appletの `.class` を直接実行するため、JUnitのデバッガーで処理を追跡できます。

## APDU仕様

この課題は基本論理チャネルの短いAPDUを扱います。

```text
Command:  00 10 00 00 04
Response: 50 49 4E 47 90 00
```

| 項目 | 値 | 意味 |
| --- | --- | --- |
| CLA | `00` | この課題で受け付けるクラス |
| INS | `10` | PING命令 |
| P1 / P2 | `00 / 00` | 今回はパラメーターなし |
| コマンドデータ | なし | LcもないCase 2のAPDU |
| Le | `04` | ホストが受け取れる応答データ長、4バイト |
| レスポンスデータ | `50 49 4E 47` | ASCIIの `PING` |
| SW1 / SW2 | `90 / 00` | 正常終了 |

テストでは `new byte[] {0x00, 0x10, 0x00, 0x00, 0x04}` を渡します。5番目のバイトがLeです。
Leが `00` の短いAPDUは256バイトを意味します。jCardSimの低レベルAPIを使うことで、APDUの実際のバイト列がコードから直接読めます。

## 実行順序

1. テストが `Simulator` を作成します。
2. `installApplet()` が `SampleApplet.install()` を呼びます。
3. Appletが `register()` で実行環境へ登録されます。
4. `selectApplet()` で選択し、SELECTを受けた `process()` が正常に戻ります。
5. `transmitCommand()` がPINGを送り、選択されたAppletの `process()` が呼ばれます。
6. Appletが4バイトを送信し、正常に戻ると実行環境が `9000` を付けます。

AppletはJava SEの `main()` から起動しません。この課題はインストールパラメーターを使わないため、空の配列を読まず、引数なしの `register()` を使います。
実機でもデフォルトApplet AIDを使う構成です。別のインスタンスAIDやパラメーターが必要なら、その形式を設計して実装・テストを追加します。

## 異常系

| 条件 | SW | 意味 |
| --- | --- | --- |
| 正常なPING | `9000` | データ4バイトあり |
| CLAが `00` 以外 | `6E00` | 未対応のCLA |
| INSが `10` 以外 | `6D00` | 未対応の命令 |
| P1またはP2が0以外 | `6A86` | パラメーター不正 |
| Leが4未満 | `6C04` | 応答を4バイト受け取れるように再送する |

エラー時はデータを返しません。CLA、INS、P1/P2、Leの順に判定します。
PINGは受信データのないCase 2なので、`setOutgoing()` で応答を開始します。
`setIncomingAndReceive()` はCase 3／4用です。PINGで呼ぶと、T=0ではLeをLcとして扱うなどの誤動作につながります。
Leなしや受信データ付きのPINGはこの命令の仕様外で、その応答は保証しません。
受信データを扱う命令、分割受信、拡張APDU、複数論理チャネルは次の課題です。
[Oracle APDU API](https://docs.oracle.com/en/java/javacard/3.2/jcapi/api_classic/javacard/framework/APDU.html)

## 基本設計で次に決めること

| 設計対象 | 決める内容 |
| --- | --- |
| 対象カード | Java Cardバージョン、API・暗号アルゴリズム、メモリー容量 |
| APDU仕様 | CLA、INS、P1/P2、Lc/Le、データ形式、SW、再送時の扱い |
| AID | パッケージ、Applet、インスタンスの識別子 |
| データ保存 | 永続データと一時データ、電源断時の整合性 |
| 認証 | PIN照合、試行回数、認証状態の解除タイミング |
| 配布・更新 | GlobalPlatform、ロード権限、鍵、更新とデータ移行 |

まずPINGを別の4文字に変更し、テストも変えてみてください。
次に命令を1つ追加し、テストを先に書いてからAppletを実装します。
カウンター、一時メモリー、PINの順に進めると、基本設計上の判断をコードで確かめられます。

## 参考資料

- [Oracle Java Card 3.0.5仕様・API](https://docs.oracle.com/javacard/3.0.5/index.html)
- [jCardSim forkの説明](https://github.com/ph4r05/jcardsim)
- [jCardSim Simulatorの実装](https://github.com/ph4r05/jcardsim/blob/master/src/main/java/com/licel/jcardsim/base/Simulator.java)
- [VS Code Javaのjava.smartcardio解決問題](https://github.com/redhat-developer/vscode-java/issues/2841)
