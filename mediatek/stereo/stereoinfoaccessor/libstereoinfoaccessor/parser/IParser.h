#ifndef IPARSER_H
#define IPARSER_H

#include "SerializedInfo.h"

namespace stereo {

class IParser {

public:
    virtual ~IParser() {}
    virtual void read() = 0;
    virtual void write() = 0;
    virtual SerializedInfo* serialize() = 0;

protected:
    const int INSTANTIATION_BY_BUFFER = 0;
    const int INSTANTIATION_BY_OPERATOR = 1;
    int instantiationWay;
};

}

#endif
