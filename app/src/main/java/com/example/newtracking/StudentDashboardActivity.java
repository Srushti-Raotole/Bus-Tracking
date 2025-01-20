package com.example.newtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.HashMap;

public class StudentDashboardActivity extends AppCompatActivity {

    private MapView mapView;
    private DatabaseReference locationRef, studentLocationRef;
    private FusedLocationProviderClient fusedLocationClient;
    private ItemizedOverlayWithFocus<OverlayItem> overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Initialize the map
        mapView = findViewById(R.id.mapView);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);


        // Initialize Firebase Database reference for student location
        studentLocationRef = FirebaseDatabase.getInstance().getReference("studentLocations");
        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        MapController mapController = (MapController) mapView.getController();
        mapController.setZoom(15); // Set initial zoom level

        // Fetch the location from Firebase
        locationRef = FirebaseDatabase.getInstance().getReference("Locations");

        // Initialize the overlay to display location markers
        overlay = new ItemizedOverlayWithFocus<>(this, new ArrayList<>(), null);

        // Display the location of the driver
        fetchDriverLocation();


        // Get the student's current location
        getCurrentLocation();

    }

    private void fetchDriverLocation() {
        DatabaseReference driverLocationRef = FirebaseDatabase.getInstance().getReference("Locations/drivers");
        locationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                    LocationData locationData = busSnapshot.getValue(LocationData.class);

                    if (locationData != null) {
                        double latitude = locationData.latitude;
                        double longitude = locationData.longitude;

                        // Update the map with driver's location
                        GeoPoint driverGeoPoint = new GeoPoint(latitude, longitude);
//                        updateBusLocationOnMap(busSnapshot.getKey(),driverLocation);
                        // Add the driver's location to the map
                        addDriverLocationToMap(driverGeoPoint);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StudentDashboardActivity.this, "Failed to fetch location", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void addDriverLocationToMap(GeoPoint driverGeoPoint) {
        // Create a marker for the driver's location
        Marker driverMarker = new Marker(mapView);
        driverMarker.setPosition(driverGeoPoint);
        driverMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_directions_bus_24));  // Custom icon for bus
        driverMarker.setTitle("Driver Location");

        // Add the marker to the map
        mapView.getOverlays().add(driverMarker);
    }

//    private void updateBusLocationOnMap(String busId, GeoPoint driverLocation) {
//
//        // Load the custom bus icon from resources
//        Drawable busIcon = ContextCompat.getDrawable(StudentDashboardActivity.this, R.drawable.baseline_directions_bus_24);
//
//        // Create the overlay item with the bus icon
//        OverlayItem driverMarker = new OverlayItem(busId, "Bus location", driverLocation);
//        driverMarker.setMarker(busIcon);
//
//        // Add the marker to the overlay
//        overlay.addItem(driverMarker);
//
//        // Add the overlay to the map
//        mapView.getOverlays().clear();
//        mapView.getOverlays().add(overlay);
//
//        // Center the map on the first driver's location
//        mapView.getController().setCenter(driverLocation);
//        }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Get current location and display it on map
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            GeoPoint studentLocation = new GeoPoint(latitude, longitude);
                            // Store the student's location in Firebase
                            storeLocationInFirebase(latitude, longitude);

                            // Update map with student's location
                            addStudentLocationToMap(studentLocation);
                        }
                    });
        }
    }

    private void storeLocationInFirebase(double latitude, double longitude) {
        String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Create a location data object
        LocationData studentLocationData = new LocationData(latitude, longitude);

        // Store the location in Firebase under "studentLocations"
        studentLocationRef.child(studentId).setValue(studentLocationData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(StudentDashboardActivity.this, "Student location stored in Firebase", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(StudentDashboardActivity.this, "Failed to store location in Firebase", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void addStudentLocationToMap(GeoPoint studentLocation) {
        // Create a marker for the student's current location
        Marker studentMarker = new Marker(mapView);
        studentMarker.setPosition(studentLocation);
        studentMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_location_on_24)); // Custom icon for student
        studentMarker.setTitle("Student Location");

        // Add the marker to the map
        mapView.getOverlays().add(studentMarker);

        // Center the map on the student's location
        mapView.getController().setCenter(studentLocation);
        mapView.getController().setZoom(15);
    }
//    private TextView createBusMarker(String busId) {
//        TextView busMarker = new TextView(this);
//        busMarker.setText("Bus: " + busId);
//        busMarker.setBackgroundResource(R.drawable.bus_marker); // Custom drawable for marker background
//        busMarker.setPadding(10, 5, 10, 5);
//        return busMarker;
//    }
//
//    private void updateMarkerPosition(View marker, double latitude, double longitude) {
//        // Simulate updating a marker’s position on a custom map.
//        // Scale latitude/longitude to fit your screen layout (e.g., using a percentage-based position)
//
//        int xPos = (int) (latitude * customMapContainer.getWidth()); // Replace this with real scaling
//        int yPos = (int) (longitude * customMapContainer.getHeight());
//
//        // Update position dynamically
//        marker.setX(xPos);
//        marker.setY(yPos);
//    }
}