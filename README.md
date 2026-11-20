# TCL Netflix Button → Telewizja (Android TV 9)

Aplikacja do przekierowania fizycznego przycisku **Netflix** (kod **4062**) na telewizję naziemną w TCL 55EP640 / BeyondTV, Android TV 9.

**Netflix pozostaje zainstalowany i normalnie dostępny z ekranu aplikacji.** Aplikacja nie modyfikuje APK Netflixa, plików systemowych, systemowej usługi Realtek ani ustawień pilotów.

## Działanie i ograniczenia

- Usługa foreground odczytuje wyłącznie logi z tagiem `WindowManager`, szukając `interceptKeyTi keyCode=4062 down=true repeatCount=0`.
- Po wykryciu naciśnięcia uruchamia `com.tcl.tv/com.tcl.tv.TVActivity` z akcją `tcl.sys.intent.action.TV` i `targetSource=tv`.
- Uruchamia TV po około 0,5 s i ponownie po około 1,8 s, ponieważ TCL niezależnie wysyła do Netflixa chroniony broadcast. Przy przełączaniu może być widoczny ekran Netflixa. Nie da się zagwarantować beztestowego przechwycenia klawisza w 100%.
- Normalne otwarcie Netflixa z listy aplikacji **nie generuje kodu 4062** i nie powinno uruchamiać TV.
- Działa lokalnie i nie używa internetu. `READ_LOGS` to szerokie uprawnienie pozwalające aplikacji widzieć logi urządzenia; projekt nie zapisuje ani nie wysyła ich treści, tylko znacznik naciśnięcia i wynik otwarcia TV.
- Usługa jest aktywna w tle z niewielkim stałym powiadomieniem. Automatyczne uruchomienie po restarcie zależy również od polityki TCL dotyczącej usług w tle.
- Przeznaczona dla **Android TV 9 (API 28)** i konkretnego wzorca logów; nie jest uniwersalnym remapperem pilota.

## Zbudowanie APK przez GitHub Actions

1. Utwórz nowe repozytorium na GitHub i wgraj **zawartość** tego projektu (razem z `.github/workflows/build-apk.yml`).
2. Wejdź w **Actions** → **Build Android TV APK** → **Run workflow**. Ewentualnie workflow uruchomi się przy `push` na `main`.
3. Pobierz artifact **TCL-Netflix-Button-To-TV-APK** i rozpakuj go, aby uzyskać `app-debug.apk`.

Można też zbudować lokalnie w Android Studio (Java 17, Android SDK API 35), wybierając **Build APK(s)**. Projekt nie wymaga zewnętrznych bibliotek AndroidX.

## Instalacja na telewizorze

W CMD na komputerze z ADB (`C:\platform-tools`):

```bat
adb install -r app-debug.apk
adb shell pm grant pl.tcl.netflixbutton2tv android.permission.READ_LOGS
adb shell am start -n pl.tcl.netflixbutton2tv/.MainActivity
```

W aplikacji wybierz **Włącz przekierowanie**. Najpierw przetestuj **Test: otwórz telewizję**. Potem naciśnij **fizyczny przycisk Netflix**.

**Ważne:** polecenie `pm grant` musi wykonać się pomyślnie. Samo `adb logcat` na komputerze nie dowodzi, że aplikacja ma `READ_LOGS`.

## Diagnostyka

Sprawdź, co raportuje aplikacja przy pierwszym teście, a następnie w CMD:

```bat
adb logcat -d -s NetflixToTV:I
adb shell dumpsys package pl.tcl.netflixbutton2tv | findstr READ_LOGS
```

Szukaj `Wykryto przycisk Netflix 4062` i `Uruchomiono TCL TVActivity`.

Jeśli monitor nie startuje po restarcie, otwórz aplikację. Opcjonalnie sprawdź ustawienia oszczędzania energii. Nie wyłączaj `KeyCustomFunctionService` ani Netflixa.

## Wyłączenie / cofnięcie

Wybierz **Wyłącz przekierowanie** w aplikacji. Netflix nie był modyfikowany. Ewentualnie usuń aplikację:

```bat
adb uninstall pl.tcl.netflixbutton2tv
```

## Kod źródłowy

- `app/src/main/java/pl/tcl/netflixbutton2tv/Config.java` — akcja otwierająca TV, konfiguracja
- `app/src/main/java/pl/tcl/netflixbutton2tv/ButtonMonitorService.java` — odczyt logcat i przekierowanie
- `app/src/main/java/pl/tcl/netflixbutton2tv/BootReceiver.java` — start po restarcie
- `app/src/main/java/pl/tcl/netflixbutton2tv/MainActivity.java` — ekran statusu i test

Projekt jest wersją testową dopasowaną do przekazanych logów TCL 55EP640. Nie był jeszcze uruchomiony na fizycznym telewizorze użytkownika.
