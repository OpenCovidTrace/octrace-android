package org.opencovidtrace.octrace.ext

inline fun <A, B> ifAllNotNull(a: A?, b: B?, block: (A, B) -> Unit
) {
    if (a != null && b != null) block(a, b)
}

inline fun <A, B, C, D, E> ifAllNotNull(
    a: A?,
    b: B?,
    c: C?,
    d: D?,
    e: E?,
    block: (A, B, C, D, E) -> Unit
) {
    if (a != null && b != null && c != null && d != null && e != null) block(a, b, c, d, e)
}