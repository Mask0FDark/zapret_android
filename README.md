<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="96" alt="Zapret Android"/>

# Zapret Android

**Обход DPI-блокировок на Android без root и без удалённого VPN-сервера.**

<p>
  <a href="https://github.com/Mask0FDark/zapret_android/actions/workflows/android.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/Mask0FDark/zapret_android/android.yml?style=for-the-badge&labelColor=12151c&color=8B7FF5&label=build"></a>
  <img alt="Android" src="https://img.shields.io/badge/Android-5.0%2B-34D399?style=for-the-badge&labelColor=12151c&logo=android&logoColor=white">
  <img alt="Root" src="https://img.shields.io/badge/root-%D0%BD%D0%B5%20%D0%BD%D1%83%D0%B6%D0%B5%D0%BD-34D399?style=for-the-badge&labelColor=12151c">
  <a href="LICENSE"><img alt="GPLv3" src="https://img.shields.io/badge/license-GPLv3-F5A623?style=for-the-badge&labelColor=12151c"></a>
</p>

<p>
  <a href="https://github.com/Mask0FDark/zapret_android/actions"><img alt="Сборки APK" src="https://img.shields.io/badge/%D0%A1%D0%B1%D0%BE%D1%80%D0%BA%D0%B8%20APK-8B7FF5?style=for-the-badge&logo=githubactions&logoColor=white"></a>
  &nbsp;
  <a href="CHANGELOG.md"><img alt="Changelog" src="https://img.shields.io/badge/Changelog-12151c?style=for-the-badge&logo=git&logoColor=8B7FF5"></a>
</p>

Локальный обход: приложение перехватывает трафик через Android `VpnService`, передаёт его в локальный
TUN → SOCKS5-стек и применяет DPI-desync прямо на устройстве. В обычном режиме трафик не уходит
на наш VPN-сервер, а внешний IP не меняется.

