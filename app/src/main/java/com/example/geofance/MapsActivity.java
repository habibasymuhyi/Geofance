package com.example.geofance;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.Toast;

import com.example.geofance.area.IOnLoadLocationListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GeoQueryEventListener, TextToSpeech.OnInitListener, IOnLoadLocationListener {

    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker currentUser;
    private DatabaseReference myLocationRef;
    private SupportMapFragment mapFragment;
    private GeoFire geofire;
    private List<LatLng> area;
    private IOnLoadLocationListener listener;
    private DatabaseReference kotaku;
    private Location lastLocation;
    private  GeoQuery geoQuery;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Voc.init(getApplicationContext());

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                        buildLocationRequest();
                        builLocationCallback();
                        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);

                        initArea();
                        settingGeofire();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "Anda Harus Izin Permisi", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();

    }
    private TextToSpeech myTTS;   // Define the TTS objecy
    private int MY_DATA_CHECK_CODE = 0;

    //write the following code in oncreate method or whereever you want to use this




    private void speakWords(String speech) {
        //speak straight away
        //myTTS.setLanguage(Locale.US);
        System.out.println(speech + " TTSTTTS");
        myTTS.speak(speech, TextToSpeech.LANG_COUNTRY_AVAILABLE, null);
    }

    public void onInit(int status) {
        // TODO Auto-generated method stub
        if (status == TextToSpeech.SUCCESS) {
            if(myTTS.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE)
                myTTS.setLanguage(Locale.US);
        } else if (status == TextToSpeech.ERROR) {
            Toast.makeText(getApplicationContext(), "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }
    }
    private void initArea() {
        kotaku = FirebaseDatabase.getInstance()
                .getReference("Zona Area")
                .child("kotaku");
        listener = this;
        //load from firebase

        /*kotaku.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<MyLatLng> latLngList = new ArrayList<>();
                        for(DataSnapshot locationSnapshot:dataSnapshot.getChildren())
                        {
                            MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                            latLngList.add(latLng);
                        }
                        listener.onLoadLocationSuccess(latLngList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onLoadLocationFailed(databaseError.getMessage());
                    }
                });
                */
        kotaku.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //update daftar zona area
                List<MyLatLng> latLngList = new ArrayList<>();
                for(DataSnapshot locationSnapshot:dataSnapshot.getChildren())
                {
                    MyLatLng latLng = locationSnapshot.getValue(MyLatLng.class);
                    latLngList.add(latLng);
                }
                listener.onLoadLocationSuccess(latLngList);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        /*
         area = new ArrayList<>();
        area.add(new LatLng(2.496578, 99.627445));
        area.add(new LatLng(2.492771, 99.629004));
        area.add(new LatLng(2.493671, 99.626279));
        FirebaseDatabase.getInstance()
                .getReference("Zona Area")
                .child("kotaku")
                .setValue(area)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MapsActivity.this, "Update!", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    private void addUserMarker() {
        geofire.setLocation("Anda", new GeoLocation(lastLocation.getLatitude(),
                lastLocation.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (currentUser != null) currentUser.remove();
                currentUser = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lastLocation.getLatitude(),
                                lastLocation.getLongitude()))
                        .title("Anda"));
                //gerak kamera
                mMap.animateCamera(CameraUpdateFactory
                        .newLatLngZoom(currentUser.getPosition(),15.0f));
            }
        });
    }

    private void settingGeofire() {
        myLocationRef = FirebaseDatabase.getInstance().getReference("LokasiSaya");
        geofire = new GeoFire(myLocationRef);
    }

    private void builLocationCallback() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                if (mMap != null)
                {
                    lastLocation = locationResult.getLastLocation();

                    addUserMarker();
                }
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
    }

    final int LOCATION_REQUEST_CODE = 1;
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    mapFragment.getMapAsync(this);


                } else {
                    Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (fusedLocationProviderClient != null);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        mMap.setMyLocationEnabled(true);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback, Looper.myLooper());
        //tambah zona area
        addCircleArea();

    }

    private void addCircleArea() {
        if (geoQuery != null)
        {
            geoQuery.removeGeoQueryEventListener(this);
            geoQuery.removeAllListeners();
        }
        for(LatLng latLng : area)
        {
            mMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(90.0)
                    .strokeColor(Color.RED)
                    .fillColor(Color.RED)
                    .strokeWidth(8));

            geoQuery = geofire.queryAtLocation(new GeoLocation(latLng.latitude, latLng.longitude), 0.09f);
            geoQuery.addGeoQueryEventListener(MapsActivity.this);

        }
    }

    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        sendNotification("DETEKSI",String.format(" Anda Berada Di Dalam Zona Merah, Patuhi Aturan Protokol Kesehatan ",key));
        Voc.speak(" Anda Berada Di Dalam Zona Merah, Patuhi Aturan Protokol Kesehatan ");
    }

    @Override
    public void onKeyExited(String key) {
        sendNotification("DETEKSI",String.format(" Anda Sudah Keluar Dari Zona Merah ",key));
        Voc.speak(" Anda Sudah Keluar Dari Zona Merah ");
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        sendNotification("DETEKSI",String.format(" Anda Bergerak Di Dalam Zona Merah, Tetap Patuhi Aturan Protokol Kesehatan ",key));
        Voc.speak(" Anda Bergerak Di Dalam Zona Merah, Tetap Patuhi Aturan Protokol Kesehatan ");
    }

    @Override
    public void onGeoQueryReady() {

    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Toast.makeText(this, ""+error.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String title, String content) {
        Toast.makeText(this, ""+content, Toast.LENGTH_SHORT).show();

        String NOTIFICATON_CHANNEL_ID = "angkot_multi_location";
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATON_CHANNEL_ID,"Notifikasi Saya",
                    NotificationManager.IMPORTANCE_DEFAULT);

            notificationChannel.setDescription("notifikasi channel");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0,1000,500,1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,NOTIFICATON_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));

        Notification notification = builder.build();
        notificationManager.notify(new Random().nextInt(),notification);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        myTTS = new TextToSpeech(this, this);

        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
        speakWords("Pass the String here");
    }

    @Override
    public void onLoadLocationSuccess(List<MyLatLng> latLngs) {
        area = new ArrayList<>();
        for (MyLatLng myLatLng : latLngs)
        {
             LatLng convert = new LatLng(myLatLng.getLatitude(),myLatLng.getLongitude());
             area.add(convert);
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        //bersihkan mapp dan tambah lagi
        if (mMap != null)
        {
            mMap.clear();
            //tambah pengguna marker
            addUserMarker();
            //tambah circle zona area
            addCircleArea();
        }
    }

    @Override
    public void onLoadLocationFailed(String message) {
        Toast.makeText(this, ""+message, Toast.LENGTH_SHORT).show();
    }
}
