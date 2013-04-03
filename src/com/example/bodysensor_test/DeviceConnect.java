package com.example.bodysensor_test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;



public class DeviceConnect {
	// Unique UUID for this application
    private static final UUID SERIAL_PROFILE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    
    private BluetoothAdapter mBluetoothAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private  Handler mHandler;
    private int mState;
	// Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_ERROR = 5;   //Unable to connect
    private static final String TAG = "DeviceConnect";
    private static final boolean D = true;
    
    
    
    public DeviceConnect(Handler handler) {
        
        mState = STATE_NONE;
        mHandler = handler;
    }
    
    
    public void connect(String address)
	{
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mState == STATE_NONE)
		{			
			if(address != null)
			{
				connectToDeviceWithAddress(address);
			}					     
		}
	}
    
    private synchronized void setState(int state)
    {
        mState = state;
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    
    private void connectionFailed(IOException e)
    {
        setState(STATE_ERROR);
        
    }
    
    private void connectionLost(IOException e)
    {
        setState(STATE_NONE);
        
    }
    
    private synchronized void pushData(byte[] buffer, int size)
	{
           mHandler.obtainMessage(MainActivity.MESSAGE_READ,size, -1, buffer).sendToTarget();
	}
    
    /**
     * Start the service. */
    private synchronized void start()
    {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        
        setState(STATE_LISTEN);
    }
    
    
    private synchronized void connectToDeviceWithAddress(String aAddress)
    {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
        {
        	if (aAddress != null && aAddress.length() > 0)
        	{
        		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(aAddress);

        		// Cancel any thread attempting to make a connection
        		if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        		// Cancel any thread currently running a connection
        		if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        		setState(STATE_NONE);
        		
        		// Start the thread to connect with the given device
        		mConnectThread = new ConnectThread(device);
        		if(mConnectThread.isDeviceValid())
        		{
        			mConnectThread.start();
        			setState(STATE_CONNECTING);
        		}
        	}
        }
    }
    
  
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public Boolean isDeviceValid()
        {
        	return mmSocket != null;
        }
        
        public ConnectThread(BluetoothDevice device)
        {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try
            {
                tmp = device.createRfcommSocketToServiceRecord(SERIAL_PROFILE_UUID);
            } 
            catch (IOException e)
            {
            	tmp = null;
            	connectionFailed(e);
            }
            mmSocket = tmp;
        }

        public void run()
        {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try 
            {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            }
            catch (IOException e)
            {
                connectionFailed(e);
                // Close the socket
                try
                {
                    mmSocket.close();
                }
                catch (IOException e2)
                {
                }
                // Start the service over to restart listening mode
                DeviceConnect.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (DeviceConnect.this)
            {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
            }
        }
    }
	
    
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device)
    {
    	
    	
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        if(mConnectedThread.isValidDevice())
        {
        	mConnectedThread.setPriority(Thread.NORM_PRIORITY);
        	mConnectedThread.start();
        	setState(STATE_CONNECTED);
        }
    }
    
    
    
	
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        
        public boolean isValidDevice()
        {
        	return mmInStream != null && mmOutStream != null;
        }
        
        public ConnectedThread(BluetoothSocket socket)
        {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e)
            {
            	if(tmpIn != null)
            	{
	            	tmpIn = null;
            	}
                connectionFailed(e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        volatile boolean sentinel = false;
       
        public void run()
        {
        	if(!isValidDevice()) return;        	
            
            
            final int maxBufSize = 2048;
            byte[] data = new byte[maxBufSize];

            // Keep listening to the InputStream while connected
            while (!sentinel)
            {
                try
                {
                    // Read from the InputStream
                	int length = mmInStream.read(data, 0, maxBufSize);
                    if(length > 0)
                    {
                    	pushData(data, length);
                    }
	                
                    try
                    {
                    	if(length < maxBufSize) Thread.sleep(50);
                    }
                    catch(Exception e)
                    {
                        // Interrupted exception
                    }
                }
                catch (IOException e)
                {
                    connectionLost(e);
                    break;
                }
            }
        }

        public void cancel()
        {
        	sentinel = true;
        	
            try
            {
                mmSocket.close();
            }
            catch (IOException e)
            {
            }
        }
    }	    
	
	
	

}
