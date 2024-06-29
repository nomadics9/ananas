package com.nomadics9.ananas.ui.dummy

import com.nomadics9.ananas.models.DiscoveredServer
import com.nomadics9.ananas.models.Server
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
