package kaveh.bazaarfoursquaresample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import kaveh.bazaarfoursquaresample.Adapter.ListAdapter;
import kaveh.bazaarfoursquaresample.Listener.EndlessRecyclerViewScrollListener;
import kaveh.bazaarfoursquaresample.Listener.RecyclerViewClickListener;
import kaveh.bazaarfoursquaresample.Model.Item;
import kaveh.bazaarfoursquaresample.Model.Search;
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
    private List<Item> listData = new ArrayList<>();
    private SharedPreferences sharedPref;
    private boolean checkPermissionFlag = false;
    private int page_limit = 10;
    private int radius = 1500;
    private static int page = 0;
    private boolean isRefresh = false;
    private boolean isCache = false;
    private SwipeRefreshLayout swipeLayout;
    private EndlessRecyclerViewScrollListener scrollListener;

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

        swipeLayout = findViewById(R.id.swiperefresh);
        swipeLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        isRefresh = true;
                        updateLocation();
                    }
                }
        );

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ListAdapter(listData, this);

        mRecyclerView.setAdapter(mAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                if (!isCache) {
                    String tmp = MainActivity.lastLocation.getLatitude() + "," + MainActivity.lastLocation.getLongitude();
                    MainActivity.page += 1;
                    searchVenues(tmp, radius, page_limit);
                } else {
                    this.resetState();
                }
            }
        };
        // Adds the scroll listener to RecyclerView
        mRecyclerView.addOnScrollListener(scrollListener);

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

    private void refreshData() {
        MainActivity.page = 0;
        scrollListener.resetState();
        clearCache();
        removeAll();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }

        updateLocation();
    }

    //Offline support methods
    private void checkForCache() {
        String mLocation = sharedPref.getString(getString(R.string.cache_location), "");
        if (mLocation.contains(",")) {
            String[] separated = mLocation.split(",");
            getLastLocation().setLatitude(Double.parseDouble(separated[0]));
            getLastLocation().setLongitude(Double.parseDouble(separated[1]));
            System.out.println("<><><><><><><><><>" + MainActivity.getLastLocation().toString());
        }
        String mListData = sharedPref.getString(getString(R.string.cache_list_data), "");
        System.out.println(">>>>>>>>>>>" + mListData);
        if (!mListData.equals("") && mListData.length() > 1) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Item>>() {
            }.getType();
            List<Item> mData = gson.fromJson(mListData, type);
            removeAll();
            mAdapter.notifyDataSetChanged();
            listData.addAll(mData);
            isCache = true;
        }
    }

    private void clearCache() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.cache_location), "");
        editor.apply();
        editor.putString(getString(R.string.cache_list_data), "");
        editor.apply();
    }

    private void writeLocationtoCache(Location location) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.cache_location), String.valueOf(location.getLatitude()) +
                "," + String.valueOf(location.getLongitude()));
        editor.apply();
    }

    private void writeDatatoCache(List<Item> data) {
        System.out.println("<>>>>" + data.size());
        Gson gson = new Gson();
        String json;
        SharedPreferences.Editor editor = sharedPref.edit();
        if (MainActivity.page == 0) {
            json = gson.toJson(data);
            System.out.println(">>>>>>>> First Page");
        } else {
            String mListData = sharedPref.getString(getString(R.string.cache_list_data), "");
            if (!mListData.equals("") && mListData.length() > 1) {
                Gson gTmp = new Gson();
                Type type = new TypeToken<List<Item>>() {
                }.getType();
                List<Item> mData = gTmp.fromJson(mListData, type);
                mData.addAll(data);
                json = gson.toJson(mData);
                System.out.println(">>>>>>>>>" + mData.size());
                System.out.println(">>>>>>>>> Add to Prev Page");
            } else {
                json = gson.toJson(data);
                System.out.println(">>>>>>>>> kharabi in adding");
            }
        }
        editor.putString(getString(R.string.cache_list_data), json);
        editor.apply();
    }

    //Search for list of venues in FourSquare
    private void searchVenues(String location, int radius, int limit) {
        final ProgressDialog pbdialog = ProgressDialog.show(MainActivity.this, "",
                getResources().getString(R.string.waiting), true);

        int pageTmp = MainActivity.page;
        if (isRefresh)
            pageTmp = 0;
        FoursquareService fourSquareService = FoursquareService.retrofit.create(FoursquareService.class);
        final Call<Search> call = fourSquareService.requestSearch(getResources().getString(R.string.foursquare_client_id),
                getResources().getString(R.string.foursquare_client_secret), "20180810",
                location, radius, limit, pageTmp * limit, 1);
        call.enqueue(new Callback<Search>() {
            @Override
            public void onResponse(@NonNull Call<Search> call, @NonNull Response<Search> response) {
                assert response.body() != null;
                swipeLayout.setRefreshing(false);
                if (isRefresh) {
                    refreshData();
                    isRefresh = false;
                }
                if (response.body() == null)
                    return;
                if (response.body().getMeta().getCode() == 200) {
                    isCache = false;
                    List<Item> data = response.body().getResponse().getGroups().get(0).getItems();
                    if (MainActivity.page == 0)
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
                swipeLayout.setRefreshing(false);
            }
        });
    }

    public void add(int position, Item item) {
        listData.add(position, item);
        mAdapter.notifyItemInserted(position);
    }

    public void remove(int position) {
        listData.remove(position);
        mAdapter.notifyItemRemoved(position);
    }

    public void removeAll() {
        final int sizeTmp = listData.size();
        for (int i = 0; i < sizeTmp; i++) {
            remove(0);
        }
    }

    //Location finding methods
    public static Location getLastLocation() {
        if (MainActivity.lastLocation == null) {
            MainActivity.lastLocation = new Location("");
        }
        return MainActivity.lastLocation;
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
            swipeLayout.setRefreshing(false);
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(act, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                // Logic to handle location object
                                if (isRefresh) {
                                    getLastLocation().setLongitude(location.getLongitude());
                                    getLastLocation().setLatitude(location.getLatitude());
                                    String tmp = location.getLatitude() + "," + location.getLongitude();
                                    writeLocationtoCache(MainActivity.lastLocation);
                                    searchVenues(tmp, radius, page_limit);
                                } else if (getLastLocation().distanceTo(location) > 100) {
                                    getLastLocation().setLongitude(location.getLongitude());
                                    getLastLocation().setLatitude(location.getLatitude());
                                    String tmp = location.getLatitude() + "," + location.getLongitude();
                                    writeLocationtoCache(MainActivity.lastLocation);
                                    searchVenues(tmp, radius, page_limit);
                                }
                            } else {
                                swipeLayout.setRefreshing(false);
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
        if (checkPermissionFlag)
            return;
        checkPermissionFlag = true;
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Location Permission Needed")
                .setMessage("Do you want to allow me to access your location?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        dialog.dismiss();
                        checkPermissionFlag = false;
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        checkPermissionFlag = false;
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
        i.putExtra(getResources().getString(R.string.venue_id_key), listData.get(position)
                .getVenue().getId());
        i.putExtra(getResources().getString(R.string.venue_distance_key), listData.get(position)
                .getVenue().getLocation().getDistance());
        startActivity(i);
    }
}
