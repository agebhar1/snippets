all: worker server

worker: worker.c
	gcc -Wall $< -o $@

server: server.c
	gcc -Wall $< -o $@	

.PHONY: clean
clean:
	-rm worker server *.log
