package com.example.map_app_direction;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.example.map_app_direction.databinding.ActivityMainBinding;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.geojson.Point;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private MapView mapView;
    private ActivityMainBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private MapboxNavigation mapboxNavigation;

    // Find other maps in https://cloud.maptiler.com/maps/
    String mapId = "streets-v2";

    // Get the API Key by app's BuildConfig
    String key = "OKRTVHPBV2gz7AUYTtTT";
    String styleUrl = "https://api.maptiler.com/maps/" + mapId + "/style.json?key=" + key;
    ListView lvSuggestions;
    ArrayAdapter<String> suggestionsAdapter;
    List<String> suggestionsList = new ArrayList<>();
    protected boolean isShowingSuggestions = true;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final String CHANNEL_ID = "drop_notification_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            }

            createNotificationChannel();

            // Init Mapbox
            Mapbox.getInstance(this);
            // Initialize MapboxNavigationApp
            if (!MapboxNavigationApp.isSetup()) {
                MapboxNavigationApp.setup(new NavigationOptions.Builder(this).build());
            }

            // Init layout view
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            lvSuggestions = findViewById(R.id.lvSuggestions);
            suggestionsAdapter = new ArrayAdapter<>(this, R.layout.list_item, suggestionsList);
            lvSuggestions.setAdapter(suggestionsAdapter);

            // Init the MapView
            mapView = findViewById(R.id.mapView);
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(map -> {
                map.setStyle(new Style.Builder().fromUri(styleUrl));
                map.setCameraPosition(new CameraPosition.Builder()
                        .target(new LatLng(10.8231, 106.6297)) // Coordinates for Ho Chi Minh City
                        .zoom(10.0) // Adjust the zoom level as needed
                        .build());
            });

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION_PERMISSION);
            } else {
                enableLocationComponent();
            }

            if (!MapboxNavigationApp.isSetup()) {
                MapboxNavigationApp.setup(new NavigationOptions.Builder(this).build());
            }

            // Set up the button to find directions
            Button btnFindDirections = findViewById(R.id.btnFindDirections);
            EditText etDestination = findViewById(R.id.etDestination);
            ListView lvSuggestions = findViewById(R.id.lvSuggestions);

            setupClearButton(etDestination);

            suggestionsList = new ArrayList<>();
            suggestionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestionsList);
            lvSuggestions.setAdapter(suggestionsAdapter);

            etDestination.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    Toast.makeText(MainActivity.this, s.toString(), Toast.LENGTH_SHORT).show();
                    fetchSuggestions(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            etDestination.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Update suggestions list based on input
                    updateSuggestions(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            lvSuggestions.setOnItemClickListener((parent, view, position, id) -> {
                String selectedAddress = suggestionsList.get(position);
                etDestination.setText(selectedAddress);
                isShowingSuggestions = false;
                lvSuggestions.setVisibility(View.GONE);
            });

            btnFindDirections.setOnClickListener(v -> {
                String origin = "10.8231,106.6297"; // Example origin coordinates (Ho Chi Minh City)
                String destination = etDestination.getText().toString();
//                findDirections(origin, destination);
            });
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        Log.d("SensorEvent", "x: " + x + ", y: " + y + ", z: " + z);

        double acceleration = Math.sqrt(x * x + y * y + z * z);
        if (acceleration > 20) { // Threshold for detecting a drop
//            showDropNotification();
            Toast.makeText(this, "Phát hiện ổ gà. Chờ API lưu lat long ổ gà", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

//    private void showDropNotification() {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_notification)
//                .setContentTitle("Phone Drop Detected")
//                .setContentText("Your phone has been dropped.")
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setContentIntent(pendingIntent)
//                .setAutoCancel(true);
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        notificationManager.notify(1, builder.build());
//    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Drop Notification Channel";
            String description = "Channel for drop notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void findDirections(String origin, String destination) {
        // Convert origin and destination to Point objects
        String[] originCoords = origin.split(",");
        Point originPoint = Point.fromLngLat(Double.parseDouble(originCoords[1]), Double.parseDouble(originCoords[0]));

        // Assuming destination is in the format "latitude,longitude"
        String[] destinationCoords = destination.split(",");
        Point destinationPoint = Point.fromLngLat(Double.parseDouble(destinationCoords[1]), Double.parseDouble(destinationCoords[0]));

        // Create the directions request
//        MapboxDirections client = MapboxDirections.builder()
//                .setOrigin(origin)
//                .setDestination(destination)
//                .setProfile(DirectionsCriteria.PROFILE_CYCLING)
//                .setAccessToken("<your access token>")
//                .build();
//
//        // Enqueue the call
//        directions.enqueueCall(new Callback<DirectionsResponse>() {
//            @Override
//            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//                if (response.body() != null && !response.body().routes().isEmpty()) {
//                    DirectionsRoute route = response.body().routes().get(0);
//                    List<LegStep> steps = route.legs().get(0).steps();
//                    for (LegStep step : steps) {
//                        Log.d("Directions", step.maneuver().instruction());
//                    }
//                    // Display the route on the map or update the UI with the steps
//                } else {
//                    Toast.makeText(MainActivity.this, "No routes found", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
//                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupClearButton(final EditText editText) {
        final Drawable clearButton = ContextCompat.getDrawable(this, R.drawable.ic_clear);
        clearButton.setBounds(0, 0, clearButton.getIntrinsicWidth(), clearButton.getIntrinsicHeight());

        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getX() >= (editText.getRight() - clearButton.getBounds().width())) {
                    editText.setText("");
                    isShowingSuggestions = true;
                    return true;
                }
            }
            return false;
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    editText.setCompoundDrawables(null, null, clearButton, null);
                } else {
                    editText.setCompoundDrawables(null, null, null, null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "Query cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(query)
                .country("VN")
                .build();

        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if(!isShowingSuggestions) {
                    return;
                }
                if (response.body() != null) {
                    List<CarmenFeature> results = response.body().features();
                    suggestionsList.clear();
                    for (CarmenFeature feature : results) {
                        suggestionsList.add(feature.placeName());
                    }
                    suggestionsAdapter.notifyDataSetChanged();
                    lvSuggestions.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Suggestions fetched: " + results.size(), Toast.LENGTH_SHORT).show();
                } else {
                    lvSuggestions.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "No results found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                lvSuggestions.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSuggestions(String query) {
        // Placeholder for updating suggestions list
        // You need to implement the logic to fetch suggestions based on the query
        suggestionsList.clear();
        suggestionsList.add(query + " 1");
        suggestionsList.add(query + " 2");
        suggestionsList.add(query + " 3");
        suggestionsAdapter.notifyDataSetChanged();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationComponent();
            } else {
                Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableLocationComponent() {
        mapView.getMapAsync(map -> {
            map.setStyle(new Style.Builder().fromUri(styleUrl), style -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    LocationComponent locationComponent = map.getLocationComponent();
                    LocationComponentActivationOptions locationComponentActivationOptions = LocationComponentActivationOptions.builder(this, style).build();
                    locationComponent.activateLocationComponent(locationComponentActivationOptions);
                    locationComponent.setLocationComponentEnabled(true);
                    locationComponent.setCameraMode(CameraMode.TRACKING);
                    locationComponent.setRenderMode(RenderMode.COMPASS);

                    Location lastKnownLocation = locationComponent.getLastKnownLocation();
                    if (lastKnownLocation != null) {
                        map.setCameraPosition(new CameraPosition.Builder()
                                .target(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()))
                                .zoom(15.0)
                                .build());
                    }
                }
            });
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}