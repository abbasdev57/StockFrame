package com.abbas57.stockframe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.abbas57.stockframe.ui.navigation.StockframeNavHost
import com.abbas57.stockframe.ui.theme.StockFrameTheme
import dagger.hilt.android.AndroidEntryPoint


/**
 * @AndroidEntryPoint is required on every Android framework class (Activity,
 * Fragment, Service, etc.) that contains an @Inject or hosts Composables
 * using hiltViewModel(). Without this annotation here, Hilt has no way to
 * inject anything into this Activity's object graph — every hiltViewModel()
 * call inside StockframeNavHost's screens would fail at runtime.
 *
 * Note this is a DIFFERENT annotation from @HiltAndroidApp (used once, on
 * StockframeApplication). @AndroidEntryPoint goes on every entry-point
 * class; @HiltAndroidApp goes on the single Application class.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockFrameTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StockframeNavHost()
                }
            }
        }
    }
}