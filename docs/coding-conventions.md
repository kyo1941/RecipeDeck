# コーディング規約

コードを書くときとレビューするときに守ることを、短くまとめる。
背景と経緯は [architecture.md](architecture.md) を正本とし、各節・各項から対応する箇所を指す。
既存コードをまねれば同じ形になるもの（UseCase の受け取り方、`ScreenHolder` と `Screen` の分け方、Fake の作り方など）は、ここには書かない。

## 命名

- コードの識別子は実装の仕組みではなくドメインの語に寄せ、[spec.md](spec.md#用語) のコード上の名前（`Recipe`、`RecipeIngredient`、`ShoppingSession` など）をそのまま使う
- UseCase は、表示し続ける読み出しを `Observe〜`、1回だけの読み出しを `Get〜` / `Find〜` / `Search〜` のように用途に合う動詞にする。`Show〜` のような画面の振る舞いを表す名前にしない（[UseCase の方針](architecture.md#usecase-の方針)）
- UseCase の結果の型は、起きた状況を表す名前にする（`NameRequired`、`RecipeNoLongerExists`）。画面への指示（`ShowNameError`）にしない。個別の理由を設けない失敗は `Unexpected` にする（[UseCase の方針](architecture.md#usecase-の方針)）
- 一覧や表示用の UI モデルの型名は、見た目の構造（`Row`、`Card` など）ではなく概念に寄せる（`RecipeListItem`）（[UI の方針](architecture.md#ui-の方針)）

## UI 層

設計の背景は [UI の方針](architecture.md#ui-の方針) を参照する。
迷う場面では **UDF**（状態は ViewModel → Composable、操作は Composable → ViewModel）と **SSoT**（同じ事実と判定を1か所だけで持つ）を基準に決める。

### UiState

- 画面に複数の段階（読み込み中、表示中、失敗、`Closed` など）があるときは、画面ごとに独立した `sealed interface` を作る。共通のベース型を作って複数画面の UiState で継承せず、共有したい状態はプロパティとして持つ
- 画面を抜けることは、UiState の状態（`Closed` など）で表す。ViewModel から画面への1回限りのイベントは、`Channel` や `SharedFlow` ではなく UiState の状態として渡す
- 基本は、ViewModel が条件を判定して UiState のプライマリコンストラクタを直接組み立て・更新してよい
- 入力欄や操作が多く、同じ判定や制約を複数箇所で書き直すことでズレが起きやすい画面（Recipe の作成・編集など）では、必要な分だけ UiState の data class 側に寄せる
  - 2つ以上の値の組み合わせなど、元の値の変化に連動して結果が変わる判定（変更有無、複数欄のエラー表示条件など）は、元の値だけを持って導出プロパティ（getter）にする。導出の元にしかならない値は `private` にする。ただし、1つのプロパティとの単純な比較（`saveStatus == SaveStatus.Saving` など）に別名だけを挟まない
  - 一覧のソートや集計など UseCase 結果の整形は getter に置かず、ViewModel の変換で済ませて値として持つ
  - 制約や他フィールドとの連動がある状態遷移（保存中は入力を変えない、変更があれば確認ダイアログを出す、など）が複数箇所にまたがるなら、data class の関数にまとめる

### ViewModel

- ViewModel は、UseCase を呼ぶタイミング、UseCase の結果や画面の段階（読み込み中か編集中か等）による振り分け、ログ出力などの副作用を受け持つ
- UiState の data class が状態遷移の関数を持つ操作は、必ずその関数を通して更新し、ViewModel で `copy` したり同じ規則を書き直したりしない。制約も連動もない単純な反映（検索結果の反映、帯を閉じるなど）は `copy` で変えてよい
- 導出プロパティが判定している条件を、ViewModel や Composable で判定し直さない
- 操作をきっかけに1回実行する処理（保存、読み込み、終了時の後始末など）は、ViewModel の関数から直接起こす。`StateFlow` の監視で起こさない（`StateFlow` は同じ値や連続した更新をまとめるため、実行のきっかけが落ちる）
- `StateFlow` の監視で動かすのは、最新状態に追従させる処理（編集中データの下書き保存、入力に応じた候補検索など）だけにする
- UseCase の結果で画面状態全体を組み立てる変換関数（`toUiState()` など）は、ViewModel の他状態を読まず、受け取った値だけから UiState を作る。ログ出力などの副作用はその関数の外で行う
- UseCase から `Unexpected` を受けたら、その原因（`cause`）を ViewModel で1回だけログに残す。`Outcome.Failure` や `Unexpected` として呼び出し元へ返す側（`:shared:data` の Repository・`TransactionRunner` や UseCase）ではログを残さない

### Composable

- データや業務規則に関わる状態（破棄確認ダイアログの表示など）は UiState に置く。フォーカスやメニューの開閉のように、データや業務規則に関わらない UI 表示だけの局所状態は Composable の `remember` に置く（Preview したい状態は子 Composable の引数に引き上げる）
- Composable は UiState の描画とユーザー操作の通知に専念し、入力制限や状態遷移の規則は ViewModel（と UiState）に置く
- 各画面はナビゲーションの仕組み（`RecipeDeckNavigation` など）を直接受け取らず、`onRecipeClick` のようなコールバック関数を受け取る
- 画面遷移で渡すのは対象の ID（と必要なら開き方を示す最小のフラグ）だけにし、エンティティの中身は渡さない。中身は遷移先の画面が自分で読む（渡した中身が古くなって食い違うのを防ぐため）
- 画面に出す文言は `strings.xml` に置き、Composable に直書きしない（`@Preview` のダミーデータを除く）。文言にはコードの用語ではなく利用者の語（「レシピ」など）を使う。色と文字の大きさは直書きせず、テーマの値を使う

## 失敗と型

- ドメインの型や UseCase の境界では、想定内の不正な入力・状態を例外ではなく、生成関数（`of` / `ofOrNull` など）の `null` か、結果の `sealed interface` で返す（[ドメイン層の方針](architecture.md#ドメイン層の方針)）
- 有無だけを表すものは nullable、それ以上の意味を持つ状態は `enum` か `sealed interface` にする（ケースごとに持つ値が異なる場合は `sealed interface` にする）（[ドメイン層の方針](architecture.md#ドメイン層の方針)）
- 失敗の型を変換する `when`（および `sealed` / `enum` の分岐）では、`else` を使わない。種類が増えたときにコンパイルエラーで気づくため（[UseCase の方針](architecture.md#usecase-の方針)）
- コルーチンの `CancellationException` は、どの層でも失敗型に変換したり握りつぶしたりせず、そのまま再送出する（[UseCase の方針](architecture.md#usecase-の方針)・[データ層の方針](architecture.md#データ層の方針)）
