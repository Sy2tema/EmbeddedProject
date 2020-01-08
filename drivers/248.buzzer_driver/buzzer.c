#include	<linux/init.h>
#include	<linux/module.h>
#include	<linux/kernel.h>
#include	<linux/ioport.h>
#include	<linux/fs.h>
#include	<asm/io.h>
#include	<asm/uaccess.h>

#define		DRIVER_AUTHOR		"hanback"
#define		DRIVER_DESC		"buzzer test program"
#define		BUZZER_MAJOR		248
#define		BUZZER_NAME		"BUZZOR IO PORT"
#define		BUZZER_MODULE_VERSION	"BUZZER IO PORT V0.1"
#define		BUZZER_ADDRESS 		0x88000050
#define		BUZZER_ADDRESS_RANGE	0x1000

//Global variable
static int buzzer_usage	=	0;
static unsigned long *buzzer_ioremap;

//define function...
//응용 프로그램에서 디바이스를 처음 사용하는 경우를 처리하는 함수
int buzzer_open(struct inode *minode, struct file *mfile)
{
	//디바이스가 열려있는지 확인
	if(buzzer_usage != 0)	return -EBUSY;

	//buzzer의 가상 주소 매핑
	buzzer_ioremap = ioremap(BUZZER_ADDRESS, BUZZER_ADDRESS_RANGE);

	//등록할 수 있는 I/O영역인지 확인
	if(!check_mem_region((unsigned long)buzzer_ioremap, BUZZER_ADDRESS_RANGE)){
		//I/O 메모리 영역을 등록
		request_mem_region((unsigned long)buzzer_ioremap, BUZZER_ADDRESS_RANGE, BUZZER_NAME);
	}
	else
		printk(KERN_WARNING"Can't get I/O Region 0x%x\n", (unsigned int)buzzer_ioremap);

	buzzer_usage =	1;
	return 0;
}

//응용 프로그램에서 디바이스를 더이상 사용하지 않아서 닫기를 구현하는 경우
int buzzer_release(struct inode *minode, struct file *mfile)
{
	//매핑된 가상주소를 해제
	iounmap(buzzer_ioremap);

	//등록된 I/O 메모리 영역을 해제
	release_mem_region((unsigned long)buzzer_ioremap, BUZZER_ADDRESS_RANGE);

	buzzer_usage =	0;
	return 0;
}

//디바이스 드라이버의 쓰기를 구현하는 함수
ssize_t buzzer_write_byte(struct file *inode, const char *gdata, size_t length, loff_t *off_what)
{
	unsigned char *addr;
	unsigned char	c;

	//gdata의 사용자 공간의 메모리에서 c에 읽어온다.
	get_user(c,gdata);

	addr = (unsigned char *)(buzzer_ioremap);
	*addr = c;

	return length;
}

//파일 오퍼레이션 구조체
//파일을 열 떄 open()을 사용한다.
static struct file_operations buzzer_fops = {
	.owner		= THIS_MODULE,
	.open		= buzzer_open,
	.write		= buzzer_write_byte,
	.release	= buzzer_release,
};

//모듈을 커널 내부로 삽입
int buzzer_init(void)
{
	int result;

	//문자 디바이스 드라이버를 등록한다.
	result = register_chrdev(BUZZER_MAJOR, BUZZER_NAME, &buzzer_fops);

	if(result < 0) {//등록실패
		printk(KERN_WARNING"Can't get any major\n");
		return result;
	}

	//major번호를 출력한다
	printk(KERN_WARNING"Init module, Buzzer Major number : %d\n",BUZZER_MAJOR);
	return 0;
}

//모듈을 커널에서 제거
void buzzer_exit(void)
{
	//문자 디바이스 드라이버를 제거한다
	unregister_chrdev(BUZZER_MAJOR, BUZZER_NAME);

	printk(KERN_INFO"driver: %s DRIVER EXIT\n", BUZZER_NAME);
}

module_init(buzzer_init);
module_exit(buzzer_exit);

MODULE_AUTHOR(DRIVER_AUTHOR);
MODULE_DESCRIPTION(DRIVER_DESC);
MODULE_LICENSE("Dual BSD/GPL");
