package com.camera.simplemjpeg;

import java.io.IOException;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MjpegView extends SurfaceView implements SurfaceHolder.Callback {
	
    
    SurfaceHolder holder;
    
    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;    
//    private boolean showFps = false;
    private boolean mRun = false;
    private boolean surfaceDone = false;    

//    private Paint overlayPaint;
//    private int overlayTextColor;
//    private int overlayBackgroundColor;
//    private int ovlPos;
//    private int dispWidth;
//    private int dispHeight;
//    private int displayMode;

	private boolean suspending = false;

    public class MjpegViewThread extends Thread {
        private SurfaceHolder mHolder;
//        private int frameCounter = 0;
//        private long start;
//        private String fps = "";

         
        public MjpegViewThread(SurfaceHolder surfaceHolder) { 
        	mHolder = surfaceHolder; 
        }

//        private Rect destRect(int bmw, int bmh) {
//            int tempx;
//            int tempy;
//            if (displayMode == MjpegView.SIZE_STANDARD) {
//                tempx = (dispWidth / 2) - (bmw / 2);
//                tempy = (dispHeight / 2) - (bmh / 2);
//                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
//            }
//            if (displayMode == MjpegView.SIZE_BEST_FIT) {
//                float bmasp = (float) bmw / (float) bmh;
//                bmw = dispWidth;
//                bmh = (int) (dispWidth / bmasp);
//                if (bmh > dispHeight) {
//                    bmh = dispHeight;
//                    bmw = (int) (dispHeight * bmasp);
//                }
//                tempx = (dispWidth / 2) - (bmw / 2);
//                tempy = (dispHeight / 2) - (bmh / 2);
//                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
//            }
//            if (displayMode == MjpegView.SIZE_FULLSCREEN)
//                return new Rect(0, 0, dispWidth, dispHeight);
//            return null;
//        }
         
//        public void setSurfaceSize(int width, int height) {
//            synchronized(mHolder) {
//                dispWidth = width;
//                dispHeight = height;
//            }
//        }
         
//        private Bitmap makeFpsOverlay(Paint p) {
//            Rect b = new Rect();
//            p.getTextBounds(fps, 0, fps.length(), b);
//
//            // false indentation to fix forum layout             
//            Bitmap bm = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ARGB_8888);
//
//            Canvas c = new Canvas(bm);
//            p.setColor(overlayBackgroundColor);
//            c.drawRect(0, 0, b.width(), b.height(), p);
//            p.setColor(overlayTextColor);
//            c.drawText(fps, -b.left, b.bottom-b.top-p.descent(), p);
//            return bm;        	 
//        }

        public void run() {
//            start = System.currentTimeMillis();
//            PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);

//            int width;
//            int height;
//            Paint p = new Paint();
//            Bitmap ovl=null;

	    	while(mRun){
	    		Canvas c = null;
	    		try{
		    		Bitmap bmp = mIn.readMjpegFrame();
//		    		Log.v(TAG, "Image: w:"+bmp.getWidth()+" h:"+bmp.getHeight()+" first: "+String.format("#%06X", (0xFFFFFF & bmp.getPixel(0, 0))));
//    		    		Log.v(TAG, diagonal(bmp));
//    	    			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.raw.a); //testing
		    		c = mHolder.lockCanvas();
		    		if(c==null) throw new IOException();
		    		c.drawBitmap(bmp, new Rect(0,0,bmp.getWidth(),bmp.getHeight()), new Rect(0,0,bmp.getWidth(),bmp.getHeight()), null);
	    		}catch(IOException e){
	    			mRun = false;
	    		}finally{
	    			if(c != null) mHolder.unlockCanvasAndPost(c);
	    		}
    		}
        }
    }

    private void init(Context context) {
    	
    	holder = getHolder();
        holder.addCallback(this);
        thread = new MjpegViewThread(holder);
        setFocusable(true);
//        overlayPaint = new Paint();
//        overlayPaint.setTextAlign(Paint.Align.LEFT);
//        overlayPaint.setTextSize(12);
//        overlayPaint.setTypeface(Typeface.DEFAULT);
//        overlayTextColor = Color.WHITE;
//        overlayBackgroundColor = Color.BLACK;
//        ovlPos = MjpegView.POSITION_LOWER_RIGHT;
//        displayMode = MjpegView.SIZE_STANDARD;
//        dispWidth = getWidth();
//        dispHeight = getHeight();
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
	            } catch (InterruptedException e) {}
	        }
	        thread = null;
        }
        if(mIn!=null){
	        try{
	        	mIn.close();
	        }catch(IOException e){}
	        mIn = null;
        }

    }

//    public void freeCameraMemory(){
//    	if(mIn!=null){
//    		mIn.freeCameraMemory();
//    	}
//    }
    
    public MjpegView(Context context, AttributeSet attrs) { 
        super(context, attrs); init(context); 
    }

    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
//    	if(thread!=null){
//    		thread.setSurfaceSize(w, h); 
//    	}
    }

    public void surfaceDestroyed(SurfaceHolder holder) { 
        surfaceDone = false; 
        stopPlayback(); 
    }
    
    public MjpegView(Context context) { super(context); init(context); }    
    public void surfaceCreated(SurfaceHolder holder) { surfaceDone = true; }
//    public void showFps(boolean b) { showFps = b; }
    public void setSource(MjpegInputStream source) {
    	mIn = source; 
    	if(!suspending){
    		startPlayback();
    	}else{
    		resumePlayback();
    	}
    }
//    public void setOverlayPaint(Paint p) { overlayPaint = p; }
//    public void setOverlayTextColor(int c) { overlayTextColor = c; }
//    public void setOverlayBackgroundColor(int c) { overlayBackgroundColor = c; }
//    public void setOverlayPosition(int p) { ovlPos = p; }
//    public void setDisplayMode(int s) { displayMode = s; }
    
//    public void setResolution(int w, int h){
//    	IMG_WIDTH = w;
//    	IMG_HEIGHT = h;
//    }
    
	public boolean isStreaming(){
		return mRun;
	}
	
	
	//debugging
	static String diagonal(Bitmap b){
		String result = "";
		int m = Math.min(b.getWidth()-1,b.getHeight()-1);
		for(int i =0; i<m; i++){
			result+= "\n\t"+String.format("#%06X", (0xFFFFFF & b.getPixel(i, i)));
		}
		return result;
	}
}
