package com.junkfood.seal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.JsRuntimeUtil
import com.yausername.youtubedl_android.YoutubeDL
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YoutubeEjsInstrumentedTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        YoutubeDL.getInstance().init(context)
    }

    @Test
    fun testJsRuntimeDetection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val diagnostics = JsRuntimeUtil.getDiagnostics(context)

        // Verify JS runtime is detected in the APK environment
        assertTrue("JS Runtime must be available in the APK", diagnostics.isJsRuntimeAvailable)
        assertNotNull("JS Runtime path must not be null", diagnostics.jsRuntimePath)
        assertTrue("YouTube EJS must be supported", diagnostics.isEjsSupported)
    }

    @Test
    fun testYouTubeExtractionWithJsRuntime() {
        val testUrl = "https://www.youtube.com/watch?v=aqz-KE-bpKQ" // Big Buck Bunny sample on YouTube

        val result = DownloadUtil.fetchVideoInfoFromUrl(url = testUrl)

        assertTrue("Fetching YouTube video info should succeed with JS runtime: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val videoInfo = result.getOrNull()
        assertNotNull("VideoInfo must not be null", videoInfo)

        // Verify video formats exist and are not only images
        val formats = videoInfo?.formats
        assertNotNull("Formats must not be null", formats)
        assertTrue("Formats list must not be empty", formats!!.isNotEmpty())

        val playableFormats = formats.filter { format ->
            (format.vcodec != null && format.vcodec != "none") ||
            (format.acodec != null && format.acodec != "none")
        }

        assertTrue("Playable audio/video formats must be available (not only images)", playableFormats.isNotEmpty())
    }
}
