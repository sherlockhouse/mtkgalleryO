/*
 * DepthGeneratorCreator.cpp
 *
 *  Created on: Jan 17, 2017
 *      Author: mtk54497
 */

#include "DepthGeneratorCreator.h"
#include "JniDepthGenerator.h"

using namespace stereo;

DepthGeneratorCreator::DepthGeneratorCreator() {
}

DepthGeneratorCreator::~DepthGeneratorCreator() {
}

IProcessor* DepthGeneratorCreator::getInstance() {
    return new JniDepthGenerator();
}

const char* DepthGeneratorCreator::getFeatureName() {
    return "depthgenerator";
}

