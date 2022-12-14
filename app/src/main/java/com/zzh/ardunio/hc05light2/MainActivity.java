package com.zzh.ardunio.hc05light2;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.zzh.ardunio.hc05light2.ui.main.PlaceholderFragment;
import com.zzh.ardunio.hc05light2.ui.main.SectionsPagerAdapter;
import com.zzh.ardunio.hc05light2.databinding.ActivityMainBinding;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements BLESPPUtils.OnBluetoothAction{

    @SuppressLint("StaticFieldLeak")
    public static BLESPPUtils mBLESPPUtils;
    private ArrayList<BluetoothDevice> mDevicesList = new ArrayList<>();
    private DeviceDialogCtrl mDeviceDialogCtrl;

    boolean first_flag = true;
    public static final String PREFS_NAME = "com.zzh.ardunio.hc05light2.color";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.zzh.ardunio.hc05light2.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //Toast.makeText(MainActivity.this,tab.getPosition()+"",Toast.LENGTH_LONG).show();
                switch (tab.getPosition()) {
                    case PlaceholderFragment.Page_Mode_Automatic - 1:
                        MainActivity.BLsend("!");
                        break;
                    case PlaceholderFragment.Page_Mode_Setting - 1:
                        PlaceholderFragment.sendColor();
                        if(first_flag) {
                            PlaceholderFragment.binding2.radioButton2.setChecked(true);
                            first_flag = false;
                        }
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "????????????", Snackbar.LENGTH_LONG).show();
//                mBLESPPUtils.connect("00:21:13:00:71:c3");
//                Toast.makeText(MainActivity.this,"????????????",Toast.LENGTH_LONG);
                mDeviceDialogCtrl.show();
            }
        });


        initPermissions();
        mBLESPPUtils = new BLESPPUtils(MainActivity.this, this);
        mBLESPPUtils.setEnableLogOut();
        mBLESPPUtils.setStopString("\r\n");
        if (!mBLESPPUtils.isBluetoothEnable()) mBLESPPUtils.enableBluetooth();
        mBLESPPUtils.onCreate();
        mDeviceDialogCtrl = new DeviceDialogCtrl(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLESPPUtils.onDestroy();
    }

    private void initPermissions() {
        if (ContextCompat.checkSelfPermission(this, "android.permission-group.LOCATION") != 0) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            "android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.ACCESS_COARSE_LOCATION",
                            "android.permission.ACCESS_WIFI_STATE"},
                    1
            );
        }
    }

    @Override
    public void onFoundDevice(BluetoothDevice device) {
        // ????????????????????????
        for (int i = 0; i < mDevicesList.size(); i++) {
            if (mDevicesList.get(i).getAddress().equals(device.getAddress())) return;
        }
        // ?????????????????????????????????
        mDevicesList.add(device);
        // ??????????????? UI ?????????????????????
        mDeviceDialogCtrl.addDevice(device, new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                BluetoothDevice clickDevice = (BluetoothDevice) v.getTag();
                postShowToast("????????????:" + clickDevice.getName());
//                mLogTv.setText(mLogTv.getText() + "\n" + "????????????:" + clickDevice.getName());
                mBLESPPUtils.connect(clickDevice);
            }
        });
    }

    @Override
    public void onConnectSuccess(BluetoothDevice device) {
        postShowToast("????????????", new DoSthAfterPost() {
            @SuppressLint("SetTextI18n")
            @Override
            public void doIt() {
//                mLogTv.setText(
//                        mLogTv.getText() + "\n????????????:" + device.getName() + " | " + device.getAddress()
//                );
                mDeviceDialogCtrl.dismiss();
            }
        });
    }

    @Override
    public void onConnectFailed(String msg) {
        postShowToast("????????????:" + msg);
    }

    @Override
    public void onReceiveBytes(byte[] bytes) {
        Log.e("BLE","Receiving----->"+new String(bytes));
    }

    @Override
    public void onSendBytes(byte[] bytes) {
        Log.e("BLE","Sending----->"+new String(bytes));
    }

    @Override
    public void onFinishFoundDevice() {}

    /**
     * ???????????????????????????
     */
    private class DeviceDialogCtrl {
        private LinearLayout mDialogRootView;
        private ProgressBar mProgressBar;
        private AlertDialog mConnectDeviceDialog;

        DeviceDialogCtrl(Context context) {
            // ???????????????
            mProgressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            mProgressBar.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            50
                    )
            );

            // ?????????
            mDialogRootView = new LinearLayout(context);
            mDialogRootView.setOrientation(LinearLayout.VERTICAL);
            mDialogRootView.addView(mProgressBar);
            mDialogRootView.setMinimumHeight(700);

            // ????????????
            ScrollView scrollView = new ScrollView(context);
            scrollView.addView(mDialogRootView,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            700
                    )
            );

            // ???????????????
            mConnectDeviceDialog = new AlertDialog
                    .Builder(context)
                    .setNegativeButton("??????", null)
                    .setPositiveButton("??????", null)
                    .create();
            mConnectDeviceDialog.setTitle("???????????????????????????");
            mConnectDeviceDialog.setView(scrollView);
            mConnectDeviceDialog.setCancelable(false);
        }

        /**
         * ???????????????????????????
         */
        void show() {
            mBLESPPUtils.startDiscovery();
            mConnectDeviceDialog.show();
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mConnectDeviceDialog.dismiss();
                    return false;
                }
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mConnectDeviceDialog.dismiss();
                    finish();
                }
            });
            mConnectDeviceDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDialogRootView.removeAllViews();
                    mDialogRootView.addView(mProgressBar);
                    mDevicesList.clear();
                    mBLESPPUtils.startDiscovery();
                }
            });
        }

        /**
         * ???????????????
         */
        void dismiss() {
            mConnectDeviceDialog.dismiss();
        }

        /**
         * ???????????????????????????
         * @param device ??????
         * @param onClickListener ????????????
         */
        private void addDevice(final BluetoothDevice device, final View.OnClickListener onClickListener) {
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    TextView devTag = new TextView(MainActivity.this);
                    devTag.setClickable(true);
                    devTag.setPadding(20,20,20,20);
                    devTag.setBackgroundResource(R.drawable.rect_round_button_ripple);
                    devTag.setText(device.getName() + "\nMAC:" + device.getAddress());
                    devTag.setTextColor(Color.WHITE);
                    devTag.setOnClickListener(onClickListener);
                    devTag.setTag(device);
                    devTag.setLayoutParams(
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                    );
                    ((LinearLayout.LayoutParams) devTag.getLayoutParams()).setMargins(
                            20, 20, 20, 20);
                    mDialogRootView.addView(devTag);
                }
            });
        }
    }
    private void postShowToast(final String msg) {
        postShowToast(msg, null);
    }

    private void postShowToast(final String msg, final DoSthAfterPost doSthAfterPost) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                if (doSthAfterPost != null) doSthAfterPost.doIt();
            }
        });
    }

    private interface DoSthAfterPost {
        void doIt();
    }
    public static void BLsend(String str){
        mBLESPPUtils.send(str.getBytes());
    }
}