/**
 * TOTVS LABS CONFIDENTIAL
 * _______________________
 *
 *  2012-2016 (c) TOTVS Labs
 *  All Rights Reserved.
 *
 *  NOTICE:  All information contained herein is, and remains the
 *  property of TOTVS Labs and its suppliers, if any. The intellectual
 *  and technical concepts contained herein are proprietary to TOTVS Labs
 *  and its suppliers and may be covered by U.S. and Foreign Patents,
 *  patents in process, and are protected  by trade secret or copyright
 *  law. Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from TOTVS Labs.
 */

package com.lyft.data.proxyserver;

import org.testng.annotations.Test;
import com.lyft.data.proxyserver.wrapper.TenantId;
import com.lyft.data.proxyserver.wrapper.TenantLookupServiceImpl;
import static org.testng.Assert.assertEquals;

public class TestTenantLookup {

    TenantLookupServiceImpl lookup = new TenantLookupServiceImpl("https://karol.ai/");
    
    @Test
    public void testLookup() {
        TenantId tenantId = lookup.getTenantId("623cdc6dd7d343168805a47435d063e2_628c6f49379640b4bd1b0f1c33c9c346");
        assertEquals(tenantId, "8cd6e43115e9416eb23609486fa053e3");
    }

    @Test(expectedExceptions = SecurityException.class)
    public void testBadInput() {
        lookup.getTenantId("623cdc6dd7d343168805a47435d063e2_628c6f49379640b4bd1b0f1c33c9c348");
    }
}
