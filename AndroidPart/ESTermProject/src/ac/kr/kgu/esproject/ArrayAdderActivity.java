package ac.kr.kgu.esproject;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class ArrayAdderActivity extends Activity {

	static {
		System.loadLibrary("ESTermProject");
	};

	static TimerTask tt;
	/***********************************************************/
	// 7-segment �κ�
	BackThread thread = new BackThread();
	int flag = -1;
	boolean stop = false;
	int count = 0;

	/*************************************************************/

	// JNI �Լ���
	public native int BuzzerControl(int value);
	public native int SegmentControl(int data);
	public native int SegmentIOContrl(int data);
	public native int[] CreateArray(int value);
	public native int CompareArray(int value, int[] sumArray);

	private ArrayAdapter<CharSequence> adapter;
	private ArrayAdapter<String> adapter2;
	private Spinner spinner;
	public int[] array;
	public int[] sumArray;		// ���� �񱳸� ���� ���� ���� �����ϴ� �迭

	int BuzData;	// ������ �︲ ���θ� ���ϱ� ���� ����
	int lengthResult;
	String line = null;
	int index = 0; 	// �ݺ��� ����� ��������. segment ���� ���

	int sum; 		// ����ڰ� �Է��� ���������
	int realsum=0; 	//���� �迭 ���� ����� ���� ��¥ ���� ���
	int wrongsum=0;	//���� ����� Ʋ���� �� ���� ��
	int result = 0; // �迭 �� �հ� ����ڰ� �Է��� ���� �� ���� ���� �� ����
	boolean exitOuterLoop = false;	//��ø �������� ���������� ���� ���� boolean Ÿ���� ����, �׻� ���� �� �� false�� �ʱ�ȭ
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		BuzData = 0;
		// �迭�� ũ�⸦ �����ϴ� spinner ����
		spinner = (Spinner) findViewById(R.id.arraySpinner);
		adapter = ArrayAdapter.createFromResource(this, R.array.indexAmount,
				android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		// ���̾ƿ����� ���ü� ���� ����
		final View vision1_1 = (View) findViewById(R.id.vision1_1);
		final View vision1_2 = (View) findViewById(R.id.vision1_2);
		final View vision2 = (View) findViewById(R.id.vision2);

		// �迭�� �������� ����� ȭ�鿡 ������ִ� TextView
		final TextView arrayResult = (TextView) findViewById(R.id.arrayResult);

		final LinearLayout vision1 = (LinearLayout) findViewById(R.id.vision1);
		final ArrayList<String> list = new ArrayList<String>();
		final LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) vision1
				.getLayoutParams();

		// ������� ���� �� ���俩�� ���
		final TextView yesOrNo = (TextView) findViewById(R.id.yesOrNo);

		// ����ڰ� ������ �Է��ϴ� EditText
		final EditText inputNum = (EditText) findViewById(R.id.inputNum);

		// ������Ʈ��
		BuzzerControl(BuzData);

		// �迭�� �����ϴ� ��ư
		Button arrayCreate = (Button) findViewById(R.id.createButton);

		// Segment ������ ����
		thread.setDaemon(true);
		thread.start();

		arrayCreate.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// arraylist�� Textview�ʱ�ȭ
				list.clear();
				arrayResult.setText("");
				int number = Integer.parseInt(spinner.getSelectedItem()
						.toString());
				sumArray = CreateArray(number);
				Toast.makeText(getApplicationContext(), "Right here",
						Toast.LENGTH_LONG).show();

				lengthResult = 0;

				array = new int[number];
				for (int i = 0; i < array.length; i++) {
					lengthResult += 45;
					list.add("�迭 ��� #" + (i + 1) + " : " + sumArray[i] + "\n");

					// �迭�� � ���� ������ �Ǿ����� ȭ�鿡 ����ϱ� ���� ���̾ƿ��� ���̸� ����
					param.height = lengthResult;
					vision1.setLayoutParams(param);
				}

				// ArrayList�� ����� ��� ��� ���
				Iterator<String> iter = list.iterator();
				while (iter.hasNext()) {
					arrayResult.append(iter.next());
				}

				vision1.setVisibility(View.VISIBLE);
				vision1_1.setVisibility(View.VISIBLE);
				vision1_2.setVisibility(View.VISIBLE);
			}
		});

		tt = timerTaskMaker();

		final Timer timer = new Timer();
		// ok�� ������ ��� ������ �˷��ִ� TextView ���
		Button ok = (Button) findViewById(R.id.okButton);
		ok.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				try{
				sum = Integer.parseInt(inputNum.getText().toString());
				result = CompareArray(sum, sumArray);
				}catch(Exception e){
					result = -1;
				}
				if(sum > 100)	result = 2;
				
				//result: 1=����, 0=����, -1=�ƹ��͵� �Է��� �ȵ� ����, 2=99�� �Ѵ� ���ڰ� ������ ���
				if (result == 1) {
					flag = 1;
					yesOrNo.setText("��Ȯ�մϴ�.");
					showDialog(1);
					tt = timerTaskMaker();
					timer.schedule(tt, 0, 1000);
				} else if (result == -1) {
					yesOrNo.setText("�ƹ��͵� �Է��� �ȵǾ����ϴ�.");
					BuzData = 0;
				} else if (result == 2)	{
					yesOrNo.setText("������ 1~99������ �����Դϴ�.");
					BuzData = 0;
				} else {
					realsum = 0;
					for(int i=0; i<sumArray.length;i++) realsum += sumArray[i];
					BuzData = 1;
					flag = 0;
					yesOrNo.setText("Ʋ�Ƚ��ϴ�.");
				}
				BuzzerControl(BuzData);
				vision2.setVisibility(View.VISIBLE);
			}
		});

		// clear�� ������ ��� ù ���� ������ ��� �κ��� ������, �迭�� �����
		Button clear = (Button) findViewById(R.id.clearButton);
		clear.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				tt.cancel();
				BuzData = 0;
				BuzzerControl(BuzData);
				vision1.setVisibility(View.GONE);
				vision1_1.setVisibility(View.GONE);
				vision1_2.setVisibility(View.GONE);
				vision2.setVisibility(View.GONE);
				lengthResult = 0;
				flag = -1;			//segment �����·� �ǵ���
				realsum = 0;		//���� ���߾��� ���� �迭�� ���� �ٽ� 0���� �ʱ�ȭ��
			}
		});
	}
	//������ ���� �ݺ��ϸ� �︮�� �ϱ� ���� ���� TimeTask, �Ź� �������� ����� �ϴ� ������
	//�ѹ� �����ϸ� �ٽ� �� �� �ǻ츱 �� ���� ������ Clear�� �ٽ� ������ �Է����� ��, ������Ͽ� ����Ѵ�.
	//����ؼ� BuzData�� 1�ϋ��� 0����, 0�϶��� 1�� �ٲٸ鼭 �Ҹ��� ����.
	public TimerTask timerTaskMaker() {
		TimerTask tempTask = new TimerTask() {
			public void run() {
				if (BuzData == 1)
					BuzData = 0;
				else
					BuzData = 1;
				BuzzerControl(BuzData);
			}
		};
		return tempTask;
	}

	// �����ϰ� ��µǰԲ� �ǵ������� �ݺ��ϴ� �޼ҵ�, 200�� �ݺ��Ͽ� ��� ������ ��� ���� �����ִ� ��ó�� ���δ�.
	public void repeatSegmentControl(int value) {
		for (int i = 0; i < 200; i++)
			SegmentControl(value);
		return;
	}

	//����� ǥ���� ������ ��¿�, �����¶� ��� segmentcode�� ��Ȱ���Ͽ� ��ġ ���������
	//�׸� ���簢�� �ڽ��� ����ؼ� �����ִ°�ó�� ���δ�.
	public void repeatSegmentControl_Box() {
		for (int i = 0; i < 50; i++) {
			SegmentControl(-211114);
			SegmentControl(-233334);
			SegmentControl(-111111);
			SegmentControl(-333333);
		}
	}

	// segment ����� ����ϴ� ������
	class BackThread extends Thread {
		// flag : -1 -> ������, 1 -> ���� �¾�����, 0 -> Ʋ������
		public void run() {
			while (true) {
				SegmentIOContrl(0); 	// �ð��� ������ �����Ƿ� 0���� �����Ѵ�.
				while (flag == 1) {		// ���� �¾��� ��
					SegmentControl(3000000);
					try {
						for(int i=0; i<3; i++){
							repeatSegmentControl_Box();
							thread.sleep(500);
							if(flag != 1){
								exitOuterLoop = true;	
								break;
							}
						}
						if(exitOuterLoop){
							exitOuterLoop = false;
							break;
						}
						//�� for������ 3�� �ݺ��Ѵ�. ���� ���ڱ� clear��ư�� ������, ������ �������� ���������� ���� true��
						//�ٲ� ��, �Ʒ��� false�� �ʱ�ȭ�ϰ� �ݺ����� ������ ������Ų��.
						//������ ���ؼ�, Clear��ư�� �������� �ﰢ������ �����.
						
						//���� ���� ������ �ﰢ������ ���� �� �ֵ��� exitOuterLoop�� �����ְ�, ���� ���ڸ� ����Ѵ�.
						for(int i=0; i<6; i++)
						{
							repeatSegmentControl(sum+1000000);
							//sum+1000000�� �����ν� ����̹��� ���� ����ϴ� ���·� �޾Ƶ鿩 �����Ѵ�� ����Ѵ�.(��ĭ�� �̵�)
							thread.sleep(500);
							if(flag != 1)
							{
								exitOuterLoop = true;	
								break;
							}
						}
						
						if(exitOuterLoop)
						{
							exitOuterLoop = false;
							break;
						}
						
					} catch (Exception e) {
						System.out.println("flag = 1 error");
					}
				}
				//Ʋ�������� ����, 2 xx 00 xx �� ���� ���ڷ� ����.
				//���� xx�� ����ڰ� �Է��� ���̰�, �� xx�� ���� ���䰪�̴�.
				while (flag == 0) {
					SegmentControl(3000000);
					try{
						for(int i=0; i<3; i++){
							repeatSegmentControl_Box();
							thread.sleep(500);
							if(flag != 0){
								exitOuterLoop = true;	
								break;
							}
						}
						
						if(exitOuterLoop){
							exitOuterLoop = false;
							break;
						}
						wrongsum = realsum * 10000 + sum + 2000000;
						for(int i=0; i<6; i++){
							repeatSegmentControl(wrongsum);
							thread.sleep(500);
							if(flag != 0){
								exitOuterLoop = true;	
								break;
							}
						}
						
						if(exitOuterLoop){
							exitOuterLoop = false;
							break;
						}
					}
					catch(Exception e)
					{
						System.out.println("flag 0 Error");
					}
				}

				while (flag == -1) {
					SegmentControl(3000000);
					//����� ��� �ٲ�� ��⸦ ����ؼ� �ݺ��ϸ鼭 ����ؾ� �ϹǷ�, switch������ 
					//index�� �ϳ��� �����ϸ鼭 �������� �Ѿ�� ������� �������.
					switch (index) {
					case 0:
						repeatSegmentControl((-555551));	index++;	break;
					case 1:
						repeatSegmentControl((-555515));	index++;	break;
					case 2:
						repeatSegmentControl((-555155));	index++;	break;
					case 3:
						repeatSegmentControl((-551555));	index++;	break;
					case 4:
						repeatSegmentControl((-515555));	index++;	break;
					case 5:
						repeatSegmentControl((-155555));	index++;	break;
					case 6:
						repeatSegmentControl((-255555));	index++;	break;
					case 7:
						repeatSegmentControl((-355555));	index++;	break;
					case 8:
						repeatSegmentControl((-535555));	index++;	break;
					case 9:
						repeatSegmentControl((-553555));	index++;	break;
					case 10:
						repeatSegmentControl((-555355));	index++;	break;
					case 11:
						repeatSegmentControl((-555535));	index++;	break;
					case 12:
						repeatSegmentControl((-555553));	index++;	break;
					case 13:
						repeatSegmentControl((-555554));	index = 0;	break;
					}
				}
			}
		}
	}

}
