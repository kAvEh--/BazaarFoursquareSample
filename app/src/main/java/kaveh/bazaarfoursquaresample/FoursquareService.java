package kaveh.bazaarfoursquaresample;

import kaveh.bazaarfoursquaresample.Model.Detail;
import kaveh.bazaarfoursquaresample.Model.Search;
import kaveh.bazaarfoursquaresample.Model.VenueDetail;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FoursquareService {
    @GET("venues/search/")
    Call<Search> requestSearch(
            @Query("client_id") String client_id,
            @Query("client_secret") String client_secret,
            @Query("v") String v,
            @Query("ll") String ll,
            @Query("radius") int radius,
            @Query("limit") int limit);

    @GET("venues/{venue_id}")
    Call<Detail> requestVenue(
            @Path(value = "venue_id", encoded = true) String venue_id,
            @Query("client_id") String client_id,
            @Query("client_secret") String client_secret,
            @Query("v") String v);


    HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("https://api.foursquare.com/v2/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.addInterceptor(interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)).build())
            .build();
}
