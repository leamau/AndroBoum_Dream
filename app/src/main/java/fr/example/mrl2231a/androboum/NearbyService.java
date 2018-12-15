package fr.example.mrl2231a.androboum;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.gms.common.api.GoogleApiClient;

public class NearbyService extends Service  {
    private GoogleApiClient mGoogleApiClient;
    final String SERVICE_ID = "123";

    public NearbyService() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).build();
        if(mGoogleApiClient.isConnected()){
            onConnect();
        }
    }

    private void onConnect(){





    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
