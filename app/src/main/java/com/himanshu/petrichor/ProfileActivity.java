package com.himanshu.petrichor;

import android.app.ProgressDialog;
import android.icu.text.DateFormat;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Date;
import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {
    private TextView displayName, displayStatus, displayFriends;
    private ImageView imageView;
    private Button sendRequest, declineRequest;

    private ProgressDialog mProgressDialog;

    private String mcurrent_state;

    private DatabaseReference mNotificationDatabase, mUserDatabase, mFriendReqDatabase, mFriendDatabase;
    private FirebaseUser mCurrentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");

        mcurrent_state = "not_friends";

        displayName = (TextView) findViewById(R.id.profile_name);
        displayFriends = (TextView) findViewById(R.id.profile_friends);
        displayStatus = (TextView) findViewById(R.id.profile_status);
        imageView = (ImageView) findViewById(R.id.profile_imageView);
        sendRequest = (Button) findViewById(R.id.profile_sendRequest_button);
        declineRequest = (Button) findViewById(R.id.profile_decline_frd_req);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please wait while we load the details.");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendReqDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("Notifications");

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue().toString();
                String status = dataSnapshot.child("status").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                displayName.setText(name);
                displayStatus.setText(status);

                Picasso.with(ProfileActivity.this).load(image).placeholder(R.mipmap.default_avatar).into(imageView);

                //---------------FRIEND LIST / REQUEST FEATURE------------------------

                mFriendReqDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(user_id)) {
                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if (req_type.equals("recieved")) {
                                mcurrent_state = "req_recieved";
                                sendRequest.setText("Accept Friend Request");

                                declineRequest.setVisibility(View.VISIBLE);
                                declineRequest.setEnabled(true);
                            } else if (req_type.equals("recieved")) {
                                mcurrent_state = "req_sent";
                                sendRequest.setText("Cancel Friend Request");

                                declineRequest.setVisibility(View.INVISIBLE);
                                declineRequest.setEnabled(false);
                            }
                            mProgressDialog.dismiss();
                        } else {
                            //-----------IF ALREADY A FRIEND-------
                            mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild(user_id)) {

                                        mcurrent_state = "friends";
                                        sendRequest.setText("Unfriend this person");

                                        declineRequest.setVisibility(View.INVISIBLE);
                                        declineRequest.setEnabled(false);
                                    }
                                    mProgressDialog.dismiss();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    mProgressDialog.dismiss();
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mProgressDialog.dismiss();
                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        sendRequest.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                sendRequest.setEnabled(false);

                //-------------------SENDING FRIEND REQUEST----------------

                if (mcurrent_state.equals("not_friends")) {
                    mFriendReqDatabase.child(mCurrentUser.getUid()).child(user_id).child("request_type")
                            .setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                mFriendReqDatabase.child(user_id).child(mCurrentUser.getUid()).child("request_type")
                                        .setValue("recieved").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //  sendRequest.setEnabled(true);
                                        HashMap<String, String> notification_data = new HashMap<String, String>();
                                        notification_data.put("from", mCurrentUser.getUid());
                                        notification_data.put("type", "request");

                                        mNotificationDatabase.child(user_id).push().setValue(notification_data).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                mcurrent_state = "req_sent";
                                                sendRequest.setText("Cancel Friend Request");
                                                Toast.makeText(ProfileActivity.this, "Friend Request Sent", Toast.LENGTH_SHORT).show();
                                                declineRequest.setVisibility(View.INVISIBLE);
                                                declineRequest.setEnabled(false);
                                            }
                                        });

                                    }
                                });
                            } else {
                                Toast.makeText(ProfileActivity.this, "Friend Request Failed", Toast.LENGTH_SHORT).show();
                                sendRequest.setEnabled(true);
                            }
                        }
                    });
                }

                //-------------------CANCELLING FRIEND REQUEST----------------

                if (mcurrent_state.equals("req_sent")) {
                    mFriendReqDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                mFriendReqDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        sendRequest.setEnabled(true);
                                        mcurrent_state = "not_friends";
                                        sendRequest.setText("Send Friend Request");
                                        declineRequest.setVisibility(View.INVISIBLE);
                                        declineRequest.setEnabled(false);
                                    }
                                });
                            }
                        }
                    });
                }


                //---------------REQUEST RECIEVED STATE-------------------
                if (mcurrent_state.equals("req_recieved")) {
                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());
                    mFriendDatabase.child(mCurrentUser.getUid()).child(user_id).setValue(currentDate)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    mFriendDatabase.child(user_id).child(mCurrentUser.getUid()).setValue(currentDate)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    mFriendReqDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                mFriendReqDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        sendRequest.setEnabled(true);
                                                                        mcurrent_state = "friends";
                                                                        sendRequest.setText("Unfriend this person");

                                                                        declineRequest.setVisibility(View.INVISIBLE);
                                                                        declineRequest.setEnabled(false);
                                                                    }
                                                                });
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                        }
                    });
                }
            }
        });
    }
}
