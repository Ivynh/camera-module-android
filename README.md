# Camera ML Validator

An Android application that uses TensorFlow Lite models to validate captured images for blur detection and document classification in real-time.

## Features

- **Real-time Camera Preview**: Live camera feed using CameraX
- **Image Capture**: Take photos with the device camera
- **Gallery Upload**: Select and validate images from the device gallery
- **Dual ML Validation**:
  - **Blur Detection**: Determines if an image is blurry or sharp
  - **Document Classification**: Identifies if the image contains a document

## Technology Stack

- **Language**: Kotlin
- **ML Framework**: TensorFlow Lite
- **Camera**: CameraX
- **UI**: Android Views with ViewBinding
- **Architecture**: MVVM pattern with lifecycle-aware components

## Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 21+ (Android 5.0 Lollipop)
- Device with camera support
- TensorFlow Lite model files (see setup section)

## Setup

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/ivynh-camera-module-android.git
cd ivynh-camera-module-android
```

### 2. Add TensorFlow Lite Models

Place your trained TensorFlow Lite models in the `app/src/main/assets/` directory with these exact filenames:

- `final_blur_model_compatible.tflite` - For blur detection
- `final_document_model_compatible.tflite` - For document classification

**Important**: The model files must be compatible with TensorFlow Lite and include preprocessing layers (rescaling) as the app expects normalized inputs.

### 3. Update Dependencies

The project uses Gradle version catalogs. Dependencies are managed in `gradle/libs.versions.toml`. Key dependencies include:

- CameraX for camera functionality
- TensorFlow Lite for ML inference
- AndroidX libraries for modern Android development

### 4. Build and Run

```bash
./gradlew assembleDebug
```

Or open the project in Android Studio and run it directly.

## Project Structure

```
app/src/main/java/com/example/cameraxmlvalidator/
├── MainActivity.kt          # Main activity with camera and UI logic
├── ImageValidator.kt        # ML inference and image validation logic
└── AndroidManifest.xml     # App permissions and configuration

app/src/main/res/
├── layout/
│   └── activity_main.xml   # Main UI layout
├── values/
│   ├── strings.xml         # String resources
│   ├── colors.xml          # Color definitions
│   └── themes.xml          # App themes
└── drawable/               # App icons and drawables
```

## Key Components

### MainActivity.kt
- Handles camera permissions and initialization
- Manages UI state transitions
- Coordinates between camera capture and ML validation
- Provides user feedback through UI updates

### ImageValidator.kt
- Loads and initializes TensorFlow Lite models
- Processes images for ML inference
- Returns validation results with confidence scores
- Handles model input preprocessing automatically

### ValidationResult Data Class
```kotlin
data class ValidationResult(
    val isBlurry: Boolean,
    val isDocument: Boolean,
    val blurConfidence: Float,
    val documentConfidence: Float,
    val blurLabel: String,
    val documentLabel: String
)
```

## How It Works

1. **Camera Initialization**: App requests camera permissions and initializes CameraX
2. **Image Capture**: User can capture photos or select from gallery
3. **ML Processing**: 
   - Image is preprocessed and resized to model input requirements
   - Both blur and document detection models run inference
   - Results are processed and confidence scores calculated
4. **Validation Logic**:
   - Success: Image is sharp AND contains a document
   - Failure: Image is blurry OR doesn't contain a document
5. **User Feedback**: Specific error messages guide user to retake photo if needed

## Model Requirements

Your TensorFlow Lite models should:

- Accept image inputs (the app reads input dimensions dynamically)
- Include preprocessing layers (rescaling/normalization)
- Output single float values where:
  - **Blur Model**: Lower scores indicate blurry images
  - **Document Model**: Lower scores indicate document presence

## Customization

### Threshold Adjustment
Modify validation thresholds in `ImageValidator.kt`:

```kotlin
// Blur detection threshold (currently 0.7)
val isBlurry = blurScore < 0.7

// Document detection threshold (currently 0.5)
val isDocument = docScore < 0.5
```

### Model File Names
Update model filenames in `ImageValidator.kt`:

```kotlin
const val BLUR_MODEL = "your_blur_model.tflite"
const val DOC_MODEL = "your_document_model.tflite"
```

## Permissions

The app requires the following permissions:

- `android.permission.CAMERA` - For camera access
- `android.hardware.camera.any` - Camera hardware feature

## Troubleshooting

### Common Issues

1. **Model Loading Errors**
   - Ensure model files are in `assets/` folder with correct names
   - Check Logcat for specific error messages
   - Verify models are TensorFlow Lite compatible

2. **Camera Permission Issues**
   - Grant camera permission when prompted
   - Check device camera availability

3. **Performance Issues**
   - Models are optimized with XNNPACK
   - Consider model quantization for better performance

### Debug Logging

Enable detailed logging by checking `ImageValidator.kt` logs:

```bash
adb logcat -s ImageValidator
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Acknowledgments

- TensorFlow Lite team for mobile ML framework
- CameraX team for modern camera API
- Android Jetpack team for lifecycle-aware components

## Support

For questions or issues:
- Create an issue in this repository
- Check the troubleshooting section above
- Review TensorFlow Lite documentation for model-related issues
