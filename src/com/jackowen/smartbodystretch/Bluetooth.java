package com.jackowen.smartbodystretch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;

import com.jackowen.smartbodystretch.MainActivity.ReceiverDataHandler;

/*
 * 这个类中可用的成员变量有
 * ReceiverDataHandler receiverHandler
 * 
 * 这个类中可调用方法有
 * 1) boolean isConnected()//检查连接状况
 * 2) boolean connect()//启动连接（启动后将自动启动数据接收线程）
 * 3) void sendMessage(String message)（启动发送数据的线程 ）
 * 4) 记得Bluetooth的构造函数必须传递一个ReceiverDataHandler对象
 * */

public class Bluetooth {
	
	private boolean isConnected=false;
	private BluetoothSocket mySocket=null;
	private BluetoothDevice myDevice=null;
	private String bluetoothDeviceName=null;
	
	private BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
	private ReceiverDataHandler receiverHandler=null;
	
	//构造函数，得到一个蓝牙适配器
	Bluetooth(ReceiverDataHandler handler){
		receiverHandler = handler;
	}
	//检查是否与外部蓝牙设备连接
	public boolean isConnected(){
		return isConnected;
	}
	
	public void openAdapter(){
		bluetoothAdapter.enable();
	}
	
	public void closeAdapter(){
		bluetoothAdapter.disable();
	}
	
	public void setDeviceName(String name){
		bluetoothDeviceName = name;
	}
	
	public String getDeviceName(){
		return bluetoothDeviceName;
	}
	
	//启动（若蓝牙未启动）、检测已配对的适配器、找到智能设备并与之连接、并启动接收数据的线程（一般在按钮中调用该方法）
	public void connect(){
		if(bluetoothAdapter.isEnabled()){
			if(!bluetoothAdapter.isDiscovering()){
				bluetoothAdapter.startDiscovery();
			}
		}else{
			bluetoothAdapter.enable();
			while(!bluetoothAdapter.isEnabled());
			if(!bluetoothAdapter.isDiscovering()){
				bluetoothAdapter.startDiscovery();
			}
		}
		//得到已匹配的蓝牙适配器对象
		Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
		if(devices.size() > 0){
			for(Iterator iterator = devices.iterator();	iterator.hasNext();){
				BluetoothDevice bluetoothDevice =(BluetoothDevice) iterator.next();
				if(bluetoothDevice.getName().equals(bluetoothDeviceName)){
					//找到对应名称的智能设备，启动连接线程
					ConnectThread ct = new ConnectThread(bluetoothDevice);
					ct.start();
				}
			}
		}//if is end
		while(!isConnected);
		sendMessage("start");
		//连接成功，开启接收数据的线程
		ReceiverThread rt = new ReceiverThread(mySocket);
		rt.start();
	}//connect is  end
	
	//客户端的方式、创建蓝牙的连接、  得到mySocket
	class ConnectThread extends Thread{
		ConnectThread(BluetoothDevice device){
			myDevice = device;
			BluetoothSocket tmp = null;
			try {
    			UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    			tmp = device.createRfcommSocketToServiceRecord(uuid);
			} catch (Exception e) {	}
    		mySocket = tmp;
		}
		@Override
		public void run(){
			bluetoothAdapter.cancelDiscovery();
			try {
				mySocket.connect();
				isConnected=true;
			}catch (IOException e) {
				try {
					mySocket.close();
					isConnected=false;
				} catch (IOException e1) { }
				return;
			}
		}
	}// ConnectThread is End
	
	//创建一个接收数据的线程
	//并将接收的数据通过消息队列传出去
	class ReceiverThread extends Thread{
		private BluetoothSocket Socket=null;
		private InputStream inStream=null;
		ReceiverThread(BluetoothSocket socket){
			Socket = socket;
			try {
				inStream = Socket.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		}
		//线程体
	    @Override
		public void run() {
	    	byte[] buffer = new byte[32];
			int bytesRead = -1;
			String result = "";
			while(true){
				try {
					bytesRead = inStream.read(buffer);
					result = new String(buffer,0,bytesRead);
					Message msg = receiverHandler.obtainMessage();
					msg.obj = result;
					receiverHandler.sendMessage(msg);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
	    }
	}
	    
    //通过BluetoothSocket得到相应的outputStream
    //再通过该outputStream发送数据
    public void sendMessage(String message){
    	OutputStream outStream=null;
    	try {
			outStream = mySocket.getOutputStream();
			byte[] datas = (message+" ").getBytes();
			datas[datas.length-1]=0;
			outStream.write(datas);
		} catch (IOException e) {	}
    }
}
