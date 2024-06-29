package org.askartv.phone.ui.dummy

import org.askartv.phone.models.DiscoveredServer
import org.askartv.phone.models.Server
import java.util.UUID

val dummyDiscoveredServer = DiscoveredServer(
    id = "",
    name = "Demo server",
    address = "https://demo.jellyfin.org/stable",
)

val dummyDiscoveredServers = listOf(dummyDiscoveredServer)

val dummyServer = Server(
    id = "",
    name = "Demo server",
    currentServerAddressId = UUID.randomUUID(),
    currentUserId = UUID.randomUUID(),
)

val dummyServers = listOf(dummyServer)
