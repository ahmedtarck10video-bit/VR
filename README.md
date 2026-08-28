# VR/MR Mixed Reality Studio

Enterprise-grade Augmented Reality (AR) and Mixed Reality (MR) application built with **Google ARCore 1.47+**, **Jetpack Compose**, **SceneView/Filament**, and **CameraX**.

## 🎯 Project Overview

This is a complete spatial computing platform featuring:
- **3D Object Viewer** - Pure 3D model inspection with gesture controls
- **AR Mode** - Real-time plane detection, surface tracking, 6DoF anchors
- **MR Mode** - Stereoscopic dual-camera passthrough with virtual object occlusion
- **Advanced Features** - Geospatial positioning, Cloud Anchors, Augmented Images, Face Tracking

## 🏗️ Architecture

```
CAMERA PERMISSION
    ↓
ARCORE AVAILABILITY CHECK
    ↓
SESSION CREATION & CONFIGURATION
    ↓
CAMERA PASSTHROUGH (real-time video)
    ↓
FRAME UPDATE LOOP
    ↓
MOTION TRACKING (6DoF)
    ↓
PLANE DETECTION & SURFACE ANALYSIS
    ↓
HIT-TEST & PLACEMENT VALIDATION
    ↓
DEPTH & OCCLUSION PROCESSING
    ↓
ANCHOR LIFECYCLE MANAGEMENT
    ↓
3D MODEL RENDERING (Filament)
    ↓
LIGHTING & MATERIAL PROCESSING
    ↓
MR COMPOSITION & FINAL DISPLAY
```

## 📋 Engineering Standards

### 1. Session Lifecycle
- Camera permission verified before ANY AR initialization
- ARCore availability checked with proper exception handling
- Session creation gracefully handles unavailable devices
- Camera background rendered immediately after successful resume
- No floating UI elements or black screens during valid camera state
- Proper pause/destroy cleanup without resource leaks

### 2. Model Placement
- Models ONLY appear after valid ARCore surface detection
- Hit-test results validated before anchor creation
- No arbitrary z-positioning (e.g., z = -1.2f) as primary placement
- Real ARCore HitResult required for production placement
- Fallback states clearly marked as estimates, not real surfaces

### 3. Hit-Testing Cascade
- Plane polygons checked first (most accurate)
- Depth-based hit-tests for close objects
- Feature points as fallback only
- Instant Placement used appropriately
- Augmented Images tracked with single managed anchors

### 4. Anchor Management
- One managed anchor per tracked object
- No duplicate anchor creation per frame
- Proper lifecycle: creation → tracking → detachment
- Safe cleanup on session pause/restart
- Stale anchor references prevented

### 5. Geospatial & Cloud Features
- GPS ≠ VPS validation
- Cloud Anchors report real status, never fake IDs
- Terrain/Rooftop anchors use correct ARCore APIs
- Altitude semantics clearly documented (WGS84 vs relative)
- Heading correctly applied to transform rotations

### 6. MR Rendering Pipeline
- Real camera feed always visible (no black backgrounds)
- Depth occlusion applied when supported
- Virtual objects correctly composited over real scene
- Camera passthrough synchronized with render thread
- Stereo disparity properly calculated

### 7. Error Handling
- All exceptions caught and logged (never silently ignored)
- UI state reflects real ARCore state, not optimistic assumptions
- Failed operations don't transition to "success" states
- Security exceptions properly handled for permissions
- Camera unavailability gracefully degrades

## 🛠️ Building & Testing

### Prerequisites
- Android 8.0+ (API 26+)
- ARCore-compatible device
- Google Play Services for AR installed

### Build
```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

### Testing Checklist
- [ ] AR mode starts only after camera permission granted
- [ ] Camera preview visible within 1s of entering AR mode
- [ ] Model appears ONLY after tapping detected surface
- [ ] No black background when camera is streaming
- [ ] Plane detection begins automatically
- [ ] Anchors persist across pause/resume
- [ ] Cloud Anchors return real IDs (never null/empty)
- [ ] Geospatial accuracy reported truthfully
- [ ] MR mode shows real environment + 3D object
- [ ] Model interaction doesn't restart AR camera

## 📚 Key Components

### ARCoreManager
Complete ARCore lifecycle and feature management:
- Session creation/configuration/resume/pause
- Plane detection and tracking
- Hit-test processing
- Anchor lifecycle
- Cloud Anchor hosting/resolving
- Geospatial Earth API
- Streetscape geometry
- Scene semantics
- Depth fusion
- Face mesh tracking
- Recording/playback

### MixedRealityViewModel
UI state management and AR integration:
- Mode transitions (3D → AR → MR)
- Model loading and management
- Surface anchor creation
- Sensor orientation tracking
- Recording state
- Notification system

### SceneviewARViewport
Hardware-accelerated AR rendering:
- ARSceneView for ARCore devices
- Camera background composition
- Model node lifecycle
- Pose transformation

### StereoDualCameraPreview
Stereoscopic MR rendering:
- Dual camera feeds (left/right)
- IPD-based disparity
- Synchronized overlays
- Real-time passthrough

## ⚠️ Known Limitations & Future Work

- Streetscape identifier uses reflection (API availability varies)
- Scene Semantics support conditional on device capabilities
- Augmented Faces requires face-mode session restart
- Cloud Anchors require network connectivity
- Geospatial accuracy depends on VPS availability

## 📄 License

Internal Development Project

## 🔗 References

- [Google ARCore Documentation](https://developers.google.com/ar)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [CameraX](https://developer.android.com/training/camerax)
- [SceneView](https://github.com/SceneView/sceneview-android)
- [Filament Engine](https://google.github.io/filament/)
