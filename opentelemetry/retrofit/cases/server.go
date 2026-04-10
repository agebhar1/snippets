package main

import (
	"encoding/binary"
	"log"
	"net"
	"os"
	"os/signal"
	"sync"
	"sync/atomic"
	"syscall"
)

var SockAddr = "main.sock"

func serve(conn net.Conn, shutdown <-chan struct{}) {
	defer func() {
		log.Printf("client disconnected")
		_ = conn.Close()
	}()

	log.Printf("client connected")

	buffer := make([]byte, 64)
	for {
		if err := binary.Read(conn, binary.LittleEndian, buffer); err != nil {
			break
		}
		//log.Printf("client data received: %d bytes", len(buffer))
	}
}

func main() {

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

	wg := sync.WaitGroup{}
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
			wg.Go(func() { serve(conn, shutdown) })
		}
	})

	<-shutdown
	log.Println("Shutting down")
	_ = l.Close()

	wg.Wait()
	if err := os.RemoveAll(SockAddr); err != nil {
		log.Println(err)
	}
}
