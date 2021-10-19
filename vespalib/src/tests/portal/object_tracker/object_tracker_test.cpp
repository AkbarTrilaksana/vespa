// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <vespa/vespalib/gtest/gtest.h>
#include <vespa/vespalib/portal/object_tracker.h>

using namespace vespalib;
using namespace vespalib::portal;

struct MyObj {
    int value;
    MyObj(int value_in) noexcept : value(value_in) {}
};

TEST(ObjectTrackerTest, test_object_tracking) {
    ObjectTracker<MyObj> tracker;
    auto obj = std::make_shared<MyObj>(7);
    tracker.attach(std::make_shared<MyObj>(3));
    tracker.attach(obj);
    tracker.attach(std::make_shared<MyObj>(3));
    auto obj_cpy = tracker.detach(obj.get());
    EXPECT_EQ(obj.get(), obj_cpy.get());
    auto rest = tracker.detach_all();
    ASSERT_EQ(rest.size(), 2);
    EXPECT_EQ(rest[0]->value, 3);
    EXPECT_EQ(rest[0]->value, 3);
    EXPECT_NE(rest[0].get(), rest[1].get());
}

GTEST_MAIN_RUN_ALL_TESTS()
