// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <vespa/vespalib/testkit/test_kit.h>
#include <vespa/vespalib/portal/http_response.h>

using namespace vespalib;
using namespace vespalib::portal;

vespalib::string simple_res("HTTP/1.1 200 OK\r\n"
                            "Content-Type: text/plain\r\n"
                            "Content-Length: 10\r\n"
                            "CustomHeader: CustomValue\r\n"
                            "\r\nmy_content123456789");
size_t simple_res_padding = 9;
size_t simple_res_size = (simple_res.size() - simple_res_padding);

void verify_simple_res(const HttpResponse &res) {
    EXPECT_TRUE(!res.need_more_data());
    EXPECT_TRUE(res.valid());
    EXPECT_TRUE(res.is_ok());
    EXPECT_EQUAL(res.get_version(), "HTTP/1.1");
    EXPECT_EQUAL(res.get_code(), 200);
    EXPECT_EQUAL(res.get_message(), "OK");
    EXPECT_EQUAL(res.get_header("customheader"), "CustomValue");
    EXPECT_EQUAL(res.get_content(), "my_content");
}

void verify_invalid_response(const vespalib::string &res) {
    HttpResponse result;
    EXPECT_EQUAL(result.handle_data(res.data(), res.size()), res.size());
    EXPECT_TRUE(!result.need_more_data());
    EXPECT_TRUE(!result.valid());
}

HttpResponse make_response(const vespalib::string &res) {
    HttpResponse result;
    ASSERT_EQUAL(result.handle_data(res.data(), res.size()), res.size());
    ASSERT_TRUE(result.valid());
    return result;
}

TEST("require that response can be parsed in one go") {
    HttpResponse res;
    EXPECT_EQUAL(res.handle_data(simple_res.data(), simple_res_size), simple_res_size);
    TEST_DO(verify_simple_res(res));
}

TEST("require that trailing data is not consumed") {
    HttpResponse res;
    EXPECT_EQUAL(res.handle_data(simple_res.data(), simple_res.size()), simple_res_size);
    TEST_DO(verify_simple_res(res));
}

TEST("require that response can be parsed incrementally") {
    HttpResponse res;
    size_t chunk = 7;
    size_t done = 0;
    while (done < simple_res_size) {
        size_t expect = std::min(simple_res_size - done, chunk);
        EXPECT_EQUAL(res.handle_data(simple_res.data() + done, chunk), expect);
        done += expect;
    }
    EXPECT_EQUAL(done, simple_res_size);
    TEST_DO(verify_simple_res(res));
}

TEST("require that empty reason phrase is allowed") {
    auto res = make_response("HTTP/1.1 200 \r\n"
                             "content-length: 0\r\n"
                             "\r\n");
    EXPECT_TRUE(res.is_ok());
    EXPECT_EQUAL(res.get_message(), "");
}

TEST("require that content length is required") {
    TEST_DO(verify_invalid_response("HTTP/1.1 200 OK\r\n"
                                    "\r\n"));
}

TEST("require that non-200 status code is not ok") {
    auto res = make_response("HTTP/1.1 201 OK-ish\r\n"
                             "content-length: 0\r\n"
                             "\r\n");
    EXPECT_TRUE(!res.is_ok());
}

TEST_MAIN() { TEST_RUN_ALL(); }
