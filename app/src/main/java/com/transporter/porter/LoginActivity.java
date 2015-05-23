package com.transporter.porter;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.UUID;


public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        /*PorterDbHelper dbHelper = new PorterDbHelper(LoginActivity.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        db.execSQL(PorterContract.RequestDetails.TRUNCATE_TABLE);
        db.execSQL(PorterContract.RequestDetails.DROP_TABLE);
        db.execSQL(PorterContract.RequestDetails.CREATE_TABLE);*/

        if(userLoggedIn()) {
            // Open ViewRequestsActivity
            Toast.makeText(this, "Already logged in", Toast.LENGTH_LONG).show();
            /*Intent i = new Intent(this, StandbyActivity.class);
            startActivity(i);*/
        }

    }

    private boolean userLoggedIn() {

        if(getAPIKey() == null) return false;

        // Check if the user info is available in the user db
        PorterDbHelper dbHelper = new PorterDbHelper(LoginActivity.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = { PorterContract.User._ID };
        Cursor c = db.query(PorterContract.User.TABLE_NAME, projection, null, null, null, null, null);
        int rows = c.getCount();
        c.close();
        dbHelper.close();

        if(rows == 0) return false;

        return true;
    }

    private String getAPIKey() {

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.apikey_file), Context.MODE_PRIVATE);
        return sharedPref.getString(getString(R.string.saved_apikey), null);
    }

    public void onLogin(View view) throws IOException, JSONException {

        String usrEmail = ((EditText) findViewById(R.id.userEmail)).getText().toString();
        String usrPwd   = ((EditText) findViewById(R.id.userPwd)).getText().toString();
        String deviceUUID;

        if(!validateInputs()) return;
        deviceUUID = getDeviceUUID(usrEmail);
        if(!verifyCredentials(usrEmail, usrPwd, deviceUUID)) return;

        Toast.makeText(this, "Yay!! I should go to View Requests", Toast.LENGTH_LONG).show();
        /*Intent i = new Intent(this, StandbyActivity.class);
        startActivity(i);*/
    }

    public void onSignup(View view) {
        /*Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);*/
    }

    private boolean validateInputs() {
        boolean isValid = true;

        EditText usrEmailEditText   = ((EditText) findViewById(R.id.userEmail));
        String usrEmail             = usrEmailEditText.getText().toString();
        EditText usrPwdEditText     = ((EditText) findViewById(R.id.userPwd));
        String usrPwd               = usrPwdEditText.getText().toString();

        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(usrEmail).matches()) {
            usrEmailEditText.setError("Invalid email address");
            isValid = false;
        }
        else if(usrPwd.isEmpty()) {
            usrPwdEditText.setError("Invalid password");
            isValid = false;
        }

        return isValid;
    }

    private String getDeviceUUID(String usrEmail) {
        String deviceUUID;

        if((deviceUUID = getDeviceUUIDFromLocalDb(usrEmail)) != null) {
            return deviceUUID;
        }
        deviceUUID = generateDeviceUUID(usrEmail);

        return deviceUUID;
    }

    private String getDeviceUUIDFromLocalDb(String usrEmail) {

        String deviceUUID;
        // If there is an existing device uuid return it, else generate a new one
        PorterDbHelper dbHelper = new PorterDbHelper(LoginActivity.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                PorterContract.EmailDeviceUUIDMapping._ID,
                PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_EMAIL,
                PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_DEVICE_UUID
        };
        String selection = PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_EMAIL + " = ?";
        String[] selectionArgs = {
                usrEmail
        };

        Cursor c = db.query(
                PorterContract.EmailDeviceUUIDMapping.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if(c.moveToFirst()) {
            deviceUUID = c.getString(c.getColumnIndex(PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_DEVICE_UUID));
            db.close();
            c.close();
            return deviceUUID;
        }
        db.close();
        c.close();
        return null;
    }

    /*public int i = 0;
    public void check(View view) {

        EditText usrEmailEditText   = ((EditText) findViewById(R.id.userEmail));
        String android_id = UUID.randomUUID().toString();
        //String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        //usrEmailEditText.setText("i : " + String.valueOf(i) + " Length : " + android_id.length());
        //i++;
        PorterDbHelper dbHelper = new PorterDbHelper(LoginActivity.this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(PorterContract.User.TRUNCATE_TABLE);
        db.execSQL(PorterContract.EmailDeviceUUIDMapping.TRUNCATE_TABLE);
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.apikey_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();


    }*/

    private String generateDeviceUUID(String usrEmail) {

        String android_id = UUID.randomUUID().toString();
        return android_id;
    }

    private boolean verifyCredentials(String usrEmail, String usrPwd, String deviceUUID) throws IOException, JSONException {

        boolean isVerified = false;

        String apiKey;// = "r6XFEyMvEYTA6RbdrZmW"; // The api key from the server should be encoded

        // Send the email, password and device uuid to server
        JSONObject responseJson = sendLoginRequest(usrEmail, usrPwd, deviceUUID);
        // In case of errors, notify the user
        if(!responseJson.optBoolean("success", false)) {
            switch(responseJson.getInt("error_code")) {
                case 1:
                    deviceUUID = getDeviceUUID(usrEmail);
                    verifyCredentials(usrEmail, usrPwd, deviceUUID);
                    break;
                case 2:
                    Toast.makeText(this, getString(R.string.error_invalid_credentials), Toast.LENGTH_LONG).show();
                    break;
            }
            return isVerified;
        }

        // If verified, get the api key and user info. Update the local user db
        apiKey = responseJson.getString("api_key");
        JSONObject userJson = responseJson.getJSONObject("user");

        if(apiKey == null || userJson == null) {
            Toast.makeText(this, getString(R.string.server_error), Toast.LENGTH_LONG).show();
            return isVerified;
        }

        saveUserInfo(userJson, deviceUUID, apiKey);
        isVerified = true;
        return isVerified;
    }

    public JSONObject sendLoginRequest(String emailId, String password, String deviceUuid) throws IOException, JSONException {

        HashMap<String, String> requestContent = ApiUtil.getLoginRequest();
        String api_key = requestContent.get(ApiUtil.API_KEY_REQUIRED).equals(ApiUtil.YES) ? "api_key" : null;
        requestContent.put("api_key", api_key);


        //requestContent.put("url_params", urlEncodeUTF8(urlParams));

        JSONObject device = new JSONObject();
        JSONObject userSession = new JSONObject();
        JSONObject userInfo = new JSONObject();

        device.put("device_uuid", deviceUuid);
        userInfo.put("email", emailId);
        userInfo.put("password", password);
        userInfo.put("device", device);
        userSession.put("user_session", userInfo);

        requestContent.put("url_params", userSession.toString());

        HttpURLConnection connection = ConnectionUtil.getConnection(requestContent.get(ApiUtil.URL_KEY),requestContent.get(ApiUtil.METHOD),
                requestContent.get(ApiUtil.API_KEY),requestContent.get(ApiUtil.URL_PARAMS));

        int responseCode = connection.getResponseCode();
        JSONObject responseJson = ConnectionUtil.convertToJsonObject(connection);

        //return responseJson.getBoolean("success");
        return responseJson;
    }

    private void saveUserInfo(JSONObject userJson, String deviceUUID, String apiKey) throws JSONException {

        // clear user db and api key
        PorterDbHelper dbHelper = new PorterDbHelper(LoginActivity.this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(PorterContract.User.TRUNCATE_TABLE);

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.apikey_file), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        editor.commit();

        // insert into user db and write the api key to shared preg
        ContentValues values = new ContentValues();
        values.put(PorterContract.User.COLUMN_NAME_FIRST_NAME, userJson.getString("first_name"));
        values.put(PorterContract.User.COLUMN_NAME_LAST_NAME, userJson.getString("last_name"));
        values.put(PorterContract.User.COLUMN_NAME_EMAIL, userJson.getString("email"));
        values.put(PorterContract.User.COLUMN_NAME_PHONE, userJson.getString("phone_number"));
        values.put(PorterContract.User.COLUMN_NAME_ADDRESS, userJson.getString("address"));
        db.insert(PorterContract.User.TABLE_NAME, null, values);

        editor.putString(getString(R.string.saved_apikey), apiKey);
        editor.commit();

        String usrEmail = ((EditText) findViewById(R.id.userEmail)).getText().toString();
        // if the user has not previously logged in using this device, store the new device id
        if(getDeviceUUIDFromLocalDb(usrEmail) == null) {
            values = new ContentValues();
            values.put(PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_DEVICE_UUID, deviceUUID);
            values.put(PorterContract.EmailDeviceUUIDMapping.COLUMN_NAME_EMAIL, usrEmail);
            db.insert(PorterContract.EmailDeviceUUIDMapping.TABLE_NAME, null, values);
        }
        db.close();
    }
}