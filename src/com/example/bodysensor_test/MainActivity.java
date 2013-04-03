package com.example.bodysensor_test;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity 
{
	
    // Message types sent from the DeviceConnect Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the DeviceConnect Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";     
   private BluetoothAdapter btAdapter;		
	private final static String TAG = "MainActivity";	
	public static final int REQUEST_ENABLE_BT = 3;
	public static final int INTENT_SELECT_DEVICES = 0;
	public static final int INTENT_DISCOVERY = 1;
	public static final int INTENT_VIEW_DEVICES = 2;
	
	protected static final int START = 0;
	protected static final int STOP = 1;
	protected static final int SURVEY = 2;
	protected static final int STATE = 3;
	private static final int REQUEST_CONNECT_DEVICE = 4;
	private static final boolean D = true;	
	private TextView tvSetData;	
	private TextView tvTest;
	private ImageView ivPhoto;
	private Button btnStart;
	
	private int DATA_STATE;
	private static final int DATA_STATE_CALM = 1;
	private static final int DATA_STATE_FRIGHT = 2;
	private static final int DATA_STATE_NONE = 0;
	private int calmCount;
	
	private ArrayList<Float> calmData;
	private ArrayList<Float> frightData;
	private MediaPlayer mp;
	private MediaPlayer mp1;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        tvSetData=(TextView)findViewById(R.id.tvSetData);
        tvTest=(TextView)findViewById(R.id.tvTest);
        btnStart=(Button)findViewById(R.id.btnStart);
        ivPhoto=(ImageView)findViewById(R.id.ivPhoto);
        
        calmData = new ArrayList<Float>();
        frightData = new ArrayList<Float>();
        
        btnStart.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
        		calmData.clear();
        		frightData.clear();
        		calmCount = 0;
        		tvTest.setText("--");
				Timer timer = new Timer();
				Random rand = new Random();
				DATA_STATE = MainActivity.DATA_STATE_CALM;
				if(mp != null) mp.stop();
				mp = MediaPlayer.create(getApplicationContext(), R.raw.nyan_cat);
			    mp.start();
				timer.schedule(new CalmTimer(), 1000);
			}
        });
        
       
        
        if(btAdapter==null)
    	{
    		Toast.makeText(getApplicationContext(),"No Bluetooth Detected",Toast.LENGTH_LONG).show();
    		finish();
    		
    	}
    	
    	else
    	{
    		if(!btAdapter.isEnabled())
    		{
    			turnOnBt();
    		}
    		
    		
    		
    	}
         
        
    
     		
    }
    
    final Handler imageHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		switch(msg.what) {
    		case MainActivity.DATA_STATE_CALM:
    			DATA_STATE = MainActivity.DATA_STATE_CALM;
    	        Random rand = new Random();
    	        switch(rand.nextInt(4)) {
    	        case 0:
    	        	ivPhoto.setImageResource(R.drawable.nyan_cat);
    	        	break;
    	        case 1:
    	        	ivPhoto.setImageResource(R.drawable.kitten1);
    	        	break;
    	        case 2:
    	        	ivPhoto.setImageResource(R.drawable.kitten2);
    	        	break;
    	        case 3:
    	        	ivPhoto.setImageResource(R.drawable.kitten3);
    	        	break;
    	        default:
    	        	ivPhoto.setImageResource(R.drawable.nyan_cat);	
    	        }
    	        Timer timer = new Timer();
    	        if(calmCount < (rand.nextInt(3)+5)) {
    	        	timer.schedule(new CalmTimer(), 1000);
    	        } else {
    	        	timer.schedule(new FrightTimer(), 1000);
    	        }
    	        calmCount++;
    			break;
    			
    		case MainActivity.DATA_STATE_FRIGHT:
    			DATA_STATE = MainActivity.DATA_STATE_FRIGHT;
    			mp.stop();
    			mp = MediaPlayer.create(getApplicationContext(),R.raw.scream);
    			mp.start();
    			ivPhoto.setImageResource(R.drawable.scary_pic);
    			timer = new Timer();
    			timer.schedule(new FinalTimer(), 3000);
    			break;
    			
    		case MainActivity.DATA_STATE_NONE:
    			DATA_STATE = MainActivity.DATA_STATE_NONE;
    			mp.stop();
        		ivPhoto.setImageResource(R.drawable.ic_launcher);
        		float avgCalmData = 0;
        		float avgFrightData = 0;
        		Iterator<Float> it = calmData.iterator();
        		while(it.hasNext()) {
        			avgCalmData += it.next().floatValue();
        		}
        		avgCalmData = avgCalmData / calmData.size();
        		it = frightData.iterator();
        		while(it.hasNext()) {
        			avgFrightData += it.next().floatValue();
        		}
        		avgFrightData = avgFrightData / frightData.size();
        		float score = avgFrightData - avgCalmData;
        		tvTest.setText(Float.toString(score));
        		Log.i(TAG, "SCORE: "+score);
        		calmData.clear();
        		frightData.clear();
        		calmCount = 0;
        		break;
    		default:
    		}
    	}
    };
    
    private class CalmTimer extends TimerTask
    {
		@Override
		public void run() {
			imageHandler.sendEmptyMessage(MainActivity.DATA_STATE_CALM);
		}
    }
    
    private class FrightTimer extends TimerTask
    {
		@Override
		public void run() {
			imageHandler.sendEmptyMessage(MainActivity.DATA_STATE_FRIGHT);
		}
    }
    
    private class FinalTimer extends TimerTask {
    	@Override
    	public void run() {
    		imageHandler.sendEmptyMessage(MainActivity.DATA_STATE_NONE);
    	}
    }
    
    
    public boolean turnOnBt() {
		// TODO Auto-generated method stub
		Intent Enable_Bluetooth=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(Enable_Bluetooth, 1234);
		return true;
	}
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bs_menu, menu);
        return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item){
		
		if(item.getItemId() == R.id.Connect){
			if(btAdapter.isEnabled())
			{			
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
            }
			else
			{
			    
				Toast.makeText(getApplicationContext(),"Enable BT before connecting",Toast.LENGTH_LONG).show();
				
				
			}
			
			
		}
		else if (item.getItemId() == R.id.Enable){
			if(btAdapter.isEnabled())
			{
				Toast.makeText(getApplicationContext(),"Bluetooth is already enabled ",Toast.LENGTH_LONG).show();
				
			}
			else
			{
				
				turnOnBt();
				
			}
			
            return true;
		}
		else if (item.getItemId() == R.id.Disable){
			btAdapter.disable();
			Toast.makeText(getApplicationContext(),"Bluetooth is disabled",Toast.LENGTH_LONG).show();
			
            return true;
		}
		return false;
	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		}

	@SuppressWarnings("unused")
	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case DeviceConnect.STATE_CONNECTED:
                    tvSetData.setText("Device Connected");
                    break;
                case DeviceConnect.STATE_CONNECTING:
                	tvSetData.setText("Connecting Device...");
                    break;
                case DeviceConnect.STATE_LISTEN:
                	tvSetData.setText("Listening for incoming data");
                	break;
                case DeviceConnect.STATE_NONE:
                	tvSetData.setText("not connected");
                	break;
                case DeviceConnect.STATE_ERROR:
                	tvSetData.setText("Disconnected due to an error");
                	break;
                }
                break;
            
            case MESSAGE_READ:            	              
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                String[] splitString = readMessage.split(",");
                if(splitString.length != 7) break;
                try {
                	Float EDA = Float.valueOf(Float.parseFloat(new String(splitString[6])));
                    switch(DATA_STATE) {
                    case MainActivity.DATA_STATE_CALM:
                    	calmData.add(EDA);
                    	Log.i(TAG, "CALM: "+EDA.toString());
                    	break;
                    case MainActivity.DATA_STATE_FRIGHT:
                    	frightData.add(EDA);
                    	Log.i(TAG, "FRIGHT: "+EDA.toString());
                    	break;
                    case MainActivity.DATA_STATE_NONE:
                    default:
                    }
                	//Toast.makeText(getApplicationContext(), splitString[6], Toast.LENGTH_LONG).show();
                } catch (NumberFormatException e) {
                	Log.i(TAG, "FAIL");
                }
                break;
           
            
            }
        }
    };

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub		
		if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
        	// When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);                
                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                Toast.makeText(getApplicationContext(),"Trying to connect to"+ device.getName(), Toast.LENGTH_LONG).show();
                DeviceConnect SensorConnect;
                SensorConnect=new DeviceConnect(mHandler);
                SensorConnect.connect(address);
                
              }
           
            break;

        }
	}






	



	
    
    
    
    
    
    
    
}
