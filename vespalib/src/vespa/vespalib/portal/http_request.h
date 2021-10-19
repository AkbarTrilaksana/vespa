// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "http_headers.h"
#include "http_request_line.h"
#include <vespa/vespalib/stllike/string.h>

namespace vespalib::portal {

class HttpRequest
{
private:
    HttpRequestLine  _request_line;
    HttpHeaders      _headers;
    vespalib::string _host;
    bool             _first;
    bool             _done;
    bool             _error;
    vespalib::string _line;

    void handle(const vespalib::string &line);

public:
    HttpRequest();
    ~HttpRequest();
    size_t handle_data(const char *buf, size_t len);
    bool need_more_data() const { return (!_error && !_done); }
    bool valid() const { return (!_error && _done); }
    bool is_get() const { return _request_line.is_get(); }
    void resolve_host(const vespalib::string &my_host);
    const vespalib::string &get_header(const vespalib::string &name) const {
        return _headers.get_header(name);
    }
    const vespalib::string &get_host() const { return _host; }
    const vespalib::string &get_uri() const { return _request_line.get_uri(); }
    const vespalib::string &get_path() const { return _request_line.get_path(); }
    bool has_param(const vespalib::string &name) const { return _request_line.has_param(name); }
    const vespalib::string &get_param(const vespalib::string &name) const { return _request_line.get_param(name); }
    const vespalib::string &get_version() const { return _request_line.get_version(); }
    std::map<vespalib::string, vespalib::string> export_params() const { return _request_line.export_params(); }
    std::map<vespalib::string, vespalib::string> export_headers() const { return _headers.export_headers(); }
};

} // namespace vespalib::portal
