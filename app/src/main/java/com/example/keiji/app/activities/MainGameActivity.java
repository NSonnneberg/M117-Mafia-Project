package com.example.keiji.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.keiji.app.objects.DecisionMessage;
import com.example.keiji.app.objects.Game;
import com.example.keiji.app.objects.Message;
import com.example.keiji.app.objects.NominateMessage;
import com.example.keiji.app.objects.PhaseChangeMessage;
import com.example.keiji.app.objects.Player;
import com.example.keiji.app.objects.PlayerLynchMessage;
import com.example.keiji.app.objects.StartGameMessage;
import com.example.keiji.app.utilities.SerializationHandler;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainGameActivity extends AppCompatActivity {

    private static final int PLAYER_LIST = 0;
    private static final int SEARCH = 1;
    private static final int DAY = 2;
    private static final int NIGHT = 3;

    private static final long MAX_COUNTDOWN = 30000;

    private TextView countdownText;
    private Button countdownButton;
    private Button countdownButtonReset;

    private CountDownTimer countdownTimer;
    private long timeLeftInMilliseconds = MAX_COUNTDOWN;//5 minutes
    private boolean timerRunning;

    private TextView gmnameview;
    private TextView gmnametext;
    private TextView pnnameview;
    private TextView pnnametext;
    private TextView listtext;
    private Button startgamebutton;
    private ListView list;

    List<String> player_list = new ArrayList<>();
    HashMap<String, Player> player_map = new HashMap<String, Player>();
    Player player;
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    String pname = "";
    String gname = "";
    boolean host = false;
    Game game;
    int REQUEST_LOCATION = 1;
    android.app.Activity curr_activity;

    // used for calculating if a player is lynched
    int yes = 0;
    int no = 0;
    int total = 0;
    String nominatedPlayer = "";

    int mode;

    ArrayAdapter p_list_adapter;

    static String TAG = "MainGameActivity";

    String serviceId = "com.example.keiji";

    ConnectionsClient connectionsClient;

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull final String endpointId, @NonNull Payload payload) {
            Object received = null;
            try {
                received = SerializationHandler.deserialize(payload.asBytes());
            } catch (IOException | ClassNotFoundException e) {
                Log.d(TAG, e.getMessage());
            }

            if (received == null) {
                Log.d(TAG, "Messaged was not received");
            }

            else {
                if (received.getClass() == StartGameMessage.class) {
                    StartGameMessage message = (StartGameMessage) received;
                    player_list.addAll(message.getPlayer_list());
                    player = message.getPlayer();
                    p_list_adapter.notifyDataSetChanged();
                    Log.d(TAG,  "Player List is: " + player_list.toString());
                    Log.d(TAG, "Received player object from " + endpointId + ". Their role was: " + (player.getRole()));
                    playerListToDay();
                }

                else if (received.getClass() == NominateMessage.class) {
                    NominateMessage message = (NominateMessage) received;
                    nominatedPlayer = message.getNominatedPlayer();

                    android.app.AlertDialog.Builder builder;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        builder = new android.app.AlertDialog.Builder(curr_activity, android.R.style.Theme_Material_Dialog_Alert);
                    } else {
                        builder = new android.app.AlertDialog.Builder(curr_activity);
                    }
                    if (host) {
                        builder.setTitle("Nomination")
                                .setMessage("Player nominated " + message.getNominatedPlayer() + ". Do you want to second?")
                                .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
                                    public void onClick(android.content.DialogInterface dialog, int which) {
                                        // if yes
                                        yes++;
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new android.content.DialogInterface.OnClickListener() {
                                    public void onClick(android.content.DialogInterface dialog, int which) {
                                        // if no
                                        no++;
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                    else {
                        builder.setTitle("Nomination")
                                .setMessage("Player nominated " + ((NominateMessage) received).getNominatedPlayer() + ". Do you want to second?")
                                .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
                                    public void onClick(android.content.DialogInterface dialog, int which) {
                                        // if yes
                                        try {
                                            connectionsClient.sendPayload(endpointId, Payload.fromBytes(SerializationHandler.serialize(new DecisionMessage(true))));
                                        } catch (IOException e) {
                                            Log.d(TAG, e.getMessage());
                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new android.content.DialogInterface.OnClickListener() {
                                    public void onClick(android.content.DialogInterface dialog, int which) {
                                        // if no
                                        try {
                                            connectionsClient.sendPayload(endpointId, Payload.fromBytes(SerializationHandler.serialize(new DecisionMessage(false))));
                                        } catch (IOException e) {
                                            Log.d(TAG, e.getMessage());
                                        }
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }

                else if (received.getClass() == PhaseChangeMessage.class) {
                    PhaseChangeMessage message = (PhaseChangeMessage) received;

                    if (message.getNewPhase() == DAY) {
                        player_list = message.getNewPlayerList();
                        nightToDay();
                        p_list_adapter.notifyDataSetChanged();
                    }

                    else if (message.getNewPhase() == NIGHT) {
                        player_list = message.getNewPlayerList();
                        dayToNight();
                        p_list_adapter.notifyDataSetChanged();
                    }
                }

                else if (received.getClass() == DecisionMessage.class) {
                    DecisionMessage message = (DecisionMessage) received;

                    if (message.isVotedFor()) {
                        yes++;
                    } else {
                        no++;
                    }

                    total++;

                    if (total == player_list.size()) {
                        if (yes > no) {
                            player_list.remove(nominatedPlayer);
                            player_map.remove(nominatedPlayer);
                            p_list_adapter.notifyDataSetChanged();

                            for (String player_name : player_map.keySet()) {
                                Player playerObj = player_map.get(player_name);

                                try {
                                    connectionsClient.sendPayload(playerObj.getConnectId(), Payload.fromBytes(SerializationHandler.serialize(new PlayerLynchMessage(nominatedPlayer, player_list))));
                                } catch (IOException e) {
                                    Log.d(TAG, e.getMessage());
                                }
                            }
                        }

                        yes = 0;
                        no = 0;
                        total = 0;
                    }
                }

                else if (received.getClass() == PlayerLynchMessage.class) {
                    PlayerLynchMessage message = (PlayerLynchMessage) received;

                    player_list = message.getUpdatedPlayerList();
                    p_list_adapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String id, @NonNull ConnectionInfo connectionInfo) {
            Log.d("MainGame", "Connection initiated accepting connection");
            connectionsClient.acceptConnection(id, payloadCallback);
            if (host) {
                player_list.add(connectionInfo.getEndpointName());
                player_map.put(connectionInfo.getEndpointName(), new Player(connectionInfo.getEndpointName(), id));
                p_list_adapter.notifyDataSetChanged();
                Log.d(TAG, "Accepted connection player_list is now " + player_list.toString());
            }
        }

        @Override
        public void onConnectionResult(@NonNull String s, @NonNull ConnectionResolution connectionResolution) {
            Log.d(TAG, "Connection established");
        }

        @Override
        public void onDisconnected(@NonNull String s) {

        }
    };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String id, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            connectionsClient.stopDiscovery();
            Log.d(TAG, id);
            Log.d(TAG, "Endpoint found with serviceId: " + discoveredEndpointInfo.getServiceId());
            Log.d("SearchGameActivity", "Endpoint found, connecting to device");
            String connection_message = "Do you want to connect to game: " + discoveredEndpointInfo.getEndpointName() + "?";

            final String endpointId = id;
            serviceId = id;
            android.app.AlertDialog.Builder builder;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                builder = new android.app.AlertDialog.Builder(curr_activity, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new android.app.AlertDialog.Builder(curr_activity);
            }
            builder.setTitle("Game Found")
                    .setMessage(connection_message)
                    .setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface dialog, int which) {
                            // continue with connection
                            connectionsClient.requestConnection(pname, endpointId, connectionLifecycleCallback).addOnSuccessListener(new OnSuccessListener<Void>() {

                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "Successfully requested connection");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {

                                    Log.d(TAG, "Failed to request connection", e);
                                }
                            });
                        }
                    })
                    .setNegativeButton(android.R.string.no, new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface dialog, int which) {
                            // continue with discovery
                            startDiscovery();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        }

        @Override
        public void onEndpointLost(@NonNull String s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_game);
        curr_activity = this;

        //Hide button and listview on startup
        startgamebutton = (Button)findViewById(R.id.mg_start_game_button);
        list = (ListView)findViewById(R.id.mg_player_list);
        startgamebutton.setVisibility(View.GONE);
        list.setVisibility(View.GONE);
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

        gmnameview = (TextView)findViewById(R.id.mg_game_room_name_view);
        gmnametext = (TextView)findViewById(R.id.mg_game_room_name);
        pnnameview = (TextView)findViewById(R.id.mg_player_name_view);
        pnnametext = (TextView)findViewById(R.id.mg_player_name);
        listtext = (TextView)findViewById(R.id.mg_list_text);

        connectionsClient = Nearby.getConnectionsClient(this);

        pname = getIntent().getStringExtra("player_name");
        gname = getIntent().getStringExtra("game_name"); //TO-DO: Joining players should have gname synced to the host
        host = getIntent().getBooleanExtra("host", false);

        pnnameview.setText(pname);
        gmnameview.setText(gname);

        //Create game object and player object only for host
        if (host) {
            game = new Game(gname, pname);
            player = new Player(pname, "");
            player_list.add(pname);
            startgamebutton.setVisibility(View.VISIBLE); //enables start game button
            player_map.put(pname, player);
            list.setVisibility(View.VISIBLE); //enables list view
        }

        //Display list of players
        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.activity_list_element, player_list);

        p_list_adapter = adapter;

        //Perform function on clicking item in list
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Log.d("ListListener", "Clicked item " + position + " " + id);


            }
        });

        gname = getIntent().getStringExtra("game_name");

        if (host) {
            mode = PLAYER_LIST;
            startAdvertising();
        } else {
            mode = SEARCH;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            } else {
                startDiscovery();
            }
        }
    }

    // Change MainGameActivity configuration for running the game
    protected void startGame(View v) {
        connectionsClient.stopAdvertising();
        int numPlayers = player_map.keySet().size();
        Log.d(TAG, "numPlayers = " + numPlayers);
        Random rand = new Random();
        int mafiaNum = rand.nextInt(numPlayers);
        int doctorNum = -1;
        if  (numPlayers >  2) {
            while (mafiaNum == doctorNum) {
                doctorNum = rand.nextInt(numPlayers);
            }
        }

        int i = 0;
        for (String player : player_map.keySet()) {
            Player playerObj = player_map.get(player);
            if (i == mafiaNum) {
                playerObj.setRole("Mafia");
            }
            else if (i == doctorNum) {
                playerObj.setRole("Doctor");
            }
            else {
                playerObj.setRole("Villager");
            }

            if (!player.equals(pname)) {
                try {
                    connectionsClient.sendPayload(playerObj.getConnectId(), Payload.fromBytes(SerializationHandler.serialize(new StartGameMessage(playerObj, player_list)))).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, e.getMessage());
                        }
                    });

                    Log.d(TAG, "Sent StartGameMessage");
                } catch (IOException e) {
                    Log.d(TAG, "Failed to send payload StartGameMessage: " + e.toString());
                    //Log.d(TAG, e.getMessage());
                }
            }

            i++;
        }

        playerListToDay();
        /*
        Intent pl_intent = new Intent(MainGameActivity.this, MainGameDay.class);
        boolean host = getIntent().getBooleanExtra("host", false);
        if (host) {
            pl_intent.putExtra("Game", (Serializable)game);
        }
        pl_intent.putExtra("host", host);
        startActivity(pl_intent);
        */
    }

    //sitch from PlayerList to DayPhase
    private void playerListToDay() {
        Log.d(TAG, "Entered playerListToDay()");
        countdownText.setVisibility(View.VISIBLE);
        countdownButton.setVisibility(View.VISIBLE);
        countdownButtonReset.setVisibility(View.VISIBLE);
        pnnametext.setVisibility(View.GONE);
        pnnameview.setVisibility(View.GONE);
        gmnametext.setText("Your role is: ");
        String role = player.getRole();
        gmnameview.setText(role);
        listtext.setText("Nominate");
        list.setVisibility(View.VISIBLE);
        startgamebutton.setVisibility(View.GONE);
        mode = DAY;
        startTimer();
    }

    private void dayToNight() {
        if (player.isMafia()) {
            listtext.setText("Choose a player to kill");
        } else if (player.isDoctor()) {
            listtext.setText("Choose a player to save");
        } else {
            listtext.setVisibility(View.GONE);
            list.setVisibility(View.GONE);
        }

        mode = NIGHT;
        resetTimer();
    }

    private void nightToDay() {
        listtext.setText("Nominate");
        if (!player.isDoctor() && !player.isMafia()) {
            listtext.setVisibility(View.VISIBLE);
            list.setVisibility(View.VISIBLE);
        }
        mode = DAY;
        resetTimer();
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(gname, serviceId, connectionLifecycleCallback, new AdvertisingOptions(STRATEGY)).addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("MainGame", "Successfully started advertising");
                    }
                }
        ).addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MainGame", "Failed to start advertising", e);
                    }
                }
        );
        Log.d("startAdvertising", "Started Advertising");
    }

    private void startDiscovery() {
        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, new DiscoveryOptions(STRATEGY)).addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("SearchGame","Successfully started discovery");
                    }
                }
        ).addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("SearchGame", "Failed to start discovery", e);
                    }
                }
        );
        Log.d("SearchGameActivity)", "started discovery");
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDiscovery();
            }
        } else {
            Log.d("SearchGame", "Guess we're not running the game");
        }
    }

    // Timer functions
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
                if (host) {
                    PhaseChangeMessage message;
                    if (mode == DAY) {
                        message = new PhaseChangeMessage(NIGHT, player_list);
                    } else {
                        message = new PhaseChangeMessage(DAY, player_list);
                    }

                    for (String player_name : player_map.keySet()) {
                        Player playerObj = player_map.get(player_name);

                        try {
                            connectionsClient.sendPayload(playerObj.getConnectId(), Payload.fromBytes(SerializationHandler.serialize(message)));
                        } catch (IOException e) {
                            Log.d(TAG, e.getMessage());
                        }
                    }

                    if (mode == DAY) {
                        dayToNight();
                    } else {
                        nightToDay();
                    }
                }
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
        timeLeftInMilliseconds = MAX_COUNTDOWN;
        if(timerRunning)
        {
            stopTimer();
        }
        updateTimer();
        startTimer();
    }
}