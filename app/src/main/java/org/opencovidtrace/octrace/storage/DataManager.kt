package org.opencovidtrace.octrace.storage

import java.util.*

object DataManager {

    const val maxDays = 14

    private fun expirationDate(): Calendar = Calendar.getInstance().apply { add(Calendar.DATE, -maxDays) }

    fun expirationTimestamp() = expirationDate().timeInMillis

}