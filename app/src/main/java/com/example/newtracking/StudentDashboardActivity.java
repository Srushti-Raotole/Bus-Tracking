package com.example.newtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final String API_KEY = "5b3ce3597851110001cf6248b91bc8253a004511a3036a771a510f36";

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference studentRef, driverRef;

    private MapView mapView;
    private IMapController mapController;
    private Marker studentMarker, driverMarker;
    private Polyline routePolyline;

    private GeoPoint studentLocation, driverLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        studentRef = FirebaseDatabase.getInstance().getReference("students").child("student_1");
        driverRef = FirebaseDatabase.getInstance().getReference("buses").child("bus_1");

        requestLocationUpdates();

        // Initialize Map
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE));
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Setup markers
        studentMarker = new Marker(mapView);
        studentMarker.setTitle("Student Location");
        studentMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_location_on_24));
        mapView.getOverlays().add(studentMarker);
        studentMarker.setVisible(true);

        driverMarker = new Marker(mapView);
        driverMarker.setTitle("Driver Location");
        driverMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_directions_bus_24));
        mapView.getOverlays().add(driverMarker);

        // Setup route polyline
        routePolyline = new Polyline();
        routePolyline.setColor(Color.BLUE);
        routePolyline.setWidth(5);
        mapView.getOverlays().add(routePolyline);

        // Track locations
        trackStudentLocation();
        trackAllBuses();
//        trackDriverLocation();

//        // Fetch real-time driver location
//        databaseReference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    double latitude = snapshot.child("latitude").getValue(Double.class);
//                    double longitude = snapshot.child("longitude").getValue(Double.class);
//                    Log.d("StudentDashboard", "Driver Location: " + latitude + ", " + longitude);
//
//                    // Update driver marker
//                    updateDriverLocation(latitude, longitude);
//
//                    // Get Route from ORS
//                    getRouteFromOpenRouteService(latitude, longitude);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e("StudentDashboard", "Error fetching location");
//            }
//        });
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    studentRef.child("latitude").setValue(latitude);
                    studentRef.child("longitude").setValue(longitude);
                    Log.d("StudentDashboard", "Updated Student Location: " + latitude + ", " + longitude);
                }
            }
        }, null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocationUpdates();
        }
    }

    private void trackStudentLocation() {
        studentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("latitude") && snapshot.hasChild("longitude")) {
                    double lat = snapshot.child("latitude").getValue(Double.class);
                    double lng = snapshot.child("longitude").getValue(Double.class);
                    studentLocation = new GeoPoint(lat, lng);

                    // Update student marker position
                    studentMarker.setPosition(studentLocation);
                    studentMarker.setVisible(true);  // Ensure the marker is visible
                    mapController.setCenter(studentLocation);
                    mapView.invalidate();

                    Log.d("StudentDashboard", "Updated Student Location: " + lat + ", " + lng);

                    updateRoute();
                } else {
                    Log.e("StudentDashboard", "Student location not found in Firebase.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboard", "Error fetching student location", error.toException());
            }
        });
    }


