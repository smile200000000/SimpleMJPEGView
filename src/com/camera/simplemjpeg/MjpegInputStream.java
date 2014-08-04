package com.camera.simplemjpeg;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


import java.util.Properties;

import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

/**
 * ******************************************** NOTICE ********************************************
 * This package is a fork of a library by neuralassembly (https://github.com/neuralassembly).
 * The copyright is his. The original branch: https://bitbucket.org/neuralassembly/simplemjpegview
 *
 * I have done some significant refactoring from this. In particular, I have removed the reliance
 * on NDK code (which required agreeing to Intel's license on OpenCV) and replaced it will all
 * core image functions. My fork is here: https://github.com/TechJect/SimpleMJPEGView
 *
 * It's not clear what license neuralassembly's non-OpenCV code is, but this constitutes a
 * derived work and shares that license.
 * ************************************************************************************************
 */

public class MjpegInputStream extends DataInputStream {
	private static final String TAG = MjpegInputStream.class.getSimpleName();
	private static final boolean DEBUG = false;
	
    private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
    private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
    private final String CONTENT_LENGTH = "Content-Length";
    private final String X_TIMESTAMP = "X-Timestamp";
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int FRAME_MAX_LENGTH = 200000;
    public static final int IMAGE_ORIENTATION = 0;//-90;
    private static final int MAX_IMAGE_WIDTH = 1280;
    private static final int MAX_IMAGE_HEIGHT = 720;

    int mContentLength = -1;
    byte[] header = null;
    byte[] frameData = null;
    int headerLen = -1;
    int headerLenPrev = -1;
    int skip = 1;
    int count = 0;

    //Preallocate Bitmap so it doesn't have to be created at every frame
    Bitmap tempBmp = Bitmap.createBitmap(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
    BitmapFactory.Options bmfOptions;
    Matrix rotate;


    public MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
        if(IMAGE_ORIENTATION != 0){
            rotate = new Matrix();
            rotate.postRotate(IMAGE_ORIENTATION);
        }

        //allow BitmapFactory to return mutable bitmaps, so we can keep a single allocation
        bmfOptions = new BitmapFactory.Options();
//        bmfOptions.inMutable = true;
        bmfOptions.inBitmap = tempBmp;
    }

    private int getEndOfSeqeunce(DataInputStream in, byte[] sequence) throws IOException {
        int seqIndex = 0;
        byte c;
        for(int i=0; i < FRAME_MAX_LENGTH; i++) {
            c = (byte) in.readUnsignedByte();
            if(c == sequence[seqIndex]) {
                seqIndex++;
                if(seqIndex == sequence.length){

                    return i + 1;
                }
            } else seqIndex = 0;
        }

        return -1;
    }

    private int getStartOfSequence(DataInputStream in, byte[] sequence) throws IOException {
        int end = getEndOfSeqeunce(in, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    private int getEndOfSeqeunceSimplified(DataInputStream in, byte[] sequence) throws IOException {
        int startPos = mContentLength/2;
        int endPos = 3*mContentLength/2;

        skipBytes(headerLen+startPos);

        int seqIndex = 0;
        byte c;
        for(int i=0; i < endPos-startPos ; i++) {
            c = (byte) in.readUnsignedByte();
            if(c == sequence[seqIndex]) {
                seqIndex++;
                if(seqIndex == sequence.length){

                    return headerLen + startPos + i + 1;
                }
            } else seqIndex = 0;
        }

        return -1;
    }

    private int parseContentLength(byte[] headerBytes)
            throws IOException, NumberFormatException, IllegalArgumentException {
        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
    }

    private long parseXTimestamp(byte[] headerBytes)
            throws IOException, NumberFormatException, IllegalArgumentException {
        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        long timeSent = Long.parseLong(props.getProperty(X_TIMESTAMP));
        return System.currentTimeMillis() - timeSent;
    }


    /*
        For performance reasons, try not to do any allocations here
     */
    public void readMjpegFrame() throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLen;
        try{
            headerLen = getStartOfSequence(this, SOI_MARKER);
        }catch(IOException e){
            if(DEBUG) Log.d(TAG, "IOException in betting headerLen.");
            reset();
            return;
        }
        reset();

        if(header==null || headerLen != headerLenPrev){
            header = new byte[headerLen];
            if(DEBUG) Log.d(TAG,"header renewed "+headerLenPrev+" -> "+headerLen);
        }
        headerLenPrev = headerLen;
        readFully(header);

//        long latency = -1;
//        try{
//            latency = parseXTimestamp(header);
//            Log.v(TAG, "Latency: "+latency); //TODO synchronize as to make this meaningful
//        }catch (Exception e){
//            //oh well
//        }

        int contentLengthNew;
        try {
            contentLengthNew = parseContentLength(header);
        } catch (NumberFormatException nfe) {
            contentLengthNew = getEndOfSeqeunceSimplified(this, EOF_MARKER);

            if(contentLengthNew < 0){
                if(DEBUG) Log.d(TAG,"Worst case for finding EOF_MARKER");
                reset();
                contentLengthNew = getEndOfSeqeunce(this, EOF_MARKER);
            }
        }catch (IllegalArgumentException e) {
            if(DEBUG) Log.d(TAG,"IllegalArgumentException in parseContentLength");
            contentLengthNew = getEndOfSeqeunceSimplified(this, EOF_MARKER);

            if(contentLengthNew < 0){
                if(DEBUG) Log.d(TAG,"Worst case for finding EOF_MARKER");
                reset();
                contentLengthNew = getEndOfSeqeunce(this, EOF_MARKER);
            }
        }catch (IOException e) {
            if(DEBUG) Log.d(TAG,"IOException in parseContentLength");
            reset();
            return;
        }
        mContentLength = contentLengthNew;
        reset();

        if(frameData==null){
            frameData = new byte[FRAME_MAX_LENGTH];
            if(DEBUG) Log.d(TAG,"frameData newed cl="+FRAME_MAX_LENGTH);
        }
        if(mContentLength + HEADER_MAX_LENGTH > FRAME_MAX_LENGTH){
            frameData = new byte[mContentLength + HEADER_MAX_LENGTH];
            if(DEBUG) Log.d(TAG,"frameData renewed cl="+(mContentLength + HEADER_MAX_LENGTH));
        }

        skipBytes(headerLen);

        readFully(frameData, 0, mContentLength);

        if(count++%skip==0){
            try{
                tempBmp = BitmapFactory.decodeByteArray(frameData, 0, mContentLength, bmfOptions);
                if(IMAGE_ORIENTATION != 0){ //need to rotate the output
                    tempBmp = Bitmap.createBitmap(tempBmp,0,0,tempBmp.getWidth(),tempBmp.getHeight(),rotate, true); //TODO Problem: returned rotated bitmap is immutable
                }
        		if(DEBUG) Log.d(TAG, "w:"+tempBmp.getWidth()+"h:"+tempBmp.getHeight()
                        +"first pix color: "+String.format("#%06X", (0xFFFFFF & tempBmp.getPixel(0, 0)))
                );
            }catch(Exception e){
                Log.e(TAG, "Problem constructing bitmap", e);
            }
        }
    }
    public void setSkip(int s){
        skip = s;
    }

    @Override
    public void close() throws IOException{
        if(tempBmp!=null) tempBmp.recycle();
        super.close();
    }
}
