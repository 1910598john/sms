package com.example.smsserver;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyService extends Service {
    private Handler handler = new Handler();
    private long messageId = 1;

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://system.billingko.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    MyApi myApi = retrofit.create(MyApi.class);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();

        scheduleTask();
        return START_STICKY;
    }



    private void startForegroundService() {
        String channelId = "sms_service_channel";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "SMS Service",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }

            Intent notificationIntent = new Intent(this, MainPage.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new Notification.Builder(this, channelId)
                    .setContentTitle("SMS Server is running in the background!")
                    .setContentText("Tap to open")
                    .setSmallIcon(R.drawable.smartphone)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void test(){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("09673071955", null, "chat ka kon may nareceive ka", null, null);
        Intent smsSentIntent = new Intent("SMS_SENT");
        sendBroadcast(smsSentIntent);
    }

    private void scheduleTask() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {

                    try {
                        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                        String ss = preferences.getString("smsserver", "");
                        String savedParent = preferences.getString("parent", "");

                        if (ss.equals("stopped")) {
                            Intent intent = new Intent(MyService.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            stopSelf();
                            startActivity(intent);
                        } else {
                            if (isNetworkAvailable()) {
                                test();
                                //startSmsService(savedParent);
                                handler.postDelayed(this, 5000);
                            } else {
                                checkConnection(savedParent);
                            }
                        }

                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "SMS permission not granted. Please allow SMS permission.", Toast.LENGTH_LONG).show();
                }
            }
        }, 2500);
    }

    private void checkConnection(String parent) {
        if (!isNetworkAvailable()) {
            Log.e("SMS Server", "No network available, retrying...");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkConnection(parent);
                }
            }, 1000);
        } else {
            scheduleTask();
        }
    }

    public void sendSms(Context context, String phoneNumber, String message, int id) {
        if (!message.isEmpty()) {
            SmsManager smsManager = SmsManager.getDefault();

            PendingIntent sentIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent("SMS_SENT"),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            PendingIntent deliveryIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent("SMS_DELIVERED"), // Renamed to a more descriptive action
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);

            Toast.makeText(getApplicationContext(), "SMS Sent!", Toast.LENGTH_LONG).show();

            // Insert into the SMS content provider
            ContentValues values = new ContentValues();
            values.put("address", phoneNumber);
            values.put("body", message);
            values.put("date", System.currentTimeMillis());
            values.put("read", 0); // 0 means unread
            values.put("type", 2); // 2 means sent message

            Uri uri = getContentResolver().insert(Uri.parse("content://sms/sent"), values);

            // Delete the SMS from the server
            Call<ResponseBody> call2 = myApi.deleteSMS(new DeleteSMS(id));
            call2.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Log.e("ID", "Deleted SMS with ID: " + id);
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("Error", t.toString());
                }
            });

        } else {
            Log.e("SMS", "Message is empty, cannot send.");
        }
    }


    public void startSmsService(String parent) {
        Call<ResponseBody> call = myApi.getSms(new Sms(parent));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        Integer id = json.getInt("id");
                        String receiver = json.getString("receiver");
                        String message = json.getString("message");

                        Log.d("SMS Server", "Fetched SMS: " + message + " to " + receiver);
                        sendSms(MyService.this, receiver, message, id);

                    } catch (IOException | JSONException e) {
                        Log.e("Error", "Error parsing response: " + e.getMessage());
                    }
                } else {
                    sendSms(MyService.this, "09673071955", "test", 1);
                }

                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("update");
                editor.apply();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("SMS Server", "Server cannot be reached! " + t.getMessage());
            }
        });
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("smsserver", "stopped");
        editor.apply();
        Toast.makeText(getApplicationContext(), "SMS Server has stopped!", Toast.LENGTH_LONG).show();
        Log.e("SMS Server", "Service destroyed!");

        super.onDestroy();
    }
}
