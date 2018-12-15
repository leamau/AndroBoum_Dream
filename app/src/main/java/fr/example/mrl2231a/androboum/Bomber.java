package fr.example.mrl2231a.androboum;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Bomber {


    Profil me, newme;
    Profil other = new Profil();
    Context context;
    static public long bombedTime = 0;
    static public long timetoboum = 10000;
    private int mNotificationId = 10;
    public static CountDownTimer timer;
    int timeleft;


    public interface BomberInterface {
        void userBombed();
        void userBomber();
    }

    BomberInterface callback;
   public void setCallback(BomberInterface callback) {
        this.callback = callback;
        if (me.getStatut() == Profil.BombStatut.BOMBED) callback.userBombed();
        if (me.getStatut() == Profil.BombStatut.BOMBER) callback.userBomber();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Profil getOther() {
        return other;
    }

    public Bomber(Context context) {
        this.context = context;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser fuser = auth.getCurrentUser();
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mreference = mDatabase.getReference().child("Users").child(fuser.getUid());

        mreference.child("statut").setValue(Profil.BombStatut.IDLE);
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                newme = dataSnapshot.getValue(Profil.class);
                other.setUid(newme.getOtherUserUID());
                other.setEmail(newme.getOtherUseremail());
                if (newme.getStatut() == Profil.BombStatut.AWAITING && (me == null || me.getStatut() != Profil.BombStatut.AWAITING)) {
                    updateStatut(newme.getUid(), Profil.BombStatut.BOMBED);
                    notifyBombed();
                }
                //si bombeur
                if (newme.getStatut() == Profil.BombStatut.BOMBER && (me == null || me.getStatut() != Profil.BombStatut.BOMBER)) {
                    bombedTime = System.currentTimeMillis();
                    if (callback != null) callback.userBomber();
                }
                // si bombé
                if (newme.getStatut() == Profil.BombStatut.BOMBED && (me == null || me.getStatut() != Profil.BombStatut.BOMBED)) {
                    bombedTime = System.currentTimeMillis();
                    if (callback != null) callback.userBombed();
                }
                me = newme;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        mreference.addValueEventListener(postListener);
    }


    private void notifyBombed() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final String NOTIFICATION_CHANNEL_ID = "4655";
            CharSequence channelName = "toto";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance);
            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            final NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_priority_high_black_24dp)
                            .setContentTitle("AndroBoumApp")
                            .setContentText(context.getResources().getString(R.string.bombedText) + " " + other.getEmail() + " 5s");

            Intent resultIntent = new Intent(context, BombActivity.class);
            resultIntent.putExtra("notification", true);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);


            final NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(mNotificationId, mBuilder.build());

            if (timer != null) timer.cancel();
            timeleft = (int) (timetoboum / 1000);

            timer = new CountDownTimer(timetoboum, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeleft--;
                    mBuilder.setContentText(context.getResources().getString(R.string.bombedText) + " " + other.getEmail() + " " + timeleft + "s");
                    mNotificationManager.notify(mNotificationId, mBuilder.build());
                }

                @Override
                public void onFinish() {
                    mBuilder.setContentText(context.getResources().getString(R.string.bombexplosed) + " " + other.getEmail() + " " + timeleft + "s");
                    mNotificationManager.notify(mNotificationId, mBuilder.build());
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.bombexplosed), Toast.LENGTH_SHORT);
                    toast.show();
                    setIdle();
                }
            };

            timer.start();

        }else{

            final NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_priority_high_black_24dp)
                            .setContentTitle("AndroBoumApp")
                            .setContentText(context.getResources().getString(R.string.bombedText) + " " + other.getEmail() + " 5s")
                            .setVibrate(new long[]{0, 1000});

            Intent resultIntent = new Intent(context, BombActivity.class);
            resultIntent.putExtra("notification", true);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);

            final NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(mNotificationId, mBuilder.build());

            if (timer != null) timer.cancel();
            timeleft = (int) (timetoboum / 1000);

            timer = new CountDownTimer(timetoboum, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeleft--;
                    mBuilder.setContentText(context.getResources().getString(R.string.bombedText) + " " + other.getEmail() + " " + timeleft + "s");
                    mNotificationManager.notify(mNotificationId, mBuilder.build());
                }

                @Override
                public void onFinish() {
                    mBuilder.setContentText(context.getResources().getString(R.string.bombexplosed) + " " + other.getEmail() + " " + timeleft + "s");
                    mNotificationManager.notify(mNotificationId, mBuilder.build());
                    Toast toast = Toast.makeText(context, context.getResources().getString(R.string.bombexplosed), Toast.LENGTH_SHORT);
                    toast.show();
                    setIdle();
                }
            };

            timer.start();

        }
    }

    public void addToScore(int change) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        me.setScore(me.getScore() + change);
        mDatabase.child("Users").child(me.getUid()).child("score").setValue(me.getScore());
    }

    private void updateStatut(String uid, Profil.BombStatut statut) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("Users").child(uid).child("statut").setValue(statut);
    }

    private void updateOther(String uid, Profil other) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("Users").child(uid).child("otherUserUID").setValue(other == null ? null : other.getUid());
        mDatabase.child("Users").child(uid).child("otherUseremail").setValue(other == null ? null : other.getEmail());
    }

    public void setIdle() {
        updateStatut(me.getUid(), Profil.BombStatut.IDLE);
    }

    public void setBomb(final Profil cible, final BomberInterface callback) {

        if (cible.getUid().equals(me.getUid())) {
            Toast toast = Toast.makeText(context, context.getString(R.string.cantbombme), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (cible.getStatut() != Profil.BombStatut.IDLE) {
            Toast toast = Toast.makeText(context, context.getString(R.string.userbusy), Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        updateStatut(cible.getUid(), Profil.BombStatut.AWAITING);
        updateOther(cible.getUid(), me);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                updateStatut(cible.getUid(), Profil.BombStatut.IDLE);
                updateOther(cible.getUid(), null);
                updateStatut(me.getUid(), Profil.BombStatut.IDLE);
                updateOther(me.getUid(), null);

                Toast toast = Toast.makeText(context, context.getString(R.string.nouserresponse), Toast.LENGTH_SHORT);
                toast.show();
            }
        }, 2000);


        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(cible.getUid());
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Profil newCible = dataSnapshot.getValue(Profil.class);
                if (newCible.getStatut() == Profil.BombStatut.BOMBED) {
                    handler.removeCallbacksAndMessages(null);
                    updateStatut(me.getUid(), Profil.BombStatut.BOMBER);
                    updateOther(me.getUid(), cible);
                    callback.userBomber();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        mDatabase.addValueEventListener(postListener);

    }

    public void removeBomb() {
        updateStatut(me.getUid(), Profil.BombStatut.IDLE);
        updateOther(me.getUid(), null);
    }

    public void renvoyerBomb() {
        bombedTime = System.currentTimeMillis();
        if (me == null || other.getUid() == null) return;
        updateStatut(other.getUid(), Profil.BombStatut.BOMBED);
        updateStatut(me.getUid(), Profil.BombStatut.BOMBER);
    }
}