//    private void trackDriverLocation() {
//        driverRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    double lat = snapshot.child("latitude").getValue(Double.class);
//                    double lng = snapshot.child("longitude").getValue(Double.class);
//                    driverLocation = new GeoPoint(lat, lng);
//                    driverMarker.setPosition(driverLocation);
//                    mapView.invalidate();
//                    updateRoute();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e("StudentDashboard", "Error fetching driver location");
//            }
//        });
//    }

    private void trackAllBuses() {
        DatabaseReference busesRef = FirebaseDatabase.getInstance().getReference("buses");

        busesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                mapView.getOverlays().clear(); // Clear previous markers
                List<Overlay> overlays = mapView.getOverlays();
                overlays.remove(routePolyline); // Keep route but remove extra bus markers

                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                    if (busSnapshot.child("latitude").exists() && busSnapshot.child("longitude").exists()) {
                        double lat = busSnapshot.child("latitude").getValue(Double.class);
                        double lng = busSnapshot.child("longitude").getValue(Double.class);
                        String busId = busSnapshot.getKey(); // Get bus ID

                        // Create a new marker for each bus
                        Marker busMarker = new Marker(mapView);
                        busMarker.setPosition(new GeoPoint(lat, lng));
                        busMarker.setTitle("Bus: " + busId);
                        busMarker.setIcon(ContextCompat.getDrawable(StudentDashboardActivity.this, R.drawable.baseline_directions_bus_24));

                        mapView.getOverlays().add(busMarker);
                    }
                }
                mapView.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StudentDashboard", "Error fetching buses location");
            }
        });
    }


    private void updateRoute() {
        if (studentLocation != null && driverLocation != null) {
            getRouteFromOpenRouteService(driverLocation.getLatitude(), driverLocation.getLongitude(),
                    studentLocation.getLatitude(), studentLocation.getLongitude());
        }
    }
    private void getRouteFromOpenRouteService(double driverLat, double driverLng, double studentLat, double studentLng) {
        String url = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=" + API_KEY +
                "&coordinates=" + driverLng + "," + driverLat + "|" + studentLng + "," + studentLat;

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String responseData = response.body().string();

                List<GeoPoint> routePoints = parseRoute(responseData);

                runOnUiThread(() -> {
                    if (!routePoints.isEmpty()) {
                        routePolyline.setPoints(routePoints);
                        mapView.invalidate();
                    }
                });

            } catch (Exception e) {
                Log.e("StudentDashboard", "Error fetching route", e);
            }
        }).start();
    }
    private List<GeoPoint> parseRoute(String responseData) {
        List<GeoPoint> routePoints = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(responseData);
            JSONArray coordinates = jsonResponse.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates");

            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray point = coordinates.getJSONArray(i);
                double lon = point.getDouble(0);
                double lat = point.getDouble(1);
                routePoints.add(new GeoPoint(lat, lon));
            }
        } catch (Exception e) {
            Log.e("StudentDashboard", "Error parsing route", e);
        }
        return routePoints;
    }
//    private void updateDriverLocation(double latitude, double longitude) {
//        if (driverMarker == null) {
//            driverMarker = new Marker(map);
//            Drawable icon = getResources().getDrawable(R.drawable.baseline_directions_bus_24); // Add a bus icon in res/drawable
//            driverMarker.setIcon(icon);
//            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//            map.getOverlays().add(driverMarker);
//        }
//        driverMarker.setPosition(new GeoPoint(latitude, longitude));
//        mapController.animateTo(new GeoPoint(latitude, longitude));
//        map.invalidate();
//    }

//    private void getRouteFromOpenRouteService(double driverLat, double driverLng) {
//        String apiKey = "5b3ce3597851110001cf6248b91bc8253a004511a3036a771a510f36"; // Replace with your API Key
//
//        String profile = "driving-car"; // Vehicle mode
//        String baseUrl = "https://api.openrouteservice.org/v2/directions/" + profile + "?api_key=" + apiKey;
//
//        if (studentLocation != null) {
//            String url = baseUrl + "&coordinates=" + driverLng + "," + driverLat + "|" +
//                    studentLocation.getLongitude() + "," + studentLocation.getLatitude();
//
//            Log.d("API Request", "URL: " + url);
//        } else {
//            Log.e("Error", "Student location is null");
//        }
//
//        new Thread(() -> {
//            try {
//                OkHttpClient client = new OkHttpClient();
//                Request request = new Request.Builder().url(baseUrl).build();
//                Response response = client.newCall(request).execute();
//                String responseData = response.body().string();
//
//                JSONObject jsonResponse = new JSONObject(responseData);
//                JSONArray routes = jsonResponse.getJSONArray("routes");
//                JSONArray coordinates = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
//
//                List<GeoPoint> routePoints = new ArrayList<>();
//                for (int i = 0; i < coordinates.length(); i++) {
//                    JSONArray coord = coordinates.getJSONArray(i);
//                    double lon = coord.getDouble(0);
//                    double lat = coord.getDouble(1);
//                    routePoints.add(new GeoPoint(lat, lon));
//                }
//
//                runOnUiThread(() -> drawRoute(routePoints));
//
//            } catch (Exception e) {
//                Log.e("StudentDashboard", "Error fetching route", e);
//            }
//        }).start();
//    }

