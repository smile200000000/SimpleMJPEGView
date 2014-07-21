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
    private boolean showFps = false;
    
    private boolean mRun = false;
    private boolean surfaceDone = false;    
    private Paint overlayPaint = new Paint();
    private int overlayTextColor = Color.YELLOW;
    private int overlayBackgroundColor = Color.WHITE;
    private int overlayBackgroundAlpha = 64;
    private float overlayScale = 1.5f;
    private float overlayFontSize = 48f;
    
    public static final int SIZE_CENTER = 1;
    public static final int SIZE_BEST_FIT = 4;
    public static final int SIZE_FULLSCREEN = 8;

    private int width;
    private int height;
    private int displayMode = SIZE_CENTER;
    
    

	private boolean suspending = false;
	
	public class FPSThread extends Thread {
        int frameCounter = 0;
        long start;
        float fps = 0f;
        final int PERIOD = 1000; //1 second
        boolean running = false;
        Bitmap overlay;
        Paint p = new Paint();
        
        public void run(){
        	if(!showFps) return;
        	p.setTextSize(overlayFontSize);
        	running = true;
        	try{
	        	while(true){
	        		int currentFrames = frameCounter;
	        		start = System.currentTimeMillis();
	        		Thread.sleep(PERIOD);
	        		fps = 1000f* (float)(frameCounter - currentFrames)/(System.currentTimeMillis()-start);
	        		overlay = makeFpsOverlay(); //this way, it only draws the frame on measure
	        	}
        	}catch(Exception e){
        		running = false;
        		return;
        	}
        }
        
        public void incrementFrame(){
        	if(!running) this.start();
        	frameCounter++;
        }
        public Bitmap getFps(){
        	return overlay;
        }
        
        private Bitmap makeFpsOverlay() {
            Rect b = new Rect();
            String fpsString = String.format("%.1f", fps);
            p.getTextBounds(fpsString, 0, fpsString.length(), b);
            Bitmap bm = Bitmap.createBitmap(b.width(), b.height(), Bitmap.Config.ARGB_8888);
            if(bm ==null) return null;

            Canvas c = new Canvas(bm);
            p.setColor(overlayBackgroundColor);
            p.setAlpha(overlayBackgroundAlpha);
            c.drawRect(0, 0, b.width(), b.height(), p);
            p.setColor(overlayTextColor);
            
            c.drawText(fpsString, -b.left, b.bottom-b.top, p);
            return bm;        	 
        }
	}
	private FPSThread fpsThread = new FPSThread();

    public class MjpegViewThread extends Thread {
        private SurfaceHolder mHolder;

         
        public MjpegViewThread(SurfaceHolder surfaceHolder) { 
        	mHolder = surfaceHolder;
        }
        
        

        public void run() {
            PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER); //Overlay DST over SRC
            Paint p = new Paint();

	    	while(mRun){
	    		Canvas c = null;
	    		try{
		    		Bitmap bmp = mIn.readMjpegFrame();
//		    		Log.v(TAG, "Image: w:"+bmp.getWidth()+" h:"+bmp.getHeight()+" first: "+String.format("#%06X", (0xFFFFFF & bmp.getPixel(0, 0))));
//    		    		Log.v(TAG, diagonal(bmp));
//    	    			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.raw.a); //testing
		    		c = mHolder.lockCanvas();
		    		if(c==null) throw new IOException();
		    		Rect outputRect = outputRect(displayMode, bmp.getWidth(), bmp.getHeight());
//		    		Log.v("z", "outrect: l:"+outputRect.left+" t:"+outputRect.top+" w:"+outputRect.width()+" h:"+outputRect.height());
		    		c.drawBitmap(bmp, null, outputRect, null);
		    		
		    		if(showFps) {
		    			fpsThread.incrementFrame();
                        p.setXfermode(mode); //overlay
                        Bitmap ov = fpsThread.getFps();
                        if(ov!=null){
                        	Rect fpsRect = new Rect(outputRect.left, outputRect.top, (int)(outputRect.left + overlayScale*ov.getWidth()), (int)(outputRect.top + overlayScale*ov.getHeight()));
//                        	Log.v("z", "fpsrect: l:"+fpsRect.left+" t:"+fpsRect.top+" w:"+fpsRect.width()+" h:"+fpsRect.height());
                        	c.drawBitmap(ov, null, fpsRect, null);
                        }
                        p.setXfermode(null); //return to normal mode
                    }
		    		
	    		}catch(IOException e){
	    			mRun = false;
	    			if(showFps) fpsThread.interrupt();
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
	public void showFps(boolean show){
		showFps = show;
	}

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
