package com.example.smsserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;


public class MainPage extends AppCompatActivity{
    LottieAnimationView lottieSMS;
    Runnable runnable;
    TextView label;
    boolean animationPlayed;
    boolean lottieUpdated = false;
    boolean serverDown = false;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 0 ;

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                lottieSMS = findViewById(R.id.lottieAnimationView);

                lottieSMS.playAnimation();
                animationPlayed = false;

                label = (TextView) findViewById(R.id.textView2);
                label.setText("SMS Server is running..");
                label.setTextColor(Color.GREEN);

                checkConnection();
            }

        }, 2000);


        if (!preferences.contains("username")) {
            Intent intent = new Intent(MainPage.this, MainActivity.class);
            startActivity(intent);
        } else {
            label = (TextView) findViewById(R.id.textView2);
            label.setText("SMS Server is running..");
            label.setTextColor(Color.GREEN);

            String ss = preferences.getString("smsserver", "");
            SharedPreferences.Editor editor = preferences.edit();
            if (ss.equals("stopped")){
                editor.remove("smsserver");
                editor.apply();
                if (!isServiceRunning(getApplicationContext(), MyService.class)) {
                    startService(new Intent(getApplication(), MyService.class));
                }
            }
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.SEND_SMS)) {
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.SEND_SMS},
                            MY_PERMISSIONS_REQUEST_SEND_SMS);
                }
            }
        }
    }

    public void checkConnection(){
        Handler handler = new Handler();
        lottieSMS = findViewById(R.id.lottieAnimationView);

        runnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    if (isNetworkAvailable()){
                        if (preferences.contains("update")) {
                            label = (TextView) findViewById(R.id.textView2);
                            lottieSMS.setAnimation(R.raw.server_down);

                            if (lottieSMS.isAnimating()) {
                                lottieSMS.cancelAnimation();
                            } else {
                                lottieSMS.playAnimation();
                            }
                            label.setText("Unable to send sms..");
                            label.setTextColor(Color.RED);
                            serverDown = true;

                        } else {
                            serverDown = false;
                            label = (TextView) findViewById(R.id.textView2);
                            label.setText("SMS Server is running..");
                            label.setTextColor(Color.GREEN);
                            if (lottieSMS.isAnimating() && animationPlayed){
                                lottieSMS.cancelAnimation();
                                animationPlayed = true;
                            } else {
                                try{
                                    if (lottieUpdated || !serverDown) {
                                        lottieSMS.setAnimation(R.raw.sms);
                                        lottieUpdated = false;
                                    }
                                    if (animationPlayed){
                                        lottieSMS.cancelAnimation();
                                    } else {
                                        lottieSMS.playAnimation();
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }

                    } else {
                        serverDown = false;
                        label = (TextView) findViewById(R.id.textView2);
                        label.setText("No internet connection..");
                        label.setTextColor(Color.parseColor("#FFA500"));

                        try{
                            if (!lottieUpdated || !serverDown) {
                                lottieSMS.setAnimation(R.raw.no_wifi);
                                lottieUpdated = true;
                            }

                            if (animationPlayed){
                                lottieSMS.cancelAnimation();
                            } else {
                                lottieSMS.playAnimation();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (animationPlayed){
                            lottieSMS.cancelAnimation();
                        }
                    }

                handler.postDelayed(this, 5000);
            }
        };
        handler.post(runnable);
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        if (preferences.contains("username")) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!isServiceRunning(getApplicationContext(), MyService.class)) {
                        startService(new Intent(getApplication(), MyService.class));
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "SMS failed, please try again.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public void stopSMSServer(View view) {

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        label = (TextView) findViewById(R.id.textView2);
        label.setText("SMS Server has stopped!");
        label.setTextColor(Color.RED);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("smsserver","stopped");
        editor.remove("username");
        editor.remove("password");
        editor.apply();

        Toast.makeText(getApplicationContext(), "SMS Server has stopped!", Toast.LENGTH_LONG).show();
        Log.e("SMS Server", "SMS SERVER HAS STOPPED!");
    }
}
