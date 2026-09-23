# Changelog

Все заметные изменения проекта фиксируются здесь.

## [0.2.0] - 2026-09-23

### Добавлено
- опциональный Telegram calls relay через Cloudflare WARP/MASQUE;
- локальный SOCKS5 для Telegram на `127.0.0.1:1081`;
- автоматическая регистрация WARP после явного подтверждения условий;
- соединение WARP через локальный ByeDPI-прокси, чтобы сам MASQUE-handshake тоже проходил DPI;
- кнопка для добавления SOCKS5 в Telegram.

### Изменено
- foreground-service запускается до длительных сетевых операций, чтобы Android не убивал запуск VPN;
- CI собирает и упаковывает `usque` для Android ABI.

## [0.1.0] - 2026-09-23

### Добавлено
- первый публичный Android-релиз;
- rootless-режим на базе Android VpnService;
- локальная цепочка TUN → SOCKS5 → ByeDPI;
- профили Combo, Auto, Universal, YouTube, Discord и Telegram;
- безопасный always-on профиль Combo: целевые домены Discord/YouTube, UDP-диапазоны Discord voice, известные подсети Telegram и fallback только после признаков блокировки;
- UDP-desync в голосовых профилях;
- простой главный экран и отдельные расширенные настройки;
- Quick Settings tile;
- GitHub Actions для автоматической сборки APK.

### Изменено
- отдельный package id `com.mask0fdark.zapret2ui`;
- интерфейс и название адаптированы под Zapret Android;
- сохранены расширенные параметры и режим локального прокси из базового Android-проекта.
