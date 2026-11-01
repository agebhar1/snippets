```
make
./main $((1024 * 37)) $((1024 * 40))
# set virtual memory limit: soft: 41943040, hard: 41943040
# virtual memory limit: soft: 41943040, hard: 41943040 (RLIM_INFINITY=18446744073709551615)
# allocated 38797312 bytes at 0x7532530ff010
# press enter to continue and free the memory...

./main $((1024 * 38)) $((1024 * 40))
# set virtual memory limit: soft: 41943040, hard: 41943040
# virtual memory limit: soft: 41943040, hard: 41943040 (RLIM_INFINITY=18446744073709551615)
# malloc: Cannot allocate memory

( ulimit -v $((1024 * 40)) && ./main $((1024 * 38)) )
# virtual memory limit: soft: 41943040, hard: 41943040 (RLIM_INFINITY=18446744073709551615)
# malloc: Cannot allocate memory

( ulimit -v $((1024 * 40)) && ./main $((1024 * 37)) )
# virtual memory limit: soft: 41943040, hard: 41943040 (RLIM_INFINITY=18446744073709551615)
# allocated 38797312 bytes at 0x7f1c790ff010
# press enter to continue and free the memory...
```