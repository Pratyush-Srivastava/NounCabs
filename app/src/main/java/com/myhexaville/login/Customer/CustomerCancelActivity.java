package com.myhexaville.login.Customer;

import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.myhexaville.login.R;
import com.myhexaville.login.Rides;
import static android.content.ContentValues.TAG;

public class CustomerCancelActivity extends AppCompatActivity {
    Button cancel;
    TextView OTP;
    Rides rides;
    FirebaseFirestore db;
    private Location pickup,drop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_cancel);
        db = FirebaseFirestore.getInstance();
        rides=(Rides)getIntent().getSerializableExtra("RidesObject");
        pickup=new Location("Delhi");
        drop=new Location("Delhi");

        cancel = findViewById(R.id.customerCancel);
        OTP = findViewById(R.id.customerOTP);
        listeningToRidesCollection();
        OTP.setText("Your OTP is: " +  rides.getOtp());


        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deletingFromRidesCollection();

            }
        });
    }
    public void deletingFromRidesCollection(){

        //deleting the document from the database
        Log.d(TAG, "6TH ENTRY JUST BEFORE DELETING WITH DOCUMENT ID "+ rides);

        db.collection("Rides").document(rides.getTimeStamp()+" "+rides.getOtp())
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
                                    String modifiedOtp=dc.getDocument().getData().get("otp").toString();
                                    if(rides.getOtp().equalsIgnoreCase(modifiedOtp.substring(1)));
                                        moveToCustomerDuringRide();
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Removed city: " + dc.getDocument().getData());
                                    if(rides.getOtp().equalsIgnoreCase(dc.getDocument().getData().get("otp").toString()))
                                    customerCancelSuccessful();
                                    break;
                            }
                        }

                    }
                });

    }

    public void customerCancelSuccessful(){
        Intent intent = new Intent(CustomerCancelActivity.this, CustomerActivity.class);
        startActivity(intent);
    }

    public void moveToCustomerDuringRide(){

        pickup.setLatitude(getIntent().getDoubleExtra("PickUpLatitude",0.0));
        pickup.setLongitude(getIntent().getDoubleExtra("PickUpLongitude",0.0));
        drop.setLatitude(getIntent().getDoubleExtra("DropLatitude",0.0));
        drop.setLongitude(getIntent().getDoubleExtra("DropLongitude",0.0));

        Intent intent = new Intent(CustomerCancelActivity.this, CustomerDuringRideActivity.class);
        intent.putExtra("RidesObject", rides);
        intent.putExtra("PickUpLatitude",pickup.getLatitude());
        intent.putExtra("PickUpLongitude",pickup.getLongitude());
        intent.putExtra("DropLatitude",drop.getLatitude());
        intent.putExtra("DropLongitude",drop.getLongitude());

        startActivity(intent);
    }

}
