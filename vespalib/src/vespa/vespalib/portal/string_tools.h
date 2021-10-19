// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/vespalib/stllike/string.h>
#include <vector>

namespace vespalib::portal {

vespalib::string dequote(vespalib::stringref src);
std::vector<vespalib::string> split(vespalib::stringref str, char sep);
void strip_cr(vespalib::string &str);

} // namespace vespalib::portal
