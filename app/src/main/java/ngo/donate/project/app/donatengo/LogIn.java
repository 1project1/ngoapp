package ngo.donate.project.app.donatengo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Created by Aakash on 20-Feb-17.
 */

public class LogIn extends DialogActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    //Shared Preferences file and Editor
    SharedPreferences logInPref;

    SharedPreferences.Editor logInEditor;
    // a permanent variable storing the file name for ease of change
    public static final String LOGIN_FILE = "LogInFile";
    EditText userName, password;
    Button login;
    TextView createAccount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_in);

        // Drawable d = getBackground().setAlpha(120);

        init();

        //Firebase Listener//
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //User Signed In
                    Log.d("Firebase", "onAuthStateChanged:signed_in:" + user.getUid());
                    logInEditor.putString("Uid", user.getUid());
                    logInEditor.putString("user", user.getDisplayName());
                    logInEditor.putString("name", user.getDisplayName());
                    logInEditor.putBoolean("isLoggedIn", true);
                    logInEditor.apply();
                    Intent i = new Intent(getApplicationContext(), MainUi.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    finish();
                    startActivity(i);

                } else {
                    //User Signed out
                    Log.d("Firebase", "onAuthStateChanged:signed_out");
                }
            }
        };

        // [START config_signin]
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), SignUp.class));
            }
        });
        Button g_plus = (Button) findViewById(R.id.g_plus);

        g_plus.setOnClickListener(this);
        login.setOnClickListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }


    private void init() {

        userName = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.Password);

        TextView font = (TextView) findViewById(R.id.login_title);
        createAccount = (TextView) findViewById(R.id.create_account);
        login = (Button) findViewById(R.id.login);

        logInPref = getSharedPreferences(LOGIN_FILE, 0);
        logInEditor = logInPref.edit();

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/billabong.ttf");
        font.setTypeface(typeface);

        // FireBase
        mAuth = FirebaseAuth.getInstance();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login:
                String u = userName.getText().toString();
                String p = password.getText().toString();
                Log.i("user", u);
                Log.i("password", p);

                if (u.isEmpty() || p.isEmpty()) {
                    Snackbar.make(v, "Fill All details", Snackbar.LENGTH_LONG).show();
                    return;
                }
                if (!isNetworkAvailable()) {
                    Snackbar.make(v, getString(R.string.no_internet), Snackbar.LENGTH_LONG).show();
                    return;
                }

                showProgressDialog();
                mAuth.signInWithEmailAndPassword(u, p)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d("FireBase", "signInWithEmail:onComplete:" + task.isSuccessful());

                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Log.w("Firebase", "signInWithEmail", task.getException());
                                    Toast.makeText(getApplicationContext(), "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();

                                }
                                hideProgressDialog();
                            }
                        })
                        .addOnFailureListener(this, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d("FireBase", "signInWithEmail:onComplete: Failed");
                                if (e instanceof FirebaseAuthException) {
                                    String error = ((FirebaseAuthException) e).getErrorCode();
                                    Log.e("Error", error);

                                    //[Temp]
                                    TextView tv = (TextView) findViewById(R.id.login_error);
                                    tv.setText(error);
                                    tv.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                break;
            case R.id.g_plus:
                if (!isNetworkAvailable()) {
                    Snackbar.make(v, getString(R.string.no_internet), Snackbar.LENGTH_LONG).show();
                    return;
                }
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                Log.i(TAG, "Failed SignIn");
                Log.d(TAG, String.valueOf(result.getStatus()));
                // Google Sign In failed, update UI appropriately
                // [START_EXCLUDE]
                /// aakash updateUI(null);
                // [END_EXCLUDE]
            }
        }
    }

    // [START auth_with_google]
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("Firebase with Google", "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w("Firebase with Google", "signInWithCredential", task.getException());
                            Toast.makeText(getApplicationContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        // [START_EXCLUDE]
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
// [END auth_with_google]

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}



