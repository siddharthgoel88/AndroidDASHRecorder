package com.cs5248.androiddashrecorder;

import java.io.IOException;

import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.VideoEncoder;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

public class RecordVideo extends Activity implements SurfaceHolder.Callback{
	
	private boolean isRecording;
	private SurfaceHolder surfaceHolder;
	private SurfaceView surfaceView;
	private Camera camera;
	private MediaRecorder mediaRecorder;
	private final String TAG = "DASHRecorder";
	private String videoPath;
	private final int frameRate = 30;
	private final int bitRate = 3000000;
	private final int maxRecordDuration = 3600000;
	private final int maxFileSize = 2000000000;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record_video);
		
		isRecording = false;
		camera = null;
		
		surfaceView = (SurfaceView) findViewById(R.id.recordSurface);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		mediaRecorder = new MediaRecorder();
		videoPath = getIntent().getExtras().getString("videopath");
		
		Log.d(TAG, "The video path passed is " + videoPath);
		
		Button recordButton = (Button) findViewById(R.id.recordButton);
		recordButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (isRecording)
					stopRecording();
				else
					startRecoding();
			}
		});
	}
	
	private void startRecoding() {
		Log.d(TAG, "Started recording");
		
		camera.stopPreview();
		
		try {
			camera.unlock();
			mediaRecorder.start();
			isRecording = true;
		} catch(Exception e) {
			Log.d(TAG, "Oh God!!! Some exception " + e.getMessage());
		}
	}

	private void stopRecording() {
		Log.d(TAG, "Stopping recording");
		
		if(mediaRecorder != null)
			mediaRecorder.stop();
		
		isRecording = false;
		dispose();
	}

	private void dispose() {
		Log.d(TAG, "Performing dispose of various resources");
		if (mediaRecorder != null) {
			mediaRecorder.release();
			mediaRecorder = null;
		}
		if (camera != null) {
			camera.release();
			camera = null;
		}
		finish();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "Surface created");
		camera = Camera.open();
		camera.lock();
		if (camera == null) {
			Log.d(TAG, "Camera is null");
			finish();
		}
		try {
			mediaRecorder.setCamera(camera);
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mediaRecorder.setAudioEncoder(AudioEncoder.AMR_NB);
			mediaRecorder.setVideoEncoder(VideoEncoder.H264);
			mediaRecorder.setOutputFile(videoPath);
			mediaRecorder.setVideoSize(720, 480);
			mediaRecorder.setVideoEncodingBitRate(bitRate);
			mediaRecorder.setVideoFrameRate(frameRate);
			mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
			mediaRecorder.setMaxDuration(maxRecordDuration);
			mediaRecorder.setMaxFileSize(maxFileSize);
			mediaRecorder.prepare();
		} catch(Exception e) {
			Log.i(TAG, "Some exception in MediaRecorder init " + e.getMessage());
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG, "Surface changed called");
		Camera.Parameters arg = camera.getParameters();
		arg.setPreviewSize(720, 480);
		camera.setParameters(arg);
		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();
		} catch (IOException e) {
			Log.i(TAG, "Some camera exception :" + e.getMessage());
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "Surface Destroyed");
		stopRecording();
	}
	
	@Override
	public void onBackPressed() {
		Log.d(TAG, "Back button pressed. Stopping recording and existing!!");
		finish();
		stopRecording();
	}
}
