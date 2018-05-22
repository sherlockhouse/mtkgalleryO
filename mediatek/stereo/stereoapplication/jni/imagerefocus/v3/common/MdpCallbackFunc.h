/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef MDP_CALLBACK_FUNC_H_
#define MDP_CALLBACK_FUNC_H_

#include "DpIspStream.h"
#include "Log.h"
#include "MTKRefocus.h"

#ifdef __cplusplus
extern "C" {
#endif

int mdpCallbackFunc(RF_DP_STRUCT *p_Para)
{
    LOGE("MdpCallbackFunc, status code\n");

    // variables
    DpIspStream *dpStream = new DpIspStream(DpIspStream::ISP_ZSD_STREAM);
    int dpStatus = DP_STATUS_RETURN_SUCCESS;

    // setup input
    dpStatus |= dpStream->setSrcConfig(p_Para->inputWidth, p_Para->inputHeight, p_Para->inputYPitch, p_Para->inputUVPitch, p_Para->inputFmt);
    dpStatus |= dpStream->queueSrcBuffer(p_Para->inputBuffer, p_Para->inputSize, p_Para->inputPlaneNo);

    // setup output
    for (int portIdx=0; portIdx<p_Para->outputPortNo; portIdx++)
    {
        dpStatus |= dpStream->setDstConfig(portIdx, p_Para->outputWidth[portIdx], p_Para->outputHeight[portIdx], p_Para->outputYPitch[portIdx], p_Para->outputUVPitch[portIdx], p_Para->outputFmt[portIdx]);
        dpStatus |= dpStream->queueDstBuffer(portIdx, p_Para->outputBuffer[portIdx], p_Para->outputSize[portIdx], p_Para->outputPlaneNo[portIdx]);
    }

    // setup picture quality
    for (int portIdx=0; portIdx<p_Para->outputPortNo; portIdx++)
    {
        DpPqParam dpParam;
        dpParam.scenario = MEDIA_ISP_PREVIEW;
        if (p_Para->vsfParam[portIdx] != NULL)
        {
        #if defined(REFOCUS_MDP_PQ_NEW)
            p_Para->vsfParam[portIdx]->isRefocus = true;
            dpParam.u.isp.VSDOFPQParam = p_Para->vsfParam[portIdx];
        #elif defined(REFOCUS_MDP_PQ)
            dpParam.u.isp.feature = p_Para->vsfParam[portIdx]->feature;
            dpParam.u.isp.defaultUpTable = p_Para->vsfParam[portIdx]->defaultUpTable;
            dpParam.u.isp.defaultDownTable = p_Para->vsfParam[portIdx]->defaultDownTable;
            dpParam.u.isp.IBSEGain = p_Para->vsfParam[portIdx]->IBSEGain;
        #endif
            dpStatus |= dpStream->setPQParameter(portIdx, dpParam);
        }
    }

    // run MDP and wait for execution done
    dpStatus |= dpStream->startStream();
    dpStatus |= dpStream->stopStream();

    // dequeue buffer
    for (int portIdx=0; portIdx<p_Para->outputPortNo; portIdx++)
    {
        dpStatus |= dpStream->dequeueDstBuffer(portIdx, p_Para->outputBuffer[portIdx]);
    }
    dpStatus |= dpStream->dequeueSrcBuffer();
    dpStatus |= dpStream->dequeueFrameEnd();
    if (dpStatus != DP_STATUS_RETURN_SUCCESS)
    {
        LOGE("MDP Execution fail, status code(%d)\n", dpStatus);
    }

    delete dpStream;
    return dpStatus;
}

#ifdef __cplusplus
}
#endif
#endif // MDP_CALLBACK_FUNC_H_
