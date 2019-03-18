package com.myhexaville.login.Customer;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.myhexaville.login.DatabaseHelper;
import com.myhexaville.login.Driver.DriverActivity;
import com.myhexaville.login.Driver.DriverDuringRideActivity;

import com.myhexaville.login.R;
import com.myhexaville.login.RideRequests;
import com.myhexaville.login.Rides;
import com.myhexaville.login.WebViewMaps;

import java.util.HashMap;
import java.util.Map;

public class CustomerDuringRideActivity extends AppCompatActivity {
    private SQLiteOpenHelper openHelper;
    private SQLiteDatabase db,dbRead;
    private TextView tvFare;
    private Button btStopRide;
    private Rides rides;
    private Button btSendSmsEmergency;
    private String data;
    private static final String TAG = "TAG" ;
    private WebView wvMaps;
    private TextView tvCurrentFare;
    private FirebaseFirestore dbOnline;

    private double specificRating,newRating;
    private int numberOfCustomersServed;
    private Button btSubmitRating;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkInternetPermissions();
        checkFilePermissions();
        setContentView(R.layout.activity_customer_during_ride);
        openHelper = new DatabaseHelper(this);
        db=openHelper.getWritableDatabase();
        dbRead=openHelper.getReadableDatabase();
        btStopRide=findViewById(R.id.bt_stop_ride_customer);
        btSendSmsEmergency=findViewById(R.id.bt_send_sms_emergency_during_ride);
        tvFare=findViewById(R.id.tv_riding_during_customer);
        tvCurrentFare=findViewById(R.id.tv_fare_riding_during_customer);
        dbOnline= FirebaseFirestore.getInstance();
        rides=(Rides)getIntent().getSerializableExtra("RidesObject");
        final DocumentReference docRef = dbOnline.collection("duringRide").document(rides.getTimeStamp()+" "+rides.getOtp());
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                String source = snapshot != null && snapshot.getMetadata().hasPendingWrites()
                        ? "Local" : "Server";

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, source + " data: " + snapshot.getData());
                    String d=snapshot.getData().get("distance").toString();
                    Log.d(TAG," THE VALUE OF DISTANCE IS "+d);



                    tvCurrentFare.setText(String.format("%.2f",fare(Float.parseFloat(d))));
                } else {
                    Log.d(TAG, source + " data: null");
                }
            }
        });

        btSendSmsEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defineEmergencyButton();

            }
        });

        wvMaps=findViewById(R.id.wv_maps_customer);



        String PickUpLatitude=Double.toString(getIntent().getDoubleExtra("PickUpLatitude",0.0));
        String PickUpLongitude=Double.toString(getIntent().getDoubleExtra("PickUpLongitude",0.0));
        String DropLatitude=Double.toString(getIntent().getDoubleExtra("DropLatitude",0.0));
        String DropLongitude=Double.toString(getIntent().getDoubleExtra("DropLongitude",0.0));
        Log.d(TAG," pickup point "+PickUpLatitude+" , "+PickUpLongitude+" and DROP point"+ DropLatitude+" , "+DropLongitude);

        WebSettings webSettings = wvMaps.getSettings();
        webSettings.setJavaScriptEnabled(true);
        WebViewMaps webViewClient = new WebViewMaps(this);
        wvMaps.setWebViewClient(webViewClient);
        wvMaps.loadUrl("https://www.google.com/maps/dir/"+PickUpLatitude+"%C2%B0+N,+"+PickUpLongitude+"%C2%B0+E/"+DropLatitude+"%C2%B0+N,+"+DropLongitude+"%C2%B0+E/data=!3m1!4b1!4m10!4m9!1m3!2m2!1d78.4867!2d17.385!1m3!2m2!1d80.2707!2d13.0827!3e0");




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


                dbOnline.collection("rating").document(rides.getDriverPhoneNumber())
                        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG, "!DocumentSnapshot data: " + document.getData());
                                String avg=document.getData().get("average").toString();
                                String number=document.getData().get("number").toString();
                                double a=Double.parseDouble(avg);
                                int n=Integer.parseInt(number);
                                newRating=((a*n)+specificRating)/(n+1);
                                numberOfCustomersServed=n+1;
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

        dbOnline.collection("rating").document(rides.getDriverPhoneNumber())
                .set(city)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "!!DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });

    }


    private void defineFirstClick(){
        rides.setFare(tvCurrentFare.getText().toString());

        tvFare.setText("Final Fare = Rs. "+rides.getFare());
        //getting the final distance
        dbOnline.collection("duringRide").document(rides.getTimeStamp()+" "+rides.getOtp())
        .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        String finalDist=document.getData().get("distance").toString();
                        rides.setDistance(finalDist);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
        btStopRide.setText("Go To Home Page");
        //pushing values to the ride history of that driver
        showRatingDialog();

        insertValuesToRidesTable(rides.getPickupPoint(),rides.getDropPoint(),rides.getDistance(),rides.getOtp(),rides.getTimeStamp(),rides.getFare(),rides.getDriverPhoneNumber(),rides.getCustomerPhoneNumber());
        btStopRide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                defineSecondClick();

            }
        });



    }
    private void insertValuesToRidesTable(String pickupPoint,String dropPoint, String distance,String otp,String timeStamp,String fare,String driverPhoneNumber, String customerPhoneNumber){
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
        Intent i=new Intent(CustomerDuringRideActivity.this,CustomerActivity.class);
        startActivity(i);
    }
    private void defineEmergencyButton(){
        data=((CabHiring) this.getApplication()).getPhoneNumber();
        //openWhatsAppNew();
        Cursor cursor=dbRead.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_NAME_6 + " WHERE " + DatabaseHelper.COL_61 + "=? ", new String[]{data});
        if(cursor.getCount()==0){
            Toast.makeText(this,"Please Add Emergency Contacts",Toast.LENGTH_SHORT).show();

        }else{

            if(cursor.moveToFirst()){


                String phoneNumber1=cursor.getString(cursor.getColumnIndex("emergencyPhoneNumber"));
                Log.d(TAG,"phone number sending sms to "+phoneNumber1);

                sendSms(phoneNumber1);

                if(cursor.moveToNext()){

                    String phoneNumber2=cursor.getString(cursor.getColumnIndex("emergencyPhoneNumber"));
                    Log.d(TAG,"phone number sending sms to "+phoneNumber2);
                    sendSms(phoneNumber2);

                    Toast.makeText(this," Message Successfully Sent ",Toast.LENGTH_SHORT).show();

                }
            }

        }



    }
    private void sendSms(String phoneNumber){
        String messageToSend = "I need help. DRIVER: "+rides.getDriverPhoneNumber();
        String number = "+91"+phoneNumber;

        SmsManager.getDefault().sendTextMessage(number, null, messageToSend, null,null);
        Toast.makeText(this," Message sent by Sms Manager ",Toast.LENGTH_SHORT).show();

    }

    private void checkFilePermissions() {
        if (ContextCompat.checkSelfPermission(CustomerDuringRideActivity.this, android.Manifest.permission.SEND_SMS)
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
    private void checkInternetPermissions(){
        if (ContextCompat.checkSelfPermission(CustomerDuringRideActivity.this, android.Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
        }
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
        {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.INTERNET");

            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{android.Manifest.permission.INTERNET}, 1001); //Any number
            }
        }else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

}
