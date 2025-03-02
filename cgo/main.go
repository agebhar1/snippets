package main

/*
#include <stdint.h> // fort uintptr_t
*/
import "C"
import (
	"fmt"
	"math/rand"
	"runtime/cgo"
	"strings"
	"time"
)

type MyStruct struct {
	seed  int64
	calls uint
}

//export NewMyStruct
func NewMyStruct() C.uintptr_t {
	ms := &MyStruct{
		seed: rand.Int63(),
	}
	return C.uintptr_t(cgo.NewHandle(ms))
}

//export DeleteHandles
func DeleteHandles(handles []C.uintptr_t) {
	for _, h := range handles {
		h := cgo.Handle(h)
		val := h.Value().(*MyStruct)
		fmt.Printf("DeleteHandles %#v\n", val)
	}
}

//export DeleteHandle
func DeleteHandle(handle C.uintptr_t) {
	h := cgo.Handle(handle)
	val := h.Value().(*MyStruct)
	fmt.Printf("DeleteHandle %#v\n", val)
	h.Delete()
}

//export MyStructFunction
func MyStructFunction(handle C.uintptr_t) {
	h := cgo.Handle(handle)
	val := h.Value().(*MyStruct)
	val.calls++
	fmt.Printf("MyStructFunction %#v\n", val)
}

//export GoRun
func GoRun() {
	go func() {
		for {
			fmt.Println("GoRun")
			time.Sleep(time.Duration(rand.Intn(1000)) * time.Millisecond)
		}
	}()
}

//export IsBlank
func IsBlank(arg string) bool {
	return len(strings.TrimSpace(arg)) == 0
}

func main() {}
