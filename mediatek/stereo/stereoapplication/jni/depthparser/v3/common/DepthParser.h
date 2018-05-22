/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2016. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

#ifndef DEPTHPARSER_H_
#define DEPTHPARSER_H_

#include "StereoStruct.h"
#include "StereoType.h"
#include "Log.h"

namespace stereo {

class DepthParser {
public:
    DepthParser(DepthBuf* depthBuf) {
        LOGD("<DepthParser><comp> new DepthParser (v3)");
        m_pDepthBuf = depthBuf;
    }

    virtual ~DepthParser() {
    }

    MUINT8* getDepthMap() {
        LOGD("<getDepthMap><comp> (v3)");
        if (m_pDepthBuf == NULL) {
            LOGD("<getDepthMap> m_pDepthBuf invalid, return NULL!!");
            return NULL;
        }
        MINT32 len = m_pDepthBuf->depthWidth * m_pDepthBuf->depthHeight;
        if (len <= 0) {
            LOGD("<getDepthMap> len <= 0, return NULL!!");
            return NULL;
        }
        MUINT8* pDepthMap = new MUINT8[len];
        memcpy(pDepthMap, m_pDepthBuf->buffer, len);
        return pDepthMap;
    }

    MUINT8* getOccMap() {
        LOGD("<getOccMap><comp> (v3)");
        if (m_pDepthBuf == NULL) {
            LOGD("<getOccMap> m_pDepthBuf invalid, return NULL!!");
            return NULL;
        }
        MUINT8* pOccBuf = NULL;
        MINT32 len = m_pDepthBuf->depthWidth * m_pDepthBuf->depthHeight;
        if (len <= 0) {
            LOGD("<getOccMap> len <= 0, return NULL!!");
            return NULL;
        }
        pOccBuf = new MUINT8[len];
        MINT32 offset = 3 * m_pDepthBuf->depthWidth * m_pDepthBuf->depthHeight;
        memcpy(pOccBuf, m_pDepthBuf->buffer + offset, len);
        return pOccBuf;
    }

protected:
    DepthBuf* m_pDepthBuf;
};

}  // namespace stereo

#endif /* DEPTHPARSER_H_ */
