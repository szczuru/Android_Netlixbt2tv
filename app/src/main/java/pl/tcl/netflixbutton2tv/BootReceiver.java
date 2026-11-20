package pl.tcl.netflixbutton2tv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!Config.enabled(context) || !Config.hasLogPermission(context)) return;
        Intent svc = new Intent(context, ButtonMonitorService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        } catch (RuntimeException e) {
            Log.e(Config.LOG_TAG, "Nie mozna wystartowac po boot", e);
        }
    }
}
