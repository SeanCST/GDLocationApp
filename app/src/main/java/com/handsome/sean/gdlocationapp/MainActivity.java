package com.handsome.sean.gdlocationapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import org.json.JSONException;
import org.json.JSONObject;

//import com.handsome.sean.gdlocationapp.

public class MainActivity extends AppCompatActivity {

    private TextView locationTextView;
    private Button reportButton;
    private Switch autoReportSwitch;
    private Switch locationReportSwitch;
    private Switch speedReportSwitch;
    private Switch timestampReportSwitch;

    private Intent mIntent;
    private AMapLocation lastAmapLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = (TextView)findViewById(R.id.location_tv);
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){//未开启定位权限
            //开启定位权限,200是标识码
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},200);
        } else {
            //声明AMapLocationClient类对象
            AMapLocationClient mLocationClient = null;
            //初始化定位
            mLocationClient = new AMapLocationClient(getApplicationContext());

            //初始化AMapLocationClientOption对象
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
            //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            mLocationOption.setSensorEnable(true);
            mLocationOption.setLocationCacheEnable(false);
            //给定位客户端对象设置定位参数
            mLocationClient.setLocationOption(mLocationOption);

            //异步获取定位结果
            AMapLocationListener mAMapLocationListener = new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation amapLocation) {
                    Log.d("STATE", "onLocationChanged");
                    if (amapLocation != null) {
                        if (amapLocation.getErrorCode() == 0) {
                            // 解析定位结果
                            try {
                                showLocation(amapLocation);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };

            //设置定位回调监听
            mLocationClient.setLocationListener(mAMapLocationListener);
            //启动定位
            mLocationClient.startLocation();
        }

        reportButton = findViewById(R.id.button_report);
        reportButton.setOnClickListener(onReportButtonClickListener);

        autoReportSwitch = findViewById(R.id.switch_autoReport);
        locationReportSwitch = findViewById(R.id.switch_location);
        speedReportSwitch = findViewById(R.id.switch_speed);
        timestampReportSwitch = findViewById(R.id.switch_timestamp);


        mIntent = new Intent(MainActivity.this, MyMqttService.class);
        //开启服务
        startService(mIntent);
    }

    private void showLocation(AMapLocation amapLocation) throws JSONException {
        lastAmapLocation = amapLocation;
//        Toast.makeText(this, "onLocationChanged", Toast.LENGTH_SHORT).show();

        String result = "纬度： " + amapLocation.getLatitude() + "\n经度： " + amapLocation.getLongitude() + "\n速度： " + amapLocation.getSpeed() + " m/s";
//        Log.d("STATE", result);
        locationTextView.setText(result);

        if (autoReportSwitch.isChecked()) {
            reportCurrentInfo(amapLocation);
        }
    }

    private void reportCurrentInfo(AMapLocation amapLocation) throws JSONException  {
        // 按下后， 通过 MQTT 上报(设置界面的内容 + 本地时间戳 + 手机当前位置, 组合成 JSON 字符串)

        if (amapLocation == null) {
            amapLocation = lastAmapLocation;
        }

        JSONObject jsonObject = new JSONObject();
        if (locationReportSwitch.isChecked()) {
            jsonObject.put("latitude", amapLocation.getLatitude());
            jsonObject.put("longitude", amapLocation.getLongitude());
        }

        if (speedReportSwitch.isChecked()) {
            jsonObject.put("speed", amapLocation.getSpeed());
        }

        if (timestampReportSwitch.isChecked()) {
            long timeStamp = System.currentTimeMillis();
            jsonObject.put("timeStamp", String.valueOf(timeStamp).substring(0, 10));
        }

        MyMqttService.publish(jsonObject.toString());
    }

    private View.OnClickListener onReportButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("STATE", "onReportButtonClickListener —— locationEnable: " + locationReportSwitch.isChecked() + " speedEnable: " + speedReportSwitch.isChecked() + " timestampEnable: " + timestampReportSwitch.isChecked());

            try {
                reportCurrentInfo(null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止服务
        stopService(mIntent);
    }
}
