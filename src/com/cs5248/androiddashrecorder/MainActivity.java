package com.cs5248.androiddashrecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	

    static final int REQUEST_VIDEO_CAPTURE = 1;
    private Intent videoIntent;

	private void dispatchTakeVideoIntent() {
	    videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	    if (videoIntent.resolveActivity(getPackageManager()) != null) {
	        startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE);
	    }
}	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TextView tv = (TextView) findViewById(R.id.helloId);
        //tv.setText("Yuhu !!!");
        dispatchTakeVideoIntent();
    }
}
