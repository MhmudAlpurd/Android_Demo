package edu.cmu.pocketsphinx.demo;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class SpeechRecognition_Service extends Service implements RecognitionListener {

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    private SpeechRecognizer speechRecognizer;

    public SpeechRecognition_Service() {

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"start Service.",Toast.LENGTH_SHORT).show();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizer.setRecognitionListener(this);

        Intent voice = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        voice.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
                .getPackage().getName());
        voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        voice.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);

        speechRecognizer.startListening(voice);
        Log.v("TEST_ASR_SERVICE", "speech recognition started ");
        return START_REDELIVER_INTENT ;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        Log.v("TEST_ASR_SERVICE", "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {
        Log.v("TEST_ASR_SERVICE", "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.v("TEST_ASR_SERVICE", "onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float v) {
        Log.v("TEST_ASR_SERVICE", "onRmsChanged");

    }

    @Override
    public void onBufferReceived(byte[] bytes) {
        Log.v("TEST_ASR_SERVICE", "onBufferReceived");

    }

    @Override
    public void onEndOfSpeech() {
        Log.v("TEST_ASR_SERVICE", "onEndOfSpeech");

    }

    @Override
    public void onError(int i) {
        Log.v("TEST_ASR_SERVICE", "onError");

    }

    @Override
    public void onResults(Bundle bundle) {
        Log.v("TEST_ASR_SERVICE", "OnResult: " + bundle);
    }

    @Override
    public void onPartialResults(Bundle bundle) {
        Log.v("TEST_ASR_SERVICE", "onPartialResults");

    }

    @Override
    public void onEvent(int i, Bundle bundle) {
        Log.v("TEST_ASR_SERVICE", "onEvent");

    }



}