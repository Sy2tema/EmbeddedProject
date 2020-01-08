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
	// 7-segment 부분
	BackThread thread = new BackThread();
	int flag = -1;
	boolean stop = false;
	int count = 0;

	/*************************************************************/

	// JNI 함수들
	public native int BuzzerControl(int value);
	public native int SegmentControl(int data);
	public native int SegmentIOContrl(int data);
	public native int[] CreateArray(int value);
	public native int CompareArray(int value, int[] sumArray);

	private ArrayAdapter<CharSequence> adapter;
	private ArrayAdapter<String> adapter2;
	private Spinner spinner;
	public int[] array;
	public int[] sumArray;		// 덧셈 비교를 위한 랜덤 수를 저장하는 배열

	int BuzData;	// 부저의 울림 여부를 정하기 위한 변수
	int lengthResult;
	String line = null;
	int index = 0; 	// 반복문 실행시 쓰기위함. segment 에서 사용

	int sum; 		// 사용자가 입력한 덧셈결과값
	int realsum=0; 	//실제 배열 합의 결과로 나온 진짜 덧셈 결과
	int wrongsum=0;	//덧셈 결과가 틀렸을 때 들어가는 값
	int result = 0; // 배열 총 합과 사용자가 입력한 값의 참 거짓 여부 비교 변수
	boolean exitOuterLoop = false;	//중첩 루프문을 빠져나오기 위해 쓰는 boolean 타입의 변수, 항상 쓰고 난 후 false로 초기화
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		BuzData = 0;
		// 배열의 크기를 생성하는 spinner 생성
		spinner = (Spinner) findViewById(R.id.arraySpinner);
		adapter = ArrayAdapter.createFromResource(this, R.array.indexAmount,
				android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		// 레이아웃들의 가시성 관리 변수
		final View vision1_1 = (View) findViewById(R.id.vision1_1);
		final View vision1_2 = (View) findViewById(R.id.vision1_2);
		final View vision2 = (View) findViewById(R.id.vision2);

		// 배열의 랜덤값의 결과를 화면에 출력해주는 TextView
		final TextView arrayResult = (TextView) findViewById(R.id.arrayResult);

		final LinearLayout vision1 = (LinearLayout) findViewById(R.id.vision1);
		final ArrayList<String> list = new ArrayList<String>();
		final LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) vision1
				.getLayoutParams();

		// 결과값을 비교한 후 정답여부 출력
		final TextView yesOrNo = (TextView) findViewById(R.id.yesOrNo);

		// 사용자가 정답을 입력하는 EditText
		final EditText inputNum = (EditText) findViewById(R.id.inputNum);

		// 부저컨트롤
		BuzzerControl(BuzData);

		// 배열을 생성하는 버튼
		Button arrayCreate = (Button) findViewById(R.id.createButton);

		// Segment 스레드 시작
		thread.setDaemon(true);
		thread.start();

		arrayCreate.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// arraylist와 Textview초기화
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
					list.add("배열 요소 #" + (i + 1) + " : " + sumArray[i] + "\n");

					// 배열에 어떤 수가 저장이 되었는지 화면에 출력하기 위해 레이아웃의 높이를 변경
					param.height = lengthResult;
					vision1.setLayoutParams(param);
				}

				// ArrayList에 저장된 모든 요소 출력
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
		// ok를 눌렀을 경우 정답을 알려주는 TextView 출력
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
				
				//result: 1=정답, 0=오답, -1=아무것도 입력이 안된 오류, 2=99가 넘는 숫자가 들어왔을 경우
				if (result == 1) {
					flag = 1;
					yesOrNo.setText("정확합니다.");
					showDialog(1);
					tt = timerTaskMaker();
					timer.schedule(tt, 0, 1000);
				} else if (result == -1) {
					yesOrNo.setText("아무것도 입력이 안되었습니다.");
					BuzData = 0;
				} else if (result == 2)	{
					yesOrNo.setText("정답은 1~99사이의 숫자입니다.");
					BuzData = 0;
				} else {
					realsum = 0;
					for(int i=0; i<sumArray.length;i++) realsum += sumArray[i];
					BuzData = 1;
					flag = 0;
					yesOrNo.setText("틀렸습니다.");
				}
				BuzzerControl(BuzData);
				vision2.setVisibility(View.VISIBLE);
			}
		});

		// clear를 눌렸을 경우 첫 줄을 제외한 모든 부분이 숨겨짐, 배열도 사라짐
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
				flag = -1;			//segment 대기상태로 되돌림
				realsum = 0;		//전에 더했었던 실제 배열의 합을 다시 0으로 초기화함
			}
		});
	}
	//부저를 일정 반복하며 울리게 하기 위해 쓰인 TimeTask, 매번 쓸때마다 재생성 하는 이유는
	//한번 중지하면 다시 켤 때 되살릴 수 없기 때문에 Clear후 다시 정답을 입력했을 때, 재생성하여 사용한다.
	//계속해서 BuzData가 1일떄는 0으로, 0일때는 1로 바꾸면서 소리를 낸다.
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

	// 일정하게 출력되게끔 의도적으로 반복하는 메소드, 200번 반복하여 사람 눈에는 계속 불이 들어와있는 것처럼 보인다.
	public void repeatSegmentControl(int value) {
		for (int i = 0; i < 200; i++)
			SegmentControl(value);
		return;
	}

	//결과값 표시의 대기상태 출력용, 대기상태때 썼던 segmentcode를 재활용하여 마치 사람눈에는
	//네모난 직사각형 박스가 계속해서 들어와있는것처럼 보인다.
	public void repeatSegmentControl_Box() {
		for (int i = 0; i < 50; i++) {
			SegmentControl(-211114);
			SegmentControl(-233334);
			SegmentControl(-111111);
			SegmentControl(-333333);
		}
	}

	// segment 출력을 담당하는 스레드
	class BackThread extends Thread {
		// flag : -1 -> 대기상태, 1 -> 답이 맞았을때, 0 -> 틀렸을때
		public void run() {
			while (true) {
				SegmentIOContrl(0); 	// 시간은 쓸일이 없으므로 0으로 고정한다.
				while (flag == 1) {		// 답이 맞았을 때
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
						//위 for문에서 3번 반복한다. 만약 갑자기 clear버튼이 들어오면, 완전히 루프문을 빠져나오기 위해 true로
						//바꾼 후, 아래서 false로 초기화하고 반복문을 완전히 정지시킨다.
						//간단히 말해서, Clear버튼이 눌러지면 즉각적으로 멈춘다.
						
						//위와 같은 원리로 즉각적으로 멈출 수 있도록 exitOuterLoop를 끼워넣고, 정답 숫자를 출력한다.
						for(int i=0; i<6; i++)
						{
							repeatSegmentControl(sum+1000000);
							//sum+1000000을 함으로써 드라이버는 합을 출력하는 상태로 받아들여 설정한대로 출력한다.(한칸씩 이동)
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
				//틀렸을떄의 동작, 2 xx 00 xx 와 같은 숫자로 들어간다.
				//앞의 xx는 사용자가 입력한 값이고, 뒤 xx는 실제 정답값이다.
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
					//모양이 계속 바뀌는 대기를 계속해서 반복하면서 출력해야 하므로, switch문으로 
					//index가 하나씩 증가하면서 다음으로 넘어가는 방식으로 만들었다.
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
