# AGENTS.md

仕様は [docs/spec.md](docs/spec.md)、設計は [docs/architecture.md](docs/architecture.md) が正本です。
作業の前に、関係する箇所を読んでください。

- 未確定事項を「一般的にはこうだから」という理由で埋めない。実際の UseCase と最小の選択肢を示して相談する
- 案を推すときは、その案がどの条件や場面のために要るのかと、その条件や場面がこのアプリにあるかを書く。なければ、ないと書く。前例や原則を当てはめるときは、前例でその形が要った理由が、今回にも当てはまるかを確かめる
- 仕様や設計に関わる変更をした PR では、`docs/` の該当ファイルも同じ PR で更新する

## 確認コマンド

```sh
./gradlew :shared:domain:jvmTest :shared:domain:iosSimulatorArm64Test
./gradlew :shared:usecase:jvmTest :shared:usecase:iosSimulatorArm64Test
./gradlew :shared:data:iosSimulatorArm64Test
./gradlew :shared:ui:iosSimulatorArm64Test
./gradlew :shared:domain:compileKotlinIosArm64 :shared:usecase:compileKotlinIosArm64 :shared:data:compileKotlinIosArm64 :shared:ui:compileKotlinIosArm64 :shared:app:compileKotlinIosArm64
./gradlew :androidApp:assembleDebug
```

iOS アプリのビルドは時間がかかるので、PR の前と、ビルドの設定（Gradle・Xcode のプロジェクト・モジュールの構成）を変えたときに流す。

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' -derivedDataPath build/ios-derived CODE_SIGNING_ALLOWED=NO build
```

Android の端末テストはエミュレータか実機が要るので、PR の前と、`:shared:data` のトランザクションやドライバに関わる変更をしたときに流す。

```sh
./gradlew :shared:data:connectedAndroidDeviceTest
```
