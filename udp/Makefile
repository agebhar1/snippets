.PHONY: clean all

all: client server

%: %.o
	$(CC) -o $@ $<

clean:
	- rm client server *.o
