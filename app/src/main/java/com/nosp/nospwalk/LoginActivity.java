package com.nosp.nospwalk;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.nosp.nospwalk.connectors.Config;
import com.nosp.nospwalk.connectors.HttpBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private Button mSignInButton;
    private EditText mPasswordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mSignInButton = (Button) findViewById(R.id.login_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mPasswordView.setError(null);

        ObjectAnimator.ofFloat(mSignInButton, "alpha", 1,0,1).setDuration(500).start();
        String password = mPasswordView.getText().toString();
        mAuthTask = new UserLoginTask(password);
        mAuthTask.execute();
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Integer> {
        private final Integer LOGGED_IN = 0;
        private final Integer WRONG_CODE = 1;
        private final Integer ERROR = 2;

        private final String mPassword;

        UserLoginTask(String password) {
            mPassword = password;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                Map<String, String> data = new HashMap<>();
                try {
                    byte[] hash = MessageDigest.getInstance("MD5").digest(mPassword.getBytes(StandardCharsets.UTF_8));
                    data.put("key", new BigInteger(1, hash).toString(16));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                HttpBuilder.Response resp = new HttpBuilder()
                        .url(Config.LOGIN)
                        .post()
                        .jsonData(data)
                        .request();
                if (resp.code == 200) {
                    Log.d("LOGIN_ASYNC_TASK", "FINE, 200");
                    return LOGGED_IN;
                } else if (resp.code == 403) {
                    Log.d("LOGIN_ASYNC_TASK", "WRONG CODE");
                    return WRONG_CODE;
                } else return ERROR;
            } catch (IOException | ClassCastException e) {
                Log.e("LOGIN_ASYNC_TASK", "Error", e);
                return ERROR;
            }
        }

        @Override
        protected void onPostExecute(final Integer status) {
            mAuthTask = null;

            if (status.equals(LOGGED_IN)) {
                Intent intent = new Intent(LoginActivity.this, WalkerPageActivity.class);
                startActivity(intent);
            } else if (status.equals(WRONG_CODE)) {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else {
                mPasswordView.setError(getString(R.string.internal_error));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }
}

