package main

import (
	"encoding/binary"
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"os/signal"
	"sync"
	"sync/atomic"
	"syscall"
)

var SockAddr = "main.sock"

func serve(size uint, conn net.Conn, req chan []byte) {
	r := uint(0)
	defer func() {
		log.Printf("client disconnected, received: %d (%d bytes)", r, r*size)
		_ = conn.Close()
	}()

	log.Printf("client connected")

	buffer := make([]byte, size)
	for {
		if err := binary.Read(conn, binary.LittleEndian, buffer); err != nil {
			break
		}
		r++
		req <- buffer
	}
}

func main() {

	size := flag.Uint("size", 72, "size of buffer")
	flag.Parse()

	if err := os.RemoveAll(SockAddr); err != nil {
		log.Fatal(err)
	}

	l, err := net.Listen("unix", SockAddr)
	if err != nil {
		log.Fatal(err)
	}

	sigs := make(chan os.Signal, 1)
	signal.Notify(sigs, syscall.SIGINT, syscall.SIGTERM)

	onShutdown := atomic.Bool{}
	onShutdown.Store(false)

	shutdown := make(chan struct{})
	go func() {
		<-sigs
		onShutdown.Store(true)
		close(shutdown)
	}()

	req := make(chan []byte)

	wg := sync.WaitGroup{}
	wg.Go(func() {
		for {
			select {
			case <-shutdown:
				return
			case <-req:
				// process
			}
		}
	})
	wg.Go(func() {
		for {
			conn, err := l.Accept()
			if err != nil {
				if onShutdown.Load() {
					break
				}
				log.Fatalf("failed to accept connection: %v", err)
			}
			wg.Go(func() {
				<-shutdown
				_ = conn.Close()
			})
			wg.Go(func() { serve(*size, conn, req) })
		}
	})

	fmt.Printf("listening on: %s\n", l.Addr())
	fmt.Printf("buffer size : %d\n", *size)

	<-shutdown
	log.Println("Shutting down")
	_ = l.Close()

	wg.Wait()
	close(req)
	if err := os.RemoveAll(SockAddr); err != nil {
		log.Println(err)
	}
}
