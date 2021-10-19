// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "http_response.h"
#include "string_tools.h"

namespace vespalib::portal {

void
HttpResponse::handle(const vespalib::string &line)
{
    if (_first) {
        if (!_status_line.handle(line)) {
            _error = true;
        }
        _first = false;
    } else {
        if (line.empty()) {
            _header_done = true;
            const auto &len_str = _headers.get_header("content-length");
            _content_length = atoi(len_str.c_str());
            if (len_str.empty() || ((_content_length == 0) && (len_str != "0"))) {
                _error = true; // bad content length
            } else if (_content_length == 0) {
                _done = true; // no content
            }
        } else if (!_headers.handle(line)) {
            _error = true;
        }
    }
}

HttpResponse::HttpResponse()
  : _status_line(),
    _headers(),
    _content(),
    _first(true),
    _header_done(false),
    _done(false),
    _error(false),
    _line(),
    _content_length(0)
{
}

HttpResponse::~HttpResponse() = default;

size_t
HttpResponse::handle_data(const char *buf, size_t len)
{
    size_t used = 0;
    while (need_more_data() && (used < len)) {
        char c = buf[used++];
        if (_header_done) {
            _content.push_back(c);
            if (_content.size() == _content_length) {
                _done = true;
            }
        } else {
            if (c != '\n') {
                _line.push_back(c);
            } else {
                strip_cr(_line);
                handle(_line);
                _line.clear();
            }
        }
    }
    return used;
}

} // namespace vespalib::portal
