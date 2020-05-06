package org.opencovidtrace.octrace.api


class ContactsApiClient(private val client: ContactsApiEndpoint) : ContactsApiEndpoint by client
