/*
 * FreeViewCreator.cpp
 *
 *  Created on: Jan 17, 2017
 *      Author: mtk54497
 */

#include "FreeViewCreator.h"
#include "JniFreeView.h"

using namespace stereo;

FreeViewCreator::FreeViewCreator() {
}

FreeViewCreator::~FreeViewCreator() {
}

IProcessor* FreeViewCreator::getInstance() {
    return new JniFreeView();
}

const char* FreeViewCreator::getFeatureName() {
    return "freeview";
}
