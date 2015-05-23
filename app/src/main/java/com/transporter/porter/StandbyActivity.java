package com.transporter.porter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;

public class StandbyActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {

    // LogCat tag
    private static final String TAG = StandbyActivity.class.getSimpleName();

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 15000; // 20 sec
    private static int FASTEST_INTERVAL = 10000; // 20 sec
    private static int DISPLACEMENT = 0; // 10 meters

    // UI elements
    private TextView lblLocation;
    private Button btnShowLocation, btnStartLocationUpdates;

    private String mApiKey, mDeviceUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_standby);

        lblLocation = (TextView) findViewById(R.id.lblLocation);
        /*btnShowLocation = (Button) findViewById(R.id.btnShowLocation);*/
        btnStartLocationUpdates = (Button) findViewById(R.id.btnLocationUpdates);

        // Get the api key of the logged in user and the device uuid
        mApiKey = getAPIKey();
        mDeviceUuid = getDeviceUUID();

        // First we need to check availability of play services
        checkLocationEnabled();
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
            createLocationRequest();
        }

        /*// Show location button click listener
        btnShowLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                displayLocation();
            }
        });*/
        // Toggling the periodic location updates
        btnStartLocationUpdates.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {togglePeriodicLocationUpdates();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_view_all_requests:
                /*Intent intent = new Intent(this, AllRequestsActivity.class);
                startActivity(intent);
                return true;*/
            case R.id.action_logout:
                onLogout();
                return true;
            case R.id.action_settings:
                //openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onLogout() {
        PorterDbHelper dbHelper = new PorterDbHelper(StandbyActivity.this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(PorterContract.User.TRUNCATE_TABLE);
        db.close();

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.apikey_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationEnabled();
        checkPlayServices();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private String getDeviceUUID() {
        String email = "";
        String deviceUuid = "";
        // Check if the user info is available in the user db
        PorterDbHelper dbHelper = new PorterDbHelper(StandbyActivity.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] userProjection = { PorterContract.User._ID,
                PorterContract.User.COLUMN_NAME_EMAIL };
        Cursor c = db.query(PorterContract.User.TABLE_NAME, userProjection,
                null, null, null, null, null);
        if(c.moveToFirst()) {
            email = c.getString(c.getColumnIndex(PorterContract.User.COLUMN_NAME_EMAIL));
        }
        c.close();

        // Find the device uuid
        String[] deviceProjection = { PorterContract.EmailDeviceUUIDMapping._ID,
                PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_DEVICE_UUID,
        };
        String selection = PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_EMAIL + " = ?";
        String[] selectionArgs = {
                email
        };

        c = db.query(PorterContract.EmailDeviceUUIDMapping.TABLE_NAME, deviceProjection, selection,
                selectionArgs, null, null, null);
        if(c.moveToFirst()) {
            deviceUuid = c.getString(c.getColumnIndex(PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_DEVICE_UUID));
        }
        dbHelper.close();
        c.close();

        return deviceUuid;
    }

    private String getAPIKey() {

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.apikey_file), Context.MODE_PRIVATE);
        return sharedPref.getString(getString(R.string.saved_apikey), null);
    }

    /**
     * Method to display the location on UI
     * */
    private void displayLocation() throws IOException, JSONException {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            lblLocation.setText(latitude + ", " + longitude);
            if(sendCurrentLocationRequest(latitude, longitude)) {
                JSONObject responseJson = sendAssignDeliveryRequest();
                // The moment a delivery is assigned stop the location updates to the server
                if(responseJson.optBoolean("success", false)) {
                    togglePeriodicLocationUpdates();
                    engageDeliverer(responseJson);
                }
            } else {
                lblLocation.setText("(The location update call to the server failed!!)");
            }
        } else {
            lblLocation.setText("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }


    private void engageDeliverer(JSONObject response) throws JSONException, IOException {
        if(!response.optBoolean("success", false)) {
            return;
        }
        JSONObject requestJson, receiverJson, requesterJson, pickupJson, dropJson;
        requestJson = response.optJSONObject("request");
        requesterJson = response.optJSONObject("requester");
        pickupJson = response.optJSONObject("picked_up_location");
        receiverJson = response.optJSONObject("receiver");
        dropJson = response.optJSONObject("drop_location");

        if(requestJson == null || requesterJson == null || pickupJson == null || receiverJson == null || dropJson == null ) {
            Log.d(TAG, "response = " + response);
            Toast.makeText(this, getString(R.string.server_error), Toast.LENGTH_LONG).show();
            return;
        }

        PorterDbHelper dbHelper = new PorterDbHelper(StandbyActivity.this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        double[] pickupLatLong = {pickupJson.getDouble("lattitude"), pickupJson.getDouble("longitude")};
        double[] dropLatLong = {dropJson.getDouble("lattitude"), dropJson.getDouble("longitude")};
        String requestId = requestJson.optString("id");

        ContentValues values = new ContentValues();
        values.put(PorterContract.RequestDetails.COLUMN_NAME_REQUEST_ID, requestId);
        values.put(PorterContract.RequestDetails.COLUMN_NAME_PICKUP_LAT, String.valueOf(pickupLatLong[0]));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_PICKUP_LONG, String.valueOf(pickupLatLong[1]));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_PICKUP_ADDRESS, pickupJson.optString("address"));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_PICKUP_AREA, getArea(pickupLatLong[0], pickupLatLong[1]));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_DROP_LAT, String.valueOf(dropLatLong[0]));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_DROP_LONG, String.valueOf(dropLatLong[1]));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_DROP_ADDRESS, dropJson.optString("address"));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_DROP_AREA, getArea(dropLatLong[0], dropLatLong[1]));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_CUSTOMER_NAME, receiverJson.optString("name"));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_CUSTOMER_PHONE, receiverJson.optString("phone_number"));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_ORDER_ID, receiverJson.optString("order_id"));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_REQUESTER_NAME, requesterJson.optString("name"));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_REQUESTER_PHONE, requesterJson.optString("phone_number"));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_STATUS, requestJson.optString("status"));
        values.put(PorterContract.RequestDetails.COLUMN_NAME_DISTANCE, requestJson.optString("distance"));

        db.insert(PorterContract.RequestDetails.TABLE_NAME, null, values);
        db.close();

        // Start new intent and pass the request id
        Toast.makeText(this, "Yay!! I have been assigned a delivery", Toast.LENGTH_LONG).show();
        /*Intent intent = new Intent(this, RequestSummaryActivity.class);
        intent.putExtra(RequestSummaryActivity.EXTRA_REQUEST_ID, requestId);
        startActivity(intent);*/
    }

    private String getArea(double latitude, double longitude) throws IOException {
        Geocoder gc = new Geocoder(getBaseContext());
        List<Address> address = gc.getFromLocation(latitude, longitude, 1);
        String area = "";
        if (address.size() > 0) {
            area = address.get(0).getAddressLine(1);
            area = area == null ? "" : area;
        }
        return area;
    }

    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Changing the button text
            btnStartLocationUpdates
                    .setText(R.string.go_offline);

            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");

        } else {
            // Changing the button text
            btnStartLocationUpdates
                    .setText(R.string.go_online);

            mRequestingLocationUpdates = false;

            // Stopping the location updates
            stopLocationUpdates();

            Log.d(TAG, "Periodic location updates stopped!");
        }
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        /*try {
            displayLocation();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

        Toast.makeText(getApplicationContext(), "Location changed!",
                Toast.LENGTH_SHORT).show();

        // Send current location request to the server
        // Displaying the new location on UI
        try {
            displayLocation();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void checkLocationEnabled() {
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean enabled = service
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!enabled) {
            showLocationTurnOnAlert();
        }
    }

    public void showLocationTurnOnAlert() {
        AlertDialog alert = new AlertDialog.Builder(this).create();
        alert.setTitle("Turn on Location");

        // Setting Dialog Message
        alert.setMessage("Please turn on the location");

        alert.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        alert.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                StandbyActivity.this.onResume();
            }
        });
        alert.show();
    }

    public boolean sendCurrentLocationRequest(double dLat, double dLong) throws IOException, JSONException {

        HashMap<String, String> requestContent = ApiUtil.getCurrentLocationUpdationRequest();
        String api_key = requestContent.get(ApiUtil.API_KEY_REQUIRED).equals(ApiUtil.YES) ? mApiKey : null;
        requestContent.put("api_key", api_key);

        JSONObject device = new JSONObject();
        JSONObject locationInfo = new JSONObject();
        JSONObject currentLocationParams = new JSONObject();

        device.put("device_uuid", mDeviceUuid);

        locationInfo.put("lattitude", String.valueOf(dLat));
        locationInfo.put("longitude", String.valueOf(dLong));
        locationInfo.put("device", device);

        currentLocationParams.put("current_location", locationInfo);

        requestContent.put("url_params", currentLocationParams.toString());

        HttpURLConnection connection = ConnectionUtil.getConnection(requestContent.get(ApiUtil.URL_KEY),requestContent.get(ApiUtil.METHOD),
                requestContent.get(ApiUtil.API_KEY),requestContent.get(ApiUtil.URL_PARAMS));

        int responseCode = connection.getResponseCode();
        JSONObject responseJson = ConnectionUtil.convertToJsonObject(connection);

        return responseJson.getBoolean("success");
    }

    public JSONObject sendAssignDeliveryRequest() throws IOException, JSONException {

        HashMap<String, String> requestContent = ApiUtil.getAssignDeliveryRequest();
        String api_key = requestContent.get(ApiUtil.API_KEY_REQUIRED).equals(ApiUtil.YES) ? mApiKey : null;
        requestContent.put("api_key", api_key);

        JSONObject device = new JSONObject();
        JSONObject deviceParams = new JSONObject();

        device.put("device_uuid", mDeviceUuid);
        deviceParams.put("device", device);

        requestContent.put("url_params", deviceParams.toString());

        HttpURLConnection connection = ConnectionUtil.getConnection(requestContent.get(ApiUtil.URL_KEY),requestContent.get(ApiUtil.METHOD),
                requestContent.get(ApiUtil.API_KEY),requestContent.get(ApiUtil.URL_PARAMS));

        int responseCode = connection.getResponseCode();
        JSONObject responseJson = ConnectionUtil.convertToJsonObject(connection);

        //return responseJson.getBoolean("success");
        return responseJson;
    }
}