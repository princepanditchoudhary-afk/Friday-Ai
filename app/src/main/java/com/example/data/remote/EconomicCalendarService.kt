package com.example.data.remote

import com.example.data.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EconomicCalendarService {

    fun getUpcomingEvents(): List<CalendarEvent> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)

        return listOf(
            CalendarEvent(
                id = "cal-fomc",
                title = "FOMC Interest Rate Decision & Press Conference",
                dateStr = sdf.format(Date(now + (2 * dayMs))),
                timestamp = now + (2 * dayMs),
                impact = "High",
                category = "Fed/Central Bank",
                actual = "5.50%",
                forecast = "5.25%",
                previous = "5.50%",
                details = "Federal Open Market Committee policy statement followed by Chair press conference."
            ),
            CalendarEvent(
                id = "cal-cpi",
                title = "US Consumer Price Index (CPI YoY)",
                dateStr = sdf.format(Date(now + (5 * dayMs))),
                timestamp = now + (5 * dayMs),
                impact = "High",
                category = "Macro",
                actual = null,
                forecast = "2.6%",
                previous = "2.9%",
                details = "Bureau of Labor Statistics measure of core and headline inflation pressures."
            ),
            CalendarEvent(
                id = "cal-nfp",
                title = "Non-Farm Payrolls & Unemployment Rate",
                dateStr = sdf.format(Date(now + (9 * dayMs))),
                timestamp = now + (9 * dayMs),
                impact = "High",
                category = "Macro",
                actual = null,
                forecast = "165K",
                previous = "142K",
                details = "Monthly labor market expansion and wage growth telemetry."
            ),
            CalendarEvent(
                id = "cal-nvda-earnings",
                title = "NVDA Quarterly Earnings Release",
                dateStr = sdf.format(Date(now + (14 * dayMs))),
                timestamp = now + (14 * dayMs),
                impact = "High",
                category = "Earnings",
                actual = null,
                forecast = "$0.68 EPS",
                previous = "$0.61 EPS",
                details = "Data center GPU demand, Blackwell architecture ramp, and margin commentary."
            ),
            CalendarEvent(
                id = "cal-options-exp",
                title = "Monthly Options Expiration (OpEx)",
                dateStr = sdf.format(Date(now + (18 * dayMs))),
                timestamp = now + (18 * dayMs),
                impact = "Medium",
                category = "Market Structure",
                actual = null,
                forecast = null,
                previous = null,
                details = "Monthly equity and index derivatives settlement. Heightened delta-hedging volatility expected."
            )
        )
    }
}
