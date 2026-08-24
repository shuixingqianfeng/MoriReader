package io.github.shuixingqianfeng.morireader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.shuixingqianfeng.morireader.ui.MoriReaderApp
import io.github.shuixingqianfeng.morireader.ui.theme.MoriReaderTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MoriReaderTheme { MoriReaderApp(viewModel) } }
    }
}
