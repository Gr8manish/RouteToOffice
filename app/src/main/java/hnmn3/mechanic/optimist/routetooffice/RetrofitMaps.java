package hnmn3.mechanic.optimist.routetooffice;

import hnmn3.mechanic.optimist.routetooffice.POJO.Example;
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by navneet on 17/7/16.
 */
public interface RetrofitMaps {

    /*
     * This method will return the details from Google Direction API
     */
    @GET("api/directions/json?key=AIzaSyDrMCjKrTEWVf3EUmsPbb4pcLImga6a5WM")
    Call<Example> getDetailsFromDirectionAPI(@Query("units") String units, @Query("origin") String origin, @Query("destination") String destination, @Query("mode") String mode, @Query("alternatives") String bool);

}
