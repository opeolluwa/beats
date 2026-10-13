package com.beats.app.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beats.app.network.DiscoveredPeer
import com.beats.app.ui.theme.Accent
import com.beats.app.ui.theme.Accent50
import com.beats.app.ui.theme.Accent100
import com.beats.app.ui.theme.Accent200
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val peerAngles = listOf(210f, 45f, 135f, 330f, 270f, 90f)

@Composable
fun RadarDiscoveryView(
    peers: List<DiscoveredPeer>,
    onPeerClick: (DiscoveredPeer) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val density = LocalDensity.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(300.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = size.minDimension / 2

            drawCircle(
                color = Accent50,
                radius = maxRadius,
                center = Offset(centerX, centerY)
            )

            val ringRadii = listOf(0.33f, 0.66f, 1f)
            for (fraction in ringRadii) {
                drawCircle(
                    color = Accent100,
                    radius = maxRadius * fraction,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            rotate(sweepAngle, pivot = Offset(centerX, centerY)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to Color.Transparent,
                        0.08f to Accent200.copy(alpha = 0.6f),
                        0.12f to Accent.copy(alpha = 0.3f),
                        0.15f to Color.Transparent,
                        1f to Color.Transparent,
                        center = Offset(centerX, centerY)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = true,
                    size = size
                )
            }

            drawCircle(
                color = Accent,
                radius = 24.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Accent,
                radius = 5.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }

        peers.forEachIndexed { index, peer ->
            val angle = Math.toRadians(peerAngles[index % peerAngles.size].toDouble())
            val ringRadius = with(density) { 150.dp.toPx() * 0.6f }

            val offsetX = (cos(angle) * ringRadius).roundToInt()
            val offsetY = (sin(angle) * ringRadius).roundToInt()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .clickable { onPeerClick(peer) }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Accent)
                ) {
                    Text(
                        text = peer.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = peer.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(width = 70.dp, height = 16.dp)
                )
            }
        }
    }
}
