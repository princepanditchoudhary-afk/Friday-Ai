package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIOrbState
import com.example.ui.theme.*

@Composable
fun AIOrb(
    state: AIOrbState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AIOrbState.RUNNING_AGENTS, AIOrbState.THINKING -> 700
                    AIOrbState.LISTENING -> 500
                    AIOrbState.FETCHING_DATA -> 800
                    AIOrbState.RESPONDING -> 1000
                    AIOrbState.ERROR -> 400
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AIOrbState.RUNNING_AGENTS -> 3000
                    AIOrbState.THINKING, AIOrbState.FETCHING_DATA -> 5000
                    else -> 12000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val orbColor by animateColorAsState(
        targetValue = when (state) {
            AIOrbState.IDLE -> CyanPrimary
            AIOrbState.LISTENING -> EmeraldBullish
            AIOrbState.THINKING -> CyanAccent
            AIOrbState.FETCHING_DATA -> AmberCaution
            AIOrbState.RUNNING_AGENTS -> Color(0xFF818CF8) // Indigo
            AIOrbState.VERIFYING -> Color(0xFFA855F7) // Purple
            AIOrbState.RESPONDING -> CyanPrimary
            AIOrbState.ERROR -> RubyBearish
        },
        animationSpec = tween(500),
        label = "OrbColor"
    )

    val stateText = when (state) {
        AIOrbState.IDLE -> "SYSTEM READY"
        AIOrbState.LISTENING -> "VOICE STREAM ACTIVE"
        AIOrbState.THINKING -> "ORCHESTRATING INTENT"
        AIOrbState.FETCHING_DATA -> "RETRIEVING LIVE TELEMETRY"
        AIOrbState.RUNNING_AGENTS -> "EXECUTING SPECIALIZED AGENTS"
        AIOrbState.VERIFYING -> "CROSS-VALIDATING DATA & RISKS"
        AIOrbState.RESPONDING -> "SYNTHESIZING INTELLIGENCE"
        AIOrbState.ERROR -> "SYSTEM ALERT / TELEMETRY GAP"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .clickable { onClick() }
                .testTag("ai_orb_central")
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val baseRadius = (size.minDimension / 2) * pulseScale

                // Outer aura glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(orbColor.copy(alpha = 0.25f), Color.Transparent),
                        center = center,
                        radius = baseRadius * 1.35f
                    ),
                    center = center,
                    radius = baseRadius * 1.35f
                )

                // Outer rotating thin orbital ring
                drawCircle(
                    color = orbColor.copy(alpha = 0.4f),
                    center = center,
                    radius = baseRadius * 0.95f,
                    style = Stroke(width = 1.5f)
                )

                // Inner core gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            orbColor,
                            orbColor.copy(alpha = 0.2f)
                        ),
                        center = center,
                        radius = baseRadius * 0.7f
                    ),
                    center = center,
                    radius = baseRadius * 0.7f
                )
            }

            // Center icon based on state
            Icon(
                imageVector = when (state) {
                    AIOrbState.LISTENING -> Icons.Default.Mic
                    AIOrbState.RUNNING_AGENTS -> Icons.Default.Hub
                    AIOrbState.FETCHING_DATA -> Icons.Default.CloudSync
                    AIOrbState.THINKING -> Icons.Default.Psychology
                    AIOrbState.VERIFYING -> Icons.Default.VerifiedUser
                    AIOrbState.RESPONDING -> Icons.Default.SmartToy
                    AIOrbState.ERROR -> Icons.Default.Warning
                    else -> Icons.Default.GraphicEq
                },
                contentDescription = stateText,
                tint = DeskDarkBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(DeskSurfaceVariant, RoundedCornerShape(20.dp))
                .border(1.dp, DeskBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(orbColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stateText,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
