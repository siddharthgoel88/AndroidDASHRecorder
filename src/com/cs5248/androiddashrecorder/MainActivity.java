package com.cs5248.androiddashrecorder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
	
	//Hard coding the external storage directory due to some path issue.
	private static final File ExternalStorageDir = new File("/storage/sdcard0/");
	private static final String DIR_NAME = "DASHRecorder";
	private static final String SERVER_URI = "http://pilatus.d1.comp.nus.edu.sg/~a0110280/";
	private static final String END_UPLOAD_PAGE = "process.php";
	private static final String UPLOAD_PHP_PAGE = "upload.php";
    static final int REQUEST_VIDEO_CAPTURE = 1;
    private String fileName;
    private Uri videoUri;
    private String outputPath;
    private Intent videoIntent;
    private ProgressBar segmentProgressBar;
    private ProgressBar uploadProgressBar;
    private TextView segmentProgressView;
    private TextView uploadProgressView;
    //Keeping FileObserver global else it will be garbage collected
    private FileObserver observer; 
    
    /**
     * This method initiates an intent to for recording a video
     * and storing it at a specific location. 
     */
	private void recordVideoIntent() {
		
		videoIntent = new Intent(MainActivity.this, RecordVideo.class);
		
	    //creates a file to save the video and returns its uri
	    videoUri = getOutputMediaFileUri();
	    videoIntent.putExtra("videopath", videoUri.getPath());
	    Log.i("DASH", "VideoUri:" + videoUri.getPath());
	    
	    startActivity(videoIntent);

	    String videoPath = ExternalStorageDir.getPath() + "/" + DIR_NAME + "/video";
	    
	    //Keeps tracks when the video write is complete so as to segment it
	    observer = new FileObserver(videoPath) { 
	        @Override
	        public void onEvent(int event, String path) {
	        	if (event == FileObserver.CLOSE_WRITE) {
	        		Log.i("DASH", "File created"  );
	        		this.stopWatching();
	        		segmentVideo();
	        	}
	        }
	    };
	    observer.startWatching();
}	

	/**
	 * Convenience function to get the location where the segments
	 * have to be saved.
	 * @param innerFolderName Name of folder that we need to create 
	 * in segment folder
	 * @return Location in form of String where segments have to be 
	 * saved  
	 */
    private String getSegmentFolder(String innerFolderName) {
    	String folderName = DIR_NAME + "/segments/" + innerFolderName;
    	File segmentFolder = new File(ExternalStorageDir, folderName);
		segmentFolder.mkdirs();
		
		return segmentFolder.getPath() + "/";
	}

	/**
     * Convenience method to generate the path where to store the video 
     * recording.
     * @return Uri where the recording is to be saved
     */
	@SuppressLint("SimpleDateFormat")
	private Uri getOutputMediaFileUri() {
		String folderName = DIR_NAME + "/video/" ;
		fileName = "DASH_Video_" + new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss").format(new Date());
		String fileNameWithExt = fileName + ".mp4";
		
		File videoFolder = new File(ExternalStorageDir, folderName);
		videoFolder.mkdirs();

		File video = new File(videoFolder, fileNameWithExt);
		Uri uriSavedImage = Uri.fromFile(video);
		return uriSavedImage;
	}
	
	/**
	 * Makes a call to video segmentation in the form of 
	 * asynchronous task.
	 */
	private void segmentVideo() {
		//Segment the video in splits of 3 seconds 
		Log.i("DASH","Inside segmentVideo()");
        outputPath = getSegmentFolder(fileName);
        Log.i("DASH", "Path where segments have to be saved is " + outputPath);
                
        SplitVideo obj = new SplitVideo(segmentProgressBar, segmentProgressView);
        obj.execute(videoUri.getPath(), outputPath, "3.0");
	}
	
	/**
	 * Convenience method for uploading the segments to the server 
	 */
	private void uploadVideo()
	{
		Log.d("DASH", "Entering uploadVideo");

		UploadVideoToServer uploadToServer = new UploadVideoToServer(outputPath, SERVER_URI, UPLOAD_PHP_PAGE, END_UPLOAD_PAGE, 
				uploadProgressBar, uploadProgressView);

		Log.d("DASH", "Calling upload function");
		uploadToServer.execute();
		
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        segmentProgressBar = (ProgressBar) findViewById(R.id.segmentProgress);
        segmentProgressView = (TextView) findViewById(R.id.segmentTextView);
        uploadProgressBar = (ProgressBar) findViewById(R.id.uploadProgress);
        uploadProgressView = (TextView) findViewById(R.id.uploadTextView);
        
        Button record = (Button) findViewById(R.id.recordButton);
        record.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				recordVideoIntent();
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
