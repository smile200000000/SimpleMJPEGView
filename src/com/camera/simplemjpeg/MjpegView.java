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
    
    private SurfaceHolder holder;
    private MjpegViewThread thread;
    private MjpegInputStream mIn = null;    
//    private boolean showFps = false;
    
    private boolean mRun = false;
    private boolean surfaceDone = false;    

//    private Paint overlayPaint;
//    private int overlayTextColor;
//    private int overlayBackgroundColor;
//    private int ovlPos;
    
    public static final int SIZE_CENTER = 1;
    public static final int SIZE_BEST_FIT = 4;
    public static final int SIZE_FULLSCREEN = 8;

    private int width = -1;
    private int height = -1;
    private int displayMode = SIZE_CENTER;
    
    

	private boolean suspending = false;

    public class MjpegViewThread extends Thread {
        private SurfaceHolder mHolder;
//        private int frameCounter = 0;
//        private long start;
//        private String fps = "";

         
        public MjpegViewThread(SurfaceHolder surfaceHolder) { 
        	mHolder = surfaceHolder;
        }
        
         
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
		    		c.drawBitmap(bmp, null, outputRect(displayMode, bmp.getWidth(), bmp.getHeight()), null);
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
    
    public MjpegView(Context context, AttributeSet attrs) { 
        super(context, attrs); init(context); 
    }

    public void surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
    	setResolution(w, h); //TODO
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
    
    
	public boolean isStreaming(){
		return mRun;
	}	
	public void setDisplayMode(int mode){
		displayMode = mode;
	}

	/*
	 * The actual surface is as big as the View (based on the layout and screen size).
	 * These dimensions can be captured with getWidth() and getHeight() (these are marked as final).
	 * If we want to alter the area we actually use for drawing, use these methods.
	 * By default, we use the whole surface.
	 */
	public int getDrawableWidth(){
		if(width==-1) width = super.getWidth();
		return width;
	}
	public int getDrawableHeight(){
		if(height==-1) height = super.getHeight();
		return height;
	}
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
        int dispWidth = getDrawableWidth();
        int dispHeight = getDrawableHeight();
        switch(displayMode){
	          case SIZE_CENTER:
	              tempx = (dispWidth / 2) - (bitmapWidth / 2);
	              tempy = (dispHeight / 2) - (bitmapHeight / 2);
	              return new Rect(tempx, tempy, bitmapWidth + tempx, bitmapHeight + tempy);

	          case SIZE_BEST_FIT:
	              float bmasp = (float) bitmapWidth / (float) bitmapHeight;
	              bitmapWidth = dispWidth;
	              bitmapHeight = (int) (dispWidth / bmasp);
	              if (bitmapHeight > dispHeight) {
	            	  bitmapHeight = dispHeight;
	                  bitmapWidth = (int) (dispHeight * bmasp);
	              }
	              tempx = (dispWidth / 2) - (bitmapWidth / 2);
	              tempy = (dispHeight / 2) - (bitmapHeight / 2);
	              return new Rect(tempx, tempy, bitmapWidth + tempx, bitmapHeight + tempy);
	
	          case SIZE_FULLSCREEN:
	              return new Rect(0, 0, dispWidth, dispHeight);
	          default:
	        	  return null;
        }
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
