package com.example.pleb2

import android.app.Application
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Pleb2Application : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Log.d("debug workflow", "Pleb2Application.kt started")
    }

    /**
     * This function is automatically called by the Coil library because this class implements
     * the ImageLoaderFactory interface. It's responsible for creating a single, app-wide
     * ImageLoader instance that will be used for all image loading requests.
     */
    override fun newImageLoader(): ImageLoader {
        // We use the ImageLoader.Builder to construct a new loader.
        return ImageLoader.Builder(this)
            // The 'components' block is where we can add custom functionality, like decoders.
            .components {
                // To support animated GIFs, we need to add a specific decoder.
                // The correct decoder depends on the Android version (SDK level).
                if (Build.VERSION.SDK_INT >= 28) {
                    // For modern Android versions (API 28+), ImageDecoderDecoder is the
                    // efficient and recommended way to handle GIFs and other animated formats.
                    add(ImageDecoderDecoder.Factory())
                } else {
                    // For older versions, we fall back to the classic GifDecoder.
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }
}
