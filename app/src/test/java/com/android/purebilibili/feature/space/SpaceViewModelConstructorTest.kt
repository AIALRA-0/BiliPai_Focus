package com.android.purebilibili.feature.space

import kotlin.test.Test
import kotlin.test.assertNotNull

class SpaceViewModelConstructorTest {
    @Test
    fun supportsMiuixEntryDefaultViewModelFactory() {
        assertNotNull(SpaceViewModel::class.java.getConstructor())
    }
}
