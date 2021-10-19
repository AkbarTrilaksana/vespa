// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "http_request_line.h"
#include "string_tools.h"
#include <vector>

namespace vespalib::portal {

HttpRequestLine::HttpRequestLine()
    : _method(),
      _uri(),
      _path(),
      _params(),
      _version()
{
}

HttpRequestLine::~HttpRequestLine() = default;

bool
HttpRequestLine::handle(const vespalib::string &line)
{
    auto parts = split(line, ' ');
    if (parts.size() != 3) {
        return false; // malformed request line
    }
    _method = parts[0];
    _uri = parts[1];
    _version = parts[2];
    size_t query_sep = _uri.find('?');
    if (query_sep == vespalib::string::npos) {
        _path = dequote(_uri);
    } else {
        _path = dequote(_uri.substr(0, query_sep));
        auto query = split(_uri.substr(query_sep + 1), '&');
        for (const auto &param: query) {
            size_t value_sep = param.find('=');
            if (value_sep == vespalib::string::npos) {
                _params[dequote(param)] = "";
            } else {
                auto key = param.substr(0, value_sep);
                auto value = param.substr(value_sep + 1);
                _params[dequote(key)] = dequote(value);
            }
        }
    }
    return true;
}

bool
HttpRequestLine::has_param(const vespalib::string &name) const
{
    return (_params.find(name) != _params.end());
}

const vespalib::string &
HttpRequestLine::get_param(const vespalib::string &name) const
{
    auto pos = _params.find(name);
    if (pos == _params.end()) {
        return empty_string();
    }
    return pos->second;
}

} // namespace vespalib::portal
