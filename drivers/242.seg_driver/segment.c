#include	<linux/init.h>
#include	<linux/module.h>
#include	<linux/kernel.h>
#include	<linux/fs.h>
#include	<linux/errno.h>
#include	<linux/types.h>
#include	<asm/fcntl.h>
#include	<linux/ioport.h>
#include	<linux/delay.h>

#include	<asm/ioctl.h>
#include	<asm/uaccess.h>
#include	<asm/io.h>

#define		DRIVER_AUTHOR		"hanback"
#define		DRIVER_DESC		"7-Segment program"

#define		SEGMENT_MAJOR		242
#define		SEGMENT_NAME		"SEGMENT"
#define		SEGMENT_MODULE_VERSION	"SEGMENT PORT V0.1"

#define		SEGMENT_ADDRESS_GRID	0x88000030
#define		SEGMENT_ADDRESS_DATA	0x88000032
#define		SEGMENT_ADDRESS_1	0x88000034
#define		SEGMENT_ADDRESS_RANGE	0x1000
#define		MODE_0_TIMER_FORM	0x0
#define		MODE_1_CLOCK_FORM	0x1

static unsigned int segment_usage	 = 0;
static unsigned long *segment_data;
static unsigned long *segment_grid;
static int mode_select			 = 0x0;
static int index			= 0;
static int index2			= 0;

int
segment_open(struct inode *inode, struct file *filp)
{
	if( segment_usage != 0 )		return -EBUSY;
	//가상주소 맵핑
	segment_grid	= ioremap(SEGMENT_ADDRESS_GRID, SEGMENT_ADDRESS_RANGE);
	segment_data	= ioremap(SEGMENT_ADDRESS_DATA, SEGMENT_ADDRESS_RANGE);

	if(!check_mem_region((unsigned long)segment_data, SEGMENT_ADDRESS_RANGE)
		&& !check_mem_region((unsigned long)segment_grid, SEGMENT_ADDRESS_RANGE))
	{
		request_region((unsigned long)segment_grid, SEGMENT_ADDRESS_RANGE, SEGMENT_NAME);
		request_region((unsigned long)segment_data, SEGMENT_ADDRESS_RANGE, SEGMENT_NAME);
	}
	else	printk("driver: unable to register this!!!!\n");

	segment_usage = 1;
	return 0;
}

int
segment_release(struct inode *inode, struct file *filp)
{
	iounmap(segment_grid);
	iounmap(segment_data);

	release_region((unsigned long)segment_data, SEGMENT_ADDRESS_RANGE);
	release_region((unsigned long)segment_grid, SEGMENT_ADDRESS_RANGE);

	segment_usage = 0;
	return 0;
}

unsigned short
Getsegmentcode(short x)
{
	unsigned short code;
	switch(x)
	{	//case 0x0~0x9 : 일반 숫자 출력용
		case 0x0 : code = 0xfc;		break;
                case 0x1 : code = 0x60;		break;
                case 0x2 : code = 0xda;		break;
                case 0x3 : code = 0xf2;		break;
                case 0x4 : code = 0x66;		break;
                case 0x5 : code = 0xb6;         break;
                case 0x6 : code = 0xbe;         break;
                case 0x7 : code = 0xe4;         break;
                case 0x8 : code = 0xfe;         break;
                case 0x9 : code = 0xf6;         break;
		//case -1~-6 : 임의로 만든 segment 모양 출력용
                case -1 : code = 0x80;		break;
		case -2 : code = 0x0C;		break;
		case -3 : code = 0x10;		break;
		case -4 : code = 0x60;		break;
		case -5 : code = 0x00;		break;
		case -6 : code = 0x02;		break;
		default : code = 0;		break;
	}
	return code;
}

