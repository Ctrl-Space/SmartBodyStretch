package com.jackowen.smartbodystretch;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private int whichs=0;
	private int whichState=0;
	private int whichLevel=1;
	//---------------------------//
	private int finalWorkScore = 0;
	private int workcount = 0;
	private int finalWalkScore = 0;
	private int walkcount = 0;
	// ----------------------------//
	private Time time = new Time();
	// ----------------------------//
	private boolean deviceState=false;	//�����������豸��״̬��trueΪwalk��falseΪwork
	// ���ֿؼ�
	private Button syncBtn;
	private TextView score_textview;
	private RatingBar scoreBar;
	// ---------------------------------//
	// �ڲ���Ա����
	private boolean sync = false; // ͬ�����ֵΪtrue
	// ����һ��Handler���ڴ������������������յ�����
	public ReceiverDataHandler receiverDataHandler = new ReceiverDataHandler();
	// ����һ��������Ķ���,��������Ϊ����
	private Bluetooth myBluetooth = new Bluetooth(receiverDataHandler);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		syncBtn        = (Button) findViewById(R.id.syncButtonId);
		score_textview = (TextView) findViewById(R.id.scoreTextViewId);
		scoreBar       = (RatingBar) findViewById(R.id.ratingBarId);

		syncBtn.setOnClickListener(new syncBtnListener());

		time.setToNow();
		checkDate();
		Toast toast = Toast.makeText(this, "����Ҫ������Ŷ��", Toast.LENGTH_LONG);
		toast.show();
	}

	@Override
	protected void onDestroy() {
		myBluetooth.closeAdapter();
		sync = false;
		super.onDestroy();
	}
	private void checkDate() {
		int year = time.year;
		int month = time.month;
		int date = time.monthDay;

		SharedPreferences mydatas = getSharedPreferences("datas",Activity.MODE_PRIVATE);
		SharedPreferences.Editor mydatasEditor = mydatas.edit();
		int today_date = mydatas.getInt("today_date", 0);
		int today_month = mydatas.getInt("today_month", 0);
		int today_year = mydatas.getInt("doday_year", 0);
		if ((month == today_month) && (date == today_date)&& (year == today_year)) {
			finalWorkScore = mydatas.getInt("finalWorkScore", 0);
			workcount = mydatas.getInt("workcount", 1);
			finalWalkScore = mydatas.getInt("finalWalkScore", 0);
			walkcount = mydatas.getInt("walkcount", 1);
		} else {
			finalWorkScore = 0;
			workcount = 0;
			finalWalkScore = 0;
			walkcount = 0;
			mydatasEditor.putInt("dateWorkScore"+date, 0);
			mydatasEditor.putInt("dateWalkScore"+date, 0);

			mydatasEditor.putInt("finalWalkScore", 0);
			mydatasEditor.putInt("walkcount", 0);
			mydatasEditor.putInt("finalWorkScore", 0);
			mydatasEditor.putInt("workcount", 0);
			
			mydatasEditor.putInt("today_year", year);
			mydatasEditor.putInt("today_month", month);
			mydatasEditor.putInt("today_date", date);
			mydatasEditor.commit();
		}
	}

	// ʹ��ʵ�ֽӿ���ķ���
	class syncBtnListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			// �ȶ�ȡ�Ѿ�����������豸���������ƣ����û�У��򷵻�null
			SharedPreferences mydatas = getSharedPreferences("datas",Activity.MODE_PRIVATE);
			myBluetooth.setDeviceName(mydatas.getString("DeviceName", null));

			if (myBluetooth.getDeviceName() == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("��ʾ��");
				builder.setMessage("�������������豸���������ƣ�");
				builder.setPositiveButton("ȷ��", null);
				builder.show();
			} else if (sync == false) {
				myBluetooth.connect();
				syncBtn.setText("��ͬ��");
				sync = true;
			} else if (sync == true) {
				myBluetooth.sendMessage("stop ");
				myBluetooth.closeAdapter();
				syncBtn.setText("�ѶϿ�");
				sync = false;
			}
		}
	}// LISTENER END

