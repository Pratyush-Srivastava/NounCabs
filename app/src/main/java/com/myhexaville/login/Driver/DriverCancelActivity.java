package com.myhexaville.login.Driver;

import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.myhexaville.login.R;
import com.myhexaville.login.RideRequests;

public class DriverCancelActivity extends AppCompatActivity {
    private static final String TAG ="DriverCancel" ;
    Button cancel;
    EditText enterOTP;
    Button start;
    RideRequests rideRequests;
    FirebaseFirestore db;
    Location pickup, drop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_cancel);
        cancel = findViewById(R.id.driverCancel);
        enterOTP = findViewById(R.id.enterOTP);
        start = findViewById(R.id.startRide);
        db = FirebaseFirestore.getInstance();
        rideRequests=(RideRequests) getIntent().getSerializableExtra("RequestObject");
        listeningToRidesCollection();

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deletingFromRidesCollection();


            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(rideRequests.getOtp().equalsIgnoreCase(enterOTP.getText().toString())){
                    //this function changes otp and takes it to the new activity
                    changeOTP();


                }
                else{
                    enterOTP.setText("");
                    Toast.makeText(DriverCancelActivity.this, "Wrong OTP", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    public void deletingFromRidesCollection(){

        //deleting the document from the database
        Log.d(TAG, "6TH ENTRY JUST BEFORE DELETING WITH DOCUMENT ID "+rideRequests);

        db.collection("Rides").document(rideRequests.getTimeStamp()+" "+rideRequests.getOtp())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DELETED DOCUMENT SUCCESSFULLY");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting document", e);
                    }
                });
        return;


    }


    public void listeningToRidesCollection(){
        db.collection("Rides")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    Log.d(TAG, "New city: " + dc.getDocument().getData());
                                    break;
                                case MODIFIED:
                                    Log.d(TAG, "Modified city: " + dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Removed city: " + dc.getDocument().getData());
                                    if(rideRequests.getOtp().equalsIgnoreCase(dc.getDocument().getData().get("otp").toString()))
                                    driverCancelSuccessful();
                                    break;
                            }
                        }

                    }
                });

    }

    public void driverCancelSuccessful(){
        Intent intent = new Intent(DriverCancelActivity.this, DriverActivity.class);
        startActivity(intent);
    }


    public void changeOTP(){
        DocumentReference washingtonRef = db.collection("Rides").document(rideRequests.getTimeStamp() + " " + rideRequests.getOtp());

// Set the "isCapital" field of the city 'DC'
        washingtonRef
                .update("otp", "1"+rideRequests.getOtp())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                        moveToDriverDuringRideActivity();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                })
        ;


    }
    public void moveToDriverDuringRideActivity(){
        Toast.makeText(this,"OTP Matched",Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(DriverCancelActivity.this, DriverDuringRideActivity.class);
        intent.putExtra("RequestCancelObject", rideRequests);
        Log.d(TAG,"JUST BEFORE PASSING AN INTENT");
        startActivity(intent);

    }

}
