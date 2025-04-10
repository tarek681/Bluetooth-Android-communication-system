package fh.kiel.interlockapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class BTNotAvalible extends AppCompatActivity {
Button closeApp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btnot_avalible);

        closeApp = findViewById(R.id.CloseAppBtn);

        closeApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                System.exit(0);
            }
        });
    }
}