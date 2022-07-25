// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
// vespa logfmt command
// Author: arnej

package logfmt

import (
	"fmt"
	"os"
	"strings"
)

type flagValueForLevel struct {
	levels map[string]bool
	changed bool
}

func defaultLevelFlags() map[string]bool {
	return map[string]bool{
		"fatal": true,
		"error": true,
		"warning": true,
		"info": true,
		"config": false,
		"event": false,
		"debug": false,
		"spam": false,
	}
}

func (v *flagValueForLevel) Type() string {
	return "level flags"
}

func (v *flagValueForLevel) String() string {
	mv := v.levels
	var buf strings.Builder
	buf.WriteString("level flags:")
	for flag, active := range(mv) {
		if (active) {
			buf.WriteString(" +")
		} else {
			buf.WriteString(" -")
		}
		buf.WriteString(flag)
	}
	return buf.String()
}

func (v *flagValueForLevel) Set(val string) error {
	fmt.Fprintln(os.Stdout, "v set: ", val)
	minus := strings.HasPrefix(val, "-")
	plus := strings.HasPrefix(val, "+")
	if !v.changed {
		fmt.Println("init v.levels")
		if minus == false && plus == false {
			for k,_ := range(v.levels) {
				v.levels[k] = false
			}
		}
	}
	toShow := !minus
	fmt.Fprintln(os.Stdout, "split", val)
	for _, k := range(strings.Split(val, ",")) {
		if suppress, minus := trimPrefix(k, "-"); minus {
			k = suppress
			toShow = false
		}
		if surface, plus := trimPrefix(k, "+"); plus {
			k = surface
			toShow = true
		}
		if k == "all" {
			for k,_ := range(v.levels) {
				fmt.Println("v.levels", k, ":=", toShow)
				v.levels[k] = toShow
			}			
		} else if _, ok := v.levels[k]; !ok {
			return fmt.Errorf("not a valid show flag: '%s'", k)
		} else {
			fmt.Println("v.levels", k, ":=", toShow)
			v.levels[k] = toShow
		}
	}
	return nil
}
