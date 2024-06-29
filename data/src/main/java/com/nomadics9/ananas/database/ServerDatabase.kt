package com.nomadics9.ananas.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.nomadics9.ananas.models.FindroidEpisodeDto
import com.nomadics9.ananas.models.FindroidMediaStreamDto
import com.nomadics9.ananas.models.FindroidMovieDto
import com.nomadics9.ananas.models.FindroidSeasonDto
import com.nomadics9.ananas.models.FindroidShowDto
import com.nomadics9.ananas.models.FindroidSourceDto
import com.nomadics9.ananas.models.FindroidTrickplayInfoDto
import com.nomadics9.ananas.models.FindroidUserDataDto
import com.nomadics9.ananas.models.IntroDto
import com.nomadics9.ananas.models.Server
import com.nomadics9.ananas.models.ServerAddress
import com.nomadics9.ananas.models.User

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
