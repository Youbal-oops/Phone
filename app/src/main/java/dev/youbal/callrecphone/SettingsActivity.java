package dev.youbal.callrecphone;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import dev.youbal.callrec.lib.CallRecorderSetup;
import dev.youbal.callrec.lib.RecordingStore;
import java.io.File;
import java.util.Locale;

/** Setup (five steps with ticks), recording options, how the app works, and problem reports. */
public final class SettingsActivity extends Activity {
    private static final int REQ_PERMISSIONS = 19, REQ_ROLE = 12, REQ_SHIZUKU = 71;
    private PhoneApp app;
    private LinearLayout content;

    @Override protected void onCreate(Bundle state) { super.onCreate(state); Ui.init(this); app = PhoneApp.get(this); }
    @Override protected void onResume() { super.onResume(); build(); }
    @Override protected void onActivityResult(int request, int result, Intent data) { super.onActivityResult(request, result, data); build(); }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) { super.onRequestPermissionsResult(request, permissions, grants); app.reloadNames(null); build(); }

    private int dp(float n) { return Ui.dp(this, n); }
    private LinearLayout.LayoutParams lp(int w, int h, float weight) { return new LinearLayout.LayoutParams(w, h, weight); }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private boolean has(String permission) { return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED; }
    private boolean isDefaultPhone() { return DefaultPhone.held(this); }
    private String[] permissions() {
        return Build.VERSION.SDK_INT >= 33
            ? new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}
            : new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO};
    }
    private boolean permissionsDone() { for (String p : permissions()) if (!has(p)) return false; return true; }

    private void build() {
        Ui.init(this);
        LinearLayout root = Ui.column(this); root.setBackgroundColor(Ui.BG); root.setPadding(dp(16), dp(8), dp(16), dp(8));
        if (Build.VERSION.SDK_INT >= 30) root.setOnApplyWindowInsetsListener((v, insets) -> { android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars()); v.setPadding(dp(16) + bars.left, dp(8) + bars.top, dp(16) + bars.right, dp(8) + bars.bottom); return insets; });
        else root.setOnApplyWindowInsetsListener((v, i) -> { v.setPadding(dp(16) + i.getSystemWindowInsetLeft(), dp(8) + i.getSystemWindowInsetTop(), dp(16) + i.getSystemWindowInsetRight(), dp(8) + i.getSystemWindowInsetBottom()); return i; });
        setContentView(root); root.requestApplyInsets();
        LinearLayout header = Ui.row(this);
        header.addView(Ui.icon(this, "Back", Ui.INK, v -> finish()), lp(dp(48), dp(48)));
        header.addView(Ui.title(this, "Settings", 22), lp(0, -2, 1)); root.addView(header, lp(-1, dp(56)));
        ScrollView scroll = new ScrollView(this); scroll.setVerticalScrollBarEnabled(false); content = Ui.column(this); scroll.addView(content); root.addView(scroll, lp(-1, 0, 1));
        setupCard(); recordingCard(); helpCard(); reportCard(); aboutCard();
    }

    private void step(LinearLayout card, int n, boolean done, String title, String text) {
        TextView t = Ui.title(this, n + "   " + title + (done ? "   ✓" : ""), 16); t.setTextColor(done ? Ui.OK : Ui.INK); t.setPadding(0, dp(10), 0, 0); card.addView(t); card.addView(Ui.text(this, text, 13, Ui.MUTED));
    }

    // ---- setup -------------------------------------------------------------------------------------------------------
    private void setupCard() {
        LinearLayout card = Ui.card(this); content.addView(card); card.addView(Ui.title(this, "Setup", 18));
        CallRecorderSetup.State shizuku = CallRecorderSetup.state(this);
        boolean running = shizuku == CallRecorderSetup.State.NEEDS_PERMISSION || shizuku == CallRecorderSetup.State.READY;
        step(card, 1, running, "Install and start Shizuku", "Lets the app record both sides of a call. Developer options → Wireless debugging → pair with the code in Shizuku → Start. Start it again after every reboot. Skip it to record with the microphone only.");
        card.addView(Ui.button(this, shizuku == CallRecorderSetup.State.NOT_INSTALLED ? "Install Shizuku" : "Open Shizuku", !running, v -> startActivity(CallRecorderSetup.shizukuIntent(this))));
        card.addView(Ui.button(this, "Shizuku setup guide", false, v -> startActivity(CallRecorderSetup.guideIntent())));

        step(card, 2, permissionsDone(), "Allow restricted settings and permissions", "App info → ⋮ (top right) → Allow restricted settings. Then allow the permissions.");
        card.addView(Ui.button(this, "Open app info", false, v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))));
        card.addView(Ui.button(this, permissionsDone() ? "Permissions allowed ✓" : "Allow permissions", !permissionsDone(), v -> requestPermissions(permissions(), REQ_PERMISSIONS)));

        boolean isDefault = isDefaultPhone();
        step(card, 3, isDefault, "Set as default phone app", "Gives you the call screen and lets the app see your calls.");
        card.addView(Ui.button(this, isDefault ? "Default phone app ✓" : "Set as default", !isDefault, v -> {
            if (isDefault) { startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)); return; }
            Intent ask = DefaultPhone.request(this);
            if (ask != null) startActivityForResult(ask, REQ_ROLE);
            else Toast.makeText(this, "This phone does not offer the phone-app role.", Toast.LENGTH_LONG).show();
        }));

        boolean ready = shizuku == CallRecorderSetup.State.READY;
        step(card, 4, ready, "Authorise in Shizuku", "Allow the app when Shizuku asks. Recording then works on its own.");
        card.addView(Ui.button(this, ready ? "Authorised ✓" : "Authorise in Shizuku", !ready, v -> {
            if (CallRecorderSetup.state(this) == CallRecorderSetup.State.NOT_RUNNING || CallRecorderSetup.state(this) == CallRecorderSetup.State.NOT_INSTALLED) Toast.makeText(this, "Start Shizuku first (step 1).", Toast.LENGTH_LONG).show();
            else CallRecorderSetup.requestPermission(REQ_SHIZUKU);
        }));

        boolean tested = !new RecordingStore(app.recordingsDir()).list().isEmpty();
        step(card, 5, tested, "Make a test call", "Call someone for a few seconds. The recording appears in the Recordings tab.");
    }

    // ---- recording ---------------------------------------------------------------------------------------------------
    private void recordingCard() {
        LinearLayout card = Ui.card(this); content.addView(card); card.addView(Ui.title(this, "Recording", 18));
        boolean on = app.prefs.recording();
        card.addView(Ui.text(this, on ? "On. Calls with one person are recorded once they connect." : "Off. Nothing is recorded.", 14, on ? Ui.OK : Ui.MUTED));
        card.addView(Ui.button(this, on ? "Turn recording off" : "Turn recording on", !on, v -> { app.prefs.recording(!on); build(); }));
        String[] modes = {"AUTO", "SHIZUKU", "MIC"}, modeNames = {"Automatic: both sides with Shizuku, else the microphone", "Shizuku only: both sides", "Microphone only: needs speakerphone"};
        int current = 0; for (int i = 0; i < modes.length; i++) if (modes[i].equals(app.prefs.mode())) current = i;
        card.addView(Ui.button(this, "Source: " + modeNames[current].split(":")[0], false, v -> Ui.sheet(this, "Recording source", modeNames, indexOf(modes, app.prefs.mode()), n -> { app.prefs.mode(modes[n]); build(); }).show()));
        int[] days = {0, 7, 30, 90}; String[] dayNames = {"Keep forever", "Delete after 7 days", "Delete after 30 days", "Delete after 90 days"};
        int keep = 0; for (int i = 0; i < days.length; i++) if (days[i] == app.prefs.keepDays()) keep = i;
        card.addView(Ui.button(this, dayNames[keep], false, v -> Ui.sheet(this, "Keep recordings", dayNames, indexOf(days, app.prefs.keepDays()), n -> { app.prefs.keepDays(days[n]); app.io.execute(() -> new RecordingStore(app.recordingsDir()).deleteOlderThan(days[n], System.currentTimeMillis())); build(); }).show()));
        RecordingStore store = new RecordingStore(app.recordingsDir()); int count = store.list().size(); long bytes = store.totalBytes();
        card.addView(Ui.text(this, count + (count == 1 ? " recording" : " recordings") + " · " + (bytes < 1048576 ? Math.max(0, bytes / 1024) + " KB" : String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0)), 13, Ui.MUTED));
        card.addView(Ui.text(this, "Saved in this app's own folder on the phone. Nothing is uploaded.", 13, Ui.MUTED));
        String status = app.prefs.lastStatus(); if (!status.isEmpty()) card.addView(Ui.text(this, "Last: " + status, 13, Ui.MUTED));
        card.addView(Ui.text(this, "Recording calls may need everyone's consent where you live. You are responsible for following the law.", 12, Ui.WARN));
    }
    private static int indexOf(String[] values, String value) { for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i; return 0; }
    private static int indexOf(int[] values, int value) { for (int i = 0; i < values.length; i++) if (values[i] == value) return i; return 0; }

    // ---- help, problems, about ---------------------------------------------------------------------------------------
    private void helpCard() {
        LinearLayout card = Ui.card(this); content.addView(card); card.addView(Ui.title(this, "How it works", 18));
        String[][] items = {
            {"Recents", "Your calls, with names. Tap a row to call back. Search any name or number."},
            {"Contacts", "People saved in Google, on the phone or on the SIM. Tap one for numbers and a note."},
            {"Dialpad", "Type or paste a number. Saved names show as suggestions."},
            {"Call screen", "Shows who is calling, where they are saved, your last call and your note. Answer, Decline, Silence, Mute, Speaker, Hold, Keypad, End."},
            {"Notes", "A short note per number. Tap the note on the call screen or in a contact."},
            {"Recording", "Connected calls with one person record by themselves. Stop it from the call screen."},
            {"Recordings", "Play, share or delete. Old ones can delete themselves."},
            {"Something broke", "Settings → Problem report → Copy."}};
        for (String[] item : items) { TextView t = Ui.title(this, item[0], 15); t.setPadding(0, dp(10), 0, 0); card.addView(t); card.addView(Ui.text(this, item[1], 13, Ui.MUTED)); }
    }
    private void reportCard() {
        String problem = CrashLog.read(this); if (problem.isEmpty()) return;
        LinearLayout card = Ui.card(this); content.addView(card); card.addView(Ui.title(this, "Problem report", 18));
        card.addView(Ui.text(this, "Saved on this phone only. Copy it when you report a bug.", 13, Ui.MUTED));
        TextView trace = Ui.text(this, problem.length() > 1200 ? problem.substring(0, 1200) + "…" : problem, 11, Ui.MUTED); trace.setTextIsSelectable(true); card.addView(trace);
        card.addView(Ui.button(this, "Copy report", true, v -> { getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Callrec Phone report", problem)); Toast.makeText(this, "Copied.", Toast.LENGTH_SHORT).show(); }));
        card.addView(Ui.button(this, "Clear report", false, v -> { CrashLog.clear(this); build(); }));
    }
    private void aboutCard() {
        LinearLayout card = Ui.card(this); content.addView(card); card.addView(Ui.title(this, "About", 18));
        String version = ""; try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (PackageManager.NameNotFoundException ignored) { /* Shown without a version. */ }
        card.addView(Ui.text(this, "Callrec Phone " + version + ". Open source. No account, no ads, no tracking: your calls, contacts and recordings never leave the phone.", 13, Ui.MUTED));
        card.addView(Ui.text(this, "Call recording by callrec. Uses Shizuku (Apache-2.0) when you start it.", 13, Ui.MUTED));
        card.addView(Ui.button(this, "Source code", false, v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Youbal-oops/Phone")))));
    }
}
