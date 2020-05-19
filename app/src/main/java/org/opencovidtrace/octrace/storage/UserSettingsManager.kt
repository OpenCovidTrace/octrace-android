package org.opencovidtrace.octrace.storage


object UserSettingsManager : PreferencesHolder("userSettings") {

    private const val NORMAL = "NORMAL"
    const val EXPOSED = "EXPOSED"

    private const val USER_STATUS = "USER_STATUS"
    private const val RECORD_TRACK = "UPLOAD_TRACK"
    private const val UPLOAD_TRACK = "UPLOAD_TRACK"
    private const val DISCLOSE_META_DATA = "DISCLOSE_META_DATA"

    var status: String
        get() = getString(USER_STATUS) ?: NORMAL
        set(value) {
            setString(USER_STATUS, value)
        }

    fun sick(): Boolean {
        return status == EXPOSED
    }

    var recordTrack: Boolean
        get() = getBoolean(RECORD_TRACK)
        set(value) {
            setBoolean(RECORD_TRACK, value)
        }

    var uploadTrack: Boolean
        get() = getBoolean(UPLOAD_TRACK)
        set(value) {
            setBoolean(UPLOAD_TRACK, value)
        }

    var discloseMetaData: Boolean
        get() = getBoolean(DISCLOSE_META_DATA)
        set(value) {
            setBoolean(DISCLOSE_META_DATA, value)
        }

}
