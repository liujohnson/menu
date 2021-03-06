package com.example.menuui;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cognitoidentityprovider.AmazonCognitoIdentityProviderClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.raycoarana.codeinputview.CodeInputView;

import org.json.JSONException;

public class VerifyUser extends AppCompatActivity {
    private Button verify_button;
    private CodeInputView code;
    Context context = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verify);

        Bundle user_credentials = getIntent().getExtras();
        final String user_name = user_credentials.getString("username");

        code = (CodeInputView) findViewById(R.id.code_verify); ;

        verify_button = (Button) findViewById(R.id.verify_pass);
        verify_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String verify_code = code.getCode();
                new Confirm().execute(verify_code, user_name);
            }
        });

    }

    private class Confirm extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... strings) {
            final String[] result = new String[1];
            final GenericHandler confirmationCallback = new GenericHandler() {
                @Override
                public void onSuccess() {
                    result[0] = "Succeeded!";
                }

                @Override
                public void onFailure(Exception exception) {
                    result[0] = "Failed!" + exception.getMessage();
                }
            };

            Region REGION = Region.getRegion(Regions.US_EAST_2);

            AmazonCognitoIdentityProviderClient identityProviderClient = new AmazonCognitoIdentityProviderClient(new AnonymousAWSCredentials(),
                    new ClientConfiguration());

            identityProviderClient.setRegion(REGION);

            Bundle user_credentials = getIntent().getExtras();
            final String user_name = user_credentials.getString("username");

            Parcel user_input = Parcel.obtain();
            user_input.writeString(user_name);

            CognitoSettings user_creds = new CognitoSettings(user_input);

            try { user_creds.getCredentials(VerifyUser.this); }

            catch (JSONException e) { e.printStackTrace(); }
            String userPoolId = user_creds.getUserPoolId();
            String clientId = user_creds.getClientId();
            String clientSecret = user_creds.getClientSecret();

            Log.i("tagyoureit", userPoolId);
            Log.i("tagyoureit", clientId);
            Log.i("tagyoureit", clientSecret);

            //CognitoUserPool userPool = new CognitoUserPool(context, userPoolId, clientId, clientSecret, identityProviderClient);
            CognitoUser cognito_user = user_creds.getUserPool().getUser(strings[1]);
            cognito_user.confirmSignUp(strings[0], false, confirmationCallback);

            return result[0];

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.i("confirm",  "confirm result");
            go_to_landing();
        }

    }

    private void go_to_landing() {
        Intent landing_intent = new Intent(this, Login.class);
        startActivity(landing_intent);
    }
}