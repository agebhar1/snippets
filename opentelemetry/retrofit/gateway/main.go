package main

import (
	"context"
	"encoding/binary"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"os/signal"
	"sync"
	"sync/atomic"
	"syscall"
	"time"

	"github.com/go-logr/stdr"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/codes"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.38.0"
	"go.opentelemetry.io/otel/trace"
)

// https://eli.thegreenplace.net/2019/unix-domain-sockets-in-go/
// https://opentelemetry.io/docs/concepts/context-propagation/
// https://github.com/open-telemetry/opentelemetry-go-contrib/tree/main/examples
// https://opentelemetry.io/docs/languages/go/

const SockAddr = "/tmp/opentelemetry.sock"

type ClientOpCode uint32

type LookUpKey [8 + 32]byte

const (
	ClientRead ClientOpCode = iota
	ClientWrite
)

type Timespec struct {
	Seconds int64
	Nanos   int64
}

type OpenTelemetryEvent struct {
	OpCode   ClientOpCode // size 4, offset 0
	QueueRaw [8 + 1]byte  // size 9, offset 4
	_        [3]byte      // size 3
	Ts       Timespec     // size 16, offset 16
	Key      [32]byte     // size 32, offset 32
}

func (e OpenTelemetryEvent) unixTs() time.Time {
	return time.Unix(e.Ts.Seconds, e.Ts.Nanos)
}

func (e OpenTelemetryEvent) lookUpKey() LookUpKey {
	var key LookUpKey
	copy(key[0:], e.QueueRaw[:8])
	copy(key[8:], e.Key[:])
	return key
}

func (e OpenTelemetryEvent) String() string {
	ts := time.Unix(e.Ts.Seconds, e.Ts.Nanos)
	return fmt.Sprintf("OpCode: %d, Queue: '%s', Ts: %s, Key: %x", e.OpCode, string(e.QueueRaw[:]), ts.Format(time.RFC3339Nano), e.Key)
}

type SharedSpanProcessor struct {
	delegate sdktrace.SpanProcessor
}

func (p SharedSpanProcessor) RealShutdown(ctx context.Context) error {
	log.Println("Shutting down SharedSpanProcessor")
	return p.delegate.Shutdown(ctx)
}

func (p SharedSpanProcessor) OnStart(parent context.Context, s sdktrace.ReadWriteSpan) {
	p.delegate.OnStart(parent, s)
}

func (p SharedSpanProcessor) OnEnd(s sdktrace.ReadOnlySpan) {
	defer p.delegate.OnEnd(s)
}

func (p SharedSpanProcessor) Shutdown(ctx context.Context) error {
	log.Println("NoOp Shutting down SharedSpanProcessor")
	return nil
}

func (p SharedSpanProcessor) ForceFlush(ctx context.Context) error {
	return p.delegate.ForceFlush(ctx)
}

var kv = make(map[LookUpKey]trace.SpanContext)

func serve(conn net.Conn, sp sdktrace.SpanProcessor) {

	var size uint64 // ssize_t
	if binary.Read(conn, binary.LittleEndian, &size) != nil {
		return
	}
	buff := make([]byte, size)
	if _, err := io.ReadFull(conn, buff); err != nil {
		return
	}
	serviceName := string(buff)

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithSampler(sdktrace.AlwaysSample()),
		sdktrace.WithSpanProcessor(sp),
		sdktrace.WithResource(resource.NewWithAttributes(semconv.SchemaURL, semconv.ServiceName(serviceName))),
	)
	tr := tp.Tracer("")

	defer func() {
		log.Printf("Client disconnected [%s]", serviceName)
		_ = conn.Close()
		_ = tp.Shutdown(context.Background())
	}()

	log.Printf("Client connected [%s]", serviceName)
	var event OpenTelemetryEvent
	var read *OpenTelemetryEvent = nil
	for {
		err := binary.Read(conn, binary.LittleEndian, &event)
		if err != nil {
			break
		}

		switch event.OpCode {
		case ClientRead:
			read = new(event)
			log.Printf("[%s] read: {%s}", serviceName, event)

		case ClientWrite:
			log.Printf("[%s] read: {%s}, write: {%s}", serviceName, read, event)

			write := &event
			ctx := context.Background()
			if read == nil {
				_, span := tr.Start(ctx, fmt.Sprintf("produce %s", string(write.QueueRaw[:])), trace.WithTimestamp(write.unixTs()))
				span.SetStatus(codes.Ok, "")
				span.SetAttributes(attribute.String("write", fmt.Sprintf("%x", write.lookUpKey())))
				span.End(trace.WithTimestamp(write.unixTs()))

				kv[write.lookUpKey()] = span.SpanContext()
			} else {
				if sc, ok := kv[read.lookUpKey()]; ok {
					ctx = trace.ContextWithRemoteSpanContext(ctx, sc)
				}
				_, span := tr.Start(ctx, fmt.Sprintf("produce %s", string(write.QueueRaw[:])), trace.WithTimestamp(read.unixTs()))
				span.SetStatus(codes.Ok, "")
				span.SetAttributes(attribute.String("read", fmt.Sprintf("%x", read.lookUpKey())))
				span.SetAttributes(attribute.String("write", fmt.Sprintf("%x", write.lookUpKey())))
				span.End(trace.WithTimestamp(write.unixTs()))

				kv[write.lookUpKey()] = span.SpanContext()
			}
		}
	}
}

func main() {
	stdr.SetVerbosity(5)

	// https://pkg.go.dev/go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp#pkg-overview
	exp, err := otlptracehttp.New(context.Background(), otlptracehttp.WithInsecure())
	if err != nil {
		log.Fatal(err)
	}
	sp := SharedSpanProcessor{delegate: sdktrace.NewBatchSpanProcessor(exp)}

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
				log.Fatalf("accept: %v", err)
			}
			wg.Go(func() {
				<-shutdown
				// TODO already closed by client
				_ = conn.Close()
			})
			wg.Go(func() { serve(conn, sp) })
		}
	})

	<-shutdown
	log.Println("Shutting down")
	_ = l.Close()
	_ = sp.RealShutdown(context.Background())

	wg.Wait()
	if err := os.RemoveAll(SockAddr); err != nil {
		log.Println(err)
	}
}
