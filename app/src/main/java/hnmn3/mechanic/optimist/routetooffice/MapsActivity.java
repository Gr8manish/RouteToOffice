package hnmn3.mechanic.optimist.routetooffice;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import hnmn3.mechanic.optimist.routetooffice.POJO.Example;
import hnmn3.mechanic.optimist.routetooffice.POJO.Route;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

import static android.view.View.GONE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Long prevTime=null,currTime=null;

    //Buttons to start and stop
    Button btnStart,btnStop;

    //Current location marker
    Marker currMarker=null;

    //Current location & previous location
    Location currLocation,prevLocation;

    //Latitude & Longitude of Some random destination in pune
    String dest="26.854260,75.805000";
    TextView tvDistance,tvTime,tvCost;

    /*
    * LocationRequest is used to get quality of service for location updates
    * from the FusedLocationProviderApi using requestLocationUpdates.
    * */
    LocationRequest mLocationRequest;

    Boolean isInitialized=false,isStart=false,isFirstTimeLocationChange=true;

    /*
    * In a Google Maps App, it is always required to update current location of user at regular intervals.
     * Also we may also want current velocity, altitude etc. These all are covered inside location object
    * */
    Location mLastLocation;

    //Measured distance;
    float distance=0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        tvDistance = (TextView)findViewById(R.id.tvDistance);
        tvTime = (TextView)findViewById(R.id.tvTime);
        tvCost = (TextView)findViewById(R.id.tvCost);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setVisibility(GONE);
        btnStart.setVisibility(View.VISIBLE);
        tvCost.setVisibility(GONE);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        /*
        * In Android 6.0 Marshmallow, application will not be granted any permission at installation time.
         * Instead, application has to ask user for a permission one-by-one at runtime.
        * */
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        //closing the app if Google Play Services not available
        if (!isGooglePlayServicesAvailable()) {
            Log.d("onCreate", "Google Play Services not available. Ending Test case.");
            finish();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
            }
        }
        else {
            buildGoogleApiClient();
        }

        mMap.setMyLocationEnabled(true);
    }


    /*Initializing Google Client API
    * .addConnectionCallbacks provides callbacks that are called when client connected or disconnected.
    * .addOnConnectionFailedListener covers scenarios of failed attempt of connect client to service
    * .addApi adds the LocationServices API endpoint from Google Play Services.
    * mGoogleApiClient.connect(): A client must be connected before excecuting any operation.
    * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    /*
    * onLocationChanged() will be called as soon as user location change.
    * */
    @Override
    public void onLocationChanged(Location location)
    {
        mLastLocation = location;
        if (currMarker != null) {
            currMarker.remove();
        }

        if(isFirstTimeLocationChange){
            build_retrofit_and_get_response();
            isFirstTimeLocationChange=false;
        }

        if(!isInitialized && isStart){
            prevLocation = location;
            currLocation = location;
            prevTime = System.currentTimeMillis();
            currTime = System.currentTimeMillis();
            isInitialized=true;
        }else if(isStart){
            prevLocation = currLocation;
            currLocation = location;

            long millis = prevTime - System.currentTimeMillis();
            long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
            String time = String.format("%02d:%02d:%02d",-hours, -minutes, -(seconds% 60));

            tvTime.setText("Time = "+time);

            //1 paisa per second & 1 paisa per meter
            float cost = ((-seconds) + distance)/100.0f;

            tvCost.setText("Cost = "+cost+" rupees");
            distance+= prevLocation.distanceTo(currLocation);
            tvDistance.setText("Distance = "+distance+" meters");
            currMarker = setMarkerToLocation(mLastLocation);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btnStart:
                btnStop.setVisibility(View.VISIBLE);
                btnStart.setVisibility(GONE);
                tvCost.setVisibility(View.VISIBLE);

                tvTime.setText("Time = 00:00:00");
                tvDistance.setText("Distance = 0 meters");
                tvCost.setText("Cost = 0 rupees");
                distance = 0;
                isInitialized = false;
                isStart=true;
                break;
            case R.id.btnStop:
                btnStop.setVisibility(GONE);
                btnStart.setVisibility(View.VISIBLE);
                isStart=false;
                break;
        }
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            //getting last known location
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            currMarker = setMarkerToLocation(mLastLocation);
            //setting location change listener
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    public Marker setMarkerToLocation(Location location){
        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

        currMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        return currMarker;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /*
    * In Android 6.0 Marshmallow, application will not be granted any permission at installation time.
     * Instead, application has to ask user for a permission one-by-one at runtime.
    * */
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                /*
                * Here I can add code to explain user that why this app require
                * ACCESS_FINE_LOCATION permission if If the permission was denied previously
                * */

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    // Checking if Google Play Services Available or not
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if(result != ConnectionResult.SUCCESS) {
            if(googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
        return true;
    }


    private void build_retrofit_and_get_response() {

        String url = "https://maps.googleapis.com/maps/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitMaps service = retrofit.create(RetrofitMaps.class);

        Call<Example> call = service.getDetailsFromDirectionAPI("metric", mLastLocation.getLatitude()
                + "," + mLastLocation.getLongitude(),dest, "driving","true");

        call.enqueue(new Callback<Example>() {
            @Override
            public void onResponse(Response<Example> response, Retrofit retrofit) {

                try {

                    List<Route> routes = response.body().getRoutes();

                    // This loop will go through all the results and add marker on each location.
                    if(routes.size()>0){
                        String distance = routes.get(0).getLegs().get(0).getDistance().getText();
                        String time = routes.get(0).getLegs().get(0).getDuration().getText();
                        tvDistance.setText(tvDistance.getText() + distance);
                        tvTime.setText(tvTime.getText() + time);

                        String encodedString = routes.get(0).getOverviewPolyline().getPoints();
                        List<LatLng> list = decodePoly(encodedString);
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(list)
                                .width(10)
                                .color(Color.BLUE)
                                .geodesic(true)
                        );
                    }
                    if(routes.size()>1){
                        String encodedString = routes.get(1).getOverviewPolyline().getPoints();
                        List<LatLng> list = decodePoly(encodedString);
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(list)
                                .width(10)
                                .color(Color.BLUE)
                                .geodesic(true)
                        );
                    }
                } catch (Exception e) {
                    Log.d("onResponse", "There is an error");
                    e.printStackTrace();
                }

                LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                //move map camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("onFailure", t.toString());
            }
        });

    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng( (((double) lat / 1E5)),
                    (((double) lng / 1E5) ));
            poly.add(p);
        }

        return poly;
    }
}
