// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/vespalib/stllike/string.h>

namespace vespalib::portal {

class HttpStatusLine
{
private:
    int _code;
    vespalib::string _message;
    vespalib::string _version;

public:
    HttpStatusLine();
    ~HttpStatusLine();
    bool handle(const vespalib::string &line);
    bool is_ok() const { return _code == 200; }
    int get_code() const { return _code; }
    const vespalib::string &get_message() const { return _message; }
    const vespalib::string &get_version() const { return _version; }
};

} // namespace vespalib::portal
