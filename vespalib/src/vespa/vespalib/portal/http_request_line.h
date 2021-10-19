// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/vespalib/stllike/string.h>
#include <map>

namespace vespalib::portal {

class HttpRequestLine
{
private:
    vespalib::string _method;
    vespalib::string _uri;
    vespalib::string _path;
    std::map<vespalib::string, vespalib::string> _params;
    vespalib::string _version;

public:
    HttpRequestLine();
    ~HttpRequestLine();
    bool handle(const vespalib::string &line);
    bool is_get() const { return _method == "GET"; }
    const vespalib::string &get_uri() const { return _uri; }
    const vespalib::string &get_path() const { return _path; }
    bool has_param(const vespalib::string &name) const;
    const vespalib::string &get_param(const vespalib::string &name) const;
    const vespalib::string &get_version() const { return _version; }
    std::map<vespalib::string, vespalib::string> export_params() const { return _params; }
};

} // namespace vespalib::portal
