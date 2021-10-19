# Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
vespa_add_library(vespalib_vespalib_portal OBJECT
    SOURCES
    handle_manager.cpp
    http_connection.cpp
    http_headers.cpp
    http_request.cpp
    http_request_line.cpp
    http_response.cpp
    http_status_line.cpp
    listener.cpp
    object_tracker.cpp
    portal.cpp
    reactor.cpp
    string_tools.cpp
    DEPENDS
)
