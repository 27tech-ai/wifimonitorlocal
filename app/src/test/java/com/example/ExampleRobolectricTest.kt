package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.discovery.NetworkUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WiFi Traffic Monitor", appName)
  }

  @Test
  fun `test local subnet info fallback in test environment`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val subnet = NetworkUtils.getLocalSubnetInfo(context)
    assertNotNull(subnet)
    assertEquals("192.168.1.1", subnet?.gatewayIp)
  }
}

