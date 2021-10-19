// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "http_status_line.h"
#include "string_tools.h"

namespace vespalib::portal {

HttpStatusLine::HttpStatusLine()
  : _code(0),
    _message(),
    _version()
{
}

HttpStatusLine::~HttpStatusLine() = default;

bool
HttpStatusLine::handle(const vespalib::string &line)
{
    size_t sp1 = line.find(' ');
    size_t sp2 = line.find(' ', sp1 + 1);
    if ((sp1 > line.size()) || (sp2 > line.size())) {
        return false; // malformed status line
    }
    _message = line.substr(sp2 + 1);
    _version = line.substr(0, sp1);
    auto code_str = line.substr(sp1 + 1, (sp2 - sp1) - 1);
    _code = atoi(code_str.c_str());
    return (_code != 0);
}

} // namespace vespalib::portal
