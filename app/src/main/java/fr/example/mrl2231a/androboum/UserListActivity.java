package fr.example.mrl2231a.androboum;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private class MyArrayAdapter extends ArrayAdapter<Profil> {
        List<Profil> liste, origListe;
        FirebaseStorage storage = FirebaseStorage.getInstance();

        private MyArrayAdapter(Context context, int resource, List<Profil> liste) {
            super(context, resource, liste);
            this.liste = liste;
            // on fait une copie de la liste dans une autre variable
            this.origListe = this.liste;
        }

        @Override
        public void notifyDataSetChanged() {
            if (filterConnected) {
                // on alloue une nouvelle liste et on la remplit uniquement avec les utilisateurs connectés.
                liste = new ArrayList<>();
                for (Profil p : origListe) {
                    if (p.isConnected()) liste.add(p);
                }
                // sinon on reprend la liste complète que l'on avait sauvegardé dans la variable origListe.
            } else liste = origListe;
            super.notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Profil p = liste.get(position);
            View layout = View.inflate(getContext(), R.layout.profil_list_item, null);
            // on va chercher les trois composants du layout
            ImageView imageProfilView = (ImageView) layout.findViewById(R.id.imageView);
            TextView textView = (TextView) layout.findViewById(R.id.textView);
            ImageView imageConnectedView = (ImageView) layout.findViewById(R.id.imageView2);

            // on télécharge dans le premier composant l'image du profil
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference photoRef = storage.getReference().child(p.getEmail() + "/photo.jpg");
            if (photoRef != null) {
                GlideApp.with(getContext()).load(photoRef).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(R.drawable.manager).into(imageProfilView);
                // on positionne le email dans le TextView
                textView.setText(p.getEmail());
                // si l'utilisateur n'est pas connecté, on rend invisible le troisième // composant
                if (!p.isConnected) {
                    imageConnectedView.setVisibility(View.INVISIBLE);
                }
            }
            return layout;
        }

        @Override
        public int getCount() {
            return liste.size();
        }
    }

    final List<Profil> userList = new ArrayList<>();
    private MyArrayAdapter adapter;
    boolean filterConnected = false;
    final Context context = this;
    @Override
    protected void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_list_activity);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar3);
        setSupportActionBar(myToolbar);
        ListView listeView = (ListView) findViewById(R.id.list_users);
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        adapter = new MyArrayAdapter(this, android.R.layout.simple_list_item_1, userList);
        listeView.setAdapter(adapter);

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    userList.add(child.getValue(Profil.class));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.v("AndroBoum", "loadPost:onCancelled", databaseError.toException());
            }
        };
        mDatabase.addValueEventListener(postListener);

        //ce qu'on fait lorsqu'on click sur un user de la liste affichée
        listeView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int
                    position, long l) {
                // code exécuté quand on clique sur un des items de la liste.
                // le paramètre position contient le numéro de l'item cliqué.
                Intent intent = new Intent(context,OtherUserActivity.class);
                intent.putExtra("position",position);
                startActivity(intent);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions_users, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filtre:
                // choix de l'action "Filtre"
                showFilterDialog();
                return true;
            case R.id.action_map:
                // choix de l'action Map
                Intent intent = new Intent(this, MapsActivity.class);
                startActivity(intent);
                return true;
            default:
                /// aucune action reconnue
                return super.onOptionsItemSelected(item);
        }
    }

    //Méthode pour créer et afficher la boîte de dialogue pou filtrer les users
    private void showFilterDialog() {
        // on crée un nouvel objet de type boite de dialogue
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // on lui affecte un titre, et une liste de choix possibles
        builder.setTitle(R.string.filter_dialog_title)
                .setSingleChoiceItems(R.array.users_filter, filterConnected ? 0 : 1, new
                        DialogInterface.OnClickListener() {
                            @Override
                            // méthode appelée quand l'utilisateur fait un choix, i contient le numéro du choix
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //si le premier item a été choisie, on filtre sur uniquement les utilisateurs connectés.
                                filterConnected = (i == 0);
                                //et on signale a l'adaptateur qu'il faut remettre la liste à jour.
                                adapter.notifyDataSetChanged();
                            }
                        })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    // on a cliqué sur "ok", on ne fait rien.

                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        // on crée la boite
        AlertDialog dialog = builder.create();
        // et on l'affiche
        dialog.show();
    }


}