//	class ConnectThread extends Thread{
//		@Override
//		public void run(){
//			// �ȶ�ȡ�Ѿ�����������豸���������ƣ����û�У��򷵻�null
//			SharedPreferences mydatas = getSharedPreferences("datas",Activity.MODE_PRIVATE);
//			myBluetooth.setDeviceName(mydatas.getString("DeviceName", null));
//
//			if (myBluetooth.getDeviceName() == null) {
//				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//				builder.setTitle("��ʾ��");
//				builder.setMessage("�������������豸���������ƣ�");
//				builder.setPositiveButton("ȷ��", null);
//				builder.show();
//			} else if (sync == false) {
//				myBluetooth.connect();
//				syncBtn.setText("��ͬ��");
//				sync = true;
//			} else if (sync == true) {
//				myBluetooth.sendMessage("stop ");
//				myBluetooth.closeAdapter();
//				syncBtn.setText("�ѶϿ�");
//				sync = false;
//			}
//		}
//	}
	// ������Ϣ������ȡ�����ݣ�����sendMessage�������ͳ�ȥ�����鹦�ܣ�
	// ʵ�ʹ�����Ҫ�޸Ĵ˷���
	class ReceiverDataHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			String result = (String) msg.obj;
			RecordScore(result.trim());
		}
	}

	// ������յ������ݣ����ַ���ת�������֣����ۼ�
	private void RecordScore(String result) {
		 String sNum = result.replaceAll(" ", "");
		int score = Integer.parseInt(sNum);
		if (score >= 100) {
			score = 99;
		} else if (score < 10) {
			score = 10;
		}
		
		if(deviceState){
			finalWalkScore += score;
			walkcount += 1;
			// ���������ֺܷͼ�¼�Ĵ�������һ��ķ��������ֳܷ��Լ�¼�Ĵ���
			SharedPreferences mydatas = getSharedPreferences("datas",Activity.MODE_PRIVATE);
			SharedPreferences.Editor mydatasEditor = mydatas.edit();
			mydatasEditor.putInt("finalWalkScore", finalWalkScore);
			mydatasEditor.putInt("walkcount", walkcount);
//			mydatasEditor.putInt("dateWalkScore"+time.monthDay, finalWalkScoring/(walkcount));
			mydatasEditor.putInt("dateWalkScore"+time.monthDay, score);
			mydatasEditor.commit();
		}else{
			finalWorkScore += score;
			workcount += 1;
			// ���������ֺܷͼ�¼�Ĵ�������һ��ķ��������ֳܷ��Լ�¼�Ĵ���
			SharedPreferences mydatas = getSharedPreferences("datas",Activity.MODE_PRIVATE);
			SharedPreferences.Editor mydatasEditor = mydatas.edit();
			mydatasEditor.putInt("finalWorkScore", finalWorkScore);
			mydatasEditor.putInt("workcount", workcount);
//			mydatasEditor.putInt("dateWorkScore"+time.monthDay, finalWorkScoring/(workcount));
			mydatasEditor.putInt("dateWorkScore"+time.monthDay, score);
			mydatasEditor.commit();
		}
		score_textview.setText(Integer.toString(score));
		setRatingBar(score);
	}

	// ���ݷ����趨RantingBar���Ǽ�
	private void setRatingBar(int score) {
		if (score >= 90) {
			scoreBar.setRating((float) 4.5);
		} else if (score >= 80) {
			scoreBar.setRating((float) 4.0);
		} else if (score >= 70) {
			scoreBar.setRating((float) 3.5);
		} else if (score >= 60) {
			scoreBar.setRating((float) 3.0);
		} else if (score >= 55) {
			scoreBar.setRating((float) 2.5);
		} else if (score >= 50) {
			scoreBar.setRating((float) 2.0);
		} else if (score >= 45) {
			scoreBar.setRating((float) 1.5);
		} else if (score >= 35) {
			scoreBar.setRating((float) 1.0);
		} else if (score >= 25) {
			scoreBar.setRating((float) 0.5);
		} else {
			scoreBar.setRating((float) 0);
		}
	}

	private void ShowLineChart() {
		
		int[] dateWorkScore = new int[32];
		int[] dateWalkScore = new int[32];
		
		SharedPreferences mydatas = getSharedPreferences("datas",Activity.MODE_PRIVATE);
//		SharedPreferences.Editor mydatasEditor = mydatas.edit();
		for(int i=1;i<=time.monthDay;i++){
			dateWorkScore[i] = mydatas.getInt("dateWorkScore"+i, 90-i);
		}
		
		for(int i=1;i<=time.monthDay;i++){
			dateWalkScore[i] = mydatas.getInt("dateWalkScore"+i, 85+i);
		}
		
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        XYMultipleSeriesDataset	dataset = new XYMultipleSeriesDataset();
        
        renderer.setChartTitle("����÷�����ͼ");
        renderer.setBackgroundColor(Color.WHITE);
        renderer.setApplyBackgroundColor(true);
        
        renderer.setXTitle("����");
        renderer.setYTitle("�÷�");
        renderer.setAxisTitleTextSize(30);
        
        renderer.setAxesColor(Color.BLUE);
        renderer.setXLabelsColor(Color.BLUE);
        renderer.setYLabelsColor(0, Color.BLUE);
        
        renderer.setYAxisMax(100);
        renderer.setYAxisMin(0);
        renderer.setXAxisMax(time.monthDay);
        renderer.setXAxisMin(1);
        
        renderer.setChartTitleTextSize(30);
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(30);
        renderer.setLegendHeight(50);
        
        renderer.setMargins(new int[] {40,40,40,40});
        renderer.setMarginsColor(Color.WHITE);
        
        XYSeries Workseries = new XYSeries("Walk");
        for(int i=1;i<=time.monthDay;i++){
        	Workseries.add(i, dateWalkScore[i]);
        }
        dataset.addSeries(Workseries);

        XYSeries Walkseries = new XYSeries("Work");
        for(int i=1;i<=time.monthDay;i++){
        	Walkseries.add(i, dateWorkScore[i]);
        }
        
        dataset.addSeries(Walkseries);
        
        XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
        seriesRenderer.setColor(Color.BLUE);
        seriesRenderer.setPointStyle(PointStyle.SQUARE);
        renderer.addSeriesRenderer(seriesRenderer);
        
        seriesRenderer = new XYSeriesRenderer();
        seriesRenderer.setColor(Color.RED);
        seriesRenderer.setPointStyle(PointStyle.CIRCLE);
        renderer.addSeriesRenderer(seriesRenderer);
        
        
        Intent intent = ChartFactory.getLineChartIntent(MainActivity.this, dataset, renderer);
        startActivity(intent);
	}
	
	// ֻ�ڲ˵���һ�γ�ʼ��ʱ����
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// �˵�����ʾǰ����
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	// �˵�����ʱ���ã�Ҳ���ǲ˵��ļ�������
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final EditText et = new EditText(MainActivity.this);
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			if (!myBluetooth.isConnected()) {
				Toast.makeText(this, "���������������豸��", Toast.LENGTH_LONG).show();
				return true;
			}else{
				final String[] state =new String[]{"����ģʽ","����ģʽ"};
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("����ѡ���豸�Ĺ���������");
				builder.setSingleChoiceItems(state, whichState, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						whichs = which;
						whichState = which;
					}
				});
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(whichs ==0){
							myBluetooth.sendMessage("work ");
							deviceState= false;
						}
						else if(whichs == 1){
							myBluetooth.sendMessage("walk ");
							deviceState= false;
						}
					}
				});
				builder.show();
				return true;
			}
		} else if (id == R.id.action_auther) {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					MainActivity.this);
			builder.setTitle("����");
			builder.setMessage("Team: APlus�Ŷ�");
			builder.setPositiveButton("OK", null);
			builder.show();
			return true;
		} else if (id == R.id.action_logout) {
			//�ȴ���֮ǰ�����DeviceName
			SharedPreferences theDeviceName = getSharedPreferences("datas",Activity.MODE_PRIVATE);
			et.setText(theDeviceName.getString("DeviceName", null));
			
			AlertDialog.Builder builder = new AlertDialog.Builder(
					MainActivity.this);
			builder.setTitle("���������豸���������ƣ�");
			builder.setView(et);
			builder.setPositiveButton("ȷ��",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String result = et.getText().toString();

							SharedPreferences mydatas = getSharedPreferences(
									"datas", Activity.MODE_PRIVATE);
							SharedPreferences.Editor mydatasEditor = mydatas
									.edit();
							mydatasEditor.putString("DeviceName", result);
							mydatasEditor.commit();

							myBluetooth.setDeviceName(result);
						}
					});
			builder.setNegativeButton("ȡ��", null);
			builder.show();
			return true;
		}
		else if(id == R.id.action_linechart){
			ShowLineChart();
		}
		else if(id == R.id.action_level){
			if (!myBluetooth.isConnected()) {
				Toast.makeText(this, "���������������豸��", Toast.LENGTH_LONG).show();
				return true;
			}else{
				final String[] state =new String[]{"�ϸ�ģʽ","��ͨģʽ","����ģʽ"};
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("����ѡ�����˼��ĵȼ���");
				builder.setSingleChoiceItems(state, whichLevel, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						whichs = which;
						whichLevel = which;
					}
				});
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(whichs ==0){
							myBluetooth.sendMessage("high ");
						}
						else if(whichs == 1){
							myBluetooth.sendMessage("middl");
						}else if(whichs == 2){
							myBluetooth.sendMessage("low  ");
						}
					}
				});
				builder.show();
				return true;
			}			
		}
		return super.onOptionsItemSelected(item);
	}

}
