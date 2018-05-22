/*
 * FancyColorCreator.cpp
 */

#include "FancyColorCreator.h"
#include "JniFancyColor.h"

using namespace stereo;

FancyColorCreator::FancyColorCreator() {
}

FancyColorCreator::~FancyColorCreator() {
}

IProcessor* FancyColorCreator::getInstance() {
    return new JniFancyColor();
}

const char* FancyColorCreator::getFeatureName() {
    return "fancycolor";
}

