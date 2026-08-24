package io.github.shuixingqianfeng.morireader

import android.content.Context
import io.github.shuixingqianfeng.morireader.data.BookRepository
import io.github.shuixingqianfeng.morireader.data.EpubImporter
import io.github.shuixingqianfeng.morireader.data.MoriDatabase
import io.github.shuixingqianfeng.morireader.data.SettingsRepository

class AppContainer(context: Context) {
    private val database = MoriDatabase.get(context)
    val books = BookRepository(database.dao(), EpubImporter(context))
    val settings = SettingsRepository(context)
}
