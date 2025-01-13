package com.owlvation.project.genedu.Tool.Compass;


import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.owlvation.project.genedu.Network.NetworkChangeReceiver;
import com.owlvation.project.genedu.R;


public class CompassActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager SensorManage;
    private ImageView compassimage;
    private float DegreeStart = 0f;
    TextView DegreeTV;
    ImageView icBack;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        icBack = findViewById(R.id.ic_back);

        compassimage = (ImageView) findViewById(R.id.compass_image);
        DegreeTV = findViewById(R.id.degreeTV);
        SensorManage = (SensorManager) getSystemService(SENSOR_SERVICE);
        networkChangeReceiver = new NetworkChangeReceiver();
        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        SensorManage.unregisterListener(this);
        unregisterReceiver(networkChangeReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SensorManage.registerListener(this, SensorManage.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);

        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float degree = Math.round(event.values[0]);
        DegreeTV.setText(Float.toString(degree) + getString(R.string.degrees));
        RotateAnimation ra = new RotateAnimation(
                DegreeStart,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setFillAfter(true);
        ra.setDuration(210);
        compassimage.startAnimation(ra);
        DegreeStart = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }


}