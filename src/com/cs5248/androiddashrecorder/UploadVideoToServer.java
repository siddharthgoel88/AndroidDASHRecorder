package com.cs5248.androiddashrecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import ch.boye.httpclientandroidlib.HttpEntity;
import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.StatusLine;
import ch.boye.httpclientandroidlib.client.HttpClient;
import ch.boye.httpclientandroidlib.client.methods.HttpPost;
import ch.boye.httpclientandroidlib.entity.mime.HttpMultipartMode;
import ch.boye.httpclientandroidlib.entity.mime.MultipartEntityBuilder;
import ch.boye.httpclientandroidlib.entity.mime.content.FileBody;
import ch.boye.httpclientandroidlib.impl.client.HttpClientBuilder;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;


public class UploadVideoToServer extends AsyncTask<Void, Integer, Void>
{
	private int numberOfSegments;
	private int numberOfSegmentsUploaded;
	private ProgressBar uploadProgress;
	private TextView textView;
	private String uploadUri;
	private String processUri;
	private String filesLocation;
	
	public UploadVideoToServer(String filesLocation, String serverUri, String upload, String process, 
			ProgressBar uploadProgress, TextView textView) 
	{
		this.filesLocation = filesLocation;
		uploadUri = serverUri + upload;
		processUri = serverUri + process;
		this.uploadProgress = uploadProgress;
		this.textView = textView;
		numberOfSegmentsUploaded = 0;
	}
	
	@Override
    protected void onPreExecute() {
    	uploadProgress.setMax(100);
    	textView.setText("Started to upload segments ...");
    }
	
	@Override
    protected void onProgressUpdate(Integer... values) 
	{
		if (values[0] < 0)
		{
			textView.setText(numberOfSegmentsUploaded  + " segments uploaded but failed to load the next segment");
		}
		else
		{
	    	uploadProgress.setProgress(values[0]);
	    	textView.setText(numberOfSegmentsUploaded  + " segments uploaded");
		}
	}
	
	public void uploadFilesFromLocation()
	{
		File[] files = new File(filesLocation).listFiles();
		numberOfSegments = files.length;
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost postRequest = new HttpPost(uploadUri);
		HttpPost endRequest = new HttpPost(processUri);
		
		String folderName = files[0].getName().substring(0, files[0].getName().indexOf("---1.mp4"));
		
		Log.i("DASH", "Entering the for loop");
		for (int i=numberOfSegmentsUploaded;i<=files.length;i++)
		{
			MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
			reqEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			if (i==files.length)
			{
				reqEntity.addTextBody("foldername", folderName);
				HttpEntity entity = reqEntity.build();
				endRequest.setEntity(entity);
			}
			else
			{
				String videoPath = files[i].getAbsolutePath();
	            FileBody filebodyVideo = new FileBody(new File(videoPath));
	            reqEntity.addPart("uploaded", filebodyVideo);
				HttpEntity entity = reqEntity.build();
				postRequest.setEntity(entity);
			}
			
			try
			{
				HttpResponse response;
				Log.i("DASH", "Executing the httpClient execute");
				if (i==files.length)
				{
					response = httpClient.execute(endRequest);
				}
				else
				{
					response = httpClient.execute(postRequest);
				}
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				String sResponse;
		        StringBuilder s = new StringBuilder();
		        
		        StatusLine a = response.getStatusLine();
				Log.i("DASH" , a.toString());
		        
		        while ((sResponse = reader.readLine()) != null) 
		        {
		        	s = s.append(sResponse);
		        }
		        Log.i("DASH", s.toString());
			}
			catch(Exception e)
			{
				Log.i("DASH" , "Exception raised while trying to upload the video"+ e.getMessage());
				publishProgress(-1);
				i--;
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		        continue;
			}
			
			numberOfSegmentsUploaded++;
			if (numberOfSegmentsUploaded<=numberOfSegments)
				publishProgress((numberOfSegmentsUploaded * 100) / numberOfSegments);
		}
	}

	@Override
	protected Void doInBackground(Void... params) 
	{
		Log.i("DASH" , "Inside doInBackground");
		
		uploadFilesFromLocation();
		return null;
	}
}
