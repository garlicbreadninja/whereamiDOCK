package com.example.anurag.whereamidock;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;

import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PermissionsListener, OnMapReadyCallback {
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private boolean serviceStartToaster = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final Context usbl_context = this;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoiYW5yZ2FrbGEiLCJhIjoiY2pzem4zMWswMDk2czRhb296ampkNGJibSJ9.6-8R4zurua3dX8nzUfJmHQ");
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        final Button button = findViewById(R.id.restartbutton);
        final Button endbutton = findViewById(R.id.close_btn);
        endbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(usbl_context);
                builder.setMessage("Are you sure you want to exit?");
                builder.setPositiveButton("Stop Service", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent endTrackIntent = new Intent();
                        endTrackIntent.setAction(Tracker.STOP_SERVICE);
                        sendBroadcast(endTrackIntent);
                    }
                })
                        .setNegativeButton("Exit App", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(0);
                            }
                        });
                builder.create();
                builder.show();
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startServiceTracker();
                Toast.makeText(usbl_context, getString(R.string.restart_toast), Toast.LENGTH_SHORT).show();
                serviceStartToaster = false;
            }
        });
        final TextView longi = (TextView) findViewById(R.id.longi);
        final TextView lat = (TextView) findViewById(R.id.lat);
        final TextView label = (TextView) findViewById(R.id.label);
        label.setText("Coordinates: ");
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double longitude = intent.getDoubleExtra(Tracker.LONGITUDE, 0);
                longi.setText("" + longitude + ", ");
            }
        }, new IntentFilter(Tracker.BROADCAST_LONGITUDE));
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double latitude = intent.getDoubleExtra(Tracker.LATITUDE, 0);
                lat.setText(latitude + "");
            }
        }, new IntentFilter(Tracker.BROADCAST_LATITUDE));
    }
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUrl("mapbox://styles/anrgakla/cjkxglohw0dvy2so94sefcksm"), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocation(style);
            }
        });
    }
    public void enableLocation(@NonNull Style style){
        if((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) == PackageManager.PERMISSION_GRANTED) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                    .setSmallIcon(R.drawable.ic_check)
                    .setContentTitle("Gotcha")
                    .setContentText("Permissions Obtained. Locating Now.")
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Permissions Obtained. Locating Now."))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
            builder.build();
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(this, style);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
            startServiceTracker();
        }
        else{
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }

    }
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, "Tap on allow to send GPS data", Toast.LENGTH_LONG).show();
    }

    @Override
        public void onPermissionResult(boolean granted){
        if(granted){
            mapboxMap.setStyle(new Style.Builder().fromUrl(""), new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocation(style);
                }
            });
        }
        else{
            Toast.makeText(this, "Tap on allow to send GPS data", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void startServiceTracker(){
       startService(new Intent(this, Tracker.class));
       if(serviceStartToaster) Toast.makeText(this, "Service: Tracker -> Started", Toast.LENGTH_SHORT).show();
       Toast.makeText(this, "Sending GPS data to Firebase.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
