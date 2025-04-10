package fh.kiel.interlockapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class CommunicationActivity extends AppCompatActivity implements ServiceConnection, SerialListener, Listener {

    //https
    OkHttpClient client;
    private Request request=null;

    //User-Elements
    TextView StatusTV, TaskTV, DeviceTV,EventTV;
    TextView AnimationTV;
    Button Trigger;
    ScrollView scrollView;
    SafeTrackCommunication safeTrackCommunication;
    Button eventVisabilityBtn,EventClearBtn;
    Animation animation;

    //Bluetooth
    private String DeviceName;
    private String DeviceAdress;
    private enum Receiving { False, Status, Logfile }
    private enum Connected { False, Pending, True }
    private Receiving receiving = Receiving.False;
    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private final String newline = TextUtil.newline_crlf;

    //service
    private CommunicationService service;

    //Date
    SimpleDateFormat Date_Format;

    //Timer
    private static final long Time_Millis = 10000;
    private CountDownTimer Count_Down;
    private boolean Timer_Status;
    private long Time_Left = Time_Millis;

    //file
    FileOutputStream fileOutputStream;
    File file=null;

    //Internet Connection
    boolean NetworkConnection=false;
    ConnectivityManager connectivityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        Intent intent= getIntent();
        Bundle B = intent.getExtras();
        DeviceAdress= B.getString("DeviceAdress");
        DeviceName= B.getString("DeviceName");

        safeTrackCommunication =new SafeTrackCommunication(this, this);
        client= safeTrackCommunication.getClient();

        Date_Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        DeviceTV=findViewById(R.id.DeviceTV);
        StatusTV=findViewById(R.id.StatusTV);
        TaskTV=findViewById(R.id.TaskTV);
        Trigger= findViewById(R.id.TriggerBtn);
        EventTV=findViewById(R.id.EventTV);
        eventVisabilityBtn= findViewById(R.id.eventVisabilityBtn);
        EventClearBtn= findViewById(R.id.eventClearBtn);

        scrollView=findViewById(R.id.scrollView);
        AnimationTV=findViewById(R.id.AnimationTv);
        DeviceTV.setText("Device: "+ DeviceName);
        StatusTV.setText("UnKnown");



        EventTV.setMovementMethod(new ScrollingMovementMethod());
        Trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BT_Send("Status");

            }
        });


        eventVisabilityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(eventVisabilityBtn.getText().equals("+")){
                    scrollView.setVisibility(View.VISIBLE);
                    eventVisabilityBtn.setText("-");
                }else {
                    scrollView.setVisibility(View.INVISIBLE);
                    eventVisabilityBtn.setText("+");
                }

            }


        });
        EventClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            EventTV.setText("");
            }
        });

        //internet Connection
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);


        if (!(connectivityManager == null)) {
            NetworkRequest Net_request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

        connectivityManager.registerNetworkCallback(Net_request, new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(@NonNull Network network) {
                NetworkConnection=true;
                System.out.println("Connection available");
                Log("NetworkConnectionAvailable");
                super.onAvailable(network);
            }

            @Override
            public void onLost(@NonNull Network network) {
                NetworkConnection=false;
                Log("NetworkConnectionLost");
                System.out.println("Lost");


                super.onLost(network);
            }

        });
        }


        Animate();
        Toast.makeText(getApplicationContext(),DeviceAdress,Toast.LENGTH_SHORT).show();
        bindService(new Intent(getBaseContext(), CommunicationService.class), this, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(service != null) {

            service.attach(this);
        }
        else
            startService(new Intent(getBaseContext(), CommunicationService.class));

    }
    @Override
    public void onStop() {

        if(service != null && !isChangingConfigurations())
            service.ShowNotification();
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        if (connected != Connected.False)
            disconnect("disconnect");
        stopService(new Intent(getBaseContext(), CommunicationService.class));
        super.onDestroy();
    }

    /*
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }*/

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            runOnUiThread(this::connect);
        }
    }

   //SerialListener
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        System.out.println("On Service C O N N E C T E D");

        service = ((CommunicationService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart ) {
            initialStart = false;
            runOnUiThread(this::connect);
        }
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }




    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DeviceAdress);
            StatusTV.setText("connecting");
            Log("connecting started");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getApplicationContext(), device);
            service.connect(socket);

        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }


    private void disconnect(String Massage) {
        connected = Connected.False;
        StatusTV.setText("Not connected");
        CancelAnimation();
        service.disconnect();
        Log(Massage+"connection failed: " );
    }

    private void BT_Send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getBaseContext(), "not connected", Toast.LENGTH_SHORT).show();
            Log("Unable to send massage. no connection to:"+DeviceName);
            return;
        }
        if(receiving!= Receiving.False && !str.equals("Logfile")){
            Log("please wait until task is finished");
            return;
        }
        try {
            byte[] data;
                data = (str + newline).getBytes();
            service.write(data);

            if(data.toString().equals("Trigger"))
            Log("new trigger massage was send");
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }



    private void BT_Receive(byte[] data) {

            if(receiving != Receiving.False)
            TaskTV.setText("Reciving:"+ receiving);

            switch (receiving){

                case False:
                    switch (new String(data)){
                        case "Status":
                            receiving = Receiving.Status;
                            StartTimer();
                            if(file == null)
                            file = new File(getFilesDir(), Constants.StatusFile);

                            if(file.exists())
                                System.out.println("file.exists()");
                            else
                                System.out.println("file_Does_not_exists.exists()");

                            try {
                                fileOutputStream = new FileOutputStream(file);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "Logfile":
                            receiving = Receiving.Logfile;
                            StartTimer();
                            file = new File(getFilesDir(), Constants.LogfileFile);

                            if(file.exists())
                                System.out.println("file.exists()");
                            else
                                System.out.println("file_Does_not_exists.exists()");
                            try {
                                fileOutputStream = new FileOutputStream(file);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                            break;
                        case "Trigger":
                            Log("Trigger massage was received");
                            BT_Send("status");
                            break;
                        default:
                            Log(new String(data));
                            //System.out.println(new String(data));

                    }
                    break;
                case Status:
                case Logfile:
                    if(Timer_Status) {
                        resetTimer();
                    }
                    else {
                        Count_Down.cancel();
                        break;
                    }
                    try {
                        fileOutputStream.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }

    }


    //Timer
    private void StartTimer() {
        Count_Down = new CountDownTimer(Time_Left, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                Time_Left = millisUntilFinished;

            }


            @Override
            public void onFinish() {
                Time_Left = Time_Millis;
                Timer_Status = false;

                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log(receiving +" was received and saved to file");
                    SendToSafeTrack();

             }
        }.start();

        Timer_Status = true;

    }
    private void resetTimer() {
        Count_Down.cancel();
        Time_Left = Time_Millis;
        StartTimer();

    }




    void SendToSafeTrack(){

        if(! NetworkConnection) {
            Log("unable to upload " + receiving + " !No internet access available");
            safeTrackCommunication.task = SafeTrackCommunication.Task.Nothing;
            receiving = Receiving.False;

            return;
        }else if(receiving == Receiving.Status) {
            request=safeTrackCommunication.CreateStatusRequest();
        } else if(receiving == Receiving.Logfile) {
            request=safeTrackCommunication.CreateLogfileRequest();
        }else{
            Log("Task error");
            return;
        }
        TaskTV.setText("uploading:"+ receiving);
        Log("start Uploading " + receiving +" to SafeTrack");

        try {
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    receiving = Receiving.False;
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    System.out.println("ok");
                    if (response.isSuccessful()) {

                        ResponseBody responseBody = response.body();
                        //if(reciving==Reciving.Status)

                            Log("Uploading "+ receiving +" was successful. Massage: "+ responseBody.string());
                            BT_Send("Logfile");

                    } else {
                        System.out.println("response is not successful" + response.code());
                        if(receiving == Receiving.Logfile)
                            Log("Uploading "+ receiving +" was not accepted. Error Code: "+ response.code());
                    }
                    if (file.exists()) {
                        file.delete();
                        file=null;
                        System.out.println("File is deleted");
                    }
                    receiving = Receiving.False;
                }
        });
    } catch (Exception e) {  e.printStackTrace(); }

}



        void Log(String Text) {
            DeviceTV.post(new Runnable() {
                @Override
                public void run() {
                    EventTV.append(Date_Format.format(new Date())+"\n");
                    EventTV.append(Text +"\n\n");
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    if(receiving == Receiving.False)
                        TaskTV.setText("No Task");
                }
            });
        }



        @Override
        public void onFailure(IOException e) {
        }
        @Override
        public void onResponseSuccessful() { }
        @Override
        public void onResponseNotSuccessful(int code) { }



        //Animation
        void Animate(){
            animation = AnimationUtils.loadAnimation(this, R.anim.animation);
            AnimationTV.startAnimation(animation);
        }

        void CancelAnimation() {
            AnimationTV.clearAnimation();

            if(connected==Connected.False)
            AnimationTV.setBackground(getDrawable(R.drawable.ic_not_connected));
            else
            AnimationTV.setBackground(getDrawable(R.drawable.ic_connected));
        }



        //SerialListener
        @Override
        public void onSerialConnect() {
            connected = Connected.True;
            CancelAnimation();
            StatusTV.setText("connected: ");
            System.out.println("connected");
            Log("connected to Device:/n"+ DeviceName);

            //send("anythink");
        }

        @Override
        public void onSerialConnectError(Exception e) {
            disconnect("connection failed: " + e.getMessage());
        }

        @Override
        public void onSerialRead(byte[] data) {
            BT_Receive(data);
        }


        @Override
        public void onSerialIoError(Exception e) {
            disconnect("connection lost: " + e.getMessage());
        }

    }