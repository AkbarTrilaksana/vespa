// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "http_status_line.h"
#include "http_headers.h"
#include <vespa/vespalib/stllike/string.h>

namespace vespalib::portal {

// used for testing

class HttpResponse
{
private:
    HttpStatusLine   _status_line;
    HttpHeaders      _headers;
    vespalib::string _content;
    bool             _first;
    bool             _header_done;
    bool             _done;
    bool             _error;
    vespalib::string _line;
    size_t           _content_length;

    void handle(const vespalib::string &line);

public:
    HttpResponse();
    ~HttpResponse();
    size_t handle_data(const char *buf, size_t len);
    bool need_more_data() const { return (!_error && !_done); }
    bool valid() const { return (!_error && _done); }
    const vespalib::string &get_header(const vespalib::string &name) const {
        return _headers.get_header(name);
    }
    const vespalib::string &get_content() const {
        return _content;
    }
    bool is_ok() const { return _status_line.is_ok(); }
    int get_code() const { return _status_line.get_code(); }
    const vespalib::string &get_message() const { return _status_line.get_message(); }
    const vespalib::string &get_version() const { return _status_line.get_version(); }
};

} // namespace vespalib::portal
