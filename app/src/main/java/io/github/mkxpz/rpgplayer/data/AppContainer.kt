package io.github.mkxpz.rpgplayer.data

import android.content.Context
import androidx.room.Room
import io.github.mkxpz.rpgplayer.domain.GameDirectoryDetector
import io.github.mkxpz.rpgplayer.domain.GameImportService
import io.github.mkxpz.rpgplayer.domain.GameLaunchController
import io.github.mkxpz.rpgplayer.domain.MkxpConfigWriter

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "mkxp_player.db",
    )
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()

    val settingsRepository = SettingsRepository(appContext)
    private val configWriter = MkxpConfigWriter()
    private val detector = GameDirectoryDetector()

    val gameRepository = GameRepository(
        context = appContext,
        dao = database.gameDao(),
        detector = detector,
        configWriter = configWriter,
        settingsRepository = settingsRepository,
    )

    val importService = GameImportService(
        context = appContext,
        repository = gameRepository,
    )

    val launchController = GameLaunchController(
        context = appContext,
        repository = gameRepository,
        settingsRepository = settingsRepository,
    )
}
