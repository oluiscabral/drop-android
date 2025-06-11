package dev.arkbuilders.drop.app.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(modifier: Modifier = Modifier) {
    Column(modifier) {
        val innerModifier = Modifier.fillMaxWidth()
        Row(
            modifier = innerModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val innerModifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
            Column(modifier = innerModifier) {
                Text(
                    text = "Hi, stranger!",
                    style = Typography.bodySmall,
                )
                Text(
                    text = "Welcome back!",
                    style = Typography.titleSmall,
                )
            }
            IconButton(
                modifier = innerModifier, onClick = {}, enabled = false, colors = IconButtonColors(
                    containerColor = Color.LightGray,
                    disabledContainerColor = Color.LightGray,
                    contentColor = Color.Gray,
                    disabledContentColor = Color.Gray,
                )
            ) {
                Icon(imageVector = Icons.Rounded.Person, contentDescription = null)
            }
        }
        HorizontalDivider(modifier = innerModifier, thickness = 0.3.dp)
    }
}

@Preview
@Composable
fun AppBarPreview() {
    TopBar()
}