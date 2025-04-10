package fh.kiel.interlockapp;

import android.bluetooth.BluetoothDevice;

public class PostDevice {

    private String DeviceNr;
    private BluetoothDevice Device;

    public PostDevice(String DeviceNr, BluetoothDevice  Device) {
        this.DeviceNr= DeviceNr;
        this.Device= Device;
    }

    public String getDeviceNr() {
        return DeviceNr;
    }

    public void setDeviceNr(String deviceNr) {
        DeviceNr = deviceNr;
    }

    public BluetoothDevice  getDevice() {
        return Device;
    }

    public void setDevice(BluetoothDevice  device) {
        Device = device;
    }
}
