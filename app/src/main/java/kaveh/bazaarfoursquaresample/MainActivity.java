package kaveh.bazaarfoursquaresample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.app.ProgressDialog;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

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
    public static Location lastLocation;
    private RecyclerView.Adapter mAdapter;
    private List<Venue> listData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        updateValuesFromBundle(savedInstanceState);
        setTitle(getResources().getString(R.string.main_page_title));

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        act = this;

        RecyclerView mRecyclerView = findViewById(R.id.my_recycler_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ListAdapter(listData, this);

        mRecyclerView.setAdapter(mAdapter);

        createLocationRequest(1);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                updateLocation();
            }
        });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                System.out.println("============================= response");
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    updateLocation();
                    System.out.println(location.getLatitude() + ":" + location.getLongitude() + ":::::");
                }
            }
        };

        updateLocation();
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
                List<Venue> datas = response.body().getResponse().getVenues();

                removeAll();
                mAdapter.notifyDataSetChanged();
                listData.addAll(datas);
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
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(act, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                if (MainActivity.lastLocation != null) {
                                    System.out.println(MainActivity.lastLocation.distanceTo(location) + "***************");
                                }
                                if (MainActivity.lastLocation == null || MainActivity.lastLocation.distanceTo(location) > 100) {
                                    MainActivity.lastLocation = location;
                                    String tmp = location.getLatitude() + "," + location.getLongitude();
                                    searchVenues(tmp, 1500, 20);
                                }
                            } else {
                                Intent gpsOptionsIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(gpsOptionsIntent);
                            }
                        }
                    });
        }
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
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            System.out.println("============================= start");
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
            mRequestingLocationUpdates = false;
        }
    }

    private void stopLocationUpdates() {
        System.out.println("============================= stop");
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mRequestingLocationUpdates = true;
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
