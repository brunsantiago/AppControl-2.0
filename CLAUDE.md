# AppControl - Android Application

## Project Overview
Android application for control system with face detection capabilities, Firebase integration, and TensorFlow Lite for ML features.

## Commands
```bash
# Build the project
build: ./gradlew build

# Clean build
clean: ./gradlew clean

# Run tests
test: ./gradlew test

# Run instrumented tests
android_test: ./gradlew connectedAndroidTest

# Build debug APK
debug: ./gradlew assembleDebug

# Build release APK
release: ./gradlew assembleRelease

# Install on device
install: ./gradlew installDebug

# Lint check
lint: ./gradlew lint
```

## Project Structure
```
app/
├── src/main/java/ar/com/appcontrol/
│   ├── AlertDialog/          # Custom alert dialogs
│   ├── FaceRecognition/      # Face detection and recognition
│   ├── MainActivityFragment/ # Main activity fragments
│   ├── POJO/                 # Plain Old Java Objects
│   └── Utils/                # Utility classes
├── src/main/res/             # Android resources
└── src/androidTest/          # Instrumented tests
```

## Technologies Used
- **Language**: Java
- **Platform**: Android (API 26-28)
- **Build System**: Gradle
- **Backend**: Firebase (Auth, Firestore, Storage)
- **ML/AI**: TensorFlow Lite, ML Kit Face Detection
- **UI**: Material Design Components, CardView, RecyclerView
- **Image Loading**: Glide, Picasso
- **Navigation**: Android Navigation Component
- **Time Sync**: TrueTime Android
- **Image Picker**: Matisse
- **HTTP**: Volley

## Code Conventions
- Follow Android development best practices
- Use camelCase for variables and methods
- Use PascalCase for class names
- Package structure follows feature-based organization
- Firebase integration for backend services
- Material Design guidelines for UI

## Development Notes 
- Target SDK: 28 (Android 9)
- Min SDK: 26 (Android 8)
- ProGuard disabled for debugging
- TensorFlow Lite files marked as no-compress
- Uses Java 8 language features
- Firebase services configured via google-services plugin

## Build Configuration
- Package Name: ar.com.appcontrol
- Compile SDK: 28
- Build Tools: Gradle 4.1.1
- Release builds signed with keystore (not committed)
- Version: 1.5.0 (versionCode 13)