package com.sansan.radio.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.FragmentActivity;

import com.sansan.radio.R;

public class MainTestActivity extends FragmentActivity {

    private Button bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_main);

        bt = findViewById(R.id.bt);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}
