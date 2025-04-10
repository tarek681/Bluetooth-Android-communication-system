package fh.kiel.interlockapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    //Bluetooth enumeration
    private enum BTStatus { BTNotSupported, BTDisabled, BTEnabled }
    private BTStatus btStatus;

    //Bluetooth-device enumeration
    private enum BTDevice {Selected, NotSelected}
    private static BTDevice btDevice;

    private static BluetoothAdapter mybluetoothAdapter;
    private static String DeviceName,DeviceAddress;

    private static DataBase dataBase ;

    
    //device list 
    private ListView listView;
    private ArrayList<PostDevice> arrayList;
    private PostDevice_Adapter postDevice_adapter;

    //widgets
    View layout;
    Button ToConnectActBtn;
    TextView DeviceNameTV, TopTV;
    AlertDialog Dialog;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mybluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        layout=findViewById(R.id.layout);
        ToConnectActBtn =findViewById(R.id.ToConnectAcrBtn);
        DeviceNameTV= findViewById(R.id.DeviceNameTV);

        TopTV=findViewById(R.id.TopTV);
        dataBase = new DataBase(this);

        listView = findViewById(R.id.DeviceList);
        arrayList = new ArrayList<>();
        postDevice_adapter = new PostDevice_Adapter(this, arrayList);
        listView.setAdapter(postDevice_adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PostDevice Dev = arrayList.get(position);
                DeviceName= Dev.getDevice().getName();
                DeviceAddress = Dev.getDevice().getAddress();
                dataBase.Insert_Data("Device", DeviceName, DeviceAddress);
                System.out.println(Dev.getDeviceNr() + " D " + Dev.getDevice());
                StartCommunication();
            }
        });

        ToConnectActBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartCommunication();
            }
        });



    }

    @Override
    protected void onResume() {
        System.out.println(" onResume()");
        if (Dialog != null && mybluetoothAdapter.isEnabled()) {
            if (Dialog.isShowing())
                Dialog.dismiss();
        }
        SetUp();
        super.onResume();
    }
    

    //check Bluetooth status and then if a device is stored
    private void SetUp(){
        TopTV.setText("");
        if(mybluetoothAdapter == null)
            btStatus=btStatus.BTNotSupported;
        else if (!mybluetoothAdapter.isEnabled()) {
            btStatus= btStatus.BTDisabled;
            //System.out.println("<bluetooth is disabled>");
            //Alert_Dialog();
        }else if (mybluetoothAdapter.isEnabled()) {
            btStatus= btStatus.BTEnabled;
        }
        
        Cursor Device=ReadFromDB("Device");
        Device.moveToNext();
        DeviceName=Device.getColumnName(1);
        DeviceAddress =Device.getColumnName(2);

        if((Device.getCount() == 0) ){

            btDevice=BTDevice.NotSelected;
        }else {
            System.out.println("Selected");
            //Device.moveToNext();
            btDevice = BTDevice.Selected;

            DeviceName=Device.getString(1);
            DeviceAddress = Device.getString(2);
            System.out.println(Device.getString(2));
        }


        switch (btStatus){
            case BTNotSupported:
                Intent intent = new Intent(getBaseContext(),BTNotAvalible.class);
                startActivity(intent);
                break;

            case BTDisabled:
                Alert_Dialog();
                TopTV.setText("Bluetooth disabled");
                break;


            case BTEnabled:
                switch (btDevice){
                    case Selected:
                        System.out.println("Selected");
                        DeviceNameTV.setText("you are connected to Device : "+ DeviceName +"\n"+"do you want to start communication?\n\n");
                        SetVisibility(true);
                        break;

                    case NotSelected:
                        SetVisibility(false);
                        ShowDevices();
                        break;
                }
                break;
        }

    }


    //OptionMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.optionmenu, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.DDevice:

                System.out.println("DDevice");
                boolean result=dataBase.Delete_Data("Device");
                if(result) {
                    Toast.makeText(getApplicationContext(),"Device was deleted",Toast.LENGTH_SHORT).show();
                    onResume();
                }else{
                    Toast.makeText(getApplicationContext(),"No saved Device",Toast.LENGTH_SHORT).show();
                    System.out.println("No saved Device");
                }
                break;
            case R.id.BTSettings:
                System.out.println("BTSettings");
                OpenBTSettings();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    // show pired Bluetooth devices in a list
    private void ShowDevices() {
        arrayList.clear();
        if (mybluetoothAdapter != null) {
            int Nr = 1;
            for (BluetoothDevice device : mybluetoothAdapter.getBondedDevices()) {

                arrayList.add(new PostDevice(Nr + "", device));
                Nr++;
            }

            if(Nr==1)
                TopTV.setText("No paired devices!");
            else
                TopTV.setText("paired devices");
        }
        postDevice_adapter.notifyDataSetChanged();
    }

    
    private void Alert_Dialog() {
        Dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Bluetooth is disabled")
                .setMessage("do you want to open bluetooth sittings?")
                .setIcon(getDrawable(R.drawable.ic_bluetooth_disabaled))
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OpenBTSettings();
                        System.out.println("return from ACTION_BLUETOOTH_SETTINGS");
                    }
                }).setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Dialog != null ) {
                            if (Dialog.isShowing())
                                Dialog.dismiss();
                        }
                        listView.setVisibility(View.INVISIBLE);
                        layout.setVisibility(View.INVISIBLE);
                    }
                }).create();
        Dialog.show();
    }

    //Bluetooth_settings
    private void OpenBTSettings(){
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intent);
    }

    
    //write content to Database
    private void WriteToDB(String Name,String Content1,String Content2){
        dataBase.Insert_Data( Name, Content1, Content2);
    }

    
   //Read content From Database
    private Cursor ReadFromDB(String Name) {
        Cursor res = dataBase.Get_Data("Device");
        if (res.getCount() == 0) {
            System.out.println("No Entry Exists");
        }
        else
            System.out.println("Entry do Exists");
        return res;
    }


    //show either a List or a Button due to savesd device
    private void SetVisibility(boolean Case){
        if(Case) {
            listView.setVisibility(View.INVISIBLE);
            layout.setVisibility(View.VISIBLE);
        }else {
            layout.setVisibility(View.INVISIBLE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    //open StartCommunicationActivity
    private void StartCommunication(){
	Intent intent = new Intent(getBaseContext(),CommunicationActivity.class);
        intent.putExtra("DeviceAdress", DeviceAddress);
        intent.putExtra("DeviceName", DeviceName);
        startActivity(intent);
    }


}