package io.github.shuixingqianfeng.morireader

import android.app.Application

class MoriReaderApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
