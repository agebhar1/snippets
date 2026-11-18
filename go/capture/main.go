package main

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"log"
	"os"
	"os/exec"
	"sync"
	"time"
)

func capture(r io.Reader, ch chan<- string) func() {
	// [bufio/scan.go] MaxScanTokenSize is the maximum size used to buffer a token
	// unless the user provides an explicit buffer with [Scanner.Buffer].
	// The actual maximum token size may be smaller as the buffer
	// may need to include, for instance, a newline.
	scanner := bufio.NewScanner(r)

	return func() {
		for scanner.Scan() {
			ch <- scanner.Text()
		}
		close(ch)
	}
}

func writeTo(prefix string, ch <-chan string, w io.Writer) func() {
	return func() {
		for str := range ch {
			_, _ = fmt.Fprint(w, prefix, str, "\n")
		}
	}
}

func tap(ch <-chan string, cb func(string)) <-chan string {
	cpy := make(chan string)
	go func() {
		for str := range ch {
			cb(str)
			cpy <- str
		}
		close(cpy)
	}()
	return cpy
}

func main() {

	if len(os.Args) < 2 {
		_, _ = fmt.Fprintln(os.Stderr, "ERROR: requires command")
		os.Exit(-1)
	}

	cmd := exec.CommandContext(context.Background(), os.Args[1], os.Args[2:]...)
	cmd.Env = os.Environ() // not required, for demo purpose

	cmdStdout, _ := cmd.StdoutPipe()
	cmdStderr, _ := cmd.StderrPipe()

	chOut := make(chan string)
	chErr := make(chan string)
	chLog := make(chan string)

	go func() {
		f, err := os.Create(fmt.Sprintf("%s.%d.log", os.Args[1], time.Now().UnixMilli()))
		if err != nil {
			log.Fatal(err)
		}
		for str := range chLog {
			_, _ = f.WriteString(fmt.Sprintf("%s\n", str))
		}
		_ = f.Close()
	}()
	logfile := func(str string) { chLog <- str }

	prefix := fmt.Sprintf("[%s] ", os.Args[1])

	wg := sync.WaitGroup{}
	wg.Go(capture(cmdStdout, chOut))
	wg.Go(writeTo(prefix, tap(chOut, logfile), os.Stdout))

	wg.Go(capture(cmdStderr, chErr))
	wg.Go(writeTo(prefix, tap(chErr, logfile), os.Stderr))

	_ = cmd.Start()
	wg.Wait()
	_ = cmd.Wait()

	close(chLog)

	os.Exit(cmd.ProcessState.ExitCode())
}
