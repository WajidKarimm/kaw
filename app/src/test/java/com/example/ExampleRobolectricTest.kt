package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SportsbookRepository
import com.example.util.QrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RefReels", appName)
    }

    @Test
    fun `test repository toggle save and custom link`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SportsbookRepository(context)
        val initialApps = repository.getInitialApps()
        assertTrue(initialApps.isNotEmpty())

        val firstApp = initialApps.first()
        val isSaved = repository.toggleSave(firstApp.id)
        assertTrue(isSaved)
        assertTrue(repository.savedIds.value.contains(firstApp.id))

        val isRemoved = repository.toggleSave(firstApp.id)
        assertFalse(isRemoved)

        repository.setCustomLink(firstApp.id, "MY_CUSTOM_CODE")
        val effective = repository.getEffectiveLink(firstApp)
        assertTrue(effective.contains("MY_CUSTOM_CODE"))
    }

    @Test
    fun `test qr code bitmap generation in Robolectric`() {
        val testUrl = "https://pointsbet.com/ref/REFREELS150"
        val bmp = QrCodeGenerator.generateBitmap(testUrl, pixelSize = 256)
        assertNotNull(bmp)
        assertTrue(bmp.width > 0)
        assertTrue(bmp.height > 0)
    }
}
