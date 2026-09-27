package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarEvent
import com.example.data.model.NewsItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntelligenceScreen(
    news: List<NewsItem>,
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: News, 1: Calendar

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeskDarkBackground)
    ) {
        // Tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeskSurface)
                .border(1.dp, DeskBorder)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("FINANCIAL & GLOBAL NEWS", "MACRO ECONOMIC CALENDAR")
            for (i in tabs.indices) {
                val isSelected = selectedTab == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanPrimary else DeskSurfaceVariant)
                        .clickable { selectedTab = i }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabs[i],
                        color = if (isSelected) DeskDarkBackground else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == 0) {
                // News Items
                if (news.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "News wire loading or currently unavailable...",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    items(news) { item ->
                        NewsCard(item = item)
                    }
                }
            } else {
                // Calendar Events
                items(events) { event ->
                    CalendarCard(event = event)
                }
            }
        }
    }
}

@Composable
fun NewsCard(item: NewsItem) {
    val dateStr = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date(item.timestamp))
    val sentimentColor = when {
        item.sentimentScore > 0.1 -> EmeraldBullish
        item.sentimentScore < -0.1 -> RubyBearish
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeskCardBackground, RoundedCornerShape(10.dp))
            .border(1.dp, DeskBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("news_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.source,
                    color = CyanPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (item.symbol != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${item.symbol}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(sentimentColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "SENTIMENT ${if (item.sentimentScore >= 0) "+" else ""}${String.format("%.1f", item.sentimentScore)}",
                    color = sentimentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.summary,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = dateStr,
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CalendarCard(event: CalendarEvent) {
    val impactColor = when (event.impact) {
        "High" -> RubyBearish
        "Medium" -> AmberCaution
        else -> TextSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeskCardBackground, RoundedCornerShape(10.dp))
            .border(1.dp, DeskBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
            .testTag("calendar_event_${event.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(impactColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, impactColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${event.impact.uppercase()} IMPACT",
                        color = impactColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = event.category,
                    color = CyanPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = event.dateStr,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = event.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = event.details,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        if (event.forecast != null || event.previous != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                event.forecast?.let {
                    Text("Forecast: $it", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                event.previous?.let {
                    Text("Previous: $it", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
