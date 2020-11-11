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
        StateMachine.WAIT_MODE.apply(this);
        mStatusLoadTask.execute();
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
                    if (respJson.getString("turn").equals("you"))
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
                StateMachine.ERROR.apply(WalkerPageActivity.this);
            }
            else if (status.equals(MY_TURN)){
                StateMachine.YOUR_TURN.apply(WalkerPageActivity.this);
            } else {
                StateMachine.NOT_YOUR_TURN.apply(WalkerPageActivity.this);
            }
        }
    }

    enum StateMachine {
        NOT_YOUR_TURN {
            @Override
            protected void applyState(WalkerPageActivity a){
                a.mWalkButton.setVisibility(View.VISIBLE);
                a.mTurnNotice.setVisibility(View.VISIBLE);
                a.mPaws2.setVisibility(View.VISIBLE);

                a.mPaws1.setVisibility(View.GONE);

                a.mTurnNotice.setText(R.string.not_your_turn);
                a.mWalkButton.setText(R.string.walk_anyway);
                a.mWalkButton.setBackground(a.mButtonNotYourTurn);
            }
        },
        YOUR_TURN{
            @Override
            protected void applyState(WalkerPageActivity a){
                a.mWalkButton.setVisibility(View.VISIBLE);
                a.mTurnNotice.setVisibility(View.VISIBLE);
                a.mAskMateButton.setVisibility(View.VISIBLE);
                a.mPaws1.setVisibility(View.VISIBLE);

                a.mPaws2.setVisibility(View.GONE);

                a.mTurnNotice.setText(R.string.your_turn);
                a.mWalkButton.setText(R.string.walk);
                a.mWalkButton.setBackground(a.mButtonNotYourTurn);
            }
        },
        WAIT_MODE {
            @Override
            protected void applyState(WalkerPageActivity a){
                a.mWalkButton.setVisibility(View.VISIBLE);
                a.mTurnNotice.setVisibility(View.VISIBLE);

                a.mTurnNotice.setText(R.string.loading);
                a.mWalkButton.setEnabled(false);
            }
        },
        ERROR {
            @Override
            protected void applyState(WalkerPageActivity a) {
                a.mTurnNotice.setVisibility(View.VISIBLE);

                a.mTurnNotice.setText(R.string.internal_error);
            }
        };

        public final void apply(WalkerPageActivity a){
            reset(a);
            applyState(a);
        }

        private void reset(WalkerPageActivity a){
            a.mWalkButton.setVisibility(View.INVISIBLE);
            a.mAskMateButton.setVisibility(View.INVISIBLE);
            a.mTurnNotice.setVisibility(View.INVISIBLE);
            a.mPaws1.setVisibility(View.INVISIBLE);
            a.mPaws2.setVisibility(View.INVISIBLE);
            a.mWalkButton.setEnabled(true);
            a.mWalkButton.setBackground(a.mButtonYourTurn);
        }

        protected abstract void applyState(WalkerPageActivity a);
    }
}
