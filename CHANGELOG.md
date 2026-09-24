# Changelog

Все заметные изменения проекта фиксируются здесь.

## [0.3.1] - 2026-09-24

### Исправлено
- усилен обход Discord: отдельная host-scoped стратегия для API, gateway, CDN и Cloudflare ECH;
- расширен список доменов Discord;
- Discord и YouTube теперь используют разные TCP-профили внутри Combo, чтобы агрессивный Discord-desync не применялся к YouTube;
- сохранён отдельный UDP-профиль Discord voice.


## [0.3.0] - 2026-09-24

### Исправлено
- удалён отдельный DPI-профиль Telegram, который мог ломать Discord и другой трафик;
- сохранённый старый профиль Telegram теперь автоматически откатывается на **Combo (recommended)**;
- Telegram больше не смешивается с общей DPI-стратегией.

### Изменено
- экспериментальный WARP/MASQUE relay заменён на локальный MTProto → WebSocket прокси на `127.0.0.1:1443`;
- Telegram-прокси использует Cloudflare-fronted WebSocket маршруты и работает отдельно от ByeDPI;
- кнопка Telegram теперь добавляет MTProto-прокси, а не SOCKS5;
- CI больше не собирает и не пакует `usque`.

## [0.2.0] - 2026-09-23

### Добавлено
- экспериментальный Telegram calls relay через Cloudflare WARP/MASQUE;
- локальный SOCKS5 для Telegram на `127.0.0.1:1081`;
- диагностический вывод ошибок запуска VPN.

### Изменено
- foreground-service запускается до длительных сетевых операций, чтобы Android не убивал запуск VPN.

## [0.1.0] - 2026-09-23

### Добавлено
- первый публичный Android-релиз;
- rootless-режим на базе Android VpnService;
- локальная цепочка TUN → SOCKS5 → ByeDPI;
- профили Combo, Auto, Universal, YouTube и Discord;
- безопасный always-on профиль Combo для Discord/YouTube и fallback после признаков блокировки;
- UDP-desync в голосовых профилях;
- простой главный экран и отдельные расширенные настройки;
- Quick Settings tile;
- GitHub Actions для автоматической сборки APK.

### Изменено
- отдельный package id `com.mask0fdark.zapret2ui`;
- интерфейс и название адаптированы под Zapret Android;
- сохранены расширенные параметры и режим локального прокси из базового Android-проекта.
