# Android-PDF-Reader

A high-performance, "smart" PDF reader library for Android built with Kotlin, powered by the native `android.graphics.pdf.PdfRenderer` API.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🚀 **Lazy Rendering** | Only visible pages are decoded into memory using `RecyclerView` + `LinearLayoutManager` |
| ♻️ **Bitmap Recycling** | Off-screen page bitmaps are released immediately via `onViewRecycled` |
| 🌙 **Night Mode** | One-line toggle using a `ColorMatrix` luminance-inversion filter |
| 🔍 **Pinch-to-Zoom** | Smooth 1×–4× zoom powered by `ScaleGestureDetector` |
| 📄 **Drop-in Widget** | Single `<SmartPdfView>` XML tag — no boilerplate |
| 🔒 **Memory Safe** | `PdfRenderer` resources are released in `onDetachedFromWindow` |

---

## 📦 Project Structure

```
Android-PDF-Reader/
├── smartpdf/               ← Reusable library module
│   └── src/main/kotlin/com/example/smartpdf/
│       ├── PdfPage.kt          # Data class: page model
│       ├── PdfPageAdapter.kt   # RecyclerView adapter (lazy render + recycle)
│       └── SmartPdfView.kt     # Drop-in FrameLayout widget
└── app/                    ← Sample application
    └── src/main/kotlin/com/example/smartpdfreader/
        └── MainActivity.kt
```

---

## 🔧 Requirements

- **Min SDK**: 21 (Android 5.0 Lollipop)
- **Compile SDK**: 35
- **Language**: Kotlin

---

## 🚀 Integrating the Library

### 1. Add the module dependency

In your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":smartpdf"))
}
```

### 2. Add to your layout XML

```xml
<com.example.smartpdf.SmartPdfView
    android:id="@+id/pdfView"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:nightMode="false" />
```

### 3. Load a PDF in Kotlin

```kotlin
// From a content URI (e.g., a file picker result)
val uri: Uri = ...
binding.pdfView.load(uri)

// From a File object
val file: File = File(filesDir, "document.pdf")
binding.pdfView.load(file)
```

### 4. Toggle Night Mode

```kotlin
// Toggle and get new state
val isNight: Boolean = binding.pdfView.toggleNightMode()

// Or set explicitly
binding.pdfView.setNightMode(true)
```

### 5. Zoom & Navigation

```kotlin
// Reset zoom to 100%
binding.pdfView.resetZoom()

// Scroll to a specific page (zero-based index)
binding.pdfView.scrollToPage(5)
```

---

## 📸 Screenshots

| Day Mode | Night Mode |
|---|---|
| White background, sharp text | Inverted colors for low-light |

---

## 🏗️ Architecture

### `PdfPage.kt`
A minimal data class holding a zero-based page index. Passed to `RecyclerView` as a lightweight list item.

### `PdfPageAdapter.kt`
The heart of the library. On `onBindViewHolder`:
1. Opens the requested page from `PdfRenderer`
2. Allocates a `Bitmap` sized to the display width (maintaining aspect ratio)
3. Renders the page with `RENDER_MODE_FOR_DISPLAY`
4. Applies the night-mode `ColorMatrix` if enabled
5. Sets the bitmap on the `ImageView`

On `onViewRecycled`: the `Bitmap` is recycled and the `ImageView` is cleared.

### `SmartPdfView.kt`
A `FrameLayout` that:
- Hosts a `RecyclerView` for scrollable page display
- Intercepts touch events and forwards them to `ScaleGestureDetector` for pinch-to-zoom
- Copies content-URI PDFs to the app cache dir (required by `PdfRenderer`)
- Shows a `ProgressBar` while the PDF is being prepared
- Cleans up `PdfRenderer` and `Bitmap` resources on `onDetachedFromWindow`

---

## 🏃 Building & Running

```bash
# Clone the repository
git clone https://github.com/kumarjadhav/Android-PDF-Reader.git
cd Android-PDF-Reader

# Build the debug APK
./gradlew assembleDebug

# Install on a connected device / emulator
./gradlew installDebug
```

---

## 📄 License

Apache 2.0 — see [LICENSE](LICENSE) for details.
