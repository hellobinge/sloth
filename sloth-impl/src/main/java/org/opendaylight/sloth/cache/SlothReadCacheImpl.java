/*
 * Copyright © 2016 Northwestern University LIST Lab, Libin Song and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sloth.cache;


import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlothReadCacheImpl implements SlothReadCache {
    private static final Logger LOG = LoggerFactory.getLogger(SlothReadCacheImpl.class);

    private final DataBroker dataBroker;
    private final SlothPermissionCache slothPermissionCache;

    public SlothReadCacheImpl(DataBroker dataBroker) {
        Preconditions.checkNotNull(dataBroker, "SlothReadCacheImpl initialization failure: empty data broker");
        this.dataBroker = dataBroker;
        slothPermissionCache = new SlothPermissionCache(dataBroker);
    }

    @Override
    public void close() throws Exception {
        slothPermissionCache.close();
    }
}