# План: Zygisk diagnostic path

## Зачем меняется доставка кода

Обычный LSPosed scope не попадает в Chrome `sandboxed_process`. Это видно на
устройстве: entry point запускается в основном и privileged-процессах, а
renderer создаётся отдельно с isolated UID. Следующий этап требует native
payload, который Zygisk загружает при specialization процесса.

Android APK остаётся companion-приложением: позже он будет хранить правила и
сертификаты. Magisk ZIP отвечает только за загрузку native кода. На первом
этапе этот код ничего не меняет в TLS.

## Этапы

1. **Сборка и упаковка.**
   Собрать `.so` для `arm64-v8a` и `armeabi-v7a`; получить Magisk ZIP. Текущий
   payload — Zygisk diagnostic module с фильтром Chrome, без hook'ов.

2. **Проверить инъекцию.**
   Добавить `zygisk::ModuleBase`, отфильтровать только `com.android.chrome` и
   писать package, process name, UID и ABI в logcat. Проверка: после открытия
   Chrome есть запись из основного, privileged и sandboxed процессов.

3. **Найти verifier.**
   Не менять результат. В отдельном compatibility-модуле определить реальный
   Java или native entry point Chrome для конкретной версии. Проверять на
   `expired.badssl.com` и `self-signed.badssl.com`: браузер обязан показать
   штатную ошибку, а hook — исходный статус.

4. **Добавить конфигурацию.**
   APK публикует immutable snapshot через узкий IPC-канал. Native payload
   читает только snapshot; direct access к SharedPreferences из sandboxed
   процесса не допускается. В кэше остаются распарсенные CA и verifier'ы.

5. **Реализовать selective trust.**
   Только после подтверждения точки hook: сопоставление домена с label boundary
   и IDN-normalization, затем custom CA только для `NO_TRUSTED_ROOT`. Ошибки
   срока действия, SAN, EKU, pinning и неизвестные статусы не меняются.

6. **Проверить rollback.**
   Отключение модуля или ошибка IPC/compatibility возвращают штатное поведение
   Chrome. Публичные сертификаты не должны заходить в custom verifier.

## Команды

```bash
# APK companion и unit tests
./gradlew :app:testDebugUnitTest

# native payload
ANDROID_NDK_HOME=/path/to/ndk ./gradlew buildZygiskNative

# Magisk ZIP
ANDROID_NDK_HOME=/path/to/ndk ./gradlew packageZygiskDebug
```

ZIP создаётся в `build/distributions/`. Его можно установить только для этапа 2:
payload пишет process name и UID, но не меняет TLS.
