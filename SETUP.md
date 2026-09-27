# Setup & Configuration Guide

## Prerequisites
- Android SDK (API 36, minimum SDK 24)
- JDK 17 or 21
- Gradle 8.x

## Secrets & API Key Configuration
1. In Google AI Studio, set your `GEMINI_API_KEY` in the Secrets panel.
2. The key is automatically injected at build time into `BuildConfig.GEMINI_API_KEY` via `.env`.
3. If using an OpenAI-compatible provider (e.g. OpenRouter, Groq, or OpenAI), open the in-app **System Settings** tab and provide your custom endpoint and API key.

## Switching Between Live Mode and Demo Mode
- Tap the **LIVE / DEMO** indicator pill in the top app bar or toggle it in **System Settings**.
- **Live Mode**: Uses genuine real-time market data from public exchange feeds. Unavailable data renders as `—`.
- **Demo Mode**: Clearly displays an orange banner `DEMO MODE — SIMULATED DATA` for offline demonstration.
