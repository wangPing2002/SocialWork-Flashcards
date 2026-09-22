# Project structure

```text
SEU-SocialWork-Flashcards/
├─ .github/workflows/
│  ├─ android-apk.yml       # push/manual -> APK artifact
│  └─ release-apk.yml       # v* tag -> GitHub Release APK
├─ app/
│  ├─ build.gradle
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ assets/
│     │  ├─ cards.json
│     │  └─ details.json
│     ├─ java/com/seu/socialworkcards/
│     │  ├─ MainActivity.java
│     │  ├─ Card.java
│     │  ├─ CardRepository.java
│     │  ├─ ReviewEngine.java
│     │  └─ StudyStore.java
│     └─ res/values/
├─ build.gradle
├─ settings.gradle
└─ README.md
```
