package com.example.anurag.whereamidock;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.w3c.dom.Text;

public class Tracker extends Service {
    public static String LONGITUDE;
    public static String LATITUDE;
    public static String BROADCAST_LATITUDE = Tracker.class.getName() + "latitiudebroadcast";
    public static String BROADCAST_LONGITUDE = Tracker.class.getName() + "longitudebroadcast";
    Context trc_con = this;
    public static String STOP_SERVICE = "STOP_TRACKER_SERVICE";
    public boolean logSuccess = true;
    private static final String TAG = Tracker.class.getSimpleName();
    private FirebaseAuth firebaseAuth;
    public Tracker() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public void onCreate(){
        super.onCreate();
        showNotification();
        loginToFireBase();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(stopReceiverFromMain, new IntentFilter(STOP_SERVICE));
        return START_NOT_STICKY;
    }
    private void showNotification(){
        String stop = "stop";
        registerReceiver(stopReceiver, new IntentFilter(stop));
        PendingIntent broadcastIntent = PendingIntent.getBroadcast(this, 0, new Intent(stop), PendingIntent.FLAG_UPDATE_CURRENT);
        final String chanID = "CHANNEL_ID";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, chanID)
                    .setContentTitle("Tracking Enabled")
                    .setContentText("Logging GPS data to Firebase")
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentIntent(broadcastIntent)
                    .setSmallIcon(R.drawable.ic_check);
            startForeground(1, builder.build());

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(chanID, "constGPS", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(notificationChannel);
            }

    }
    public BroadcastReceiver stopReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            unregisterReceiver(stopReceiver);
            stopSelf();

        }
    };

    public BroadcastReceiver stopReceiverFromMain = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            unregisterReceiver(stopReceiverFromMain);
            stopSelf();

        }
    };
    private void loginToFireBase(){
        firebaseAuth = FirebaseAuth.getInstance();
        String email = "publicusr@mail.com" ;
        String password = "speakwater";
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                logSuccess = true;
                if(task.isSuccessful()){
                    requestLocationUpdates();
                }
                else{
                    Log.d(TAG, "Firebase Auth Failed");
                    logSuccess = false;
                }
            }
        });
        if(logSuccess){
            Toast.makeText(this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "Firebase authentication failed", Toast.LENGTH_SHORT).show();
        }
    }
    private void requestLocationUpdates(){
        LocationRequest request = new LocationRequest();
        request.setInterval(800);
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        final String path = getString(R.string.firebase_path);
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permission == PackageManager.PERMISSION_GRANTED){
            client.requestLocationUpdates(request, new LocationCallback(){
                @Override
                public void onLocationResult(LocationResult locationResult){
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference(path);
                    Location location = locationResult.getLastLocation();
                    if(location != null){
                        reference.setValue(location);
                        Intent latintent = new Intent(BROADCAST_LATITUDE);
                        latintent.putExtra(LATITUDE, location.getLatitude());
                        Intent longintent = new Intent(BROADCAST_LONGITUDE);
                        longintent.putExtra(LONGITUDE, location.getLongitude());
                        LocalBroadcastManager.getInstance(trc_con).sendBroadcast(latintent);
                        LocalBroadcastManager.getInstance(trc_con).sendBroadcast(longintent);
                    }
                }
            }, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Stopped Tracker", Toast.LENGTH_SHORT).show();
    }
}