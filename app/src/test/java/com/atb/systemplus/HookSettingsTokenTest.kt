package com.atb.systemplus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HookSettingsTokenTest {

    @Test
    fun acceptsNonEmptyToken() {
        assertTrue(HookSettings.hasPrivilegeToken("token-123"))
    }

    @Test
    fun rejectsBlankAndFalseLikeValues() {
        assertFalse(HookSettings.hasPrivilegeToken(null))
        assertFalse(HookSettings.hasPrivilegeToken(""))
        assertFalse(HookSettings.hasPrivilegeToken("   "))
        assertFalse(HookSettings.hasPrivilegeToken("false"))
        assertFalse(HookSettings.hasPrivilegeToken("0"))
    }
}

