package com.ignaciovinarro.stayfocused.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderPermissionsTest {

    @Test
    fun `con todo permitido no falta nada`() {
        assertNull(missingReminderPermission(notificationsEnabled = true, exactAlarmAllowed = true))
    }

    @Test
    fun `sin notificaciones falta el permiso de notificaciones`() {
        assertEquals(
            ReminderPermissionIssue.NOTIFICATIONS,
            missingReminderPermission(notificationsEnabled = false, exactAlarmAllowed = true)
        )
    }

    @Test
    fun `sin alarma exacta falta el permiso de alarmas`() {
        assertEquals(
            ReminderPermissionIssue.EXACT_ALARM,
            missingReminderPermission(notificationsEnabled = true, exactAlarmAllowed = false)
        )
    }

    @Test
    fun `si faltan ambos se pide primero el de notificaciones`() {
        assertEquals(
            ReminderPermissionIssue.NOTIFICATIONS,
            missingReminderPermission(notificationsEnabled = false, exactAlarmAllowed = false)
        )
    }
}
