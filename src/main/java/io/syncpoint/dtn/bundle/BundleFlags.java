/*
 * Bundle.java
 *
 * Copyright (C) 2011 IBR, TU Braunschweig
 *
 * Written-by:
 *  Johannes Morgenroth <morgenroth@ibr.cs.tu-bs.de>
 *  Julian Timpner      <timpner@ibr.cs.tu-bs.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package io.syncpoint.dtn.bundle;

/**
 * The flags are bit values where the offset is the position
 * within a 32 bit dword
 */
public enum BundleFlags {

    FRAGMENT(0),
    ADM_RECORD(1),
    NO_FRAGMENT(2),
    CUSTODY_REQUEST(3),
    DESTINATION_IS_SINGLETON(4),
    APP_ACK_REQUEST(5),
    RESERVED_6(6),
    PRIORITY_BIT1(7),
    PRIORITY_BIT2(8),
    CLASSOFSERVICE_9(9),
    CLASSOFSERVICE_10(10),
    CLASSOFSERVICE_11(11),
    CLASSOFSERVICE_12(12),
    CLASSOFSERVICE_13(13),
    RECEPTION_REPORT(14),
    CUSTODY_REPORT(15),
    FORWARD_REPORT(16),
    DELIVERY_REPORT(17),
    DELETION_REPORT(18),
    // DTNSEC BUNDLE_FLAGS (these are customized flags and not written down in any draft)
    DTNSEC_REQUEST_SIGN(26),
    DTNSEC_REQUEST_ENCRYPT(27),
    DTNSEC_STATUS_VERIFIED(28),
    DTNSEC_STATUS_CONFIDENTIAL(29),
    DTNSEC_STATUS_AUTHENTICATED(30),
    COMPRESSION_REQUEST(31);

    private int offset;

    BundleFlags(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
}
