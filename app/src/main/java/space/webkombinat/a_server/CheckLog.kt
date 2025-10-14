package space.webkombinat.a_server

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun CheckStatus(
    modifier: Modifier = Modifier,
    label: String,
    status: Boolean
) {
    Row(modifier = modifier.fillMaxWidth(0.9f)) {
        Text(text = label)
        Spacer(modifier = modifier.weight(1f))
        Text(
            text = if (status) "OK" else "NOT OK",
            color = if (status) Color.Green else Color.Red
        )
    }
}