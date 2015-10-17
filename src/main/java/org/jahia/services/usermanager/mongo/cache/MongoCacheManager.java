/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 * <p/>
 * Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 * <p/>
 * THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 * 1/GPL OR 2/JSEL
 * <p/>
 * 1/ GPL
 * ======================================================================================
 * <p/>
 * IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p/>
 * "This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * <p/>
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, also available here:
 * http://www.jahia.com/license"
 * <p/>
 * 2/ JSEL - Commercial and Supported Versions of the program
 * ======================================================================================
 * <p/>
 * IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p/>
 * Alternatively, commercial and supported versions of the program - also known as
 * Enterprise Distributions - must be used in accordance with the terms and conditions
 * contained in a separate written agreement between you and Jahia Solutions Group SA.
 * <p/>
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 * <p/>
 * <p/>
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 * <p/>
 * Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 * streamlining Enterprise digital projects across channels to truly control
 * time-to-market and TCO, project after project.
 * Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 * marketing teams to collaboratively and iteratively build cutting-edge
 * online business solutions.
 * These, in turn, are securely and easily deployed as modules and apps,
 * reusable across any digital projects, thanks to the Jahia Private App Store Software.
 * Each solution provided by Jahia stems from this overarching vision:
 * Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 * Founded in 2002 and headquartered in Geneva, Switzerland,
 * Jahia Solutions Group has its North American headquarters in Washington DC,
 * with offices in Chicago, Toronto and throughout Europe.
 * Jahia counts hundreds of global brands and governmental organizations
 * among its loyal customers, in more than 20 countries across the globe.
 * <p/>
 * For more information, please visit http://www.jahia.com
 */
package org.jahia.services.usermanager.mongo.cache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;
import org.jahia.services.cache.ehcache.EhCacheProvider;

public class MongoCacheManager {
    public static final String MONGO_USERS_CACHE = "MongoUsersCache";
    private Ehcache userCache;
    private EhCacheProvider cacheProvider;

    /**
     *
     */
    void start() {
        final CacheManager cacheManager = cacheProvider.getCacheManager();
        userCache = cacheManager.getCache(MONGO_USERS_CACHE);
        if (userCache == null) {
            userCache = createCache(cacheManager, MONGO_USERS_CACHE);
        } else {
            userCache.removeAll();
        }
    }

    /**
     *
     * @param cacheManager
     * @param cacheName
     * @return
     */
    private Ehcache createCache(final CacheManager cacheManager,
                                final String cacheName) {
        final CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setName(cacheName);
        cacheConfiguration.setTimeToIdleSeconds(3600);
        cacheConfiguration.setEternal(false);

        // Create a new cache with the configuration
        final Ehcache cache = new Cache(cacheConfiguration);
        cache.setName(cacheName);

        // Cache name has been set now we can initialize it by putting it in the manager.
        // Only Cache manager is initializing caches.
        return cacheManager.addCacheIfAbsent(cache);
    }

    /**
     *
     */
    void stop() {
        if (userCache != null) {
            userCache.removeAll();
        }
    }

    /**
     *
     * @param cacheProvider
     */
    public void setCacheProvider(final EhCacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    /**
     *
     * @param providerKey
     * @param username
     * @return
     */
    public MongoUserCacheEntry getUserCacheEntryByName(final String providerKey,
                                                       final String username) {
        return (MongoUserCacheEntry) CacheHelper.getObjectValue(userCache, getCacheNameKey(providerKey, username));
    }

    /**
     *
     * @param providerKey
     * @param userCacheEntry
     */
    public void cacheUser(final String providerKey, final MongoUserCacheEntry userCacheEntry) {
        final ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(userCacheEntry,
                "mongo");
        userCache.put(new Element(getCacheNameKey(providerKey, userCacheEntry.getName()), cacheEntry));
    }

    /**
     *
     * @param providerKey
     * @param objectName
     * @return
     */
    private String getCacheNameKey(final String providerKey,
                                   final String objectName) {
        return providerKey + "n" + objectName;
    }
}
