package com.transporter.porter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class RequestDetailsActivity extends Activity {

    TextView mDelivererNameTxt, mDelivererPhoneTxt, mOrderIdTxt, mCustomerNameTxt, mCustomerPhoneTxt, mCustomerAddressTxt;
    TextView mDistanceTxt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_details);

        Intent intent = getIntent();
        String requestId = intent.getStringExtra(RequestSummaryActivity.EXTRA_REQUEST_ID);

        initViews();
        showRequestDetails(requestId);
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
                Intent intent = new Intent(this, AllRequestsActivity.class);
                startActivity(intent);
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
        PorterDbHelper dbHelper = new PorterDbHelper(RequestDetailsActivity.this);
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

    private void initViews() {
        mDelivererNameTxt = (TextView)findViewById(R.id.delivererName);
        mDelivererPhoneTxt = (TextView)findViewById(R.id.delivererPhone);
        mOrderIdTxt = (TextView)findViewById(R.id.orderId);
        mCustomerNameTxt = (TextView)findViewById(R.id.customerName);
        mCustomerPhoneTxt = (TextView)findViewById(R.id.customerPhone);
        mCustomerAddressTxt = (TextView)findViewById(R.id.customerAddress);
        mDistanceTxt = (TextView)findViewById(R.id.distance);
    }

    private void showRequestDetails(String requestId) {
        PorterDbHelper dbHelper = new PorterDbHelper(RequestDetailsActivity.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {
                PorterContract.RequestDetails._ID,
                PorterContract.RequestDetails.COLUMN_NAME_REQUESTER_NAME,
                PorterContract.RequestDetails.COLUMN_NAME_REQUESTER_PHONE,
                PorterContract.RequestDetails.COLUMN_NAME_ORDER_ID,
                PorterContract.RequestDetails.COLUMN_NAME_CUSTOMER_NAME,
                PorterContract.RequestDetails.COLUMN_NAME_CUSTOMER_PHONE,
                PorterContract.RequestDetails.COLUMN_NAME_DROP_ADDRESS,
                PorterContract.RequestDetails.COLUMN_NAME_STATUS,
                PorterContract.RequestDetails.COLUMN_NAME_DISTANCE
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

            SpannableString delivererPhoneContent = new SpannableString(c.getString(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_REQUESTER_PHONE)));
            delivererPhoneContent.setSpan(new UnderlineSpan(), 0, delivererPhoneContent.length(), 0);
            SpannableString customerPhoneContent = new SpannableString(c.getString(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_CUSTOMER_PHONE)));
            customerPhoneContent.setSpan(new UnderlineSpan(), 0, customerPhoneContent.length(), 0);

            mDelivererNameTxt.setText(c.getString(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_REQUESTER_NAME)));
            mDelivererPhoneTxt.setText(delivererPhoneContent);
            mOrderIdTxt.setText(c.getString(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_ORDER_ID)));
            mCustomerNameTxt.setText(c.getString(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_CUSTOMER_NAME)));
            mCustomerPhoneTxt.setText(customerPhoneContent);
            mCustomerAddressTxt.setText(c.getString(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_DROP_ADDRESS)));
            mDistanceTxt.setText(c.getString(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_DISTANCE)) + " km");
        } else {
            Toast.makeText(this, getString(R.string.error_request_details_not_available), Toast.LENGTH_LONG).show();
        }

        db.close();
        c.close();
    }

    public void callPhone(View view) {
        String phoneNumber = ((TextView)view).getText().toString();
        phoneNumber = phoneNumber == null ? "" : phoneNumber;
        if(phoneNumber.isEmpty()) return;

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:"+phoneNumber));
        startActivity(intent);
    }
}
