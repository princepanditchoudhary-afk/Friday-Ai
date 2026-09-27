package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AIOrbState
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

data class AgentNode(
    val name: String,
    val role: String,
    val angleDegrees: Double,
    val color: Color
)

@Composable
fun FridayOrbConstellation(
    state: AIOrbState,
    deskEquity: String = "$1,604,199",
    activeCount: Int = 6,
    watchingCount: Int = 231,
    signalsCount: Int = 61,
    scansCount: Int = 167,
    alertsCount: Int = 4,
    onOrbClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FridayOrbTransition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AIOrbState.RUNNING_AGENTS -> 600
                    AIOrbState.LISTENING -> 400
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    val nodes = listOf(
        AgentNode("CAPITOL", "DC / SEC", 270.0, Color(0xFF38BDF8)),
        AgentNode("SCOUT", "RECON", 315.0, Color(0xFF00E5FF)),
        AgentNode("SENTINEL", "RISK OFFICER", 0.0, Color(0xFFF43F5E)),
        AgentNode("PILOT", "EXECUTION", 45.0, Color(0xFF10B981)),
        AgentNode("LEDGER", "THE BOOK", 90.0, Color(0xFF818CF8)),
        AgentNode("ORACLE", "QUANT", 135.0, Color(0xFFF59E0B)),
        AgentNode("CHARTIST", "TECHNICAL", 180.0, Color(0xFFE879F9)),
        AgentNode("ATHENA", "ANALYST", 215.0, Color(0xFF34D399)),
        AgentNode("ATLAS", "MACRO", 245.0, Color(0xFF60A5FA))
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF070B13), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF132238), RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Constellation Canvas with Orb and 9 Satellite Agent Nodes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val orbitRadius = (size.minDimension / 2) * 0.76f

                // Outer faint radar grid ring
                drawCircle(
                    color = Color(0xFF1E3A5F).copy(alpha = 0.35f),
                    center = center,
                    radius = orbitRadius * 1.15f,
                    style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f))
                )

                // Main Orbital Ring with subtle dash
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    center = center,
                    radius = orbitRadius,
                    style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), orbitRotation))
                )

                // Inner orbital guidance ring
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                    center = center,
                    radius = orbitRadius * 0.55f,
                    style = Stroke(width = 1f)
                )

                // Draw lines connecting satellite agent nodes to center
                for (node in nodes) {
                    val angleRad = Math.toRadians(node.angleDegrees)
                    val nx = center.x + (orbitRadius * cos(angleRad)).toFloat()
                    val ny = center.y + (orbitRadius * sin(angleRad)).toFloat()

                    // Vector beam from center to node
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.3f), node.color.copy(alpha = 0.1f)),
                            start = center,
                            end = Offset(nx, ny)
                        ),
                        start = center,
                        end = Offset(nx, ny),
                        strokeWidth = 1f
                    )

                    // Satellite Node outer glow & dot
                    drawCircle(
                        color = node.color.copy(alpha = 0.25f),
                        center = Offset(nx, ny),
                        radius = 8f
                    )
                    drawCircle(
                        color = node.color,
                        center = Offset(nx, ny),
                        radius = 3.5f
                    )
                }

                // Central AI Orb Glowing Aura
                val coreRadius = (orbitRadius * 0.42f) * pulseScale

                // 1. Deep outer cyan glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                            Color(0xFF0077B6).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = coreRadius * 1.6f
                    ),
                    center = center,
                    radius = coreRadius * 1.6f
                )

                // 2. Turquoise core sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFF38BDF8),
                            Color(0xFF0284C7),
                            Color(0xFF0369A1)
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    center = center,
                    radius = coreRadius
                )

                // 3. Crisp bright boundary rim
                drawCircle(
                    color = Color(0xFFE0F2FE).copy(alpha = 0.8f),
                    center = center,
                    radius = coreRadius,
                    style = Stroke(width = 1.5f)
                )
            }

            // Clickable trigger on central orb
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .clickable { onOrbClick() }
                    .testTag("friday_orb_center")
            )

            // Agent Labels positioned around the orbit
            for (node in nodes) {
                val angleRad = Math.toRadians(node.angleDegrees)
                val isLeft = cos(angleRad) < -0.2
                val isRight = cos(angleRad) > 0.2
                val isTop = sin(angleRad) < -0.5
                val isBottom = sin(angleRad) > 0.5

                val xAlignment = if (isLeft) Alignment.End else if (isRight) Alignment.Start else Alignment.CenterHorizontally

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val offsetX = (105 * cos(angleRad)).dp
                    val offsetY = (105 * sin(angleRad)).dp

                    Column(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .padding(2.dp),
                        horizontalAlignment = xAlignment
                    ) {
                        Text(
                            text = node.name,
                            color = node.color,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = node.role,
                            color = Color(0xFF64748B),
                            fontSize = 6.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // F.R.I.D.A.Y. Branding Title (Golden / Cyan Glow from screenshot)
        Text(
            text = "F . R . I . D . A . Y .",
            color = Color(0xFFF3D279), // Warm gold from reference image
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "MASTER STRATEGY SYNTHESIS // RISK ROUTER",
            color = Color(0xFF38BDF8),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Big Desk Equity Telemetry ($1,604,199 from screenshot)
        Text(
            text = deskEquity,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )

        Text(
            text = "DESK EQUITY",
            color = Color(0xFF64748B),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Telemetry Counters Strip (6 IN PLAY | 231 WATCHING | 61 SIGNALS | 167 SCANS TODAY | 4 ALERTS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1220), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1E2E48), RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TelemetryMetric(value = "$activeCount", label = "IN PLAY", valueColor = Color(0xFF00E5FF))
            MetricDivider()
            TelemetryMetric(value = "$watchingCount", label = "WATCHING", valueColor = Color.White)
            MetricDivider()
            TelemetryMetric(value = "$signalsCount", label = "SIGNALS", valueColor = Color(0xFF10B981))
            MetricDivider()
            TelemetryMetric(value = "$scansCount", label = "SCANS TODAY", valueColor = Color(0xFF38BDF8))
            MetricDivider()
            TelemetryMetric(value = "$alertsCount", label = "ALERTS", valueColor = Color(0xFFF43F5E))
        }
    }
}

@Composable
fun TelemetryMetric(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 7.5.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .background(Color(0xFF1E2E48))
    )
}
