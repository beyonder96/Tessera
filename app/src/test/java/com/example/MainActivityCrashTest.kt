package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityCrashTest {

    @Test
    fun testActivityStarts() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        assert(activity != null)
    }
}
