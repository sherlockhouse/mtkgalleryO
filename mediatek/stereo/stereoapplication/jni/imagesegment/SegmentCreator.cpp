/*
 * SegmentCreator.cpp
 *
 *  Created on: Jan 17, 2017
 *      Author: mtk54497
 */

#include "SegmentCreator.h"
#include "JniImageSegment.h"

using namespace stereo;

SegmentCreator::SegmentCreator() {
}

SegmentCreator::~SegmentCreator() {
}

IProcessor* SegmentCreator::getInstance() {
    return new JniImageSegment();
}

const char* SegmentCreator::getFeatureName() {
    return "imagesegment";
}
