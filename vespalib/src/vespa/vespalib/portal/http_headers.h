// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/vespalib/stllike/string.h>

#include <map>

namespace vespalib::portal {

class HttpHeaders
{
private:
    std::map<vespalib::string, vespalib::string> _headers;
    vespalib::string _header_name;

public:
    HttpHeaders();
    ~HttpHeaders();
    bool handle(const vespalib::string &line);
    const vespalib::string &get_header(const vespalib::string &name) const;
    std::map<vespalib::string, vespalib::string> export_headers() const { return _headers; }
};

} // namespace vespalib::portal
