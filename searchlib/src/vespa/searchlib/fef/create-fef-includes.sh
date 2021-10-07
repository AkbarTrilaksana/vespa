#!/bin/sh
# Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

cat <<EOF
// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
// NOTE: This file was generated by the 'create-fef-includes.sh' script
#pragma once
/**
 * @file fef.h
 *
 * This is a convenience header that will include everything you need
 * to use this library.
 **/


EOF
echo "#include <vespa/searchlib/common/feature.h>"
echo ""

for f in *.h; do
  if [ $f != "fef.h" ]; then
    echo "#include \"$f\""
  fi
done

echo ""
