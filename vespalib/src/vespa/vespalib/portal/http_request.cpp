// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "http_request.h"
#include "string_tools.h"

namespace vespalib::portal {

void
HttpRequest::handle(const vespalib::string &line)
{
    if (_first) {
        if (!_request_line.handle(line)) {
            _error = true;
        }
        _first = false;
    } else {
        if (line.empty()) {
            _done = true;
        } else if (!_headers.handle(line)) {
            _error = true;
        }
    }
}

HttpRequest::HttpRequest()
  : _request_line(),
    _headers(),
    _host(),
    _first(true),
    _done(false),
    _error(false),
    _line()
{
}

HttpRequest::~HttpRequest() = default;

size_t
HttpRequest::handle_data(const char *buf, size_t len)
{
    size_t used = 0;
    while (need_more_data() && (used < len)) {
        char c = buf[used++];
        if (c != '\n') {
            _line.push_back(c);
        } else {
            strip_cr(_line);
            handle(_line);
            _line.clear();
        }
    }
    return used;
}

void
HttpRequest::resolve_host(const vespalib::string &my_host)
{
    _host = get_header("host");
    if (_host.empty()) {
        _host = my_host;
    }
}

} // namespace vespalib::portal
