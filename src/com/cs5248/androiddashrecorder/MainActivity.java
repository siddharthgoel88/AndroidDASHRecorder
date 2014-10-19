package com.cs5248.androiddashrecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity {
	
	//Hard coding the external storage directory due to some path issue.
	private static final File ExternalStorageDir = new File("/storage/sdcard0/");
	private static final String DIR_NAME = "DASHRecorder";
	private static final String SERVER_URI = "http://pilatus.d1.comp.nus.edu.sg/~a0040609/video.php";
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private String fileName;
    private Uri videoUri;
    private String outputPath;
    private Intent videoIntent;

	private void dispatchTakeVideoIntent() {
	    videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
	    videoUri = getOutputMediaFileUri(); // create a file to save the video
	    videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
	    if (videoIntent.resolveActivity(getPackageManager()) != null) {
	        startActivityForResult(videoIntent, REQUEST_VIDEO_CAPTURE);
	    }
}	

    private String getSegmentFolder(String innerFolderName) {
    	String folderName = DIR_NAME + "/segments/" + innerFolderName;
    	
//    	File segmentFolder = new File(Environment.getExternalStorageDirectory(), folderName);
    	File segmentFolder = new File(ExternalStorageDir, folderName);
		segmentFolder.mkdirs();
		
		return segmentFolder.getPath() + "/";
	}

	/**
     * Convenience method to generate the path where to store the video 
     * recording.
     * @return Uri where the recording is to be saved
     */
	private Uri getOutputMediaFileUri() {
		String folderName = DIR_NAME + "/video/" ;
		fileName = "DASH_Video_" + new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(new Date());
		String fileNameWithExt = fileName + ".mp4";
		
//		File videoFolder = new File(Environment.getExternalStorageDirectory(), folderName);
		File videoFolder = new File(ExternalStorageDir, folderName);
		videoFolder.mkdirs();

		// Delete all previous files in video folder
		//for (File tmp : videoFolder.listFiles())
		//	  tmp.delete();

		File video = new File(videoFolder, fileNameWithExt);
		Uri uriSavedImage = Uri.fromFile(video);
		return uriSavedImage;
	}
	
	private void segmentVideo() {
		 //Segment the video in splits of 10 seconds   
        outputPath = getSegmentFolder(fileName);
        Log.i("DASH", "Output path = " + outputPath);
        
        SplitVideo obj = new SplitVideo();
        String splitDuration = "10.0";
        
        obj.execute(videoUri.getPath(), outputPath, splitDuration );
	}
	
	private void uploadVideo()
	{
		Log.d("DASH", "Entering uploadVideo");
		UploadVideoToServer uploadToServer = new UploadVideoToServer();
		
		Log.d("DASH", "Calling upload function");
		uploadToServer.execute(outputPath, SERVER_URI);
		
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
        
        Button b2 = (Button) findViewById(R.id.segmentButton);
        b2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				segmentVideo();
			}
		});
        
        Button upload = (Button) findViewById(R.id.uploadButton);
        upload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) 
			{
				uploadVideo();
			}
		});
    }
    
    
}
