# Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_executable(vespalib_object_tracker_test_app TEST
    SOURCES
    object_tracker_test.cpp
    DEPENDS
    vespalib
    GTest::GTest
)
vespa_add_test(NAME vespalib_object_tracker_test_app COMMAND vespalib_object_tracker_test_app)
