package com.myhexaville.login.Driver;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.myhexaville.login.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverReportActivity extends AppCompatActivity {
    private static final String TAG ="DriverReport" ;
    ListView lv;
    List<String> places ;
    List<String> freqPlaces;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_report);
        lv=findViewById(R.id.listv);
        db=FirebaseFirestore.getInstance();
        places=new ArrayList<>();
        freqPlaces=new ArrayList<>();

        db.collection("Rides")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        if (task.isSuccessful()) {
                            HashMap<String,Integer> map=new HashMap<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());
                                String p=document.getData().get("pickupPoint").toString();
                                String d=document.getData().get("dropPoint").toString();
                                if(map.containsKey(p)){
                                    map.put(p,map.get(p)+1);
                                }
                                else{
                                    map.put(p,1);
                                }
                                if(map.containsKey(d)){
                                    map.put(d,map.get(d)+1);
                                }
                                else{
                                    map.put(d,1);
                                }
                            }
                            passingTheMap(map);



                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });



//        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                StringBuilder sb = new StringBuilder("");
//                //By position in array
//                sb.append(places.get(position)+" : ");
//                sb.append(freqPlaces.get(position)+"\n");
//                Toast.makeText(getApplicationContext(), sb.toString(),
//                        Toast.LENGTH_SHORT).show();
//            }
//        });

    }
    private void passingTheMap(HashMap<String, Integer> map){
        Log.d(TAG,"THE HASHMAP IS "+map);
        for(HashMap.Entry<String,Integer> entry: map.entrySet()){
            places.add(entry.getKey());
            freqPlaces.add(""+entry.getValue());

        }
        Log.d(TAG,"THE LIST IS "+places+" and "+freqPlaces);
        List<Map<String, String>> messages = new ArrayList<>();
        HashMap<String,String> content ;
        for(int i = 0; i < places.size(); i++) {
            content = new HashMap<String, String>();
            content.put("country", places.get(i));
            content.put("currency", freqPlaces.get(i));
            messages.add(content);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, messages,
                android.R.layout.simple_list_item_2,
                new String[] {"country", "currency"},
                new int[] {android.R.id.text1,
                        android.R.id.text2,
                });
        lv.setAdapter(adapter);


    }

}
