package com.alyj.smartconfort;

import android.speech.SpeechRecognizer;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity
         {

    private TextView returnedText;
    private ToggleButton toggleButton;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String LOG_TAG = "VoiceRecognitionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



    }


}