package com.myhexaville.login.Driver;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.myhexaville.login.CabHiring;
import com.myhexaville.login.Customer.CustomerDuringRideActivity;
import com.myhexaville.login.DatabaseHelper;
import com.myhexaville.login.R;
import com.myhexaville.login.RideRequests;
import com.myhexaville.login.WebViewMaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DriverDuringRideActivity extends AppCompatActivity implements LocationListener {
    private TextView tvFare;
    private Button btStopRide;
    private RideRequests rideRequests;
    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase db,dbRead,dbWrite;
    private FirebaseFirestore dbOnline;
    private Cursor cursor;
    private WebView wvMaps;
    private static final String TAG = "DocSnippets";
    private TextView tvCurrentFare;
    protected LocationManager locationManager;
    private LatLng temp;
    private float d=0;
    int f=0;
    private double specificRating,newRating;
    private int numberOfCustomersServed;
    private Button btSubmitRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_during_ride);
        checkFilePermissions();
        checkInternetPermissions();
        tvCurrentFare=findViewById(R.id.tv_fare_during_ride_driver);
        openHelper = new DatabaseHelper(this);
        db=openHelper.getWritableDatabase();
        wvMaps=findViewById(R.id.wv_maps);
        rideRequests=(RideRequests) getIntent().getSerializableExtra("RequestCancelObject");
        dbOnline= FirebaseFirestore.getInstance();



        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000, 0, this);

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
    public float fare(float dist)  {
        float fare;
        if(dist>5){
            fare=dist-5;
            fare*=15;
            fare+=40;
        }
        else{
            fare =40;
        }

        return fare;
    }
    protected void showRatingDialog(){
        Dialog dialog=new Dialog(this);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_rating);
        dialog.setTitle("Rate your ride!");
        RatingBar ratingBar=(RatingBar)dialog.findViewById(R.id.ratingBar);
        dialog.show();
        btSubmitRating=(Button)dialog.findViewById(R.id.button2);
        btSubmitRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                specificRating=ratingBar.getRating();


                dbOnline.collection("rating").document(rideRequests.getCustomerPhoneNumber())
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                String avg=document.getData().get("average").toString();
                                String number=document.getData().get("number").toString();
                                double a=Double.parseDouble(avg);
                                int n=Integer.parseInt(number);
                                newRating=((a*n)+specificRating)/(n+1);
                                numberOfCustomersServed=n+1;
                                Log.d(TAG,"new rating and number"+newRating+" and "+numberOfCustomersServed+" and "+specificRating);
                                updateAverage(newRating,numberOfCustomersServed);


                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });
                dialog.dismiss();
            }
        });

    }
    private void updateAverage(double newRating,int numberOfCustomersServed){
        Map<String, Object> city = new HashMap<>();
        city.put("average", ""+newRating);
        city.put("number", ""+numberOfCustomersServed);

        dbOnline.collection("rating").document(rideRequests.getCustomerPhoneNumber())
                .set(city)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "!!!DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
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
        rideRequests.setFare(tvCurrentFare.getText().toString());
        DocumentReference docRef = dbOnline.collection("duringRide").document(rideRequests.getTimeStamp()+" "+rideRequests.getOtp());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        rideRequests.setDistance(document.getData().get("distance").toString());
                        Log.d(TAG,"setting the distance"+rideRequests.getDistance());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
        tvFare.setText(" Final Fare = Rs. "+rideRequests.getFare());
        btStopRide.setText("Send Bill");
        pushingAmountEarned();
        //pushing values to the ride history of that driver
        String data=((CabHiring) this.getApplication()).getPhoneNumber();
        insertValuesToRidesTable(rideRequests.getPickupPoint(),rideRequests.getDropPoint(),rideRequests.getDistance(),rideRequests.getOtp(),rideRequests.getTimeStamp(),rideRequests.getFare(),data,rideRequests.getCustomerPhoneNumber());
        Log.d(TAG,"FIRST CLICKED");

        showRatingDialog();


        btStopRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defineSecondClick();
                Log.d(TAG,"SECOND CLICKED");

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
            Log.d(TAG,e.toString());
        }
    }

    private void sendBill(){
        sendMessage("GREETINGS FROM VELOCE CABS. YOUR RIDE BILL AND DETAILS OF YOUR TRAVEL ON : "+rideRequests.getTimeStamp()+"\n" +
                "are Distance Travelled  : "+String.format("%.2f",Double.parseDouble(rideRequests.getDistance()))+" kms, \n" +
                "Total Fare          : "+String.format("%.2f",Double.parseDouble(rideRequests.getFare()))+" Rs.\n" +
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

    @Override
    public void onLocationChanged(Location location) {

        if(f==0){
            f=1;
            temp=new LatLng(location.getLatitude(),location.getLongitude());
            return;
        }

//        txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        float[] results=new float[3];
        Location.distanceBetween(location.getLatitude(),location.getLongitude(),temp.latitude,temp.longitude,results);
        //Toast.makeText(getApplicationContext(),"In inside",Toast.LENGTH_LONG).show();
        temp=new LatLng(location.getLatitude(),location.getLongitude());
        d+=(results[0]/1000.0);
        tvCurrentFare.setText(String.format("%.2f",fare(d)));

        DocumentReference washingtonRef = dbOnline.collection("duringRide").document(rideRequests.getTimeStamp()+" "+rideRequests.getOtp());

// Set the "isCapital" field of the city 'DC'
        washingtonRef
                .update("distance", d)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });




    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
