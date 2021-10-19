// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "http_headers.h"
#include "string_tools.h"
#include <cassert>
#include <algorithm>
#include <vector>

namespace vespalib::portal {

HttpHeaders::HttpHeaders()
  : _headers(),
    _header_name()
{
}

HttpHeaders::~HttpHeaders() = default;

bool
HttpHeaders::handle(const vespalib::string &line)
{
    assert(!line.empty());
    size_t pos = 0;
    size_t end = line.size();
    bool continuation = (line[0] == ' ') || (line[0] == '\t');
    if (!continuation) {
        pos = line.find(":");
        if (pos == vespalib::string::npos) {
            return false; // missing header: value separator
        } else {
            _header_name.assign(line, 0, pos++);
            std::transform(_header_name.begin(), _header_name.end(),
                           _header_name.begin(), ::tolower);
        }
    }
    if (_header_name.empty()) {
        return false; // missing header name
    }
    while ((pos < end) && (isspace(line[pos]))) {
        ++pos; // strip leading whitespace
    }
    while ((pos < end) && (isspace(line[end - 1]))) {
        --end; // strip trailing whitespace
    }
    auto header_insert_result = _headers.insert(std::make_pair(_header_name, vespalib::string()));
    bool header_found = !header_insert_result.second;
    vespalib::string &header_value = header_insert_result.first->second;
    if (header_found) {
        if (continuation) {
            header_value.push_back(' ');
        } else { // duplicate header
            header_value.push_back(',');
        }
    }
    header_value.append(line.data() + pos, end - pos);
    return true;
}

const vespalib::string &
HttpHeaders::get_header(const vespalib::string &name) const
{
    auto pos = _headers.find(name);
    if (pos == _headers.end()) {
        return empty_string();
    }
    return pos->second;
}

} // namespace vespalib::portal
