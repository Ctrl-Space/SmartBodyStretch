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
 * ������п��õĳ�Ա������
 * ReceiverDataHandler receiverHandler
 * 
 * ������пɵ��÷�����
 * 1) boolean isConnected()//�������״��
 * 2) boolean connect()//�������ӣ��������Զ��������ݽ����̣߳�
 * 3) void sendMessage(String message)�������������ݵ��߳� ��
 * 4) �ǵ�Bluetooth�Ĺ��캯�����봫��һ��ReceiverDataHandler����
 * */

public class Bluetooth {
	
	private boolean isConnected=false;
	private BluetoothSocket mySocket=null;
	private BluetoothDevice myDevice=null;
	private String bluetoothDeviceName=null;
	
	private BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
	private ReceiverDataHandler receiverHandler=null;
	
	//���캯�����õ�һ������������
	Bluetooth(ReceiverDataHandler handler){
		receiverHandler = handler;
	}
	//����Ƿ����ⲿ�����豸����
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
	
	//������������δ���������������Ե����������ҵ������豸����֮���ӡ��������������ݵ��̣߳�һ���ڰ�ť�е��ø÷�����
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
		//�õ���ƥ�����������������
		Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
		if(devices.size() > 0){
			for(Iterator iterator = devices.iterator();	iterator.hasNext();){
				BluetoothDevice bluetoothDevice =(BluetoothDevice) iterator.next();
				if(bluetoothDevice.getName().equals(bluetoothDeviceName)){
					//�ҵ���Ӧ���Ƶ������豸�����������߳�
					ConnectThread ct = new ConnectThread(bluetoothDevice);
					ct.start();
				}
			}
		}//if is end
		while(!isConnected);
		sendMessage("start");
		//���ӳɹ��������������ݵ��߳�
		ReceiverThread rt = new ReceiverThread(mySocket);
		rt.start();
	}//connect is  end
	
	//�ͻ��˵ķ�ʽ���������������ӡ�  �õ�mySocket
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
	
	//����һ���������ݵ��߳�
	//�������յ�����ͨ����Ϣ���д���ȥ
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
		//�߳���
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
	    
    //ͨ��BluetoothSocket�õ���Ӧ��outputStream
    //��ͨ����outputStream��������
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
