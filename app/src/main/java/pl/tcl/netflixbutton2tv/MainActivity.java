package pl.tcl.netflixbutton2tv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView status;
    private TextView diagnostics;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(19, 29, 45));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(44, 35, 44, 35);
        column.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(column);

        TextView title = label("Netflix → Telewizja", 28, Color.WHITE);
        column.addView(title);
        TextView intro = label("Przycisk Netflix uruchamia telewizję naziemną. "
                + "Netflix pozostaje dostępny z menu aplikacji.", 17, Color.LTGRAY);
        column.addView(intro);

        status = label("", 18, Color.WHITE);
        column.addView(status);

        Button enable = button("Włącz przekierowanie");
        enable.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!Config.hasLogPermission(MainActivity.this)) {
                    toast("Najpierw nadaj READ_LOGS przez ADB (patrz README).");
                    refresh();
                    return;
                }
                Config.setEnabled(MainActivity.this, true);
                Intent svc = new Intent(MainActivity.this, ButtonMonitorService.class);
                if (Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(svc);
                } else {
                    startService(svc);
                }
                refresh();
                toast("Monitor uruchomiony");
            }
        });
        column.addView(enable);

        Button disable = button("Wyłącz przekierowanie");
        disable.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Config.setEnabled(MainActivity.this, false);
                stopService(new Intent(MainActivity.this, ButtonMonitorService.class));
                refresh();
                toast("Monitor wyłączony; Netflix bez zmian");
            }
        });
        column.addView(disable);

        Button test = button("Test: otwórz telewizję");
        test.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Config.launchTv(MainActivity.this);
                } catch (RuntimeException ex) {
                    toast("Nie udało się otworzyć TV: " + ex.getMessage());
                }
            }
        });
        column.addView(test);

        Button refresh = button("Odśwież diagnostykę");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { refresh(); }
        });
        column.addView(refresh);

        diagnostics = label("", 15, Color.LTGRAY);
        column.addView(diagnostics);
        setContentView(scroll);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 9;
        b.setLayoutParams(lp);
        return b;
    }

    private TextView label(String text, int size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setPadding(0, 9, 0, 10);
        return v;
    }

    private void refresh() {
        if (status == null || diagnostics == null) return;
        boolean allowed = Config.hasLogPermission(this);
        boolean enabled = Config.enabled(this);
        status.setText("READ_LOGS: " + (allowed ? "NADANE" : "BRAK UPRAWNIENIA")
                + "\nPrzekierowanie: " + (enabled ? "WŁĄCZONE" : "WYŁĄCZONE")
                + "\nMonitor: " + (ButtonMonitorService.running ? "DZIAŁA" : "NIE DZIAŁA"));
        android.content.SharedPreferences prefs = getSharedPreferences(Config.PREFS, MODE_PRIVATE);
        diagnostics.setText("Ostatni wykryty klawisz: "
                + prefs.getString(Config.LAST_EVENT, "brak")
                + "\nOstatnie uruchomienie TV / błąd: "
                + prefs.getString(Config.LAST_RESULT, "brak")
                + "\n\nWymagane uprawnienie ADB:\n"
                + "adb shell pm grant pl.tcl.netflixbutton2tv android.permission.READ_LOGS"
                + "\n\nPrzy pierwszym naciśnięciu może na chwilę pojawić się Netflix.");
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
