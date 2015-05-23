package com.transporter.porter;

import java.util.HashMap;

public class ApiUtil {

    static final String URL_KEY                     = "url";
    static final String METHOD                      = "method";
    static final String API_KEY                     = "api_key";
    static final String URL_PARAMS                  = "url_params";
    static final String API_KEY_REQUIRED            = "api_key_required";
    static final String GET                         = "GET";
    static final String POST                        = "POST";
    static final String PATCH                       = "PATCH";
    static final String DELETE                      = "DELETE";
    static final String YES                         = "yes";
    static final String NO                          = "no";

    static final String DOMAIN_NAME                 = "http://mobihelp-dd.ngrok.com";

    static final String LOGIN_URL                   = DOMAIN_NAME+"/login.json";
    static final String REQUEST_CREATION_URL        = DOMAIN_NAME+"/requests.json";
    static final String REQUEST_STATUS_URL          = DOMAIN_NAME+"/requests/{id}/status.json";
    static final String USER_SIGNUP_URL             = DOMAIN_NAME+"/user.json";
    static final String USER_UPDATE_URL             = DOMAIN_NAME+"/user.json";
    static final String RECEIVER_UPDATE_URL         = DOMAIN_NAME+"/receiver.json";
    static final String CURRENT_LOCATION_UPDATE_URL = DOMAIN_NAME+"/current_location.json";
    static final String ASSIGN_DELIVERY_URL         = DOMAIN_NAME+"/delivery/assign.json";
    static final String PICKUP_DELIVERY_URL         = DOMAIN_NAME+"/delivery/picked_up.json";
    static final String DELIVERED_URL               = DOMAIN_NAME+"/delivery/delivered.json";

    static HashMap<String, String> request;

    public static HashMap<String,String> getLoginRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, LOGIN_URL);
        request.put(METHOD, POST);
        request.put(API_KEY_REQUIRED, NO);
        return request;
    }

    public static HashMap<String,String> getRequestCreationRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, REQUEST_CREATION_URL);
        request.put(METHOD, POST);
        request.put(API_KEY_REQUIRED, YES);
        return request;
    }

    public static HashMap<String, String> getRequestDetailsRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, REQUEST_STATUS_URL);
        request.put(METHOD, GET);
        request.put(API_KEY_REQUIRED, YES);
        return request;
    }

    public static HashMap<String,String> getSignupRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, USER_SIGNUP_URL);
        request.put(METHOD, POST);
        request.put(API_KEY_REQUIRED, NO);
        return request;
    }

    public static HashMap<String,String> getUserUpdationRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, USER_UPDATE_URL);
        request.put(METHOD, PATCH);
        request.put(API_KEY_REQUIRED, YES);
        return request;
    }

    public static HashMap<String,String> getReceiverUpdationRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, RECEIVER_UPDATE_URL);
        request.put(METHOD, PATCH);
        request.put(API_KEY_REQUIRED, YES);
        return request;
    }

    public static HashMap<String,String> getCurrentLocationUpdationRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, CURRENT_LOCATION_UPDATE_URL);
        request.put(METHOD, POST);
        request.put(API_KEY_REQUIRED, YES);
        return request;
    }

    public static HashMap<String,String> getAssignDeliveryRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, ASSIGN_DELIVERY_URL);
        request.put(METHOD, POST);
        request.put(API_KEY_REQUIRED, YES);
        return request;
    }

    public static HashMap<String,String> getPickupDeliveryRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, PICKUP_DELIVERY_URL);
        request.put(METHOD, POST);
        request.put(API_KEY_REQUIRED, YES);
        return request;
    }

    public static HashMap<String,String> getDeliveredRequest() {
        request = new HashMap<String,String>();
        request.put(URL_KEY, DELIVERED_URL);
        request.put(METHOD, POST);
        request.put(API_KEY_REQUIRED, YES);
        return request;
    }
}