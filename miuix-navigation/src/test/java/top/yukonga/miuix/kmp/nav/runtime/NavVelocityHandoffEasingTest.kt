package top.yukonga.miuix.kmp.nav.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavVelocityHandoffEasingTest {
    @Test
    fun handoffStartsAtTheReleaseVelocityAndArrivesAtRest() {
        for (distance in listOf(-.8f, -.3f, .3f, .8f)) {
            for (duration in listOf(220, 299, 480)) {
                val seconds = duration / 1000f
                val velocity = distance * 1.5f / seconds
                val curve = NavVelocityHandoffEasing(distance, velocity, duration)
                val dt = .001f
                val firstVelocity = (curve.transform(dt) - curve.transform(0f)) * distance / (dt * seconds)
                val lastVelocity = (curve.transform(1f) - curve.transform(1f - dt)) * distance / (dt * seconds)
                assertEquals(velocity, firstVelocity, .01f)
                assertEquals(0f, lastVelocity, .02f)
                assertEquals(0f, curve.transform(0f), 0f)
                assertEquals(1f, curve.transform(1f), 0f)
            }
        }
    }

    @Test
    fun extremeReleaseAndNearLandingStayMonotoneWithinTheNavigationBoundary() {
        for (distance in listOf(-1f, -.001f, .001f, 1f, 0f)) {
            for (velocity in listOf(-100f, -1f, 0f, 1f, 100f, Float.NaN)) {
                val curve = NavVelocityHandoffEasing(distance, velocity, 299)
                val samples = (0..1000).map { curve.transform(it / 1000f) }
                assertTrue(samples.all { it.isFinite() && it in 0f..1f })
                assertTrue(samples.zipWithNext().all { (a, b) -> b >= a - .000001f })
            }
        }
    }
}
