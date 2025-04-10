package fh.kiel.interlockapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import java.util.List;

public class PostDevice_Adapter extends ArrayAdapter<PostDevice> {
    public PostDevice_Adapter(Context context, List<PostDevice> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView,ViewGroup parent) {

        if (convertView == null){

            convertView =LayoutInflater.from(getContext()).inflate(R.layout.devicelist, parent, false);
        }

        TextView DeviceNr= convertView.findViewById(R.id.deviceNr);
        TextView Device= convertView.findViewById(R.id.device);

        PostDevice MyDevice = getItem(position);

        DeviceNr.setText(MyDevice.getDeviceNr());
        Device.setText(MyDevice.getDevice().getName());

        return convertView;
    }
}
