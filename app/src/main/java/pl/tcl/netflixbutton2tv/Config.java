package pl.tcl.netflixbutton2tv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class Config {
    static final String PREFS = "settings";
    static final String ENABLED = "enabled";
    static final String LAST_EVENT = "last_event";
    static final String LAST_RESULT = "last_result";
    static final String LOG_TAG = "NetflixToTV";

    private Config() { }

    static boolean hasLogPermission(Context ctx) {
        return Build.VERSION.SDK_INT >= 23
                && ctx.checkSelfPermission("android.permission.READ_LOGS")
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean enabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(ENABLED, false);
    }

    static void setEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(ENABLED, enabled).apply();
    }

    static void save(Context ctx, String field, String text) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(field, text).apply();
    }

    static void launchTv(Context context) {
        RuntimeException firstError;
        try {
            context.startActivity(tvIntent());
            return;
        } catch (RuntimeException e) {
            firstError = e;
        }

        // Na niektórych wersjach firmware TVActivity nie jest eksportowana
        // dla zwykłych aplikacji. Spróbuj oficjalnego wejścia do aplikacji TV.
        Intent fallback = context.getPackageManager()
                .getLeanbackLaunchIntentForPackage("com.tcl.tv");
        if (fallback == null) {
            fallback = context.getPackageManager()
                    .getLaunchIntentForPackage("com.tcl.tv");
        }
        if (fallback != null) {
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            fallback.putExtra("targetSource", "tv");
            context.startActivity(fallback);
            return;
        }
        throw firstError;
    }

    static Intent tvIntent() {
        Intent intent = new Intent("tcl.sys.intent.action.TV");
        intent.setClassName("com.tcl.tv", "com.tcl.tv.TVActivity");
        intent.putExtra("targetSource", "tv");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
}
