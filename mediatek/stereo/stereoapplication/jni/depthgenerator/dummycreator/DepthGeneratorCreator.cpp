#include "DepthGeneratorCreator.h"
#include "Log.h"

using namespace stereo;

#define TAG "DummyDepthGeneratorCreator"

DepthGeneratorCreator::DepthGeneratorCreator() {
}

DepthGeneratorCreator::~DepthGeneratorCreator() {
}

IProcessor* DepthGeneratorCreator::getInstance() {
    LOGD("<getInstance> dummy DepthGeneratorCreator, return NULL");
    return NULL;
}

const char* DepthGeneratorCreator::getFeatureName() {
    LOGD("<getFeatureName> dummy DepthGeneratorCreator, featureName: dummydepthgenerator");
    return "dummydepthgenerator";
}

