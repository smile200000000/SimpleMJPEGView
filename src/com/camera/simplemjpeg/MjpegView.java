package com.camera.simplemjpeg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * ******************************************** NOTICE ********************************************
 * This package is a fork of a library by neuralassembly (https://github.com/neuralassembly).
 * The copyright is his. The original branch: https://bitbucket.org/neuralassembly/simplemjpegview
 *
 * I have done some significant refactoring from this. In particular, I have removed the reliance
 * on NDK code (which required agreeing to Intel's licence on OpenCV) and replaced it will all
 * core image functions. My fork is here: https://github.com/TechJect/SimpleMJPEGView
 *
 * It's not clear what license neuralassembly's non-OpenCV code is, but this constitutes a
 * derived work and shares that license.
 * ************************************************************************************************
 */
public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder holder;
    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;

    private boolean suspending = false;
    private boolean mRun = false;
    private boolean surfaceDone = false;

    private boolean showFps = false;

    private FPSTracker fpsThread = null;

    public static final int SIZE_CENTER = 1;
    public static final int SIZE_BEST_FIT = 4;
    public static final int SIZE_FULLSCREEN = 8;

    private int width;
    private int height;
    private int displayMode = SIZE_CENTER;

    Rect outputRect = null;
    public class MjpegViewThread extends Thread {
        private SurfaceHolder mHolder;

        public MjpegViewThread(SurfaceHolder surfaceHolder) {
            mHolder = surfaceHolder;
        }

        public void run() {
            while(mRun){
                Canvas c = null;
                try{
                    mIn.readMjpegFrame();
                    c = mHolder.lockCanvas();
//                    Rect outputRect = outputRect(displayMode, mIn.tempBmp.getWidth(), mIn.tempBmp.getHeight());
                    if(outputRect==null) outputRect = outputRect(displayMode, mIn.tempBmp.getWidth(), mIn.tempBmp.getHeight()); //Should be the same size every time
                    c.drawBitmap(mIn.tempBmp, null, outputRect, null);
                }catch(Exception e){
                    mRun = false;
                    if(showFps) fpsThread.interrupt();
                }finally{
                    if(c != null) mHolder.unlockCanvasAndPost(c);
                }
                if(fpsThread!=null) fpsThread.calculateAndSend();
            }
        }
    }

    public void setFpsCallback(FPSTracker.FPSCallback callback){
        fpsThread = new FPSTracker(callback);
    }

    private void init() {

        holder = getHolder();
        holder.addCallback(this);
        thread = new MjpegViewThread(holder);
        setFocusable(true);
        width = getWidth();
        height = getHeight();
    }

    public void startPlayback() {
        if(mIn != null) {
            mRun = true;
            if(thread==null){
                thread = new MjpegViewThread(holder);
            }
            thread.start();
        }
    }

    public void resumePlayback() {
        if(suspending){
            if(mIn != null) {
                mRun = true;
                SurfaceHolder holder = getHolder();
                holder.addCallback(this);
                thread = new MjpegViewThread(holder);
                thread.start();
                suspending=false;
            }
        }
    }
    public void stopPlayback() {
        if(mRun){
            suspending = true;
        }
        mRun = false;
        if(thread!=null){
            boolean retry = true;
            while(retry) {
                try {
                    thread.join();
                    retry = false;
                } catch (InterruptedException e) {e.printStackTrace();}
            }
            thread = null;
        }
        if(mIn!=null){
            try{
                mIn.close();
            }catch(IOException e){e.printStackTrace();}
            mIn = null;
        }

    }

    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
        setResolution(w, h);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceDone = false;
        stopPlayback();
    }

    public MjpegView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public MjpegView(Context context) {
        super(context);
        init();
    }
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceDone = true;
    }
    public void showFps(boolean b) {
        showFps = b;
    }

    public void setSource(MjpegInputStream source) {
        mIn = source;
        if(!suspending){
            startPlayback();
        }else{
            resumePlayback();
        }
    }

    public boolean isStreaming(){ return mRun; }
    public void setDisplayMode(int mode){ displayMode = mode; }

    /*
     * The actual surface is as big as the View (based on the layout and screen size).
     * These dimensions can be captured with getWidth() and getHeight() (these are marked as final).
     * If we want to alter the area we actually use for drawing, use these methods.
     * By default, we use the whole surface.
     */
    public void setResolution(int width, int height){
        this.width = Math.min(width, getWidth());
        this.height = Math.min(height, getHeight());
    }

    /*
     * For scaling the output image
     */
    Rect outputRect(int displayMode, int bitmapWidth, int bitmapHeight){
        int tempx;
        int tempy;
        switch(displayMode){
            case SIZE_CENTER:
                tempx = (width / 2) - (bitmapWidth / 2);
                tempy = (height / 2) - (bitmapHeight / 2);
                return new Rect(tempx, tempy, bitmapWidth + tempx, bitmapHeight + tempy);

            case SIZE_BEST_FIT:
                float bmasp = (float) bitmapWidth / (float) bitmapHeight;
                bitmapWidth = width;
                bitmapHeight = (int) (width / bmasp);
                if (bitmapHeight > height) {
                    bitmapHeight = height;
                    bitmapWidth = (int) (height * bmasp);
                }
                tempx = (width / 2) - (bitmapWidth / 2);
                tempy = (height / 2) - (bitmapHeight / 2);
                return new Rect(tempx, tempy, bitmapWidth + tempx, bitmapHeight + tempy);

            case SIZE_FULLSCREEN:
                return new Rect(0, 0, width, height);
            default:
                return null;
        }
    }
}
