package com.cs5248.androiddashrecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	

    static final int REQUEST_VIDEO_CAPTURE = 1;
    private Intent videoIntent;
    private static final String APP_ROOT_DIR = "";
    private Uri fileUri;

	private void dispatchTakeVideoIntent() {
	    videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	    //fileUri = getOutputMediaFileUri(); // create a file to save the video
	    //videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
	    
	    if (videoIntent.resolveActivity(getPackageManager()) != null) {
	        startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE);
	    }
	    //Log.i("MainActivity:dispatchTakeVideoIntent" ,videoIntent.getData().toString());
}	

    /**
     * Convenience method to generate the path where to store the video 
     * recording.
     * @return Uri where the recording is to be saved
     */
	private Uri getOutputMediaFileUri() {
		String filename = new SimpleDateFormat("dd_MM_yyyy_hh_mm").format(new Date()) + ".mp4";
		File imagesFolder = new File(Environment.getExternalStorageDirectory(), APP_ROOT_DIR);
		imagesFolder.mkdirs();
		File image = new File(imagesFolder, filename);
		Uri uriSavedImage = Uri.fromFile(image);
		return uriSavedImage;
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button b = (Button) findViewById(R.id.recordButton);
        b.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dispatchTakeVideoIntent();
			}
		});
        
    }
    
    
}
