package com.example.newtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.Manifest;
import android.widget.Toast;

public class DriverDashboardActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private static final int LOCATION_PERMISSION_REQUEST_CODE=1;
    private DatabaseReference locationRef;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRef = FirebaseDatabase.getInstance().getReference("Locations");

        // Start capturing the driver's location
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location
                            if (location != null) {
                                saveLocationToFirebase(location.getLatitude(), location.getLongitude());
                            } else {
                                Toast.makeText(DriverDashboardActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(DriverDashboardActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }

    private void saveLocationToFirebase(double latitude, double longitude) {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        LocationData locationData = new LocationData(latitude, longitude);
        locationRef.child(driverId).setValue(locationData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(DriverDashboardActivity.this, "Location saved!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(DriverDashboardActivity.this, "Failed to save location.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

//    private void requestLocationUpdates() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//            return;
//        }
//
//        LocationRequest locationRequest = LocationRequest.create()
//                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                .setInterval(5000) // Update every 5 seconds
//                .setFastestInterval(2000);
//
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult locationResult) {
//                for (Location location : locationResult.getLocations()) {
//                    double latitude = location.getLatitude();
//                    double longitude = location.getLongitude();
//
//                    // Update location in Firebase
//                    locationRef.setValue(new LocationData(latitude, longitude));
//                }
//            }
//        };
//
//        locationClient.requestLocationUpdates(locationRequest, locationCallback, null);
//    }
//
//
//// Class for storing location data
//public class LocationData {
//    public double latitude;
//    public double longitude;
//
//    public LocationData() {}
//
//    public LocationData(double latitude, double longitude) {
//        this.latitude = latitude;
//        this.longitude = longitude;
//    }
//}
    }
