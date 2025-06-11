package dev.arkbuilders.drop.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.arkbuilders.drop.app.R
import dev.arkbuilders.drop.app.ui.theme.Typography

@Composable
fun Home(modifier: Modifier = Modifier, navController: NavController) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val innerModifier = Modifier.padding(horizontal = 24.dp)
        Spacer(modifier = Modifier.height(24.dp))
        TopBar(modifier = innerModifier)
        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
//        Spacer(modifier = innerModifier.height(48.dp))
        Hero(modifier = innerModifier.padding(0.dp, 30.dp))
        CTA(modifier = innerModifier, navController = navController)
    }
}

@Composable
fun TopBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Hi John,", fontSize = 16.sp, color = Color.Black)
            Text(
                "Welcome back!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Image(
            painter = painterResource(R.drawable.avatar_placeholder),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun Hero(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size((200 + it * 60).dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(width = 1.dp, color = Color.LightGray, shape = CircleShape)
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_link), // center icon
            contentDescription = null,
            tint = Color.DarkGray,
            modifier = Modifier.size(36.dp)
        )
        Image(
            painter = painterResource(R.drawable.ic_pdf),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .absoluteOffset(y = (-100).dp)
                .background(Color.White, CircleShape)
        )
        Image(
            painter = painterResource(R.drawable.ic_music),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .absoluteOffset(x = (126).dp, y = (-30).dp)
                .background(Color.White, CircleShape)
        )
        Image(
            painter = painterResource(R.drawable.ic_document),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .absoluteOffset(x = (70).dp, y = (70).dp)
                .background(Color.White, CircleShape)
        )
        Image(
            painter = painterResource(R.drawable.ic_image),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .absoluteOffset(x = (-70).dp, y = (70).dp)

                .background(Color.White, CircleShape)
        )
        Image(
            painter = painterResource(R.drawable.ic_video),
            contentDescription = null,
            modifier = Modifier
                .size(30.dp)
                .absoluteOffset(x = (-126).dp, y = (-30).dp)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun CTA(modifier: Modifier = Modifier, navController: NavController) {
    Column(modifier) {
        val innerModifier = Modifier.fillMaxWidth()
        CTAHeading(innerModifier)
        Actions(innerModifier, navController)
    }
}

@Composable
fun CTAHeading(modifier: Modifier = Modifier) {
    Column(modifier) {
        val innerModifier = Modifier.fillMaxWidth()
        Text(
            modifier = innerModifier,
            text = "Seamless to transfer your files",
            style = Typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            modifier = innerModifier,
            text = "Simple, fast, and limitless start sharing your files now.",
            style = Typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}

@Composable
fun Actions(
    modifier: Modifier = Modifier, navController: NavController
) {
    Row(modifier, horizontalArrangement = Arrangement.Center) {
        Button(modifier = Modifier.padding(0.dp, 0.dp, 10.dp, 0.dp), onClick = {
            navController.navigate("send")
        }) {
            Text(text = "Send")
        }
        Button(onClick = {
            navController.navigate("receive")
        }) {
            Text(text = "Receive")
        }
    }
}
