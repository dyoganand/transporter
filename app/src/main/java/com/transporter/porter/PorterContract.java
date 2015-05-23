package com.transporter.porter;

import android.provider.BaseColumns;

public final class PorterContract {

    private PorterContract() {}


    private static final String TEXT_TYPE           = " TEXT";
    private static final String COMMA_SEP           = ", ";

    public static abstract class User implements BaseColumns {

        public static final String TABLE_NAME               = "user";
        //public static final String COLUMN_NAME_USER_ID    = "user_id";
        public static final String COLUMN_NAME_EMAIL        = "email";
        public static final String COLUMN_NAME_FIRST_NAME   = "first_name";
        public static final String COLUMN_NAME_LAST_NAME    = "last_name";
        public static final String COLUMN_NAME_PHONE        = "phone_number";
        public static final String COLUMN_NAME_ADDRESS      = "address";

        public static final String CREATE_TABLE             = "CREATE TABLE " + TABLE_NAME +
                " (" +
                _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                COLUMN_NAME_EMAIL + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_FIRST_NAME + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_LAST_NAME + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_ADDRESS + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_PHONE + TEXT_TYPE +
                " )";

        public static final String DROP_TABLE       = "DROP TABLE IF EXISTS " + TABLE_NAME;
        public static final String TRUNCATE_TABLE   = "DELETE FROM " + TABLE_NAME;
    }

    public static abstract class EmailDeviceUUIDMapping implements BaseColumns {

        public static final String TABLE_NAME                   = "device_email_mapping";
        public static final String COLUMN_NAME_EMAIL            = "email";
        public static final String COLUMN_NAME_DEVICE_UUID      = "device_uuid";

        public static final String CREATE_TABLE                 = "CREATE TABLE " + TABLE_NAME +
                " (" +
                _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                COLUMN_NAME_EMAIL + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_DEVICE_UUID + TEXT_TYPE +
                " )";

        public static final String DROP_TABLE       = "DROP TABLE IF EXISTS " + TABLE_NAME;
        public static final String TRUNCATE_TABLE   = "DELETE FROM " + TABLE_NAME;
    }

    public static abstract class RequestDetails implements BaseColumns {

        public static final String TABLE_NAME                   = "request_details";
        public static final String COLUMN_NAME_REQUEST_ID       = "request_id";
        public static final String COLUMN_NAME_PICKUP_LAT       = "pickup_lat";
        public static final String COLUMN_NAME_PICKUP_LONG      = "pickup_long";
        public static final String COLUMN_NAME_PICKUP_ADDRESS   = "pickup_address";
        public static final String COLUMN_NAME_PICKUP_AREA      = "pickup_area";
        public static final String COLUMN_NAME_DROP_LAT         = "drop_lat";
        public static final String COLUMN_NAME_DROP_LONG        = "drop_long";
        public static final String COLUMN_NAME_DROP_ADDRESS     = "drop_address";
        public static final String COLUMN_NAME_DROP_AREA        = "drop_area";
        public static final String COLUMN_NAME_CUSTOMER_NAME    = "customer_name";
        public static final String COLUMN_NAME_CUSTOMER_PHONE   = "customer_phone_number";
        public static final String COLUMN_NAME_ORDER_ID         = "order_id";
        public static final String COLUMN_NAME_REQUESTER_NAME   = "requester_name";
        public static final String COLUMN_NAME_REQUESTER_PHONE  = "requester_phone_number";
        public static final String COLUMN_NAME_STATUS           = "status";
        public static final String COLUMN_NAME_DISTANCE         = "distance";

        public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME +
                " (" +
                _ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                COLUMN_NAME_REQUEST_ID + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_PICKUP_LAT + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_PICKUP_LONG + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_PICKUP_ADDRESS + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_PICKUP_AREA + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_DROP_LAT + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_DROP_LONG + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_DROP_ADDRESS + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_DROP_AREA + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_CUSTOMER_NAME + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_CUSTOMER_PHONE + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_ORDER_ID + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_REQUESTER_NAME + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_REQUESTER_PHONE + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_STATUS + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_DISTANCE + TEXT_TYPE +
                " )";

        public static final String DROP_TABLE       = "DROP TABLE IF EXISTS " + TABLE_NAME;
        public static final String TRUNCATE_TABLE   = "DELETE FROM " + TABLE_NAME;
    }

}