//    private void drawRoute(List<GeoPoint> routePoints) {
//        if (routePolyline != null) {
//            map.getOverlays().remove(routePolyline);
//        }
//        routePolyline = new Polyline();
//        routePolyline.setPoints(routePoints);
//        routePolyline.setWidth(8.0f);
//        routePolyline.setColor(getResources().getColor(android.R.color.holo_blue_dark));
//        map.getOverlays().add(routePolyline);
//        map.invalidate();
//    }



//        // Initialize the map
//        mapView = findViewById(R.id.mapView);
//        Configuration.getInstance().setUserAgentValue(getPackageName());
//        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
//
//
//        // Initialize Firebase Database reference for student location
//        studentLocationRef = FirebaseDatabase.getInstance().getReference("studentLocations");
//        // Initialize FusedLocationProviderClient
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//
//        MapController mapController = (MapController) mapView.getController();
//        mapController.setZoom(15); // Set initial zoom level
//
//        // Fetch the location from Firebase
//        locationRef = FirebaseDatabase.getInstance().getReference("Locations");
//
//        // Initialize the overlay to display location markers
//        overlay = new ItemizedOverlayWithFocus<>(this, new ArrayList<>(), null);
//
//        // Display the location of the driver
//        fetchDriverLocation();
//
//
//        // Get the student's current location
//        getCurrentLocation();

    }

//    private void fetchDriverLocation() {
//        DatabaseReference driverLocationRef = FirebaseDatabase.getInstance().getReference("Locations/drivers");
//        locationRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//
//
//                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
//                    LocationData locationData = busSnapshot.getValue(LocationData.class);
//
//                    if (locationData != null) {
//                        double latitude = locationData.latitude;
//                        double longitude = locationData.longitude;
//
//                        // Update the map with driver's location
//                        GeoPoint driverGeoPoint = new GeoPoint(latitude, longitude);
////                        updateBusLocationOnMap(busSnapshot.getKey(),driverLocation);
//                        // Add the driver's location to the map
//                        addDriverLocationToMap(driverGeoPoint);
//                    }}}
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(StudentDashboardActivity.this, "Failed to fetch location", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void addDriverLocationToMap(GeoPoint driverGeoPoint) {
//
//        // Create a marker for the driver's location
//        Marker driverMarker = new Marker(mapView);
//        driverMarker.setPosition(driverGeoPoint);
//        driverMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_directions_bus_24));  // Custom icon for bus
//        driverMarker.setTitle("Driver Location");
//
//        // Add the marker to the map
//
//        mapView.getOverlays().add(driverMarker);
//        mapView.invalidate();
//    }
//
//
//    private void getCurrentLocation() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//        } else {
//            fusedLocationClient.getLastLocation()
//                    .addOnSuccessListener(this, location -> {
//                        if (location != null) {
//                            // Get current location and display it on map
//                            double latitude = location.getLatitude();
//                            double longitude = location.getLongitude();
//
//                            GeoPoint studentLocation = new GeoPoint(latitude, longitude);
//                            // Store the student's location in Firebase
//                            storeLocationInFirebase(latitude, longitude);
//
//                            // Update map with student's location
//                            addStudentLocationToMap(studentLocation);
//                        }
//                    });
//        }
//    }
//
//    private void storeLocationInFirebase(double latitude, double longitude) {
//        String studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//        // Create a location data object
//        LocationData studentLocationData = new LocationData(latitude, longitude);
//
//        // Store the location in Firebase under "studentLocations"
//        studentLocationRef.child(studentId).setValue(studentLocationData)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Toast.makeText(StudentDashboardActivity.this, "Student location stored in Firebase", Toast.LENGTH_SHORT).show();
//                    } else {
//                        Toast.makeText(StudentDashboardActivity.this, "Failed to store location in Firebase", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//    private void addStudentLocationToMap(GeoPoint studentLocation) {
//        // Create a marker for the student's current location
//        Marker studentMarker = new Marker(mapView);
//        studentMarker.setPosition(studentLocation);
//        studentMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.baseline_location_on_24)); // Custom icon for student
//        studentMarker.setTitle("Student Location");
//
//        // Add the marker to the map
//        mapView.getOverlays().add(studentMarker);
//
//        // Center the map on the student's location
//        mapView.getController().setCenter(studentLocation);
//        mapView.getController().setZoom(15);
//        mapView.invalidate();
//    }
