package com.camera.simplemjpeg;

/**
 *  This is a Thread which counts frames and returns a Bitmap of the framerate which can be
 *  overlaid on top of a canvas (e.g. video stream).
 *
 *  There are currently three ways this can be done. 1) The thread can simply request the current value
 *  for fps via getFps. 2) They could request a bitmap that can be drawn to a surface with getOverlay.
 *  3) They can give us a FPSReceiverActivity which has a method that can accept the value on the UI thread.
 */
public class FPSTracker extends Thread {
    int frameCounter = 0;
    long start;
    float fps = 0f;
    final int PERIOD = 1000; //1 second
    FPSCallback callback = null;
    boolean stopped = false;

    public static interface FPSCallback {
        public void onFPSReceive(float fps);
        public void onTrackingStopped();
    }

    public FPSTracker(FPSCallback callback){
        this.callback = callback;
    }

    public void run(){
        while(!Thread.interrupted()) {
            int currentFrames = frameCounter;
            start = System.currentTimeMillis();
            try {
                Thread.sleep(PERIOD);
            }catch(InterruptedException e){
                stopped = true;
                if(callback!=null) callback.onTrackingStopped();
                break;
            }
            fps = 1000f * (frameCounter - currentFrames) / (float) (System.currentTimeMillis() - start);
            if (callback != null) callback.onFPSReceive(fps);
        }
    }

    /*
        Start counting if we haven't already. The video stream will have to call this every time
        a thread is finished.
     */
    public void incrementFrame(){
        if(!isAlive() && !stopped) this.start();
        frameCounter++;
    }
}
