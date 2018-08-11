package kaveh.bazaarfoursquaresample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.util.List;

import kaveh.bazaarfoursquaresample.Model.Detail;
import kaveh.bazaarfoursquaresample.Model.Search;
import kaveh.bazaarfoursquaresample.Model.Venue;
import kaveh.bazaarfoursquaresample.Model.VenueDetail;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VenueActivity extends AppCompatActivity {

    TextView tv_name;
    TextView tv_address;
    TextView tv_rate;
    TextView tv_distance;
    TextView tv_category;
    ImageView iv_photo;

    private double distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        final ProgressDialog pbdialog = ProgressDialog.show(VenueActivity.this, "",
                getResources().getString(R.string.waiting), true);

        Intent intent = getIntent();
        String venue_id = intent.getStringExtra(getResources().getString(R.string.venue_id_key));
        distance = intent.getDoubleExtra(getResources().getString(R.string.venue_distance_key), 0d);

        tv_name = findViewById(R.id.venue_name);
        tv_address = findViewById(R.id.venue_address);
        tv_rate = findViewById(R.id.venue_rate);
        tv_distance = findViewById(R.id.venue_distance);
        tv_category = findViewById(R.id.venue_category);
        iv_photo = findViewById(R.id.venue_img);

        FoursquareService fourSquareService = FoursquareService.retrofit.create(FoursquareService.class);
        final Call<Detail> call = fourSquareService.requestVenue(venue_id,
                getResources().getString(R.string.foursquare_client_id),
                getResources().getString(R.string.foursquare_client_secret), "20180810");
        call.enqueue(new Callback<Detail>() {
            @Override
            public void onResponse(@NonNull Call<Detail> call, @NonNull Response<Detail> response) {
                assert response.body() != null;

                pbdialog.dismiss();
                if (response.body() == null || response.body().getMeta().getCode() != 200) {
                    Snackbar.make(findViewById(android.R.id.content), "There is some error in fetching data from FourSquare :(", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return;
                }

                loadImage(response.body().getResponse().getVenue());
                tv_name.setText(response.body().getResponse().getVenue().getName());
                if (response.body().getResponse().getVenue().getLocation().getCrossStreet() != null)
                    tv_address.setText(response.body().getResponse().getVenue().getLocation().getAddress() +
                            " " + response.body().getResponse().getVenue().getLocation().getCrossStreet());
                else
                    tv_address.setText(response.body().getResponse().getVenue().getLocation().getAddress());
                tv_rate.setText(String.valueOf(response.body().getResponse().getVenue().getRating()));
                tv_rate.setTextColor(Color.parseColor("#" + response.body().getResponse().getVenue().getRatingColor()));
                tv_distance.setText(String.valueOf(distance) + "m");
                tv_category.setText(response.body().getResponse().getVenue().getCategories().get(0).getName());
            }

            @Override
            public void onFailure(@NonNull Call<Detail> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void loadImage(VenueDetail detail) {
        try {
            final String imageUrl = detail.getBestPhoto().getPrefix() + "300x300" + detail.getBestPhoto().getSuffix();
            System.out.println(imageUrl);
            Picasso.get()
                    .load(imageUrl)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(iv_photo, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(Exception e) {
                            Picasso.get()
                                    .load(imageUrl)
                                    .into(iv_photo, new com.squareup.picasso.Callback() {
                                        @Override
                                        public void onSuccess() {
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                        }
                    });
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}