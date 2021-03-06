package com.example.keiji.app.activities;

import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.keiji.app.objects.Game;
import com.example.keiji.app.objects.Player;

public class MainGameDay extends AppCompatActivity {

    private TextView countdownText;
    private Button countdownButton;
    private Button countdownButtonReset;

    private CountDownTimer countdownTimer;
    private long timeLeftInMilliseconds = 300000;//5 minutes
    private boolean timerRunning;

    private Player player;
    private Game game;
    private boolean host;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game_day);
        countdownText = findViewById(R.id.countdown_text);
        countdownButton = findViewById(R.id.countdown_button);
        countdownButtonReset = findViewById(R.id.countdown_button_reset);
        countdownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStop();

            }

        });
        countdownButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTimer();
            }

        });
        countdownText.setVisibility(View.GONE);
        countdownButton.setVisibility(View.GONE);
        countdownButtonReset.setVisibility(View.GONE);

        //enable countdown timer and buttons for host only
        game = (Game)getIntent().getSerializableExtra("Game");
        host = getIntent().getBooleanExtra("host", false);
        if(host) {
            countdownButton.setVisibility(View.VISIBLE);
            countdownButtonReset.setVisibility(View.VISIBLE);
            countdownText.setVisibility(View.VISIBLE);
        }

    }
    public void startStop() {
        if (timerRunning) {
            stopTimer();

        } else {
            startTimer();
        }
    }

    public void startTimer () {
        countdownTimer = new CountDownTimer(timeLeftInMilliseconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMilliseconds = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {

            }
        }.start();
        countdownButton.setText("Pause");
        timerRunning = true;
    }
    public void stopTimer () {
        countdownTimer.cancel();
        timerRunning = false;
        countdownButton.setText("Start");
    }
    public void updateTimer ()
    {
        int minutes = (int) timeLeftInMilliseconds / 60000;
        int seconds = (int) timeLeftInMilliseconds % 60000 / 1000;
        String timeLeftText;
        timeLeftText = "" + minutes;
        timeLeftText += ":";
        if (seconds < 10) {
            timeLeftText += "0";
        }
        timeLeftText += seconds;
        countdownText.setText(timeLeftText);
    }
    public void resetTimer()
    {
        timeLeftInMilliseconds = 300000;
        if(timerRunning)
        {
            stopTimer();
        }
        updateTimer();
        startTimer();

    }
}
