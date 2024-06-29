package org.askartv.phone.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import org.askartv.phone.models.FindroidEpisodeDto
import org.askartv.phone.models.FindroidMediaStreamDto
import org.askartv.phone.models.FindroidMovieDto
import org.askartv.phone.models.FindroidSeasonDto
import org.askartv.phone.models.FindroidShowDto
import org.askartv.phone.models.FindroidSourceDto
import org.askartv.phone.models.FindroidTrickplayInfoDto
import org.askartv.phone.models.FindroidUserDataDto
import org.askartv.phone.models.IntroDto
import org.askartv.phone.models.Server
import org.askartv.phone.models.ServerAddress
import org.askartv.phone.models.User

@Database(
    entities = [Server::class, ServerAddress::class, User::class, FindroidMovieDto::class, FindroidShowDto::class, FindroidSeasonDto::class, FindroidEpisodeDto::class, FindroidSourceDto::class, FindroidMediaStreamDto::class, IntroDto::class, FindroidUserDataDto::class, FindroidTrickplayInfoDto::class],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5, spec = ServerDatabase.TrickplayMigration::class),
    ],
)
@TypeConverters(Converters::class)
abstract class ServerDatabase : RoomDatabase() {
    abstract fun getServerDatabaseDao(): ServerDatabaseDao

    @DeleteTable(tableName = "trickPlayManifests")
    class TrickplayMigration : AutoMigrationSpec
}
