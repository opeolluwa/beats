package com.beats.app.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beats.app.ui.theme.Accent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import com.beats.app.R
import com.beats.app.ui.theme.Accent700

@Composable()
fun PulsingCirclesAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "");
    val sizes = listOf(100.dp, 150.dp, 200.dp);
    val scaleFactors = List(sizes.size) { index ->
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 6000
                    0.0f at 0 with LinearEasing
                    0.2f at (1000 + (index + 1000)) with LinearEasing
                    1.2f at (2000 + (index + 1000)) with LinearEasing
                    1.4f at (4000 + (index + 1000)) with LinearEasing
                    1.6f at 6000 with LinearEasing
                },
                repeatMode = RepeatMode.Restart
            ),
            label = ""
        )

    }

    val alphaValues = List(sizes.size) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 6000
                    0.0f at 0 with LinearEasing
                    0.2f at (2000 + (index + 1000)) with LinearEasing
                    0.1f at (4000 + (index + 1000)) with LinearEasing
                    0.0f at 6000 with LinearEasing
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = ""

        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 48.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            sizes.forEachIndexed { index, size ->
                val scale = scaleFactors[index].value
                val alpha = alphaValues[index].value
                val radius = size.toPx() / 2

                drawCircle(
                    color = Accent,
                    radius = radius * scale,
                    center = center,
                    alpha = alpha
                )
            }

            drawCircle(
                color = Accent,
                radius = 60f,
                center = center
            )


        }
        Image(
            painter = painterResource(id = R.drawable.signal),
            contentDescription = "Custom Vector Icon",
            modifier = Modifier.size(24.dp)
        )
    }
}