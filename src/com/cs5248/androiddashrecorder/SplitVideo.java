package com.cs5248.androiddashrecorder;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Splits a video into multiple clips
 * This code is an adaption of ShortenExample.java
 * from examples of MP4Parser. 
 */
public class SplitVideo extends AsyncTask<String, Double, Integer> {
	
	private String videoPath;
	private String outputPath;
	private String filename;
	private boolean set;
	private double videoTime;
	private double percentage;
	private int segmentNumber;
	
	private  ProgressBar segmentProgress;
    private  TextView textview;
    
    public SplitVideo(ProgressBar segmentProgress, TextView textView) {
    	this.segmentProgress = segmentProgress;
    	this.textview = textView;
    }
    
    @Override
    protected void onPreExecute() {
    	segmentProgress.setMax(100);
    }
    
    /**
     * Splits a video into multiple clips of specified duration of seconds
     * 
     * @param path Path of the video to be segmented
     * @param destinationPath Path where the final segments have to be stored
     * @param splitDuration Duration of each clip into which we have to cut
     * @return Number of segments created in splitting of video
     */
    public int split(String path, String destinationPath, double splitDuration) {
    	double startTime = 0.00;
    	segmentNumber = 1;
    	
    	videoTime = 0.0;
    	set = false;
    	videoPath = path;
    	outputPath = destinationPath;	
    	filename = new File(videoPath).getName().replace(".mp4", "");

        long start1 = System.currentTimeMillis();
    	try {
    		while (performSplit(startTime, startTime + splitDuration, segmentNumber)) {
        		segmentNumber++;
        		startTime += splitDuration;
        	}
    	} catch (FileNotFoundException e) {
    		e.printStackTrace();
    	} catch (IOException e) {
    		e.printStackTrace();
		}
        long start2 = System.currentTimeMillis();
        Log.i("DASH", "Total time taken to create " + Integer.toString( segmentNumber - 1) + 
        		" segments: " + Long.toString( start2 - start1) + "ms" );
    	
    	return segmentNumber - 1;
    }

    /**
     * Convenience method which is called by split(double splitDuration) to perform 
     * the splitting of video.
     * 
     * @param startTime Start time of the new segment video
     * @param endTime End time of the new segment video
     * @param segmentNumber Segment number of the video. Used in naming for the segment
     * @return true if segment is created else false is returned
     * @throws IOException
     * @throws FileNotFoundException
     */
	private boolean performSplit(double startTime, double endTime, int segmentNumber) throws IOException, FileNotFoundException {
        Movie movie = MovieCreator.build(videoPath);
        Log.i("DASH", "Movie Time:" +Long.toString(movie.getTimescale()));
        List<Track> tracks = movie.getTracks();
        movie.setTracks(new LinkedList<Track>());
        // remove all tracks we will create new tracks from the old

        boolean timeCorrected = false;

        // Here we try to find a track that has sync samples. Since we can only start decoding
        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
        // such a frame
        for (Track track : tracks) {
            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                if (timeCorrected) {
                    // This exception here could be a false positive in case we have multiple tracks
                    // with sync samples at exactly the same positions. E.g. a single movie containing
                    // multiple qualities of the same video (Microsoft Smooth Streaming file)

                    throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
                }
                startTime = correctTimeToSyncSample(track, startTime, true);
                endTime = correctTimeToSyncSample(track, endTime, true);
                timeCorrected = true;
            	if(!set) {
            		videoTime = correctTimeToSyncSample(track, 10000, true);
            		set = true;
            		Log.i("DASH", "Video total time =" + videoTime);
            	}
            }
        }
        
        percentage = (startTime * 100) / videoTime;
        publishProgress(percentage);
        
        if (startTime == endTime) 
        	return false;
        
        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            double lastTime = 0;
            long startSample1 = 0;
            long endSample1 = -1;

            for (int i = 0; i < track.getSampleDurations().length; i++) {
                long delta = track.getSampleDurations()[i];


                if (currentTime > lastTime && currentTime <= startTime) {
                    // current sample is still before the new starttime
                    startSample1 = currentSample;
                }
                if (currentTime > lastTime && currentTime <= endTime) {
                    // current sample is after the new start time and still before the new endtime
                    endSample1 = currentSample;
                }

                lastTime = currentTime;
                currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
                currentSample++;
            }
            Log.i("DASH", "Start time = " + startTime + ", End time = " + endTime);
            movie.addTrack(new CroppedTrack(track, startSample1, endSample1));
        }
        long start1 = System.currentTimeMillis();
        Container out = new DefaultMp4Builder().build(movie);
        long start2 = System.currentTimeMillis();
        FileOutputStream fos = new FileOutputStream(outputPath + String.format("%s---%d.mp4", filename  , segmentNumber));
        FileChannel fc = fos.getChannel();
        out.writeContainer(fc);

        fc.close();
        fos.close();
        long start3 = System.currentTimeMillis();
        Log.i("DASH", "Building IsoFile took : " + (start2 - start1) + "ms");
        Log.i("DASH", "Writing IsoFile took  : " + (start3 - start2) + "ms");
        return true;
	}
	
	/**
	 * Convenience method which gives the nearest next or previous time
	 * where the segmentation of the video can be performed.
	 * 
	 * @param track Track which needs to be scanned to find the next sync sample
	 * @param cutHere Time at which the segmentation needs to be done
	 * @param next boolean false if want the cutTime less than cutHere else true
	 * @return nearest cutTime where the segmentation could be done
	 */
    private double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample >= cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }
    
    @Override
    protected void onProgressUpdate(Double... values) { 	
    	segmentProgress.setProgress(values[0].intValue());
    	//Updates the number of segments done below Progress Bar on UI
    	textview.setText(segmentNumber - 1 + " segments created");
    }

	@Override
	protected Integer doInBackground(String... params) {
		
		Log.i("DASH" , "Inside doInBackground");
		
		if (params.length != 3)
			throw new IllegalArgumentException("Three parameters needed - src" +
					" video path, dest path, split-duration");
		
		String path = params[0];
		String destPath = params[1];
		double splitDuration = Double.parseDouble(params[2]);
		return Integer.valueOf(this.split(path, destPath, splitDuration));
	}


}
