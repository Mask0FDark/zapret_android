# Changelog

Все заметные изменения проекта фиксируются здесь.

## [0.1.0] - 2026-09-23

### Добавлено
- первый публичный Android-релиз;
- rootless-режим на базе Android VpnService;
- локальная цепочка TUN → SOCKS5 → ByeDPI;
- профили Auto, Universal, YouTube, Discord и Telegram;
- UDP-desync в голосовых профилях;
- простой главный экран и отдельные расширенные настройки;
- Quick Settings tile;
- GitHub Actions для автоматической сборки APK.

### Изменено
- отдельный package id `com.mask0fdark.zapret2ui`;
- интерфейс и название адаптированы под Zapret2UI Android;
- сохранены расширенные параметры и режим локального прокси из базового Android-проекта.
