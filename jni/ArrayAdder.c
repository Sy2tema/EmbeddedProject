#include	<string.h>
#include	<jni.h>
#include	<stdio.h>
#include	<stdlib.h>
#include	<fcntl.h>
#include	<unistd.h>
#include	<termios.h>
#include	<sys/mman.h>
#include	<errno.h>
#include	<android/log.h>
#include	<time.h>


//jintArray sumArray =NULL;
//buzzer Control
jint
Java_ac_kr_kgu_esproject_ArrayAdderActivity_BuzzerControl(JNIEnv* env, jobject thiz, jint value)
{
	int	fd, ret;
	int	data = value;

	fd = open("/dev/buzzer",O_WRONLY);

	if(fd < 0)	return -errno;

	ret	= write(fd, &data, 1);
	close(fd);

	if(ret == 1)	return 0;
	return -1;
}

//SegmentControl
jint
Java_ac_kr_kgu_esproject_ArrayAdderActivity_SegmentControl(JNIEnv* env, jobject thiz, jint data)
{
	int	dev, ret;

	dev = open("dev/segment", O_RDWR | O_SYNC);
	if(dev != -1)
	{
		ret = write(dev, &data, 4);
		close(dev);
	}
	else
	{
		__android_log_print(ANDROID_LOG_ERROR, "SegmentActivity", "Device Open ERROR!\n");
		exit(1);
	}
	return 0;
}


//SegmentIOControl
jint
Java_ac_kr_kgu_esproject_ArrayAdderActivity_SegmentIOContrl(JNIEnv* env, jobject thiz, jint data)
{
	int	dev, ret;

	dev = open("/dev/segment", O_RDWR | O_SYNC);

	if(dev != -1)
	{
		ret = ioctl(dev, data, NULL, NULL);
		close(dev);
	}
	else
	{
		__android_log_print(ANDROID_LOG_ERROR, "SegmentActivity", "Device Open ERROR!\n");
		exit(1);
	}
	return 0;
}

//CreateArray 입력받은 배열 value값만큼의 길이의 배열을 만들어서
//랜덤으로 숫자를 넣고, jintArray sumArray를 반환한다.
jintArray
Java_ac_kr_kgu_esproject_ArrayAdderActivity_CreateArray(JNIEnv* env, jobject thiz, jint value)
{
	int i;

	jintArray sumArray =  (*env)->NewIntArray(env, value);		//value만큼 길이의 배열을 생성한다.
	jint * array = (*env)->GetIntArrayElements(env, sumArray, 0);	//sumArray의 배열요소들을 변경하기 위한 작업
	//직접적으로 sumArray를 건드리는게 아닌, array를 이용해서 sumArray의 배열요소들을 변경한다.

	srand(time(NULL));
	for(i = 0; i < value; i++)
	{
		array[i] = (rand() % 9) + 1;	//1~9까지의 숫자를 출력한다.
	}
	(*env)->ReleaseIntArrayElements(env, sumArray, array, 0);

	return sumArray;
}

//입력받은 배열 sumArray와 사용자가 입력한 결과값 value를 입력받아 합을 비교한 뒤,
//결과값을 return하는 함수, 정답은 1을 리턴하고 오답일시 0을 리턴한다.
jint
Java_ac_kr_kgu_esproject_ArrayAdderActivity_CompareArray(JNIEnv* env, jobject thiz, jint value, jintArray sumArray)
{
	int sum = 0;
	int i;
	jsize len = (*env)->GetArrayLength(env, sumArray);		//배열의 길이를 저장하는 len변수
	jint * array = (*env)->GetIntArrayElements(env, sumArray, 0);	//sumArray의 요소들을 받아오기 위한 작업
	for(i = 0; i < len; i++)
	{	//sumArray의 길이만큼 요소들을 받아와 sum에 저장한다.
		sum += array[i];
	}
	(*env)->ReleaseIntArrayElements(env, sumArray, array, 0);
	if( value == sum )
		return 1;
	return 0;
}