Проект сделан в том же духе, что и [Zapret2UI](https://github.com/Asterlike/zapret2UI): минимум ручной
возни, готовые профили и отдельные расширенные настройки для тех случаев, когда автоматический вариант
не подходит конкретной сети.

</div>

> ⚠️ Сейчас проект в активной разработке. До первого стабильного релиза часть профилей и поведение на
> разных операторах ещё проверяются.

---

## Содержание

- [Что это](#что-это)
- [Быстрый старт](#быстрый-старт)
- [Профили](#профили)
- [Telegram и звонки](#telegram-и-звонки)
- [Как это работает](#как-это-работает)
- [Почему Android показывает VPN](#почему-android-показывает-vpn)
- [Если не работает](#если-не-работает)
- [Сборка](#сборка)
- [Благодарности](#благодарности)
- [Лицензия](#лицензия)

---

## Что это

На Windows Zapret2UI может работать через `winws2` и WinDivert. На Android без root такого доступа к
сетевому стеку нет, поэтому этот проект использует другой путь:

```text
Приложения Android
       │
       ▼
 Android VpnService
       │
       ▼
 hev-socks5-tunnel
       │
       ▼
 локальный ByeDPI
       │
       ▼
     Интернет
```

То есть значок VPN в статус-баре нужен для локального перехвата пакетов. Сам по себе он не означает,
что трафик отправляется на внешний VPN-сервер.

В основе Android-части используется [ByeDPIAndroid](https://github.com/dovecoteescapee/ByeDPIAndroid),
движок [ByeDPI](https://github.com/hufrea/byedpi) и
[hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel).

---

## Быстрый старт

После появления первой готовой сборки APK она будет доступна в **Releases**. Текущие тестовые сборки
собираются через **GitHub Actions**.

1. Установите APK.
2. Запустите приложение.
3. Для постоянной работы оставьте **Combo (recommended)**. Отдельные профили нужны в основном для диагностики.
4. Нажмите **Connect**.
5. При первом запуске Android попросит разрешить VPN-подключение — подтвердите.

После подключения приложение может работать в фоне; отдельный Quick Settings tile позволяет
включать и выключать обход без открытия главного окна.

---

## Профили

| Профиль | Для чего |
|---|---|
| **Combo (recommended)** | постоянный режим: Discord + YouTube + голос и безопасный fallback для остальных HTTPS-соединений |
| **Auto** | вмешивается в обычный HTTPS только после reset/timeout/TLS-ошибки |
| **Universal** | общий TCP-desync для HTTPS; агрессивнее Auto |
| **YouTube** | воздействует только на домены YouTube/Google Video |
| **Discord** | домены Discord + отдельная обработка UDP-диапазонов голосовых серверов |

**Combo** сделан именно для режима «включил и оставил»: обычный HTTPS не должен без причины получать
desync. Для сторонних сайтов fallback включается только после признаков блокировки — reset, timeout или
сломанный TLS handshake.

Если готовая стратегия не подходит конкретному оператору, в **Advanced settings** остаются ручные
параметры ByeDPI: split/disorder, fake-пакеты, TTL, TLS record split, UDP fake count, SNI и фильтры.

---

## Telegram

Telegram вынесен из DPI-профилей отдельно. Причина простая: его могут блокировать по IP, и тогда
попытка «лечить Telegram desync-ом» одновременно с Discord/YouTube только делает общий профиль менее
стабильным.

При включении **Telegram proxy** приложение поднимает локальный MTProto-прокси на
`127.0.0.1:1443`. Дальше соединения Telegram уходят через WebSocket/TLS и Cloudflare-fronted
домены. После запуска нажмите **Add MTProto to Telegram** и подтвердите добавление прокси в Telegram.

DPI-обход и Telegram-прокси работают независимо: **Combo** продолжает обслуживать Discord/YouTube,
а Telegram использует свой локальный MTProto → WebSocket путь.

Нативный Telegram-прокси основан на
[amurcanov/tg-ws-proxy-android](https://github.com/amurcanov/tg-ws-proxy-android),
который в свою очередь основан на проекте Flowseal.

---

## Как это работает

При HTTPS-соединении часть информации о начале соединения доступна сетевому оборудованию провайдера.
DPI использует её, чтобы определить сервис и применить правило блокировки или замедления.

ByeDPI меняет форму первых пакетов: дробит их, меняет порядок, добавляет ложные пакеты или использует
другие desync-приёмы. Сервер продолжает получать корректное соединение, а DPI видит другой набор
данных и может перестать распознавать трафик.

Android-версия делает это без root за счёт локального `VpnService`.

---

## Почему Android показывает VPN

Android не даёт обычному приложению перехватывать весь TCP/UDP-трафик напрямую. Официальный способ
сделать это без root — API `VpnService`.

Поэтому система показывает стандартный значок VPN, хотя в основном режиме:

- удалённый VPN-сервер не используется;
- аккаунт не требуется;
- внешний IP не подменяется;
- DPI-обработка выполняется локально;
- исходный трафик после обработки уходит через обычное подключение устройства.

---

## Если не работает

Первое, что стоит сделать — переключить профиль. Одинаковая стратегия не обязана работать у всех
операторов: DPI и правила фильтрации отличаются.

Для диагностики важно разделять три разных случая:

1. **Сайт/приложение работает без обхода, но медленно** — возможно, это троттлинг.
2. **Соединение ломается только при определённом протоколе** — нужна другая TCP/UDP-стратегия.
3. **IP вообще недоступен** — это уже не обычная DPI-блокировка, и desync может не помочь.

Для ручной настройки используйте **Advanced settings**, а при отчёте об ошибке прикладывайте модель
Android, версию системы, тип сети (Wi-Fi/мобильная) и описание того, что именно не работает.

---

## Сборка

Нужны **JDK 17**, **Android SDK 34**, **NDK 25.1.8937393** и **CMake 3.22.1**.

```bash
git clone --recurse-submodules https://github.com/Mask0FDark/zapret_android.git
cd zapret_android
./gradlew assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

CI-конфигурация находится в [`.github/workflows/android.yml`](.github/workflows/android.yml).

---

## Благодарности

- [dovecoteescapee/ByeDPIAndroid](https://github.com/dovecoteescapee/ByeDPIAndroid) — Android VpnService и интеграция TUN/SOCKS.
- [hufrea/byedpi](https://github.com/hufrea/byedpi) — DPI-desync движок.
- [heiher/hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel) — TUN → SOCKS5.
- [Asterlike/zapret2UI](https://github.com/Asterlike/zapret2UI) — подход к интерфейсу, профилям и диагностике.
- [bol-van/zapret2](https://github.com/bol-van/zapret2) — развитие техник DPI desync и документация по современным стратегиям.

Подробная информация о происхождении кода и лицензиях — в [NOTICE.md](NOTICE.md).

---

## Лицензия

Android-код проекта является производной работой от ByeDPIAndroid и распространяется по
**GNU GPL v3**. Смотрите [LICENSE](LICENSE).

Сторонние компоненты распространяются по собственным лицензиям; их уведомления должны сохраняться.
