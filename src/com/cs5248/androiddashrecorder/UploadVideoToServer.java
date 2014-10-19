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
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.StatusLine;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.mime.HttpMultipartMode;
//import org.apache.http.entity.mime.MultipartEntityBuilder;
//import org.apache.http.entity.mime.content.FileBody;
//import org.apache.http.impl.client.HttpClientBuilder;

import android.os.AsyncTask;
import android.util.Log;


public class UploadVideoToServer extends AsyncTask<String, String, Void> 
{
	public void uploadFilesFromLocation(String filesLocation, String serverUri)
	{
		File[] files = new File(filesLocation).listFiles();
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost postRequest = new HttpPost(serverUri);
		
		Log.i("DASH", "Entering the for loop");
		for (int i=0;i<files.length;i++)
		{
			
			String videoPath = files[i].getAbsolutePath();
			MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
			reqEntity.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            FileBody filebodyVideo = new FileBody(new File(videoPath));
            reqEntity.addPart("uploaded", filebodyVideo);
			
			HttpEntity entity = reqEntity.build();
			
			postRequest.setEntity(entity);
			try
			{
				Log.i("DASH", "Executing the httpClient execute");
				HttpResponse response = httpClient.execute(postRequest);
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				String sResponse;
		        StringBuilder s = new StringBuilder();
		        
		        StatusLine a = response.getStatusLine();
				Log.i("DASH" , a.toString());
//		        System.out.println(a.toString());
	
		        
		        while ((sResponse = reader.readLine()) != null) 
		        {
		        	s = s.append(sResponse);
		        }
	//	        System.out.println(s.toString());
		        Log.i("DASH", s.toString());
			}
			catch(Exception e)
			{
				Log.i("DASH" , "Damn "+ e.getMessage());
			}
		}		
	}

	@Override
	protected Void doInBackground(String... params) 
	{
		Log.i("DASH" , "Inside doInBackground");
		
		if (params.length != 2)
			throw new IllegalArgumentException("Two parameters needed - the path to the folder having segment videos" +
					", the url for upload");
		
		String folderLocation = params[0];
		String serverUri = params[1];
		uploadFilesFromLocation(folderLocation, serverUri);
		return null;
	}
}
