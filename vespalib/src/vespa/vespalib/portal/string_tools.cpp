// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "string_tools.h"

namespace vespalib::portal {

namespace {

int decode_hex_digit(char c) {
    if ((c >= '0') && (c <= '9')) {
        return (c - '0');
    }
    if ((c >= 'a') && (c <= 'f')) {
        return ((c - 'a') + 10);
    }
    if ((c >= 'A') && (c <= 'F')) {
        return ((c - 'A') + 10);
    }
    return -1;
}

int decode_hex_num(vespalib::stringref src, size_t idx) {
    if (src.size() < (idx + 2)) {
        return -1;
    }
    int a = decode_hex_digit(src[idx]);
    int b = decode_hex_digit(src[idx + 1]);
    if ((a < 0) || (b < 0)) {
        return -1;
    }
    return ((a << 4) | b);
}

} // namespace vespalib::portal::<unnamed>

vespalib::string dequote(vespalib::stringref src) {
    vespalib::string dst;
    for (size_t idx = 0; idx < src.size(); ++idx) {
        char c = src[idx];
        if (c == '+') {
            c = ' ';
        } else if (c == '%') {
            int x = decode_hex_num(src, idx + 1);
            if (x >= 0) {
                c = x;
                idx += 2;
            }
        }
        dst.push_back(c);
    }
    return dst;
}

std::vector<vespalib::string> split(vespalib::stringref str, char sep) {
    vespalib::string token;
    std::vector<vespalib::string> list;
    for (char c: str) {
        if (c != sep) {
            token.push_back(c);
        } else if (!token.empty()) {
            list.push_back(token);
            token.clear();
        }
    }
    if (!token.empty()) {
        list.push_back(token);
    }
    return list;
}

void strip_cr(vespalib::string &str) {
    if (!str.empty() && str[str.size() - 1] == '\r') {
        str.resize(str.size() - 1);
    }
}

} // namespace vespalib::portal
