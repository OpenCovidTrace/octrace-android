package org.opencovidtrace.octrace.di

abstract class DependentProvider<T : Any> : Provider<T>() {
    final override lateinit var lazyInstance: Lazy<T>
    fun inject(initializer: () -> T) = run { lazyInstance = lazy(initializer) }
}

abstract class IndependentProvider<out T : Any> : Provider<T>() {
    final override val lazyInstance: Lazy<T> = lazy(::initInstance)
    protected abstract fun initInstance(): T
}

abstract class Provider<out T : Any> {
    protected abstract val lazyInstance: Lazy<T>
    operator fun invoke(): Lazy<T> = lazyInstance
}
