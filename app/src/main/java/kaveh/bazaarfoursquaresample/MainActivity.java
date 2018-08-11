package kaveh.bazaarfoursquaresample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.app.ProgressDialog;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import kaveh.bazaarfoursquaresample.Model.Search;
import kaveh.bazaarfoursquaresample.Model.Venue;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements RecyclerViewClickListener {

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private boolean mRequestingLocationUpdates = true;
    private final String REQUESTING_LOCATION_UPDATES_KEY = "location_key";
    private LocationRequest mLocationRequest;
    Activity act;
    public static Location lastLocation = new Location("");
    private RecyclerView.Adapter mAdapter;
    private List<Venue> listData = new ArrayList<>();
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateValuesFromBundle(savedInstanceState);
        setTitle(getResources().getString(R.string.main_page_title));

        sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        act = this;

        RecyclerView mRecyclerView = findViewById(R.id.my_recycler_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ListAdapter(listData, this);

        mRecyclerView.setAdapter(mAdapter);

        createLocationRequest(2);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocation();
                }
            }
        };

        checkForCache();

        updateLocation();
    }

    private void checkForCache() {
        String mLocation = sharedPref.getString(getString(R.string.cache_location), "");
        if (mLocation.contains(",")) {
            String[] separated = mLocation.split(",");
            getLastLocation().setLatitude(Double.parseDouble(separated[0]));
            getLastLocation().setLongitude(Double.parseDouble(separated[1]));
            System.out.println("<><><><><><><><><>" + MainActivity.getLastLocation().toString());
        }
        String mListData = sharedPref.getString(getString(R.string.cache_list_data), "");
        if (mListData.length() > 1) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Venue>>() {
            }.getType();
            List<Venue> mStudentObject = gson.fromJson(mListData, type);
            removeAll();
            mAdapter.notifyDataSetChanged();
            listData.addAll(mStudentObject);
        }
    }

    private void writeLocationtoCache(Location location) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.cache_location), String.valueOf(location.getLatitude()) +
                "," + String.valueOf(location.getLongitude()));
        editor.apply();
    }

    private void writeDatatoCache(List<Venue> data) {
        Gson gson = new Gson();
        String json = gson.toJson(data);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.cache_list_data), json);
        editor.apply();
    }

    public static Location getLastLocation() {
        if (MainActivity.lastLocation == null) {
            MainActivity.lastLocation = new Location("");
        }
        return MainActivity.lastLocation;
    }

    private void searchVenues(String location, int radius, int limit) {
        final ProgressDialog pbdialog = ProgressDialog.show(MainActivity.this, "",
                getResources().getString(R.string.waiting), true);

        FoursquareService fourSquareService = FoursquareService.retrofit.create(FoursquareService.class);
        final Call<Search> call = fourSquareService.requestSearch(getResources().getString(R.string.foursquare_client_id),
                getResources().getString(R.string.foursquare_client_secret), "20180810", location, radius, limit);
        call.enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {
                assert response.body() != null;
                if (response.body().getMeta().getCode() == 200) {
                    List<Venue> data = response.body().getResponse().getVenues();
                    removeAll();
                    mAdapter.notifyDataSetChanged();
                    listData.addAll(data);
                    writeDatatoCache(data);
                }
                pbdialog.dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<Search> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public void add(int position, Venue item) {
        listData.add(position, item);
        mAdapter.notifyItemInserted(position);
    }

    public void remove(int position) {
        listData.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    public void removeAll() {
        for (int i = 0; i < listData.size(); i++) {
            remove(0);
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        // Update the value of mRequestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }

        updateLocation();
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(act, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                if (getLastLocation().distanceTo(location) > 100) {
                                    getLastLocation().setLongitude(location.getLongitude());
                                    getLastLocation().setLatitude(location.getLatitude());
                                    String tmp = location.getLatitude() + "," + location.getLongitude();
                                    writeLocationtoCache(MainActivity.lastLocation);
                                    searchVenues(tmp, 1500, 20);
                                }
                            } else {
                                AlertDialog.Builder builder;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    builder = new AlertDialog.Builder(act, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
                                } else {
                                    builder = new AlertDialog.Builder(act);
                                }
                                builder.setTitle("GPS is disabled")
                                        .setMessage("Do you want to turn on GPS?")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent gpsOptionsIntent = new Intent(
                                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                                startActivity(gpsOptionsIntent);
                                            }
                                        })
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();
                            }
                        }
                    });
        }
    }

    private void getLocationPermission() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Location Permission Denied")
                .setMessage("Do you want to allow me to access your location?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    protected void createLocationRequest(int intervals) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(intervals * 60 * 1000);
        mLocationRequest.setFastestInterval(1 * 60 * 1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
            mRequestingLocationUpdates = false;
        }
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mRequestingLocationUpdates = true;
    }

    private File getTempFile(Context context, String url) {
        File file = null;
        try {
            String fileName = Uri.parse(url).getLastPathSegment();
            file = File.createTempFile(fileName, null, context.getCacheDir());
        } catch (IOException e) {
            // Error while creating file
        }
        return file;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void recyclerViewListClicked(View v, int position) {
        Intent i = new Intent(MainActivity.this, VenueActivity.class);
        i.putExtra(getResources().getString(R.string.venue_id_key), listData.get(position).getId());
        i.putExtra(getResources().getString(R.string.venue_distance_key), listData.get(position).getLocation().getDistance());
        startActivity(i);
    }
}
