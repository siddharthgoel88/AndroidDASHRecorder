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
    private static final String APP_ROOT_DIR = "DASHRecorder";
    private Uri fileUri;

	private void dispatchTakeVideoIntent() {
	    videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	    fileUri = getOutputMediaFileUri(); // create a file to save the video
	    videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
	    if (videoIntent.resolveActivity(getPackageManager()) != null) {
	        startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE);
	    }
}	

    /**
     * Convenience method to generate the path where to store the video 
     * recording.
     * @return Uri where the recording is to be saved
     */
	private Uri getOutputMediaFileUri() {
		String foldername = APP_ROOT_DIR + "/video" ;
		String filename = new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(new Date()) + ".mp4";
		File videoFolder = new File(Environment.getExternalStorageDirectory(), foldername);
		videoFolder.mkdirs();

		// Delete all previous files in video folder
		for (File tmp : videoFolder.listFiles())
			  tmp.delete();

		File image = new File(videoFolder, "DASH_Video_" + filename);
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
