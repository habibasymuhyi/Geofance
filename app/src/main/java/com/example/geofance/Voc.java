package com.example.geofance;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class Voc {

    static TextToSpeech mytts = null;

    public static void init(Context c){
        mytts=new TextToSpeech(c, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    Locale locInd = new Locale("IDN");
                    int result=mytts.setLanguage(locInd);
                    if(result==TextToSpeech.LANG_NOT_SUPPORTED ||
                            result==TextToSpeech.LANG_MISSING_DATA){
                        Log.d("error", "Language not supported");
                    }
                } else
                    Log.d("error", "TTS failed :(");
            }
        });
    }

    public static void speak(final String myText){
        if(mytts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mytts.speak(myText, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                //cover all versions...
                mytts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    public static void off(){
        if(mytts !=null) {
            mytts.stop();
            //mytts.shutdown();  //calling this here is not what we want
        }
    }

    public static void shutdown(){
        if(mytts !=null) {
            mytts.shutdown();  //if you need call shutdown with this method
        }
    }
}