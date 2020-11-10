package com.nosp.nospwalk;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.nosp.nospwalk.connectors.Config;
import com.nosp.nospwalk.connectors.HttpBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class WalkerPageActivity extends AppCompatActivity {
    private Button mWalkButton;
    private Button mAskMateButton;
    private TextView mTurnNotice;

    private ImageView mPaws1;
    private ImageView mPaws2;

    private Drawable mButtonYourTurn;
    private Drawable mButtonNotYourTurn;

    private CheckModeTask mStatusLoadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walker_page);

        mTurnNotice = findViewById(R.id.turn_notice);
        mWalkButton = findViewById(R.id.walk_button);
        mAskMateButton = findViewById(R.id.ask_mate_button);

        mPaws1 = findViewById(R.id.paws1);
        mPaws2 = findViewById(R.id.paws2);

        mButtonYourTurn = getApplicationContext().getDrawable(R.drawable.ic_button1);
        mButtonNotYourTurn = getApplicationContext().getDrawable(R.drawable.ic_walk_button_not_your_turn);

        mStatusLoadTask = new CheckModeTask();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkMode();
    }

    void checkMode(){
        setWaitingMode();
        mStatusLoadTask.execute();
    }

    private void setNotYourTurnMode() {
        mWalkButton.setEnabled(true);
        mTurnNotice.setText(R.string.not_your_turn);
        mWalkButton.setText(R.string.walk_anyway);
        mAskMateButton.setVisibility(View.INVISIBLE);
        mPaws1.setVisibility(View.GONE);
        mPaws2.setVisibility(View.VISIBLE);
        mWalkButton.setBackground(mButtonNotYourTurn);
    }

    private void setYourTurnMode() {
        mWalkButton.setEnabled(true);
        mTurnNotice.setText(R.string.your_turn);
        mWalkButton.setText(R.string.walk);
        mAskMateButton.setVisibility(View.VISIBLE);
        mPaws1.setVisibility(View.VISIBLE);
        mPaws2.setVisibility(View.GONE);
        mWalkButton.setBackground(mButtonNotYourTurn);
    }

    private void setWaitingMode() {
        mTurnNotice.setText(R.string.loading);
        mWalkButton.setEnabled(false);
        mAskMateButton.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStatusLoadTask.cancel(true);
    }

    class CheckModeTask extends AsyncTask<Void, Integer, Void> {
        private final Integer ERROR = 148;
        private final Integer MY_TURN = 634;
        private final Integer NOT_MY_TURN = 634;

        @Override
        protected Void doInBackground(Void... voids) {
            while(true) {
                Integer status = ERROR;
                try {
                    HttpBuilder.Response resp = new HttpBuilder()
                            .get()
                            .url(Config.MY_TURN)
                            .request()
                            .raiseForStatus();
                    JSONObject respJson = new JSONObject(resp.json());
                    if (respJson.getBoolean("my_turn"))
                        status = MY_TURN;
                    else status = NOT_MY_TURN;
                } catch (IOException | JSONException e) {
                    status = ERROR;
                }
                finally {
                    publishProgress(status);
                    synchronized (this) {
                        try {
                            wait(20000);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... statuses) {
            Integer status = statuses[0];
            if (status.equals(ERROR)){
                mWalkButton.setEnabled(false);
                mTurnNotice.setText(R.string.internal_error);
            }
            else if (status.equals(MY_TURN)){
                WalkerPageActivity.this.setYourTurnMode();
            } else {
                WalkerPageActivity.this.setNotYourTurnMode();
            }
        }
    }
}
