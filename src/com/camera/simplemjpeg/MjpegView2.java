package com.camera.simplemjpeg;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MjpegView2 extends SurfaceView implements SurfaceHolder.Callback {
	static final String TAG = MjpegView2.class.getSimpleName();
	
	boolean available = false;
	private MjpegInputStream mIn = null;
	SurfaceHolder mHolder;
	
	Thread t;
	class WriteThread implements Runnable {
		SurfaceHolder mHolder;
		public WriteThread(SurfaceHolder h){
			mHolder = h;
		}
		public void run(){
	    	Log.v(TAG, "starting stream");
	    	while(true){
	    		Canvas c = null;
	    		try{
		    		Bitmap bmp = mIn.readMjpegFrame();
		    		Log.v(TAG, "Image: w:"+bmp.getWidth()+" h:"+bmp.getHeight()+" first: "+String.format("#%06X", (0xFFFFFF & bmp.getPixel(0, 0))));
//		    		Log.v(TAG, diagonal(bmp));
//	    			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.raw.a); //testing
		    		c = mHolder.lockCanvas();
		    		if(c==null) throw new IOException();
		    		c.drawBitmap(bmp, new Rect(0,0,bmp.getWidth(),bmp.getHeight()), new Rect(0,0,bmp.getWidth(),bmp.getHeight()), null);
	    		}catch(IOException e){
	    			available = false;
	    			Log.v(TAG, "stream end");
	    		}finally{
	    			if(c != null) mHolder.unlockCanvasAndPost(c);
	    			if(available == false) return;
	    		}
	    	}
		}
	}
	
	static String diagonal(Bitmap b){
		String result = "";
		int m = Math.min(b.getWidth()-1,b.getHeight()-1);
		for(int i =0; i<m; i++){
			result+= "\n\t"+String.format("#%06X", (0xFFFFFF & b.getPixel(i, i)));
		}
		return result;
	}
	

    public MjpegView2(Context context, AttributeSet attrs) { 
        super(context, attrs);
        init();
    }
    
    public MjpegView2 setMjpegInputStream(MjpegInputStream mIn){
    	this.mIn = mIn;
    	startPlayback();
    	return this; //chainable
    }
    
    private void init(){
    	mHolder = getHolder();
    	mHolder.addCallback(this);
    }
    
			
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(TAG, "surface destroyed");
		available = false;
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(TAG, "surface created");
		available = true;
		
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.v(TAG, "surface changed. size: w:"+width+" h:"+height);
		if(holder==null){
			available = true;
		}
    }
    
    
    public void setResolution(int width, int height){
    }
    
    public boolean isStreaming(){
    	return available; //todo
    }
    
    public void startPlayback(){
    	if(t==null){
    		t = new Thread(new WriteThread(mHolder));
    	}
    	t.start();
    }
    
    public void stopPlayback(){
    	t.interrupt();
    	t=null;
    }
}
