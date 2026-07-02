package com.abbas57.stockframe.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abbas57.stockframe.ui.theme.Neutral0
import com.abbas57.stockframe.ui.theme.Neutral700

/**
 * Thin, dismissable-by-reconnect banner. Deliberately understated (grey,
 * not red/alarming) — being offline isn't an error state in this app,
 * since Firestore's cache and write queue mean the user can keep working
 * normally. This is informational, not a warning.
 */
@Composable
fun OfflineBanner(isOffline: Boolean) {
    AnimatedVisibility(visible = isOffline) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Neutral700)
                .padding(vertical = 6.dp)
        ) {
            Text(
                "You're offline — changes will sync when reconnected",
                color = Neutral0,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}