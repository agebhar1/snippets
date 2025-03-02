all: main

main: main.c libtinker.a
	gcc -I=. main.c libtinker.a -o $@

libtinker.a: main.go
	go build -buildmode=c-archive -o $@ $<

libtinker.so: main.go
	go build -buildmode=c-shared -o $@ $<

.PHONY: clean
clean:
	-rm main
	-rm libtinker.*
