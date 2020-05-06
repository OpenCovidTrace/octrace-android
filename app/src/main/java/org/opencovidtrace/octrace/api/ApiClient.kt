package org.opencovidtrace.octrace.api


class ApiClient(private val client: ApiEndpoint) : ApiEndpoint by client
