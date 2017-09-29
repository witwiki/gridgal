package com.example.gridgal;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Image;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    /** Request Code variable for READ_EXTERNAL_STORAGE permissions
     * (Choice of a random integer as long as it is >=0)
     */
    private static final int READ_EXTERNAL_STORAGE_PERMISSIONS_CODE = 1;
    private static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        /**
         *  Ask for user permissions !!!
         *  Generic private method to request Dangerous
         *  Permissions (in this case, "READ_EXTERNAL_STORAGE")
         */
        getUserPermission(READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_PERMISSIONS_CODE);

        GridView gridView = (GridView)findViewById(R.id.grid_view);
        ImageAdapter imageAdapter = new ImageAdapter(this);
        gridView.setAdapter(imageAdapter);

//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView parent, View view, int position, long id) {
//                // This tells the GridView to redraw itself
//                // in turn calling your BooksAdapter's getView method again for each cell
//                imageAdapter.notifyDataSetChanged();
//            }
//        });
    }

    public static Context getContext() {
        return mContext;
    }

    /** Generic private method to request Dangerous Permissions (in this case, "READ_EXTERNAL_STORAGE") */
    private void getUserPermission(String requestedPerm, Integer requestedCode){
        //  Checking if the Build version is API greater than 22
        if ((Build.VERSION.SDK_INT > 22)){
            //  Checking to see with the Permission(s) is Granted or Not
            //  If Permission(s) is NOT GRANTED
            if (ContextCompat.checkSelfPermission(MainActivity.this, requestedPerm) != PackageManager.PERMISSION_GRANTED){
                Log.v("TAG","Permission is granted");
                // Check to see the user has denied permission(s) previously
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, requestedPerm)){
                    //  Ask permission again in the case the user has denied permissions previously (as it is an important Permission)
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{requestedPerm}, requestedCode);
                }
                //  If Permission was Not Denied previously, a request is made
                else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {requestedPerm}, requestedCode);
                }
            }
            //  If Permission(s) GRANTED we add a message to say the Permission was granted
            else {
                Toast.makeText(this, "" + requestedPerm + " is already granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            //  If build is lower than 23 then Permission requested and granted during app install
            Log.v("TAG","Permission is granted");
        }
    }

    /**  Method to handle a requested permission */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //  Switch case statement is used to handle multiple request permission codes (if needed)
        switch (requestCode){
            //  Permission code for READ_EXTERNAL_STORAGE
            case READ_EXTERNAL_STORAGE_PERMISSIONS_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    //  Permission Denied
                    Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

}
