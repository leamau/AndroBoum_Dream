package fr.example.mrl2231a.androboum;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{
    private List<Profil> userList = new ArrayList<>();
    private GoogleMap mMap;
    private String mail;
    private Profil me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        mail = auth.getCurrentUser().getEmail();

        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();
                me = null;
                for (DataSnapshot child : dataSnapshot.getChildren()) {

                    if(( child.getValue(Profil.class).getEmail() != null)&&( child.getValue(Profil.class).getEmail().equals(mail))){
                        me = child.getValue(Profil.class);
                    }
                    userList.add(child.getValue(Profil.class));
                }
                if(mMap != null){
                    onMapReady(mMap);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.v("AndroBoum", "loadPost:onCancelled", databaseError.toException());
            }
        };
        mDatabase.addValueEventListener(postListener);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        final Context c = this;

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                int pos = (int) marker.getTag();
                Intent intent = new Intent(c, OtherUserActivity.class);
                intent.putExtra("position", pos);
                startActivity(intent);
            }
        });
        Log.v("COOD","Start OnmapReady");
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        if (me != null) {
            LatLng maPosition = new LatLng(me.getLatitude(), me.getLongitude());
            Log.v("COOD",""+me.getLatitude()+" "+me.getLongitude());
            builder.include(maPosition);
            mMap.addMarker(new MarkerOptions().position(maPosition).title(me.getEmail()));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(maPosition));
            for (Profil user : userList) {
                if ((user.getEmail() != null)&&(!user.getEmail().equals( me.getEmail())) && (user.getLatitude() != 0 && user.getLongitude() != 0)) {
                    LatLng position = new LatLng(user.getLatitude(), user.getLongitude());
                    builder.include(position);
                    Marker marker = mMap.addMarker((new MarkerOptions().position(position).title(user.getEmail()).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))));
                    int pos = getNum(user.getEmail());
                    marker.setTag(pos);
                }
            }
            LatLngBounds bounds = builder.build();
              int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.10);
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mMap.animateCamera(cu);

        }
    }

    private int getNum(String email) {
        int cpt=0;
        for(Profil p : userList){
            if((p.getEmail() != null)&&(p.getEmail().equals(email))){
                break;
            }
            cpt+=1;
        }
        return cpt;
    }

}
