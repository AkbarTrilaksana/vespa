// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include <vespa/fastos/fastos.h>
#include <vespa/searchlib/fef/fieldinfo.h>
#include "matchcountfeature.h"
#include "utils.h"
#include "valuefeature.h"

#include <vespa/log/log.h>
LOG_SETUP(".features.matchcountfeature");

using namespace search::fef;

namespace search {
namespace features {

MatchCountExecutor::MatchCountExecutor(uint32_t fieldId, const IQueryEnvironment &env)
    : FeatureExecutor(),
      _handles(),
      _md(nullptr)
{
    for (uint32_t i = 0; i < env.getNumTerms(); ++i) {
        TermFieldHandle handle = util::getTermFieldHandle(env, i, fieldId);
        if (handle != IllegalHandle) {
            _handles.push_back(handle);
        }
    }
}

void
MatchCountExecutor::execute(MatchData &match)
{
    size_t output = 0;
    for (uint32_t i = 0; i < _handles.size(); ++i) {
        const TermFieldMatchData *tfmd = _md->resolveTermField(_handles[i]);
        if (tfmd->getDocId() == match.getDocId()) {
            output++;
        }
    }
    outputs().set_number(0, static_cast<feature_t>(output));
}

void
MatchCountExecutor::handle_bind_match_data(MatchData &md)
{
    _md = &md;
}

MatchCountBlueprint::MatchCountBlueprint() :
    Blueprint("matchCount"),
    _field(NULL)
{
}

void
MatchCountBlueprint::visitDumpFeatures(const IIndexEnvironment &, IDumpFeatureVisitor &) const
{
}

bool
MatchCountBlueprint::setup(const IIndexEnvironment &, const ParameterList & params)
{
    _field = params[0].asField();
    describeOutput("out", "Returns number of matches in the field of all terms in the query");
    return true;
}

Blueprint::UP
MatchCountBlueprint::createInstance() const
{
    return Blueprint::UP(new MatchCountBlueprint());
}

FeatureExecutor &
MatchCountBlueprint::createExecutor(const IQueryEnvironment & queryEnv, vespalib::Stash &stash) const
{
    if (_field == nullptr) {
        return stash.create<ValueExecutor>(std::vector<feature_t>(1, 0.0));
    }
    return stash.create<MatchCountExecutor>(_field->id(), queryEnv);
}

} // namespace features
} // namespace search
