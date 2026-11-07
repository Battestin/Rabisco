# 🎨 Rabisco

[![Build APK](https://github.com/YOUR-USERNAME/rabisco/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR-USERNAME/rabisco/actions/workflows/build.yml)
![Platform](https://img.shields.io/badge/platform-Android%2010%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

**Rabisco** is a lightweight, stylus-friendly drawing app for Android tablets.
It aims to combine the **feature set of [Ibis Paint X](https://ibispaint.com/)** with the
**simple, intuitive design of [Samsung PenUp](https://www.penup.com/)**.

> ⚠️ **Early development (MVP).** Expect rough edges and missing features. Rabisco is an independent
> project and is not affiliated with Ibis Paint X or PenUp.

## ✨ Features (current MVP)

- ✏️ Pressure-sensitive drawing with stylus (finger works too)
- 🔍 Pinch to zoom, pan and rotate the canvas with two fingers
- 🎨 Color picker with a **configurable shape**: square (saturation/brightness + hue bar) or
  triangle (hue ring + inner triangle). Your choice is saved and switching keeps the current color
- 🗂️ Layers: add, delete, show/hide, per-layer opacity
- 🧽 Eraser, undo and redo
- 🖼️ Export to PNG (saved to `Pictures/Rabisco`)

## 📲 Install

1. Open the **[Releases](../../releases)** page (or the **Actions** tab → latest run → *Artifacts*).
2. Download `rabisco-debug.apk` and copy it to your tablet.
3. Open it and allow *"Install from unknown sources"* when asked.

Requires **Android 10 (API 29)** or newer.

## 🛠️ Build

### With GitHub Actions (no setup needed)

Every push to `main` builds a debug APK. Go to **Actions → Build APK → latest run → Artifacts**.
To publish a release, push a tag:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The APK is then attached to a GitHub Release automatically.

### Locally

Requirements: JDK 17, Android SDK (API 34) and Gradle 8.9 (or Android Studio).

```bash
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## 🗺️ Roadmap

- [x] **Phase 1 – MVP:** canvas, pressure brush, eraser, color picker, basic layers, undo/redo, PNG export
- [ ] **Phase 2:** gallery and auto-save, more brushes, stroke stabilizer, selection and transform tools,
      fill bucket, gradients, shapes, RGB/hex inputs, color history, layer reordering
- [ ] **Phase 3:** blend modes, clipping and layer masks, layer folders, rulers and symmetry,
      filters, process recording (timelapse)
- [ ] **Phase 4:** frame-by-frame animation, PSD export, custom project format
- [ ] **Phase 5:** community features (sharing, likes, following, downloadable brushes) and cloud sync

## 🧱 Project structure

```
app/src/main/java/com/rabisco/app/
├── MainActivity.kt      # UI layout and wiring
├── CanvasView.kt        # drawing engine, layers, gestures, undo/redo, export
└── ColorPickerView.kt   # square / triangle color picker
```

## 🤝 Contributing

Issues and pull requests are welcome. Ideas, bug reports and device-specific feedback
(especially from stylus users) are very helpful at this stage.

## 📄 License

Released under the [MIT License](LICENSE).
