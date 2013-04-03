package com.example.bodysensor_test;


import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;

public class StartScreen extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.startscreen);
        Thread timer = new Thread(){
			
			public void run(){
				
				try
				{
					MediaPlayer mp = MediaPlayer.create(getApplicationContext(), R.raw.spooky);
				    mp.start();
					sleep(7000);
					mp.stop();
				
				}
				
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				
				finally{
					Intent start_activity=new Intent("android.intent.action.SECOND");
					startActivity(start_activity);
					
				}
			
		
			
		
		
		}
		
		
	};
	
	timer.start();
		
	}
	
	
	
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();		
		finish();
	}
	
	
	
	
	
	}

	
	
	
	

