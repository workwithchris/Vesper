package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vesper", appName)
  }

  @Test
  fun `swiping back on home screen never finishes MainActivity`() {
    Robolectric.buildActivity(MainActivity::class.java).use { controller ->
      val activity = controller.setup().get()
      activity.onBackPressedDispatcher.onBackPressed()
      assertFalse(activity.isFinishing)
    }
  }
}

