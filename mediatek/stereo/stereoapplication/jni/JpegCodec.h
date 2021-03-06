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

#ifndef JPEGCODEC_H_
#define JPEGCODEC_H_

#include "StereoStruct.h"
#include "StereoType.h"
#include "DebugHelper.h"
#include "DpBlitStream.h"

#include <linux/ion_drv.h>
#include <ion/ion.h>
#include "libion_mtk/include/ion.h"
#include <sys/mman.h>

namespace stereo {

#define ALIGN16(x)  ((x + 15)&(~(16-1)))
#define ALIGN128(x)  ((x + 127)&(~(128-1)))
#define ALIGN512(x) ((x + 511)&(~(512-1)))
#define JPG_ENCODE_QUALITY      90

struct IonInfo{
    int32_t ionFd;
    int size; // ion buffer size
    int dataSize; // exclude align byte, real data size
    int width;
    int height;
    void *virAddr;
    ion_user_handle_t ionHandle;
};

class JpegCodec {
public:
    JpegCodec();

    virtual ~JpegCodec();

    bool yv12ToJpg(ImageBuf* pYv12Buf, MUINT8 *pDstBuf, MUINT32 *pDstBufSize);

    bool jpgToYv12(MUINT8 *pJpgBuf, MUINT32 bufSize, ImageBuf* pOutBuf, MUINT32 sampleSize);

    bool yuvToNv21(MUINT8 *yuvBuf, MUINT32 width, MUINT32 height, MUINT8 *outBuf);

    bool nv21ToJpg(MUINT8 *srcBuffer, MUINT32 srcWidth, MUINT32 srcHeight,
            MUINT8 *dstBuffer, MUINT32 dstSize, MUINT32* u4EncSize);

    bool nv21ToJpg(IonInfo *srcBuffer, MUINT32 srcWidth, MUINT32 srcHeight,
            IonInfo *dstBuffer, MUINT32 dstSize, MUINT32* u4EncSize);

    bool yuvToRgba(MUINT8 *yuvBuf, MUINT32 width, MUINT32 height, MUINT8 *outBuf);

    bool jpgToYv12(IonInfo* pJpgBuf, MUINT32 jpgBufSize, IonInfo** pOutBuf,
                MUINT32 sampleSize, int32_t ionHandle);

    static IonInfo* allocateIon(int size, int32_t ionHandle);

    static void destroyIon(IonInfo* info, int32_t ionHandle);

private:
    DebugHelper *m_pDebugHelper = NULL;
};

}  // namespace stereo

#endif /* JPEGCODEC_H_ */
