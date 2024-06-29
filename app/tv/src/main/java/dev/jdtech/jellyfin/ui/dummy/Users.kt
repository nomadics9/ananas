package com.nomadics9.ananas.ui.dummy

import com.nomadics9.ananas.models.User
import java.util.UUID

val dummyUser = User(
    id = UUID.randomUUID(),
    name = "Username",
    serverId = "",
)

val dummyUsers = listOf(dummyUser)
