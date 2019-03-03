package com.myhexaville.login.Customer;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.myhexaville.login.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class CustomerBookRidesFragment extends Fragment {
    private Button btViewMaps;






    public CustomerBookRidesFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);



    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v= inflater.inflate(R.layout.fragment_customer_book_rides, container, false);
        btViewMaps=v.findViewById(R.id.bt_view_maps);
        btViewMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionOfButton();

            }
        });



//        btRideNow=v.findViewById(R.id.bt_ride_now_customer);
//        btRideNow.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                rideNowButton();
//
//            }
//        });
        return v;
    }
    private void actionOfButton(){
        Intent intent=new Intent(getContext(),MapsActivity.class);
        startActivity(intent);

    }

}
