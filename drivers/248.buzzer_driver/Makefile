CC	= /usr/local/arm/arm-2010q1/bin/arm-none-eabi-gcc

obj-m := buzzer.o

KDIR := /Android/linux-2.6.32-hanback

PWD := $(shell pwd)

default:
	$(MAKE) -C $(KDIR) SUBDIRS=$(PWD) modules
	rm -f default

clean:
	rm -f *.ko
	rm -f *.o
	rm -f *.mod.*
	rm -f .*.cmd
