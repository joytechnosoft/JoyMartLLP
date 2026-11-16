package com.joymart.llp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Opens the dialler only; it never places a call directly. */
@Composable
fun MemberCallButton(phone: String?, modifier: Modifier = Modifier, label: String = "Call") {
    val context = LocalContext.current
    val number = phone?.trim().orEmpty()
    if (number.isNotBlank()) {
        TextButton(onClick = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))
            context.startActivity(intent)
        }, modifier = modifier) {
            Icon(Icons.Default.Call, contentDescription = label)
            Spacer(Modifier.width(5.dp))
            Text(label)
        }
    }
}
