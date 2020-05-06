package org.opencovidtrace.octrace.storage


object UserStatusManager : PreferencesHolder("userStatus") {

    const val healthy = "healthy"
    const val symptoms = "symptoms"

    private const val USER_STATUS = "userStatus"

    private fun getStatus(): String {
        return getString(USER_STATUS) ?: healthy
    }

    fun setStatus(value: String) {
        setString(USER_STATUS, value)
    }

    fun sick(): Boolean {
        return getStatus() == symptoms
    }
}
