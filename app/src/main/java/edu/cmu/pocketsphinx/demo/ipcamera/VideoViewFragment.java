package edu.cmu.pocketsphinx.demo.ipcamera;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.cmu.pocketsphinx.demo.R;

public class VideoViewFragment extends Fragment  {

    // Declare variables
    // VideoView videoview;
    // MainActivity main = new MainActivity();
   // MultiBoxTracker multiBoxTracker = new MultiBoxTracker(getActivity());

    private static final String TAG = "MjpegActivity";

    private MjpegView mv;
    // Physical display width and height.
    private static int displayWidth = 0;
    private static int displayHeight = 0;
    int i = 0;
    ViewGroup viewGroup;
    TextView img_test;

    // Video URL
//    public String path = main.Path;
//    String VideoURL = path + "Video1.mp4";
//        String VideoURL = "http://192.168.43.1:8080";

    //sample public cam
    String URL = "http://192.168.234.1/cgi-bin/hi3510/snap.cgi?&-getstream&-chn=2";
//    String URL = "http://192.168.43.134:5432/XMLParser/Video1.mp4";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try{
            Log.d("VVFRAGMENT", "3");

            viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_video_view, container, false);
           // View view = inflater.inflate(R.layout.fragment_video_view, container, false);
            mv = (MjpegView) viewGroup.findViewById(R.id.surfaceView);

            new DoRead().execute(URL);
           // return view;
            return viewGroup;
            // mv = new MjpegView(this.getContext());
        }catch (Exception e){
            throw e;
        }

    }

    public void onPause(){
        super.onPause();
        mv.stopPlayback();
    }

    public void onResume(){
        super.onResume();

    }



    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... Url) {
            //TODO: if camera has authentication deal with it and don't just not work
            Log.d("VVFRAGMENT", "1");


//            HttpResponse res = null;
//            DefaultHttpClient httpclient = new DefaultHttpClient();
            Log.d(TAG, "1. Sending http request");
            try {
                java.net.URL url = new URL(Url[0]); // here is your URL path
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                int responseCode=conn.getResponseCode();
                Log.v("TTTS", "ONPOST1");
//                res = httpclient.execute(new HttpGet(URI.create(url[0])));
//                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                Log.d("TestInflate", "2. Request finished, status = " + responseCode);
                if(responseCode==401){
                    Log.d("TestInflate", "401");
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(conn.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("TestInflate", "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            Log.d("VVFRAGMENT", "2");
            if(result != null) {
                mv.setSource(result);
                mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
                mv.showFps(true);
            }
        }
    }




}
