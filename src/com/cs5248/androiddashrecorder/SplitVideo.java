package com.cs5248.androiddashrecorder;

import android.os.AsyncTask;
import android.util.Log;

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
public class SplitVideo extends AsyncTask<String, String, Integer> {
	
	private static String videoPath;
	private static String outputPath;
	private static String filename;
    
    /**
     * Splits a video into multiple clips of specified duration of seconds
     * 
     * @param path Path of the video to be segmented
     * @param destinationPath Path where the final segments have to be stored
     * @param splitDuration Duration of each clip into which we have to cut
     * @return Number of segments created in splitting of video
     */
    public static int split(String path, String destinationPath, double splitDuration) {
    	double startTime = 0.01;
    	int segmentNumber = 1;
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
        Log.d("DASH", "Total time taken to create " + Integer.toString( segmentNumber - 1) + 
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
	private static boolean performSplit(double startTime, double endTime, int segmentNumber) throws IOException, FileNotFoundException {
        Movie movie = MovieCreator.build(videoPath);

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
            }
        }
        
        if (startTime == endTime) 
        	return false;
        
        for (Track track : tracks) {
            long currentSample = 0;
            double currentTime = 0;
            double lastTime = 0;
            long startSample1 = -1;
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
        Log.d("DASH", "Building IsoFile took : " + (start2 - start1) + "ms");
        Log.d("DASH", "Writing IsoFile took  : " + (start3 - start2) + "ms");
        return true;
	}

    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
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
            if (timeOfSyncSample > cutHere) {
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
	protected Integer doInBackground(String... params) {
		
		Log.d("DASH" , "Inside doInBackground");
		
		if (params.length != 3)
			throw new IllegalArgumentException("Three parameters needed - src" +
					" video path, dest path, split-duration");
		
		String path = params[0];
		String destPath = params[1];
		double splitDuration = Double.parseDouble(params[2]);
		return Integer.valueOf(split(path, destPath, splitDuration));
	}


}
