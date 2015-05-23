package com.transporter.porter;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class AllRequestsActivity extends ListActivity {

    // These are the Contacts rows that we will retrieve
    private static String[] projection = new String[] {
            PorterContract.RequestDetails._ID,
            PorterContract.RequestDetails.COLUMN_NAME_REQUEST_ID,
            PorterContract.RequestDetails.COLUMN_NAME_ORDER_ID,
            PorterContract.RequestDetails.COLUMN_NAME_DROP_AREA
    };

    private static String selection = PorterContract.RequestDetails._ID + " = ?";




    private SQLiteDatabase db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PorterDbHelper dbHelper = new PorterDbHelper(AllRequestsActivity.this);
        db = dbHelper.getReadableDatabase();

        Cursor cursor = getRequests();
        startManagingCursor(cursor);

        // now create a new list adapter bound to the cursor.
        // SimpleListAdapter is designed for binding to a Cursor.
        ListAdapter adapter = new SimpleCursorAdapter(this, // Context.
                android.R.layout.simple_list_item_2,
                cursor,
                new String[] { PorterContract.RequestDetails.COLUMN_NAME_ORDER_ID,
                        PorterContract.RequestDetails.COLUMN_NAME_DROP_AREA },
                new int[] { android.R.id.text1, android.R.id.text2 });

        // Bind to our new adapter.
        setListAdapter(adapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_all_requests, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_on_standby:
                Intent intent = new Intent(this, StandbyActivity.class);
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
        PorterDbHelper dbHelper = new PorterDbHelper(AllRequestsActivity.this);
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
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String requestId = "";
        String[] selectionArgs = {
                String.valueOf(id)
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
            requestId = c.getString(c.getColumnIndex(PorterContract.RequestDetails.COLUMN_NAME_REQUEST_ID));
        }
        c.close();
        if(!requestId.isEmpty()) {
            Intent intent = new Intent(this, RequestSummaryActivity.class);
            intent.putExtra(RequestSummaryActivity.EXTRA_REQUEST_ID, requestId);
            startActivity(intent);
        }
    }

    private Cursor getRequests() {
        // Run query
        return db.query(PorterContract.RequestDetails.TABLE_NAME, projection, null, null, null, null, null);
    }

}