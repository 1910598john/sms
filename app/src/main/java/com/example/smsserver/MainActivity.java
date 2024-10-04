package com.example.smsserver;

import static android.Manifest.permission.READ_PHONE_STATE;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Retrofit;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import org.json.JSONException;
import org.json.JSONObject;
import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_SMS;
import android.Manifest;

public class MainActivity extends AppCompatActivity{
    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://system.billingko.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private String phoneNumber;
    private static final int REQUEST_SEND_SMS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);





        //development purposes
        /*
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("username", "test");
        editor.putString("password", "test");
        editor.apply();

        Intent intent = new Intent(MainActivity.this, MainPage.class);
        startActivity(intent);

         */

        //till here

        if (ActivityCompat.checkSelfPermission(this, READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, READ_PHONE_NUMBERS) ==
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // Permission check

            // Create obj of TelephonyManager and ask for current telephone service
            TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            String phoneNum = telephonyManager.getLine1Number();
            phoneNumber = phoneNum;
        } else {
            // Ask for permission
            requestPermission();
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(getApplicationContext(), "No internet connection!", Toast.LENGTH_LONG).show();
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{READ_SMS, READ_PHONE_NUMBERS, READ_PHONE_STATE}, 100);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    REQUEST_SEND_SMS);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 100:
                TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
                if (ActivityCompat.checkSelfPermission(this, READ_SMS) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                String phoneNum = telephonyManager.getLine1Number();
                phoneNumber = phoneNum;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }

    public void disableButton(ProgressBar pbar, Button button){
        pbar.setVisibility(View.VISIBLE);
        button.setEnabled(false);
        button.setAlpha(0.5f);
    }

    public void submit(View view) {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
//                == PackageManager.PERMISSION_GRANTED) {

//            SmsManager smsManager = SmsManager.getDefault();
//            smsManager.sendTextMessage("15555215554", null, "Hello, world!", null, null);
//        } else {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.SEND_SMS},
//                    REQUEST_SEND_SMS);
//
//        }

        ProgressBar pbar = (ProgressBar) findViewById(R.id.progressBar);

        System.out.println(phoneNumber);
        if (!isNetworkAvailable()) {
            Toast.makeText(getApplicationContext(), "Not internet connection!", Toast.LENGTH_LONG).show();
        }
        MyApi myApi = retrofit.create(MyApi.class);

        EditText username = findViewById(R.id.editTextText);
        EditText password = findViewById(R.id.editTextText2);

        if (username.getText().toString().isEmpty()) {
            username.setError("This field is empty!");
        }
        if (password.getText().toString().isEmpty()) {
            password.setError("This field is empty!");
        }

        if (!username.getText().toString().equals("") && !password.getText().toString().equals("")) {
            Button button = (Button) findViewById(R.id.button);
            disableButton(pbar, button);
            Call<ResponseBody> call = myApi.loginUser(new LoginRequest(username.getText().toString(), password.getText().toString(), phoneNumber, "beta"));
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()){
                        try{
                            String res = response.body().string();
                            JSONObject json = new JSONObject(res);
                            String message = (String) json.get("message");
                            String toast = "";
                            if (message.equals("success")) {
                                toast = "Logged in!";
                                Integer parent = (Integer) json.get("username");
                                SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("username", username.getText().toString());
                                editor.putString("password", password.getText().toString());
                                editor.putString("parent",String.valueOf(parent));
                                editor.apply();
                                Intent intent = new Intent(MainActivity.this, MainPage.class);
                                startActivity(intent);
                            } else if (message.equals("unauthorized")) {
                                toast = "User unauthorized!";
                            } else if (message.equals("phoneerror")) {
                                toast = "Phone number not recognised!";
                            }else if (message.equals("invalid_login")) {
                                toast = "Invalid credentials!";
                            } else if (message.equals("update_required")) {
                                toast = "Update required!";
                            }
                            pbar.setVisibility(View.GONE);
                            button.setAlpha(1f);
                            button.setEnabled(true);
                            Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show();
                        } catch (IOException | JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    if (isNetworkAvailable()) {
                        Toast.makeText(getApplicationContext(), "Server cannot be reached!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
}
