package com.himanshu.petrichor;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mDisplayName;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button create_account;
    private Toolbar mcreateToolbar;
    private DatabaseReference mdatabase;

    private ProgressDialog mProgressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mcreateToolbar = (Toolbar) findViewById(R.id.register_toolbar);
        setSupportActionBar(mcreateToolbar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressDialog = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        mDisplayName = (TextInputLayout) findViewById(R.id.reg_name);
        mEmail = (TextInputLayout) findViewById(R.id.login_email);
        mPassword = (TextInputLayout) findViewById(R.id.login_password);
        create_account = (Button) findViewById(R.id.reg_create_account);

        create_account.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String display_name = mDisplayName.getEditText().getText().toString();
                String email = mEmail.getEditText().getText().toString();
                String password = mPassword.getEditText().getText().toString();

                if(!isNetworkAvailable()){
                    Toast.makeText(RegisterActivity.this, "Check Network Connectivity", Toast.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(display_name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
                    Toast.makeText(RegisterActivity.this, "Please Fill All Details", Toast.LENGTH_SHORT).show();
                }
                else{
                    mProgressDialog.setTitle("Registering User");
                    mProgressDialog.setMessage("Please wait while we register your account.");
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.show();
                    register_user(display_name, email, password);
                }

            }
        });
    }

    private void register_user(final String display_name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful())
                        {
                            FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                            String U_ID = current_user.getUid();

                            mdatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(U_ID);

                            HashMap<String, String> userMap = new HashMap<String, String>();
                            userMap.put("name", display_name);
                            userMap.put("status", "Hey there, I'm using Petrichor.");
                            userMap.put("image", "default");
                            userMap.put("thumb_image", "default");

                            mdatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        mProgressDialog.dismiss();
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        // this is when user press back on mainactivity page it doesn't come to startactivity page.. i.e its clears and make a new task
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                }
                            });
                        }else{
                            mProgressDialog.hide();
                            Toast.makeText(RegisterActivity.this , task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
