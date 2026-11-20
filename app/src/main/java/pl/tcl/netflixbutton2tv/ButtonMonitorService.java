package pl.tcl.netflixbutton2tv;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ButtonMonitorService extends Service {
    static volatile boolean running = false;

    private static final String CHANNEL = "button_monitor";
    private static final int NOTIFICATION_ID = 4062;
    private static final long DEBOUNCE_MS = 1100;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean stopping;
    private volatile Process logcat;
    private Thread readerThread;
    private long lastPressElapsed;
    private long generation;

    @Override public void onCreate() {
        super.onCreate();
        running = true;
        createChannel();
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Netflix → Telewizja")
                .setContentText("Monitor przycisku Netflix aktywny")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Config.enabled(this) || !Config.hasLogPermission(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        stopping = false;
        if (readerThread == null || !readerThread.isAlive()) {
            readerThread = new Thread(new Runnable() {
                @Override public void run() { monitorLoop(); }
            }, "TclNetflixLogReader");
            readerThread.start();
        }
        Log.i(Config.LOG_TAG, "Monitor uruchomiony; czekam na klawisz 4062");
        return START_STICKY;
    }

    private void monitorLoop() {
        while (!stopping && Config.enabled(this)) {
            Process process = null;
            try {
                // -T 1: nie odczytujemy całej historii bufora, tylko nasłuchujemy nowych wpisów.
                process = new ProcessBuilder("logcat", "-b", "main", "-v", "brief",
                        "-T", "1", "-s", "WindowManager:D", "*:S")
                        .redirectErrorStream(true)
                        .start();
                logcat = process;
                BufferedReader lines = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while (!stopping && (line = lines.readLine()) != null) {
                    if (line.contains("interceptKeyTi keyCode=4062 down=true")
                            && line.contains("repeatCount=0")) {
                        final long observedAt = SystemClock.elapsedRealtime();
                        handler.post(new Runnable() {
                            @Override public void run() { onNetflixKey(observedAt); }
                        });
                    }
                }
                lines.close();
                if (!stopping) Log.w(Config.LOG_TAG, "Proces logcat zakończył pracę");
            } catch (IOException e) {
                if (!stopping) Log.e(Config.LOG_TAG, "Nie mogę czytać logcat", e);
                Config.save(this, Config.LAST_RESULT, "Błąd logcat: " + e.getMessage());
            } finally {
                if (process != null) process.destroy();
                logcat = null;
            }
            if (!stopping) SystemClock.sleep(2500);
        }
    }

    private void onNetflixKey(long observedAt) {
        if (stopping || !Config.enabled(this)) return;
        if (observedAt - lastPressElapsed < DEBOUNCE_MS) return;
        lastPressElapsed = observedAt;
        final long thisPress = ++generation;
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        Config.save(this, Config.LAST_EVENT, time + " (4062)");
        Log.i(Config.LOG_TAG, "Wykryto przycisk Netflix 4062");

        // Usługa TCL nadal uruchomi Netflix: po krótkiej zwłoce
        // przenosimy TV na wierzch; druga próba neutralizuje późny start Netflixa.
        scheduleTvLaunch(thisPress, 500);
        scheduleTvLaunch(thisPress, 1800);
    }

    private void scheduleTvLaunch(final long thisPress, long delayMs) {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                if (stopping || !Config.enabled(ButtonMonitorService.this)
                        || generation != thisPress) return;
                try {
                    Config.launchTv(ButtonMonitorService.this);
                    Log.i(Config.LOG_TAG, "Uruchomiono TCL TVActivity");
                    Config.save(ButtonMonitorService.this, Config.LAST_RESULT,
                            "TVActivity: polecenie wysłane");
                } catch (RuntimeException e) {
                    Log.e(Config.LOG_TAG, "Nie można otworzyć TVActivity", e);
                    Config.save(ButtonMonitorService.this, Config.LAST_RESULT,
                            "Błąd TVActivity: " + e.getMessage());
                }
            }
        }, delayMs);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                "Przekierowanie przycisku Netflix", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Usługa nasłuchująca kodu 4062 lokalnie w dzienniku Android TV");
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    @Override public void onDestroy() {
        stopping = true;
        running = false;
        ++generation;
        handler.removeCallbacksAndMessages(null);
        Process p = logcat;
        if (p != null) p.destroy();
        Thread t = readerThread;
        if (t != null) t.interrupt();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
