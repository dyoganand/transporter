package com.transporter.porter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SignupActivity extends Activity {

    public static final String TAG                              = "DELIVERER";
    private static final String ERROR_CODE_PHONE_ALREADY_EXISTS = "2";
    private static final String ERROR_CODE_EMAIL_ALREADY_EXISTS = "2";
    public static final String FIRST_NAME                       = "first_name";
    public static final String LAST_NAME                        = "last_name";
    public static final String PHONE_NUMBER                     = "phone_number";
    public static final String EMAIL                            = "email";
    public static final String PASSWORD                         = "password";
    public static final String VEHICLE_NUMBER                   = "vehicle_number";
    public static final String ADDRESS                          = "address";
    public static final String DELIVERER                        = "deliverer";
    private EditText mEditFirstName, mEditLastName, mEditUsrEmail, mEditUsrPwd, mEditPhoneNumber;
    private EditText mEditVehicleNumber, mEditAddress;
    private List<EditText> lstNonBlankFields;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        findViewById(R.id.firstName).requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mEditFirstName      = (EditText) findViewById(R.id.firstName);
        mEditLastName       = (EditText) findViewById(R.id.lastName);
        mEditPhoneNumber    = (EditText) findViewById(R.id.phoneNumber);
        mEditUsrEmail       = (EditText) findViewById(R.id.userEmail);
        mEditUsrPwd         = (EditText) findViewById(R.id.userPwd);
        mEditVehicleNumber  = (EditText) findViewById(R.id.userVehicleNumber);
        mEditAddress        = (EditText) findViewById(R.id.userAddress);

        lstNonBlankFields   = new ArrayList<EditText>();
        lstNonBlankFields.add(mEditFirstName);
        lstNonBlankFields.add(mEditLastName);
        lstNonBlankFields.add(mEditPhoneNumber);
        lstNonBlankFields.add(mEditUsrEmail);
        lstNonBlankFields.add(mEditUsrPwd);
        lstNonBlankFields.add(mEditVehicleNumber);
        lstNonBlankFields.add(mEditAddress);
    }

    public void onSignup(View view) throws IOException, JSONException {

        HashMap<String, String> userMap = getUserDetails();
        if(blankInputs()) return;
        if(!validInputs()) return;
        createDeliverer(userMap);
    }

    private HashMap<String, String> getUserDetails() {
        HashMap<String, String> userMap = new HashMap<String, String>();

        userMap.put(FIRST_NAME, mEditFirstName.getText().toString());
        userMap.put(LAST_NAME, mEditLastName.getText().toString());
        userMap.put(PHONE_NUMBER, mEditPhoneNumber.getText().toString());
        userMap.put(EMAIL, mEditUsrEmail.getText().toString());
        userMap.put(PASSWORD, mEditUsrPwd.getText().toString());
        userMap.put(VEHICLE_NUMBER, mEditVehicleNumber.getText().toString());
        userMap.put(ADDRESS, mEditAddress.getText().toString());

        return userMap;
    }

    private boolean blankInputs() {
        boolean isBlank = false;

        for(EditText e : lstNonBlankFields) {
            if(e.getText().toString().trim().isEmpty()) {
                e.setError(getString(R.string.error_blank_field));
                isBlank = true;
            }
        }

        return isBlank;
    }

    private boolean validInputs() {
        boolean isValid = true;

        if(!Patterns.EMAIL_ADDRESS.matcher(mEditUsrEmail.getText().toString()).matches()) {
            //Toast.makeText(this, getString(R.string.error_invalid_email), Toast.LENGTH_LONG).show();
            mEditUsrEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }
        if(!Patterns.PHONE.matcher(mEditPhoneNumber.getText().toString()).matches()) {
            mEditPhoneNumber.setError(getString(R.string.error_invalid_phone));
            isValid = false;
        }

        return isValid;
    }





    private void createDeliverer(HashMap<String, String> userMap) throws IOException, JSONException {

        // Send request to server and get the response
        JSONObject responseJson = sendSignupRequest(userMap);

        // Otherwise go to LoginActivity
        if(responseJson.optBoolean("success", false)) {
            Toast.makeText(this, getString(R.string.success_signup), Toast.LENGTH_LONG).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
        } // Notify the user of errors if any
        else {
            JSONObject errorJson = responseJson.optJSONObject("errors");
            Log.d(TAG, "errorJson = "+errorJson);
            Log.d(TAG, "responseJson = "+responseJson);
            if(errorJson == null) {
                Toast.makeText(this, getString(R.string.server_error), Toast.LENGTH_LONG).show();
                return;
            }

            JSONArray emailErrors = errorJson.optJSONArray("email");
            if(emailErrors != null) {
                for(int i=0; i<emailErrors.length(); i++) {
                    if(emailErrors.getString(i).equals(ERROR_CODE_EMAIL_ALREADY_EXISTS)) {
                        mEditUsrEmail.setError(getString(R.string.error_email_already_exists));
                    }
                }

            }

            JSONArray phoneErrors = errorJson.optJSONArray("phone_number");
            if(phoneErrors != null) {
                for(int i=0; i<phoneErrors.length(); i++) {
                    mEditPhoneNumber.setError(phoneErrors.getString(i) + ERROR_CODE_PHONE_ALREADY_EXISTS);
                    if(phoneErrors.getString(i).equals(ERROR_CODE_PHONE_ALREADY_EXISTS)) {
                        mEditPhoneNumber.setError(getString(R.string.error_phone_already_exists));
                    }
                }
            }


        }
        //mEditUsrEmail.setError(getString(R.string.error_phone_already_exists));
    }



    public JSONObject sendSignupRequest(HashMap<String, String> userMap) throws IOException, JSONException {

        HashMap<String, String> requestContent = ApiUtil.getSignupRequest();
        String api_key = requestContent.get(ApiUtil.API_KEY_REQUIRED).equals(ApiUtil.YES) ? "api_key" : null;
        requestContent.put("api_key", api_key);

        JSONObject userInfo = new JSONObject();
        JSONObject userParams = new JSONObject();

        userInfo.put(FIRST_NAME, userMap.get(FIRST_NAME));
        userInfo.put(LAST_NAME, userMap.get(LAST_NAME));
        userInfo.put(PHONE_NUMBER, userMap.get(PHONE_NUMBER));
        userInfo.put(EMAIL, userMap.get(EMAIL));
        userInfo.put(PASSWORD, userMap.get(PASSWORD));
        userInfo.put(VEHICLE_NUMBER, userMap.get(VEHICLE_NUMBER));
        userInfo.put(ADDRESS, userMap.get(ADDRESS));
        userInfo.put(DELIVERER, true);

        userParams.put("user", userInfo);
        requestContent.put("url_params", userParams.toString());

        HttpURLConnection connection = ConnectionUtil.getConnection(requestContent.get(ApiUtil.URL_KEY),requestContent.get(ApiUtil.METHOD),
                requestContent.get(ApiUtil.API_KEY),requestContent.get(ApiUtil.URL_PARAMS));

        int responseCode = connection.getResponseCode();
        JSONObject responseJson = ConnectionUtil.convertToJsonObject(connection);

        //return responseJson.getBoolean("success");
        return responseJson;
    }

}