package org.askartv.phone.ui.dummy

import org.askartv.phone.models.User
import java.util.UUID

val dummyUser = User(
    id = UUID.randomUUID(),
    name = "Username",
    serverId = "",
)

val dummyUsers = listOf(dummyUser)
