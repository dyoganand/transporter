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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;


public class RequestSummaryActivity extends Activity implements OnMapReadyCallback {

    private static final String ERROR_PASSCODE_INVALID = "4";
    private GoogleMap mGoogleMap;
    private MapFragment mMapFragment;
    private Button mStatusButton;
    private MarkerOptions mPickupMarker, mDropMarker;
    private String mRequestId;
    private LatLng mPickupLatLong, mDropLatLong;
    private int mStatus;

    public static String EXTRA_REQUEST_ID = "com.transporter.porter.REQUEST_ID";

    private static final int STATUS_ASSIGNED 	= 2;
    private static final int STATUS_PICKED_UP 	= 3;
    private static final int STATUS_DELIVERED 	= 4;

    int requestStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_summary);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Intent intent = getIntent();
        mRequestId = intent.getStringExtra(EXTRA_REQUEST_ID);
        mStatusButton = (Button) findViewById(R.id.btnStatus);

        // Get the request lat long and status from local db
        if(!findRequestDetails(mRequestId)) return;
        // Initialize the map with markers and set the status
        try {
            mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
            mMapFragment.getMapAsync(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
        setStatus();
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
                startActivity(intent);*/
                return true;
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
        PorterDbHelper dbHelper = new PorterDbHelper(RequestSummaryActivity.this);
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

    public void onDeliveryAction(View view) throws IOException, JSONException {
        String currentStatusTxt = (String) mStatusButton.getText();

        if(currentStatusTxt.equals("PICK UP")) {
            if(!handlePickup()) return;
            // Commit the new status to local db
            updateStatus();
            // Change the status text
            setStatus();
            Toast.makeText(this, "Yay!! We have picked up", Toast.LENGTH_LONG).show();
        } else if(currentStatusTxt.equals("DELIVER")) {
            handleDelivery();
        }
    }

    // Update the local db with the new status
    private void updateStatus() {
        PorterDbHelper dbHelper = new PorterDbHelper(RequestSummaryActivity.this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // New value for one column
        ContentValues values = new ContentValues();
        values.put(PorterContract.RequestDetails.COLUMN_NAME_STATUS, String.valueOf(mStatus));

        // Which row to update, based on the ID
        String selection = PorterContract.RequestDetails.COLUMN_NAME_REQUEST_ID + " LIKE ?";
        String[] selectionArgs = { mRequestId };

        int count = db.update(
                PorterContract.RequestDetails.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        Log.d("REQUESTER", "We updated the row : " + count);
        db.close();
    }

    private boolean handlePickup() throws IOException, JSONException {
        boolean success = true;
        if(sendPickedupRequest()) {
            mStatus = STATUS_PICKED_UP; // Set the status to PICKED UP in local db
        }
        else {
            Toast.makeText(this, getString(R.string.server_error), Toast.LENGTH_LONG).show();
            success = false;
        }
        return success;
    }

    private void handleDelivery() throws IOException, JSONException {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Complete Delivery");
        alert.setMessage("Please enter the PIN shared by the customer");
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                Log.d("REQUESTER", "Sending this PIN : " + value);
                try {
                    JSONObject response = sendDeliveredRequest(value);
                    if(response.optBoolean("success", false)) {
                        mStatus = STATUS_DELIVERED; // Set the status to DELIVERED in local db
                        // Commit the new status to local db
                        updateStatus();
                        // Change the status text
                        setStatus();
                        Toast.makeText(RequestSummaryActivity.this, "Yay!! We have delivered. Go online for next delivery!", Toast.LENGTH_LONG).show();
                        Intent i = new Intent(RequestSummaryActivity.this, StandbyActivity.class);
                        startActivity(i);
                    } else {
                        Log.d("REQUESTER", "Could not set status to delivered");
                        if(response.getString("error_code").equals(ERROR_PASSCODE_INVALID)) {
                            Toast.makeText(RequestSummaryActivity.this, R.string.error_invalid_passcode, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RequestSummaryActivity.this, R.string.server_error, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                } catch(JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    private void setStatus() {
        Log.d("REQUESTER", "We are setting the status as : " + mStatus);
        switch(mStatus) {
            case STATUS_ASSIGNED:
                mStatusButton.setText("PICK UP"); // ASSIGNED
                break;
            case STATUS_PICKED_UP:
                mStatusButton.setText("DELIVER"); // PICKED UP
                break;
            case STATUS_DELIVERED:
                mStatusButton.setEnabled(false); // DELIVERED
                mStatusButton.setText("DELIVERED");
                break;
            default:
                mStatusButton.setText("Unknown");
                break;
        }
    }

    private boolean findRequestDetails(String requestId) {
        boolean validRequest = true;

        PorterDbHelper dbHelper = new PorterDbHelper(RequestSummaryActivity.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                PorterContract.RequestDetails._ID,
                PorterContract.RequestDetails.COLUMN_NAME_PICKUP_LAT,
                PorterContract.RequestDetails.COLUMN_NAME_PICKUP_LONG,
                PorterContract.RequestDetails.COLUMN_NAME_DROP_LAT,
                PorterContract.RequestDetails.COLUMN_NAME_DROP_LONG,
                PorterContract.RequestDetails.COLUMN_NAME_STATUS,
        };
        String selection = PorterContract.RequestDetails.COLUMN_NAME_REQUEST_ID + " = ?";
        String[] selectionArgs = {
                requestId
        };

        Cursor c = db.query(
                PorterContract.RequestDetails.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if(c.moveToFirst()) {
            double latitude, longitude;
            latitude = c.getDouble(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_PICKUP_LAT));
            longitude = c.getDouble(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_PICKUP_LONG));
            mPickupLatLong = new LatLng(latitude, longitude);
            latitude = c.getDouble(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_DROP_LAT));
            longitude = c.getDouble(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_DROP_LONG));
            mDropLatLong = new LatLng(latitude, longitude);
            mStatus = c.getInt(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_STATUS));
        } else {
            Toast.makeText(this, getString(R.string.error_request_details_not_available), Toast.LENGTH_LONG).show();
            validRequest = false;
        }

        db.close();
        c.close();
        return validRequest;
    }


    @Override
    public void onMapReady(GoogleMap map) {

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, true);
        Location myLocation = locationManager.getLastKnownLocation(provider);

        mPickupMarker = new MarkerOptions();
        mDropMarker = new MarkerOptions();

        mPickupMarker.position(mPickupLatLong);
        mPickupMarker.title("Pickup Location");
        mPickupMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        mDropMarker.position(mDropLatLong);
        mDropMarker.title("Drop Location");

        map.addMarker(mPickupMarker);
        map.addMarker(mDropMarker);
        CameraPosition cameraPosition = new CameraPosition.Builder().target(mPickupLatLong).zoom(15).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        map.setMyLocationEnabled(true);
    }

    public void onMoreInfo(View v) {
        /*Intent i = new Intent(this, RequestDetailsActivity.class);
        i.putExtra(EXTRA_REQUEST_ID, mRequestId);
        startActivity(i);*/
    }

    private String getDeviceUUID() {
        String email = "";
        String deviceUuid = "";
        // Check if the user info is available in the user db
        PorterDbHelper dbHelper = new PorterDbHelper(RequestSummaryActivity.this);
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

    public boolean sendPickedupRequest() throws IOException, JSONException {

        HashMap<String, String> requestContent = ApiUtil.getPickupDeliveryRequest();
        String api_key = requestContent.get(ApiUtil.API_KEY_REQUIRED).equals(ApiUtil.YES) ? getAPIKey() : null;
        requestContent.put("api_key", api_key);

        JSONObject device = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject pickedup_params = new JSONObject();

        device.put("device_uuid", getDeviceUUID());
        pickedup_params.put("device", device);

        request.put("id", String.valueOf(mRequestId));
        pickedup_params.put("request", request);

        requestContent.put("url_params", pickedup_params.toString());

        HttpURLConnection connection = ConnectionUtil.getConnection(requestContent.get(ApiUtil.URL_KEY),requestContent.get(ApiUtil.METHOD),
                requestContent.get(ApiUtil.API_KEY),requestContent.get(ApiUtil.URL_PARAMS));

        int responseCode = connection.getResponseCode();
        JSONObject responseJson = ConnectionUtil.convertToJsonObject(connection);

        return responseJson.getBoolean("success");
    }

    public JSONObject sendDeliveredRequest(String passCode) throws IOException, JSONException {

        HashMap<String, String> requestContent = ApiUtil.getDeliveredRequest();
        String api_key = requestContent.get(ApiUtil.API_KEY_REQUIRED).equals(ApiUtil.YES) ? getAPIKey() : null;
        requestContent.put("api_key", api_key);

        JSONObject device = new JSONObject();
        JSONObject request = new JSONObject();
        JSONObject delivered_params = new JSONObject();

        device.put("device_uuid", getDeviceUUID());
        delivered_params.put("device", device);

        request.put("id", String.valueOf(mRequestId));
        request.put("passcode", passCode);
        delivered_params.put("request", request);

        requestContent.put("url_params", delivered_params.toString());

        HttpURLConnection connection = ConnectionUtil.getConnection(requestContent.get(ApiUtil.URL_KEY),requestContent.get(ApiUtil.METHOD),
                requestContent.get(ApiUtil.API_KEY),requestContent.get(ApiUtil.URL_PARAMS));

        int responseCode = connection.getResponseCode();
        JSONObject responseJson = ConnectionUtil.convertToJsonObject(connection);

        //return responseJson.getBoolean("success");
        return responseJson;
    }


}