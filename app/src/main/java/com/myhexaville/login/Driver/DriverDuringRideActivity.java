package com.myhexaville.login.Driver;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.myhexaville.login.CabHiring;
import com.myhexaville.login.Customer.CustomerDuringRideActivity;
import com.myhexaville.login.DatabaseHelper;
import com.myhexaville.login.R;
import com.myhexaville.login.RideRequests;
import com.myhexaville.login.WebViewMaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DriverDuringRideActivity extends AppCompatActivity {
    private TextView tvFare;
    private Button btStopRide;
    private RideRequests rideRequests;
    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase db,dbRead,dbWrite;
    private Cursor cursor;
    private WebView wvMaps;
    private static final String TAG = "DocSnippets";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkFilePermissions();
        checkInternetPermissions();
        setContentView(R.layout.activity_driver_during_ride);
        openHelper = new DatabaseHelper(this);
        db=openHelper.getWritableDatabase();
        wvMaps=findViewById(R.id.wv_maps);
        rideRequests=(RideRequests) getIntent().getSerializableExtra("RequestObject");

        WebSettings webSettings = wvMaps.getSettings();
        webSettings.setJavaScriptEnabled(true);
        WebViewMaps webViewClient = new WebViewMaps(this);
        wvMaps.setWebViewClient(webViewClient);
        String PickUpLatitude=Double.toString(retrieveAddress(rideRequests.getPickupPoint()).getLatitude());
        String PickUpLongitude=Double.toString(retrieveAddress(rideRequests.getPickupPoint()).getLongitude());
        String DropLatitude=Double.toString(retrieveAddress(rideRequests.getDropPoint()).getLatitude());
        String DropLongitude=Double.toString(retrieveAddress(rideRequests.getDropPoint()).getLongitude());

        wvMaps.loadUrl("https://www.google.com/maps/dir/"+PickUpLatitude+"%C2%B0+N,+"+PickUpLongitude+"%C2%B0+E/"+DropLatitude+"%C2%B0+N,+"+DropLongitude+"%C2%B0+E/data=!3m1!4b1!4m10!4m9!1m3!2m2!1d78.4867!2d17.385!1m3!2m2!1d80.2707!2d13.0827!3e0");



        //Address address=(Address)rideRequests.getPickupPoint();
        btStopRide=findViewById(R.id.bt_stop_ride);
        tvFare=findViewById(R.id.tv_riding_during);
        btStopRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defineFirstClick();


            }
        });
    }


    private Address retrieveAddress(String addressString){
        Log.d(TAG, "geoLocateDrop: geolocating");

        Address address;


        Geocoder geocoder = new Geocoder(DriverDuringRideActivity.this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(addressString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocateDrop: IOException: " + e.getMessage() );
        }

        if(list.size() > 0){
            address = list.get(0);

            Log.d(TAG, "geoLocateDrop: found a location: " + address.toString());
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            return address;
        }
        else{
            return null;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && this.wvMaps.canGoBack()) {
            this.wvMaps.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    private void defineFirstClick(){
        tvFare.setText(" Estimated Fare = Rs. "+rideRequests.getFare());
        btStopRide.setText(" Send Bill");
        pushingAmountEarned();
        //pushing values to the ride history of that driver
        String data=((CabHiring) this.getApplication()).getPhoneNumber();
        insertValuesToRidesTable(rideRequests.getPickupPoint(),rideRequests.getDropPoint(),rideRequests.getDistance(),rideRequests.getOtp(),rideRequests.getTimeStamp(),rideRequests.getFare(),data,rideRequests.getCustomerPhoneNumber());



        btStopRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defineSecondClick();

            }
        });


    }
    public void insertValuesToRidesTable(String pickupPoint,String dropPoint, String distance,String otp,String timeStamp,String fare,String driverPhoneNumber, String customerPhoneNumber){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.COL_52, pickupPoint);
        contentValues.put(DatabaseHelper.COL_53, dropPoint);
        contentValues.put(DatabaseHelper.COL_54, distance);
        contentValues.put(DatabaseHelper.COL_55, otp);
        contentValues.put(DatabaseHelper.COL_56, timeStamp);
        contentValues.put(DatabaseHelper.COL_57, fare);
        contentValues.put(DatabaseHelper.COL_58,driverPhoneNumber);
        contentValues.put(DatabaseHelper.COL_59,customerPhoneNumber);
        long id = db.insert(DatabaseHelper.TABLE_NAME_5, null, contentValues);
        Toast.makeText(this," Values inserted in the rides table ",Toast.LENGTH_SHORT).show();

    }
    private void defineSecondClick(){
        sendBill();
        Intent i=new Intent(DriverDuringRideActivity.this,DriverActivity.class);
        startActivity(i);

    }
    protected void sendMessage(String message) {
        try {
            String phoneNumber = "+91"+rideRequests.getCustomerPhoneNumber();
            SmsManager smsManager = SmsManager.getDefault();

            ArrayList<String> parts = smsManager.divideMessage(message);
            //smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts,
                    null, null);
            Toast.makeText(getApplicationContext(), "SMS Send !", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "SMS Failed ! "+e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void sendBill(){
        sendMessage("GREETINGS FROM VELOCE CABS. YOUR RIDE BILL AND DETAILS OF YOUR TRAVEL ON : "+rideRequests.getTimeStamp()+"\n" +
                "are Distance Travelled  : "+rideRequests.getDistance()+" kms, \n" +
                "Total Fare          : "+rideRequests.getFare()+" Rs.\n" +
                "\nTHANK YOU!!!");
//        Random random=new Random();
//        String no = "+91"+rideRequests.getCustomerPhoneNumber();
//        String msg ="GREETINGS FROM VELOCE CABS. YOUR RIDE BILL AND DETAILS OF YOUR TRAVEL ON :"+rideRequests.getTimeStamp()+
//                "are Distance Travelled  : "+rideRequests.getDistance()+" kms, "+
//                " Total Fare          : "+rideRequests.getFare()+" Rs."+
//                "\nTHANK YOU!!!";
//        SmsManager.getDefault().sendTextMessage(no, null, msg, null,null);
//        Toast.makeText(this," Message sent by Sms Manager ",Toast.LENGTH_SHORT).show();

////Getting intent and PendingIntent instance
//        Intent intent = new Intent(getApplicationContext(), CustomerActivity.class);
//        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
////Get the SmsManager instance and call the sendTextMessage method to send message
//        SmsManager sms = SmsManager.getDefault();
//        sms.sendTextMessage(no, null, msg, pi, null);
//        Toast.makeText(getApplicationContext(), "Bill Sent successfully!",
//                Toast.LENGTH_LONG).show();


    }
    private void pushingAmountEarned(){

        dbRead=openHelper.getReadableDatabase();
        dbWrite=openHelper.getWritableDatabase();
        String data=((CabHiring) this.getApplication()).getPhoneNumber();
        cursor=dbRead.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_NAME_2 + " WHERE " + DatabaseHelper.COL_21 + "=? ", new String[]{data});

        if(cursor.moveToFirst()){

            do{

                String wallet=cursor.getString(cursor.getColumnIndex("amountEarned"));
                float WalletValue=Float.parseFloat(wallet);
                float newValue=WalletValue+Float.parseFloat(rideRequests.getFare());
                incrementingWallet(""+String.format("%.2f",newValue));


            } while(cursor.moveToNext());
        }
        cursor.close();

    }
    private void incrementingWallet(String newValue){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.COL_25, newValue);
        String data=((CabHiring) this.getApplication()).getPhoneNumber();
        dbWrite.update(DatabaseHelper.TABLE_NAME_2,contentValues,DatabaseHelper.COL_21+" =? ",new String[]{data});

    }
    private void checkInternetPermissions() {
        if (ContextCompat.checkSelfPermission(DriverDuringRideActivity.this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
        }
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
        {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.INTERNET");

            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.INTERNET}, 1001); //Any number
            }
        }else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
    private void checkFilePermissions() {
        if (ContextCompat.checkSelfPermission(DriverDuringRideActivity.this, android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Toast.makeText(this,"Please Give Permissions",Toast.LENGTH_SHORT).show();
        }
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
        {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.SEND_SMS");

            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{android.Manifest.permission.SEND_SMS}, 1001); //Any number
            }
        }else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
}
