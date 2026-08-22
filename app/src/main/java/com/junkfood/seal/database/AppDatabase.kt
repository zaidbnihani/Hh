package com.junkfood.seal.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.database.objects.CookieProfile
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.database.objects.OptionShortcut

@Database(
    entities = [DownloadedVideoInfo::class, CommandTemplate::class, CookieProfile::class, OptionShortcut::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoInfoDao(): VideoInfoDao
}
