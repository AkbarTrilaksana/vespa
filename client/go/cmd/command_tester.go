// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
// A helper for testing commands
// Author: bratseth

package cmd

import (
	"bytes"
	"io/ioutil"
	"log"
	"net/http"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/vespa-engine/vespa/util"
)

func executeCommand(t *testing.T, client *mockHttpClient, args []string, moreArgs []string) (standardout string) {
	util.ActiveHttpClient = client

	// Reset - persistent flags in Cobra persists over tests
	log.SetOutput(bytes.NewBufferString(""))
	rootCmd.SetArgs([]string{"status", "-t", ""})
	rootCmd.Execute()

	b := bytes.NewBufferString("")
	log.SetOutput(b)
	rootCmd.SetArgs(append(args, moreArgs...))
	rootCmd.Execute()
	out, err := ioutil.ReadAll(b)
	assert.Empty(t, err, "No error")
	return string(out)
}

type mockHttpClient struct {
	// The HTTP status code that will be returned from the next invocation. Default: 200
	nextStatus int

	// The response body code that will be returned from the next invocation. Default: ""
	nextBody string

	// A recording of the last HTTP request made through this
	lastRequest *http.Request
}

func (c *mockHttpClient) Do(request *http.Request, timeout time.Duration) (response *http.Response, error error) {
	if c.nextStatus == 0 {
		c.nextStatus = 200
	}
	c.lastRequest = request
	return &http.Response{
			Status:     "Status " + strconv.Itoa(c.nextStatus),
			StatusCode: c.nextStatus,
			Body:       ioutil.NopCloser(bytes.NewBufferString(c.nextBody)),
			Header:     make(http.Header),
		},
		nil
}