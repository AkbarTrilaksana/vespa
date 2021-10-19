// Copyright Yahoo. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vector>
#include <map>
#include <memory>
#include <mutex>

namespace vespalib::portal {

/**
 * Keeps track of all shared objects with a specific type that is
 * owned by someone (example: all HttpConnection objects owned by the
 * Portal server).
 **/
template <typename T>
class ObjectTracker
{
private:
    std::mutex _lock;
    std::map<T*,std::shared_ptr<T>> _map;

public:
    ObjectTracker() : _lock(), _map() {}  
    void attach(std::shared_ptr<T> obj) {
        std::lock_guard<std::mutex> guard(_lock);
        T *ptr = obj.get();
        _map.try_emplace(ptr, std::move(obj));
    }
    std::shared_ptr<T> detach(T *ptr) {
        std::lock_guard<std::mutex> guard(_lock);
        auto pos = _map.find(ptr);
        if (pos == _map.end()) {
            return std::shared_ptr<T>();
        }
        auto res = std::move(pos->second);
        _map.erase(pos);
        return res;
    }
    std::vector<std::shared_ptr<T>> detach_all() {
        std::lock_guard<std::mutex> guard(_lock);
        std::vector<std::shared_ptr<T>> list;
        for (auto &entry: _map) {
            list.emplace_back(std::move(entry.second));
        }
        _map.clear();
        return list;
    }
};

} // namespace vespalib::portal
