package fr.example.mrl2231a.androboum;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

public class UserActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 123;
    TextView textView;
    ImageView imageView;
    // défini un numéro unique pour repérer plus tard ce code // dans la méthode onActivityResult(...)
    private static final int SELECT_PICTURE = 124;
    FirebaseAuth auth;
    private Profil user;
    private FusedLocationProviderClient mFusedLocationClient;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

    @Override
    protected void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_user);
        textView = (TextView) findViewById(R.id.email);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        user = new Profil();
        auth = FirebaseAuth.getInstance();
        Button listUsers;


        if (auth.getCurrentUser() != null) {

            Log.v("AndroBoum", "je suis déjà connecté sous l'email : " + auth.getCurrentUser().getEmail());
            setUser();
            updateProfil(user);
            textView.setText(auth.getCurrentUser().getEmail());
            downloadImage();
        } else {

            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(Arrays.asList(new
                    AuthUI.IdpConfig.GoogleBuilder().build(), new AuthUI.IdpConfig.FacebookBuilder().build()
            )).build(), 123);
        }


        imageView = (ImageView) findViewById(R.id.imageProfil);
        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);

                Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                intent.setAction(Intent.ACTION_PICK);
                Intent chooserIntent = Intent.createChooser(intent, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});
                startActivityForResult(chooserIntent, SELECT_PICTURE);
                return true;
            }
        });

        listUsers = findViewById(R.id.listUsers);
        listUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lancerListeUsers();
            }
        });

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
// on vérifie que la réponse est bien liée au code de connexion choisi
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            // Authentification réussie
            if (resultCode == RESULT_OK) {
                Log.v("AndroBoum", "je me suis connecté et mon email est : " +
                        response.getEmail());
                if (response.getEmail() != null) {
                    textView.setText(response.getEmail());
                    downloadImage();
                }
                return;
            } else {
                // echec de l'authentification
                if (response == null) {
                    finish();
                    return;
                }
                // pas de réseau
                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Log.v("AndroBoum", "Erreur réseau");
                    finish();
                    return;
                }
                // une erreur quelconque
                if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Log.v("AndroBoum", "Erreur inconnue");
                    finish();
                    return;
                }
            }
            Log.v("AndroBoum", "Réponse inconnue");
        }

        if (requestCode == SELECT_PICTURE) {
            if (resultCode == RESULT_OK) {
                try {
                    imageView = (ImageView) findViewById(R.id.imageProfil);
                    boolean isCamera = (data.getData() == null);
                    final Bitmap selectedImage;
                    if (!isCamera) {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        selectedImage = BitmapFactory.decodeStream(imageStream);
                    } else {
                        selectedImage = (Bitmap) data.getExtras().get("data");
                    }
                    // on redimensionne le bitmap pour ne pas qu'il soit trop grand
                    Bitmap finalbitmap = Bitmap.createScaledBitmap(selectedImage, 500, (selectedImage.getHeight() * 500) / selectedImage.getWidth(), false);
                    imageView.setImageBitmap(finalbitmap);

                    uploadImage();

                } catch (Exception e) {
                    Log.v("AndroBoum", e.getMessage());
                }

            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_logout:
                AuthUI.getInstance().signOut(this);
                user.setConnected(false);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    private StorageReference getCloudStorageReference() {
        // on va chercher l'email de l'utilisateur connecté FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null) return null;
        String email = auth.getCurrentUser().getEmail();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        // on crée l'objet dans le sous-dossier de nom l'email
        StorageReference photoRef = storageRef.child(email + "/photo.jpg");
        return photoRef;
    }

    private void downloadImage() {
        StorageReference photoRef = getCloudStorageReference();
        if (photoRef == null) return;
        imageView = (ImageView) findViewById(R.id.imageProfil);
        // Load the image using Glide
        GlideApp.with(this /* context */)
                .load(photoRef)
                .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.manager).into(imageView);
    }


    private void uploadImage() {
        StorageReference photoRef = getCloudStorageReference();
        if (photoRef == null) return;
        // on va chercher les données binaires de l'image de profil
        imageView = (ImageView) findViewById(R.id.imageProfil);
        imageView.setDrawingCacheEnabled(true);
        imageView.buildDrawingCache();
        Bitmap bitmap = imageView.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        // on lance l'upload
        UploadTask uploadTask = photoRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) { // si on est là, échec de l'upload
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // ok, l'image est uploadée
                // on fait pop un toast d'information
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.imageUploaded), Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private void setUser() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser fuser = auth.getCurrentUser();
        if (fuser != null) {
            user.setUid(fuser.getUid());
            user.setEmail(fuser.getEmail());
            user.setConnected(true);
            getLocation();
        }
        AndroBoumApp.buildBomber(this);


    }

    //on récupère la position de l'user
    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // on demande les permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // location contient la position, sauf si il est null.
                        if (location != null) {

                            Log.v("Androboum","Coordonnées GPS: Latitude=" +
                                    location.getLatitude() +
                                    " Longitude=" + location.getLongitude());
                            user.setLatitude(location.getLatitude());
                            user.setLongitude(location.getLongitude());
                            updateProfil(user);
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // on est autorisé, donc on rappelle getLocation()
                    getLocation();
                } else {
                    // on n'a pas l'autorisation donc on ne fait rien
                }
                return;
            }
        }
    }

    private void updateProfil(Profil user) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference ref = mDatabase.child("Users").child(user.getUid());
        ref.child("connected").setValue(true);
        ref.child("email").setValue(user.getEmail());
        ref.child("uid").setValue(user.getUid());
        ref.child("latitude").setValue(user.getLatitude());
        ref.child("longitude").setValue(user.getLongitude());
    }

    @Override
    protected void onDestroy() {
        user.setConnected(false);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth != null) {
            FirebaseUser fuser = auth.getCurrentUser();
            if (fuser != null) {
                final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
                DatabaseReference mreference = mDatabase.getReference().child("Users").child(fuser.getUid());
                mreference.child("connected").setValue(user.isConnected());
            }
        }
        // on déconnecte l'utilisateur
        AuthUI.getInstance().signOut(this);
        super.onDestroy();
    }

    public void lancerListeUsers() {
        Intent intent = new Intent(this, UserListActivity.class);
        startActivity(intent);


    }



}