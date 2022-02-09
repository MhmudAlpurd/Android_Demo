package edu.cmu.pocketsphinx.demo;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import edu.cmu.pocketsphinx.demo.commandRecogniton.COMMANDREC;
import edu.cmu.pocketsphinx.demo.textReading.LivePreviewActivity;
import edu.cmu.pocketsphinx.demo.textToSpeech.Speech;

public class SpeechRecActivity extends Activity implements RecognitionListener {
    public static final String APP_TAG = "speech-to-text";
    private static final int RecordAudioRequestCode = 1;
    private SpeechRecognizer speechRecognizer;
    String message_from_TextRecognitionProcessor = null;
    boolean speechStarted = false;
    private static final String KWS_SEARCH = "thanks buddy";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "thanks buddy";
    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    long startTime = 0;
    long endTime = 0;
    double elapsedTimeInSecond = 0;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_rec);
        startTime = SystemClock.uptimeMillis();;


        System.out.println(APP_TAG + ": SpeechRecognizer.isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            checkPermission();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

       TextView editText = findViewById(R.id.tw_recognizedCommand_info);
       ImageView micButton = findViewById(R.id.iv_mic);


        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);  // number of maximum results
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);  // Enable this when offline
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());

        // Speech language settings
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US);

        // Speech time settings
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {



            @Override
            public void onReadyForSpeech(Bundle bundle) {
                goToMain(startTime,SystemClock.uptimeMillis());
                Log.v("TEST_ASR_SERVICE", "onReadyForSpeech: "+ goToMain(startTime,SystemClock.uptimeMillis()));
                micButton.setImageResource(R.drawable.microphone);
            }


            @Override
            public void onBeginningOfSpeech() {
                goToMain(startTime,SystemClock.uptimeMillis());

                Log.v("TEST_ASR_SERVICE", "onBeginningOfSpeech: "+ goToMain(startTime,SystemClock.uptimeMillis()));
                speechStarted=true;
                 editText.setHint("Listening...");
            }

            @Override
            public void onRmsChanged(float v) {
                goToMain(startTime,SystemClock.uptimeMillis());
                Log.v("TEST_ASR_SERVICE", "onRmsChanged: " + goToMain(startTime,SystemClock.uptimeMillis()));
                System.out.println(APP_TAG + ": onRmsChanged( " + v + " )");
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                goToMain(startTime,SystemClock.uptimeMillis());
                Log.v("TEST_ASR_SERVICE", "onBufferReceived" + + goToMain(startTime,SystemClock.uptimeMillis()));

                System.out.println(APP_TAG + ": onBufferReceived");
            }

            @Override
            public void onEndOfSpeech() {
                goToMain(startTime,SystemClock.uptimeMillis());
                Log.v("TEST_ASR_SERVICE", "onEndOfSpeech" + + goToMain(startTime,SystemClock.uptimeMillis()));
                speechStarted = false;
                speechRecognizer.startListening(speechRecognizerIntent);
                // Restart listening here if required.
            }

            @Override
            public void onError(int i) {
                goToMain(startTime,SystemClock.uptimeMillis());
                Log.v("TEST_ASR_SERVICE", "onError" + goToMain(startTime,SystemClock.uptimeMillis()));
             //  micButton.setImageResource(R.drawable.muted_mic);
                if (!speechStarted)
                    speechRecognizer.startListening(speechRecognizerIntent);
                // Restart listening here if required.
            }

            @Override
            public void onResults(Bundle bundle) {
                Log.v("TEST_ASR_SERVICE", "onResults");

                // micButton.setImageResource(R.drawable.muted_mic);
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                System.out.println(APP_TAG + ": onResults:");
                for (String s : data) {
                    System.out.println(APP_TAG + ":  " + s);
                }
                String txtResult = data.get(0);
                editText.setText(txtResult);
                Log.v("Test01", "result:"+ txtResult);

                String res =  COMMANDREC.find_madule_and_object(txtResult);

                Toast.makeText(SpeechRecActivity.this, res, Toast.LENGTH_SHORT).show();
                if (res != null && !res.isEmpty() && !res.equalsIgnoreCase("Repeat Your Command!") && !res.equalsIgnoreCase("Repeat Your Command!" + "|" + "nothing") ){
                    String whichModule = res.split("\\|")[0];
                    String whichObject = res.split("\\|")[1];
                    //tv_ModuleStatus.setText(whichModule);
                    String moduleEnabled = whichModule + "module is enabled";
                    Speech.talk(moduleEnabled, getApplicationContext());


                    switch (whichModule){

                        case "Text Reading":
                            Intent i = new Intent(SpeechRecActivity.this, LivePreviewActivity.class);
                            i.putExtra("whichModule",whichModule);
                            i.putExtra("whichObject",whichObject);
                            speechRecognizer.stopListening();
                            speechRecognizer.destroy();
                            startActivity(i);
                            break;

                        default: // "Finding Object", "Scene Description", "Trigger Word"
                            Intent j = new Intent(SpeechRecActivity.this, ModulesActivity.class);
                            j.putExtra("whichModule",whichModule);
                            j.putExtra("whichObject",whichObject);
                            speechRecognizer.stopListening();
                            speechRecognizer.destroy();
                            startActivity(j);


                    }




                }else if(res != null && !res.isEmpty() && res.equalsIgnoreCase("")){
                    startTime = SystemClock.uptimeMillis();
                    Speech.talk("Repeat Your Command!", SpeechRecActivity.this);
                }else{
                    goToMain(startTime,SystemClock.uptimeMillis());
                }

            }

            @Override
            public void onPartialResults(Bundle bundle) {
                goToMain(startTime,SystemClock.uptimeMillis());
                Log.v("TEST_ASR_SERVICE", "onPartialResults: " + goToMain(startTime,SystemClock.uptimeMillis()));
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                goToMain(startTime,SystemClock.uptimeMillis());
                Log.v("TEST_ASR_SERVICE", "onEvent: " + goToMain(startTime,SystemClock.uptimeMillis()));
            }
        });


        //Beep
        AudioManager amanager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
        amanager.setStreamMute(AudioManager.STREAM_ALARM, true);
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        amanager.setStreamMute(AudioManager.STREAM_RING, true);
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);

       speechRecognizer.startListening(speechRecognizerIntent);

    }


    private double goToMain(long start, long end){
        elapsedTimeInSecond =  (double) (end - start) / 1000 ;
        if (elapsedTimeInSecond > 20){
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            Intent intent = new Intent(SpeechRecActivity.this, PocketSphinxActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }else {

        }
        return  elapsedTimeInSecond;
    };



    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println(APP_TAG + ": Permission Granted");
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                System.out.println(APP_TAG + ": Permission Denied");
            }
        } else {
            System.out.println(APP_TAG + ": Permission Denied");
        }
    }

    private static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onResults(Bundle bundle) {

    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }
}