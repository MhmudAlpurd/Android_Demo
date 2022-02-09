package edu.cmu.pocketsphinx.demo;

import static android.widget.Toast.makeText;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import edu.cmu.pocketsphinx.demo.textToSpeech.Speech;


public class ModulesActivity extends Activity implements RecognitionListener {
    String message_from_SpeechRecActivity= null;

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "thanks buddy";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";

    /* Keyword we are looking for to activate menu */
    // private static final String KEYPHRASE = "oh mighty computer";
    private static final String KEYPHRASE = "thanks buddy";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static SpeechRecognizer recognizer;

    TextView enabledModule;
    TextView obj;

    private static long startTime = 0;
    double elapsedTimeInSecond = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modules);

        startTime = SystemClock.uptimeMillis();

        Log.v("DB_Modules", "onCreate: " + "01");

        enabledModule = findViewById(R.id.tv_enabledModule);
        obj = findViewById(R.id.tv_obj);


        String whichModule = getIntent().getStringExtra("whichModule");
        String whichObject = getIntent().getStringExtra("whichObject");

        if (whichModule != null && !whichModule.isEmpty()){
            Speech.talk(whichModule, getApplicationContext());
            enabledModule.setText(whichModule);
        }
        if (whichObject != null && !whichModule.isEmpty()){
            obj.setText(whichObject);
        }else {
            obj.setText("");
        }
        Log.v("DB_Modules", "onCreate: " + "02");


        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        Log.v("DB_Modules", "onCreate: " + "03");

        new ModulesActivity.SetupTask(this).execute();

        Log.v("DB_Modules", "onCreate: " + "04");

    }


    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<ModulesActivity> activityReference;
        SetupTask(ModulesActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {
            Log.v("DB_Modules", "onPostExecute: " + "01");

            if (result != null) {
              //  ((TextView) activityReference.get().findViewById(R.id.caption_text)).setText("Failed to init recognizer " + result);
            } else {
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        goToMain(startTime, SystemClock.uptimeMillis());
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new ModulesActivity.SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        goToMain(startTime, SystemClock.uptimeMillis());
        super.onDestroy();
        Log.v("DB_Modules", "onDestroy: " + "01");

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        goToMain(startTime, SystemClock.uptimeMillis());

        Log.v("DB_Modules", "onPartialResult: " + "01");

        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            Log.v("TEST_ASR_SERVICE", "stop Buddy");
            switchSearch(MENU_SEARCH);
            intentToPocketActivity();
            onDestroy();
        }
        /*else if (text.equals(DIGITS_SEARCH))
            switchSearch(DIGITS_SEARCH);
        else if (text.equals(PHONE_SEARCH))
            switchSearch(PHONE_SEARCH);
        else if (text.equals(FORECAST_SEARCH))
            switchSearch(FORECAST_SEARCH);*/
        else {
            //((TextView) findViewById(R.id.result_text)).setText(text);
        }
    }

    private void intentToPocketActivity() {
        goToMain(startTime, SystemClock.uptimeMillis());
        Log.v("DB_Modules", "intentToPocketActivity: " + "01");

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
        Intent intent = new Intent(ModulesActivity.this, PocketSphinxActivity.class);
        startActivity(intent);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        goToMain(startTime, SystemClock.uptimeMillis());

        Log.v("DB_Modules", "onResult: " + "01");

        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        goToMain(startTime, SystemClock.uptimeMillis());

        Log.v("DB_Modules", "onBeginningOfSpeech: " + "01");

    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        goToMain(startTime, SystemClock.uptimeMillis());

        Log.v("DB_Modules", "onEndOfSpeech: " + "01");

        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        goToMain(startTime, SystemClock.uptimeMillis());

        Log.v("DB_Modules", "switchSearch: " + "01");

        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        //String caption = getResources().getString(captions.get(searchName));
       // ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        goToMain(startTime, SystemClock.uptimeMillis());
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them
        int sensibility = 10;
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setKeywordThreshold(Float.valueOf("1.e-" + 2 * sensibility)) //Sensibility

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);

        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        // Create grammar-based search for digit recognition
        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

        // Create language model search
        File languageModel = new File(assetsDir, "weather.dmp");
        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

        // Phonetic search
        File phoneticModel = new File(assetsDir, "en-phone.dmp");
        recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
    }

    @Override
    public void onError(Exception error) {
        Log.v("DB_Modules", "onError: " + "01");
        goToMain(startTime, SystemClock.uptimeMillis());
        // ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        Log.v("DB_Modules", "onTimeout: " + "01");

        switchSearch(KWS_SEARCH);
    }


    public double goToMain(long start, long end){
        elapsedTimeInSecond =  (double) (end - start) / 1000 ;
        if (elapsedTimeInSecond > 20){
                intentToPockActivity();
        }else {

        }
        return  elapsedTimeInSecond;
    };

    private void intentToPockActivity() {
        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
        Intent intent = new Intent(ModulesActivity.this, PocketSphinxActivity.class);
        startActivity(intent);
    }

}