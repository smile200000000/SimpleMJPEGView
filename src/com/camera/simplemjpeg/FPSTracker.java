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
    boolean running = false;

    FPSCallback callback = null;


    public static interface FPSCallback {
        public void onFPSReceive(float fps);
    }


    public FPSTracker(FPSCallback callback){
        this.callback = callback;
    }

    public void run(){
        running = true;
        try{
            while(!Thread.interrupted()){
                int currentFrames = frameCounter;
                start = System.currentTimeMillis();
                Thread.sleep(PERIOD);
                fps = 1000f* (float)(frameCounter - currentFrames)/(System.currentTimeMillis()-start);
            }
        }catch(Exception e){
            running = false;
        }
    }

    /*
        Start counting if we haven't already. The video stream will have to call this every time
        a thread is finished.
     */
    public void incrementFrame(){
        if(!running) this.start();
        frameCounter++;
    }


    public void calculateAndSend(){
        incrementFrame();
        if(callback==null) return;
        callback.onFPSReceive(fps);
    }

    public float getFps(){ return fps; }

//    private Bitmap makeFpsOverlay() {
//        Rect b = new Rect();
//        String fpsString = String.format("%.1f", fps);
//        p.getTextBounds(fpsString, 0, fpsString.length(), b);
//        Bitmap bm = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ARGB_8888);
//        if(bm == null) return null;
//
//        Canvas c = new Canvas(bm);
//        c.drawRect(0, 0, b.width(), b.height(), p);
//        int backgroundColor = p.getColor();
//        p.setColor(fontColor);
//        c.drawText(fpsString, -b.left, b.bottom-b.top, p);
//        p.setColor(backgroundColor);
//        return bm;
//    }
}
