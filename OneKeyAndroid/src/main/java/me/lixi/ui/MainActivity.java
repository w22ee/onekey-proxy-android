package me.lixi.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.flexbox.FlexboxLayout;
import com.kyleduo.switchbutton.SwitchButton;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import me.lixi.R;
import me.lixi.core.LocalVpnService;
import me.lixi.store.SPUtils;

public class MainActivity extends Activity implements
        LocalVpnService.onStatusChangedListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CONFIG_URL_KEY = "CONFIG_URL_KEY";
    private static final int START_VPN_SERVICE_REQUEST_CODE = 1985;
    private static String GL_HISTORY_LOGS;
    private SwitchButton switchProxy;
    private TextView textViewLog;
    private ScrollView scrollViewLog;
    private Calendar mCalendar;
    private TextView ipChangeTips;
    private LinearLayout setttingLL;
    private EditText proxyIpAddEt;
    private EditText proxyIpPortEt;
    private EditText proxyAppEt;
    private FlexboxLayout accountGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollViewLog = (ScrollView) findViewById(R.id.scrollViewLog);
        textViewLog = (TextView) findViewById(R.id.textViewLog);
        proxyIpAddEt = (EditText) findViewById(R.id.proxy_ip_add_et);
        setttingLL = (LinearLayout) findViewById(R.id.setting_ll);
        proxyIpPortEt = (EditText) findViewById(R.id.proxy_ip_port_et);
        proxyAppEt = (EditText) findViewById(R.id.proxy_app_et);
        ipChangeTips = (TextView) findViewById(R.id.ip_change_tips);
        switchProxy = (SwitchButton) findViewById(R.id.proxy_switch);
        accountGroup = (FlexboxLayout) findViewById(R.id.account_group);
        mCalendar = Calendar.getInstance();


        ipChangeTips.setText("");
        textViewLog.setText(GL_HISTORY_LOGS);
        scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN);

        showAccountHistory();
        LocalVpnService.addOnStatusChangedListener(this);
        switchProxy.setChecked(LocalVpnService.IsRunning);
        switchProxy.setOnClickListener(this);
        switchProxy.setOnCheckedChangeListener(this);

        if (!TextUtils.isEmpty(readConfigUrl())) {
            proxyIpAddEt.setText(readConfigUrl());
        }
        System.out.println("oncreate vpn");
        duelIntent(getIntent());
    }

    private void showAccountHistory() {
        List<String> accountHistory = SPUtils.getAccountInfo(this);
        int size = accountHistory == null ? 0 : accountHistory.size();
        accountGroup.setVisibility(size > 0 ? View.VISIBLE : View.GONE);
        accountGroup.removeAllViews();
        for (int i = 0; i < size; i++) {
            final String content = accountHistory.get(i);
            View view = LayoutInflater.from(this).inflate(R.layout.item_account_layout, null);
            accountGroup.addView(view);
            ((TextView) view.findViewById(R.id.name)).setText(content);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!LocalVpnService.IsRunning) {
                        proxyIpAddEt.setText(content);
                    } else {
                        Toast.makeText(MainActivity.this, "关闭代理后，才能修改IP", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    String readConfigUrl() {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        return preferences.getString(CONFIG_URL_KEY, "");
    }

    void setConfigUrl(String configUrl) {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString(CONFIG_URL_KEY, configUrl);
        editor.apply();
    }

    boolean isValidUrl(String url) {
        try {
            if (url == null || url.isEmpty())
                return false;

            if (url.startsWith("/")) {//file path
                File file = new File(url);
                if (!file.exists()) {
                    onLogReceived(String.format("File(%s) not exists.", url));
                    return false;
                }
                if (!file.canRead()) {
                    onLogReceived(String.format("File(%s) can't read.", url));
                    return false;
                }
            } else { //url
                Uri uri = Uri.parse(url);
                if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))
                    return false;
                if (uri.getHost() == null)
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onLogReceived(String logString) {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        logString = String.format("[%1$02d:%2$02d:%3$02d] %4$s\n",
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                mCalendar.get(Calendar.SECOND),
                logString);

        System.out.println(logString);

        if (textViewLog.getLineCount() > 200) {
            textViewLog.setText("");
        }
        textViewLog.append(logString);
        scrollViewLog.fullScroll(ScrollView.FOCUS_DOWN);
        GL_HISTORY_LOGS = textViewLog.getText() == null ? "" : textViewLog.getText().toString();
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        switchProxy.setEnabled(true);
        switchProxy.setChecked(isRunning);
        onLogReceived(status);
        if (isRunning) {
//            SPUtils.saveAccountInfo(proxyIpAddEt.getEditableText().toString(), this);
            showAccountHistory();
        }
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }


    private void setEditAble(boolean editAble) {
        proxyIpAddEt.setEnabled(editAble);
        proxyIpPortEt.setEnabled(editAble);
        proxyAppEt.setEnabled(editAble);
    }


    private void startVPNService() {
        String configUrl = proxyIpAddEt.getText().toString();
        String prot = proxyIpPortEt.getText().toString();

        if (TextUtils.isEmpty(prot)) {
            prot = "8888";
        }

        String withoutPort = new String(configUrl);

        if (!configUrl.startsWith("http")) {
            configUrl = "http://" + configUrl + ":" + prot;
        }

        if (!isValidUrl(configUrl)) {
            Toast.makeText(this, R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
            switchProxy.post(new Runnable() {
                @Override
                public void run() {
                    switchProxy.setChecked(false);
                    switchProxy.setEnabled(true);
                }
            });
            return;
        }

        textViewLog.setText("");
        GL_HISTORY_LOGS = null;
        onLogReceived("starting...");
        LocalVpnService.ConfigUrl = configUrl;
        LocalVpnService.ListenPackageName = proxyAppEt.getText().toString();
        setConfigUrl(withoutPort);
        startService(new Intent(this, LocalVpnService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPNService();
            } else {
                switchProxy.setChecked(false);
                switchProxy.setEnabled(true);
                onLogReceived("canceled.");
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onDestroy() {
        LocalVpnService.removeOnStatusChangedListener(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        System.out.println("onNewIntent vpn");
        duelIntent(intent);
    }

    private void duelIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String pcIp = intent.getStringExtra("pc_ip");
        if (pcIp != null) {
            ipChangeTips.setText("pc ip is get " + pcIp);
            proxyIpAddEt.setText(pcIp);
            switchProxy.setChecked(true);
            if (LocalVpnService.IsRunning == false) {
                System.out.println("intent start vpn");
                startProxy();
            }
        } else {
            boolean close = intent.getBooleanExtra("close", false);
            if (close) {
                LocalVpnService.IsRunning = false;
            }
        }


    }

    private void startProxy() {
        Intent intent = LocalVpnService.prepare(this);
        if (intent == null) {
            startVPNService();
        } else {
            startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
        }
    }

    @Override
    public void onClick(View v) {

        boolean isChecked = switchProxy.isChecked();
        System.out.println("onCheckedclick  vpn is" + isChecked);


        if (LocalVpnService.IsRunning != isChecked) {
            if (isChecked) {
                System.out.println("onCheckedChanged start vpn");
                startProxy();
            } else {
                //关闭vpn
                LocalVpnService.IsRunning = false;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setEditAble(!isChecked);
    }
}
