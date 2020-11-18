// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "shared_string_repo.h"

namespace vespalib {

SharedStringRepo::Partition::~Partition() = default;

std::array<SharedStringRepo::Partition, SharedStringRepo::NUM_PARTS> SharedStringRepo::_partitions{};

}
