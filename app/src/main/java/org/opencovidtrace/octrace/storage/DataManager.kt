package org.opencovidtrace.octrace.storage

import java.util.*

object DataManager {

    private const val maxDays = 14

    fun expirationDate(): Calendar = Calendar.getInstance().apply { add(Calendar.DATE, -maxDays) }

    fun expirationTimestamp() = expirationDate().timeInMillis

}