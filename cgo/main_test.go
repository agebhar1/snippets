package main

import "testing"

func TestIsBlank(t *testing.T) {
	type args struct {
		arg string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "IsBlank",
			args: args{arg: ""},
			want: true,
		},
		{
			name: "IsBlank",
			args: args{arg: "  "},
			want: true,
		},
		{
			name: "IsBlank",
			args: args{arg: " -=- "},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsBlank(tt.args.arg); got != tt.want {
				t.Errorf("IsBlank() = %v, want %v", got, tt.want)
			}
		})
	}
}
