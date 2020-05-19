package org.opencovidtrace.octrace.storage


object OnboardingManager : PreferencesHolder("onboarding") {

    private const val STATUS = "status"

    fun getStatus(): OnboardingStage {
        return getString(STATUS)?.let {
            OnboardingStage.valueOf(it)
        } ?: OnboardingStage.WELCOME
    }

    fun setStatus(stage: OnboardingStage) {
        setString(STATUS, stage.name)
    }

    fun isComplete(): Boolean {
        return getStatus() == OnboardingStage.COMPLETE
    }

}

enum class OnboardingStage { WELCOME, LOCATION, COMPLETE }