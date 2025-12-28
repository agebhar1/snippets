package main

import (
	"context"
	"encoding/binary"
	"fmt"
	"log"
	"net"
	"os"
	"os/signal"
	"sync"
	"sync/atomic"
	"syscall"
	"time"
	"unsafe"

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

const SockAddr = "/tmp/echo.sock"

type timespec struct {
	Seconds int64
	Nanos   int64
}

type uid struct {
	Ts  timespec
	Key [16]byte
}

func (u uid) String() string {
	return fmt.Sprintf("{ ts: %s, key: %x }", time.Unix(u.Ts.Seconds, u.Ts.Nanos).Format(time.RFC3339Nano), u.Key)
}

type request struct {
	Cmd   byte
	_     [7]byte
	Read  uid
	Write uid
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

func serve(c net.Conn, sp sdktrace.SpanProcessor, id int) {

	m := make(map[[16]byte]trace.SpanContext)

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithSampler(sdktrace.AlwaysSample()),
		sdktrace.WithSpanProcessor(sp),
		sdktrace.WithResource(resource.NewWithAttributes(semconv.SchemaURL, semconv.ServiceName(fmt.Sprintf("client#%d", id)))),
	)
	tr := tp.Tracer("tracer")

	defer func() {
		log.Printf("Client disconnected [#%d]", id)
		_ = c.Close()
		_ = tp.Shutdown(context.Background())
	}()

	log.Printf("Client connected [#%d]", id)
	var req request
	for {
		err := binary.Read(c, binary.LittleEndian, &req)
		if err != nil {
			break
		}
		log.Printf("cmd: %d, read: %s, write: %s, len: %d", req.Cmd, req.Read, req.Write, len(m))

		readTs := time.Unix(req.Read.Ts.Seconds, req.Read.Ts.Nanos)
		writeTs := time.Unix(req.Write.Ts.Seconds, req.Write.Ts.Nanos)

		ctx := context.Background()
		if sc, ok := m[req.Read.Key]; ok {
			ctx = trace.ContextWithRemoteSpanContext(ctx, sc)
		}

		_, span := tr.Start(ctx, "«spanName»", trace.WithTimestamp(readTs))
		span.SetStatus(codes.Ok, "")
		span.SetAttributes(attribute.String("read", fmt.Sprintf("%x", req.Read.Key)))
		span.SetAttributes(attribute.String("write", fmt.Sprintf("%x", req.Write.Key)))
		span.End(trace.WithTimestamp(writeTs))

		m[req.Write.Key] = span.SpanContext()

		log.Printf("span context: traceID: %s, spanID: %s", span.SpanContext().TraceID(), span.SpanContext().SpanID())
	}
}

func main() {
	log.Printf("unsafe.Sizeof(request{}): %d", unsafe.Sizeof(request{}))

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
		ids := 0
		for {
			c, err := l.Accept()
			if err != nil {
				if onShutdown.Load() {
					break
				}
				log.Fatalf("accept: %v", err)
			}
			ids += 1
			wg.Go(func() {
				<-shutdown
				// TODO already closed by client
				_ = c.Close()
			})
			wg.Go(func() { serve(c, sp, ids) })
		}
	})

	<-shutdown
	log.Println("Shutting down")
	_ = l.Close()
	_ = sp.RealShutdown(context.Background())

	wg.Wait()
}