ssize_t
segment_write(struct file *inode, const char *gdata, size_t length, loff_t *off_what)
{
	unsigned char		data[6];
	unsigned char		digit[6] = {0x20, 0x10, 0x08, 0x04, 0x02, 0x01};
	int			i, num, ret;
	int			count = 0, temp1, temp2;
	int			temp3=0;
	int			temp4=0;
	int			temp5=0;
	ret	= copy_from_user(&num, gdata, 4);
	count	= num;
	printk("!!! if num < 0 count is %d\n", count);
	//음수일때, temp3는 count에 -1을 곱해 나머지 연산에서 문제가 없도록 만들고,
	//Getsegmentcode 함수 호출시, -1을 또 의도적으로 곱해 음수로 만든 후, 미리 지정해놓은
	//음수일때의 Segment모양을 불러온다.
	if(num<0)
	{
		temp3 = count * (-1);
		data[5] = Getsegmentcode(temp3 / 100000 * -1);
                temp1   = temp3 % 100000;
                data[4] = Getsegmentcode(temp1 / 10000 * -1);
                temp2   = temp1 % 10000;
                data[3] = Getsegmentcode(temp2 / 1000* -1);
                temp1   = temp2 % 1000;
                data[2] = Getsegmentcode(temp1 / 100* -1);
                temp2   = temp1 % 100;
                data[1] = Getsegmentcode(temp2 / 10 * -1);
                data[0] = Getsegmentcode(temp2 % 10 * -1);

		switch (mode_select)
                {
                        case MODE_0_TIMER_FORM:         break;
                        case MODE_1_CLOCK_FORM:
                                data[4] += 1;
                                data[2] += 1;
                                break;
                }
		for(i=0; i<6; i++)
                {
                        *segment_grid   = digit[i];
                        *segment_data   = data[i];
                      	mdelay(1);
                }
	}
	//100만보다 큰 경우(999999를 벗어난 경우), segment에서 사용하지 않는다는 점을 활용해서
	//특수하게 출력해야 하는경우를 100만 이상일때로 정함. 숫자가 들어오면, 100만의 나머지연산을 이용해 원래 출력하고자 하는 수를
	//가져온다. 한자리수일때, 2자리수일때를 나누어서 출력한다.
	//이 부분은 정답일 때, 한칸씩 움직이는 부분의 정답이다.
	else if(num > 1000000 && num < 2000000)
	{
		for(i=0; i < 6; i++)	data[i] = 0x00;
		temp3 = count;
		temp3 = temp3 % 1000000;


		if(temp3 < 10){
			switch(index){
				case 0 : 	data[0] = Getsegmentcode(temp3);	index2++;      	 break;
				case 1 :	data[1] = Getsegmentcode(temp3);        index2++;        break;
                                case 2 :        data[2] = Getsegmentcode(temp3);        index2++;        break;
                                case 3 :        data[3] = Getsegmentcode(temp3);        index2++;        break;
                                case 4 :        data[4] = Getsegmentcode(temp3);        index2++;        break;
                                case 5 :        data[5] = Getsegmentcode(temp3);        index2++;	 break;
			}
			if(index2 % 200 == 0 && index2 != 0)	index++;
			if(index2 == 1200){index = 0; index2 = 0;}
		}
		//2자리 수의 정답이 출력되는 부분
		else
		{
			temp4 = temp3 / 10;
	                temp3 = temp3 % 10;
			switch(index){
                                case 0 :        data[0] = Getsegmentcode(temp3); data[1] = Getsegmentcode(temp4);        index2++;        break;
                                case 1 :        data[1] = Getsegmentcode(temp3); data[2] = Getsegmentcode(temp4);        index2++;        break;
                                case 2 :        data[2] = Getsegmentcode(temp3); data[3] = Getsegmentcode(temp4);        index2++;        break;
                                case 3 :        data[3] = Getsegmentcode(temp3); data[4] = Getsegmentcode(temp4);        index2++;        break;
                                case 4 :        data[4] = Getsegmentcode(temp3); data[5] = Getsegmentcode(temp4);        index2++;        break;
                                case 5 :        data[5] = Getsegmentcode(temp3);        index2++;        break;
                        }
                        if(index2 % 200 == 0 && index2 != 0)    index++;
                        if(index2 == 1200){index = 0; index2 = 0;}
		}

	      	switch (mode_select)
              	{
              		case MODE_0_TIMER_FORM:         break;
              		case MODE_1_CLOCK_FORM:
                		data[4] += 1;
                       		data[2] += 1;
                       		break;
                }
                for(i=0; i<6; i++)
                {
                        *segment_grid   = digit[i];
                        *segment_data   = data[i];
                        mdelay(1);
                }

	}
	//틀렸을 때, 출력되는 부분이다. SegmentControl(2xx00xx, x=1~99)
	//사용자 입력값 - 결과값
	else if(num > 2000000 && num < 3000000)
	{
		for(i=0; i<6; i++)	data[i]=0x00;	//초기화
		temp3 = count;
		temp3 = temp3 % 2000000; 	//이 연산으로 xx00xx와 같은 숫자가 된다.
		temp1 = temp3 / 10000;		//이 연산으로 xx00xx중 앞 xx(입력값)을 temp1에 저장한다.
		temp2 = temp3 % 10000;		//이 연산으로 xx00xx중 뒤 xx(정답)을 temp2에 저장한다.

		if(temp1 / 10 == 0)	temp4 = 0x00;
		else			temp4 = Getsegmentcode(temp1 / 10);
		//만약 temp1(정답)이 1의 자리일때, 10의 자리 부분에 0x00코드(아무것도 출력안함)을 입력한다.

		if(temp2 / 10 == 0)	temp5 = 0x00;
		else			temp5 = Getsegmentcode(temp2 / 10);
		//만약 temp2(사용자입력값)이 1의 자리일때, 10의 자리 부분에 0x00코드를 입력한다.


		switch (index)
		{
                	case 0 :        data[0] = Getsegmentcode(temp1 % 10);
					data[1] = temp4; data[2] = Getsegmentcode(-6);
					data[3] = Getsegmentcode(temp2 % 10);
					data[4] = temp5;
					index2++;        break;
                       	case 1 :        data[1] = Getsegmentcode(temp1 % 10);
                                        data[2] = temp4; data[3] = Getsegmentcode(-6);
                                        data[4] = Getsegmentcode(temp2 % 10);
                                        data[5] = temp5;
                                        index2++;        break;
                        case 2 :	data[2] = Getsegmentcode(temp1 % 10);
					data[3] = temp4; data[4] = Getsegmentcode(-6);
					data[5] = Getsegmentcode(temp2 % 10);
                                        index2++;        break;
			case 3 :	data[3] = Getsegmentcode(temp1 % 10);
					data[4] = temp4; data[5] = Getsegmentcode(-6);
                                        index2++;        break;
			case 4 :	data[4] = Getsegmentcode(temp1 % 10);
					data[5] = temp4;
                                        index2++;        break;
			case 5 :	data[5] = Getsegmentcode(temp1 % 10);
                                        index2++;        break;
		}
		if(index2 % 200 == 0 && index2 != 0)    index++;
                if(index2 == 1200){index = 0; index2 = 0;}

		switch (mode_select)
	        {
                        case MODE_0_TIMER_FORM:         break;
                        case MODE_1_CLOCK_FORM:
                                data[4] += 1;
                                data[2] += 1;
                                break;
                }
                for(i=0; i<6; i++)
                {
                        *segment_grid   = digit[i];
                        *segment_data   = data[i];
                        mdelay(1);
                }

	}
	//index초기화용
	else if(num == 3000000)
	{
		index = 0; index2 = 0;
	}

	//일반 숫자가 출력되는 부분이다. SegmentControl(1~999999)
	else if(num > 0 && num < 1000000)
	{
		data[5] = Getsegmentcode(count / 100000);
		temp1	= count % 100000;
		data[4] = Getsegmentcode(temp1 / 10000);
		temp2 	= temp1 % 10000;
		data[3] = Getsegmentcode(temp2 / 1000);
		temp1	= temp2 % 1000;
		data[2] = Getsegmentcode(temp1 / 100);
		temp2	= temp1 % 100;
		data[1] = Getsegmentcode(temp2 / 10);
		data[0] = Getsegmentcode(temp2 % 10);

		switch (mode_select)
		{
			case MODE_0_TIMER_FORM:		break;
			case MODE_1_CLOCK_FORM:
				data[4] += 1;
				data[2] += 1;
				break;
		}
		for(i=0; i<6; i++)
		{

			*segment_grid	= digit[i];
			*segment_data	= data[i];
			mdelay(1);
		}

	}
	*segment_grid   = ~digit[0];
	*segment_data   = 0;



	return length;

}
static int
segment_ioctl(struct inode *inode, struct file *filp, unsigned int cmd, unsigned long arg)
{
	switch(cmd)
	{
		case MODE_0_TIMER_FORM:
			mode_select = 0x00;
			break;
		case MODE_1_CLOCK_FORM:
			mode_select = 0x01;
			break;
		default:
			return -EINVAL;
	}
	return 0;
}

struct file_operations segment_fops =
{
	.owner		= THIS_MODULE,
	.open		= segment_open,
	.write		= segment_write,
	.release	= segment_release,
	.ioctl		= segment_ioctl,
};

int
segment_init(void)
{
	int result;

	result = register_chrdev(SEGMENT_MAJOR, SEGMENT_NAME, &segment_fops);
	if (result < 0)
	{
		printk(KERN_WARNING"Can't get any major\n");
		return result;
	}

	printk(KERN_INFO"Init Module, 7-Segment Majot Number : %d\n", SEGMENT_MAJOR);
	return 0;
}

void
segment_exit(void)
{
	unregister_chrdev(SEGMENT_MAJOR, SEGMENT_NAME);
	printk("driver: %s DRIVER EXIT\n", SEGMENT_NAME);
}

module_init(segment_init);
module_exit(segment_exit);

MODULE_AUTHOR(DRIVER_AUTHOR);
MODULE_DESCRIPTION(DRIVER_DESC);
MODULE_LICENSE("Dual BSD/GPL");
