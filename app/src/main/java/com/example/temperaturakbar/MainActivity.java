package com.example.temperaturakbar;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.temperaturakbar.BroadCastReceiverServices.BroadCastService;
import com.example.temperaturakbar.DatabaseHelpers.LightSensorDatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    LinearLayout lightSensorPage, about;
    SensorManager sensorManager;
    Sensor lightSensor;
    SensorEventListener lightListener;
    private LightSensorDatabaseHelper lightSensorDatabaseHelper;
    long timeLeftInMilliseconds = 30000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, BroadCastService.class));

        lightSensorPage = findViewById(R.id.lightSensorPageId);
        about = findViewById(R.id.aboutId);

        lightSensorPage.setOnClickListener(this);
        about.setOnClickListener(this);

        lightSensorDatabaseHelper = new LightSensorDatabaseHelper(this);

        sensorManager = (SensorManager) getSystemService(Service.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update GUI
            updateGUI(intent);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        onSensorChangedMethod();
        registerReceiver(broadcastReceiver, new IntentFilter(BroadCastService.COUNTDOWN_BR));
//        startTimer();
        // set registerListener for each 4 sensors in sensorManager
        sensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        // set unregisterListener for each 4 sensors when app is paused
        sensorManager.unregisterListener(lightListener);
    }

    @Override
    public void onStop() {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Sensors cannot detect", Toast.LENGTH_SHORT).show();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, BroadCastService.class));
        super.onDestroy();
    }

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            long millisUntilFinished = intent.getLongExtra("countdown", 30000);
            int seconds = (int) (millisUntilFinished / 1000);

            if(seconds<0){
                long temp2 = lightSensorDatabaseHelper.countRows();
                for(long i=1; i<=temp2; i++){
                    lightSensorDatabaseHelper.deleteData(String.valueOf(i));
                }
            }
        }
    }

    // store values of 4 sensors in SQLite database
    public void onSensorChangedMethod() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss a");
        String timeData = simpleDateFormat.format(calendar.getTime());

        // Light sensor detection code >>>>>>>>>>
        lightListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
                    // Show result in "lux" unit
                    lightSensorDatabaseHelper.insertData(timeData, String.valueOf(event.values[0]));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.aboutId){
            Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        }


        if(v.getId()==R.id.lightSensorPageId){
            Intent intent = new Intent(getApplicationContext(), LightSensorActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            finish();
        }

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder;
        alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("EXIT !");
        alertDialogBuilder.setMessage("Are you sure you want to close this app ?");
        alertDialogBuilder.setIcon(R.drawable.exit);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                System.exit(0);
            }
        });

        alertDialogBuilder.setNeutralButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
