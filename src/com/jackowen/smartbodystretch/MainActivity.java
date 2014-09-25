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
	private boolean deviceState=false;	//代表了智能设备的状态，true为walk，false为work
	// 布局控件
	private Button syncBtn;
	private TextView score_textview;
	private RatingBar scoreBar;
	// ---------------------------------//
	// 内部成员对象
	private boolean sync = false; // 同步后该值为true
	// 生成一个Handler用于处理蓝牙是适配器接收的数据
	public ReceiverDataHandler receiverDataHandler = new ReceiverDataHandler();
	// 生成一个蓝牙类的对象,并将上作为参数
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
		Toast toast = Toast.makeText(this, "今天要精神饱满哦！", Toast.LENGTH_LONG);
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

	// 使用实现接口类的方法
	class syncBtnListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			// 先读取已经保存的智能设备的蓝牙名称，如果没有，则返回null
			SharedPreferences mydatas = getSharedPreferences("datas",Activity.MODE_PRIVATE);
			myBluetooth.setDeviceName(mydatas.getString("DeviceName", null));

			if (myBluetooth.getDeviceName() == null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("提示：");
				builder.setMessage("请先设置智能设备的蓝牙名称！");
				builder.setPositiveButton("确认", null);
				builder.show();
			} else if (sync == false) {
				myBluetooth.connect();
				syncBtn.setText("已同步");
				sync = true;
			} else if (sync == true) {
				myBluetooth.sendMessage("stop ");
				myBluetooth.closeAdapter();
				syncBtn.setText("已断开");
				sync = false;
			}
		}
	}// LISTENER END

//	class ConnectThread extends Thread{
//		@Override
//		public void run(){
//			// 先读取已经保存的智能设备的蓝牙名称，如果没有，则返回null
//			SharedPreferences mydatas = getSharedPreferences("datas",Activity.MODE_PRIVATE);
//			myBluetooth.setDeviceName(mydatas.getString("DeviceName", null));
//
//			if (myBluetooth.getDeviceName() == null) {
//				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//				builder.setTitle("提示：");
//				builder.setMessage("请先设置智能设备的蓝牙名称！");
//				builder.setPositiveButton("确认", null);
//				builder.show();
//			} else if (sync == false) {
//				myBluetooth.connect();
//				syncBtn.setText("已同步");
//				sync = true;
//			} else if (sync == true) {
//				myBluetooth.sendMessage("stop ");
//				myBluetooth.closeAdapter();
//				syncBtn.setText("已断开");
//				sync = false;
//			}
//		}
//	}
	// 当从消息队列中取出数据，调用sendMessage方法发送出去（试验功能）
	// 实际功能需要修改此方法
	class ReceiverDataHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			String result = (String) msg.obj;
			RecordScore(result.trim());
		}
	}

	// 处理接收到的数据，将字符串转换成数字，并累加
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
			// 保存今天的总分和记录的次数。则一天的分数等于总分除以记录的次数
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
			// 保存今天的总分和记录的次数。则一天的分数等于总分除以记录的次数
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

	// 根据分数设定RantingBar的星级
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
        
        renderer.setChartTitle("最近得分折线图");
        renderer.setBackgroundColor(Color.WHITE);
        renderer.setApplyBackgroundColor(true);
        
        renderer.setXTitle("日期");
        renderer.setYTitle("得分");
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
	
	// 只在菜单第一次初始化时调用
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// 菜单被显示前调用
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	// 菜单项被点击时调用，也就是菜单的监听方法
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final EditText et = new EditText(MainActivity.this);
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			if (!myBluetooth.isConnected()) {
				Toast.makeText(this, "请您先连接智能设备！", Toast.LENGTH_LONG).show();
				return true;
			}else{
				final String[] state =new String[]{"伏案模式","行走模式"};
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("请您选择设备的工作环境：");
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
			builder.setTitle("作者");
			builder.setMessage("Team: APlus团队");
			builder.setPositiveButton("OK", null);
			builder.show();
			return true;
		} else if (id == R.id.action_logout) {
			//先存入之前保存的DeviceName
			SharedPreferences theDeviceName = getSharedPreferences("datas",Activity.MODE_PRIVATE);
			et.setText(theDeviceName.getString("DeviceName", null));
			
			AlertDialog.Builder builder = new AlertDialog.Builder(
					MainActivity.this);
			builder.setTitle("请您输入设备的蓝牙名称：");
			builder.setView(et);
			builder.setPositiveButton("确认",
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
			builder.setNegativeButton("取消", null);
			builder.show();
			return true;
		}
		else if(id == R.id.action_linechart){
			ShowLineChart();
		}
		else if(id == R.id.action_level){
			if (!myBluetooth.isConnected()) {
				Toast.makeText(this, "请您先连接智能设备！", Toast.LENGTH_LONG).show();
				return true;
			}else{
				final String[] state =new String[]{"严格模式","普通模式","轻松模式"};
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("请您选择身姿检测的等级：");
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
