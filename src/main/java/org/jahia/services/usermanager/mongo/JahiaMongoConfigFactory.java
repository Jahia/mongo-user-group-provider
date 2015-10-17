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
package org.jahia.services.usermanager.mongo;

import org.jahia.services.cache.CacheHelper;
import org.jahia.services.usermanager.mongo.cache.MongoCacheManager;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

public class JahiaMongoConfigFactory implements ManagedServiceFactory, ApplicationContextAware {
    private static final Logger LOGGER = LoggerFactory.getLogger(JahiaMongoConfigFactory.class);

    private ConfigurationAdmin configurationAdmin;
    private ApplicationContext context;
    private Map<String, JahiaMongoConfig> mongoConfigs = new HashMap<String, JahiaMongoConfig>();
    private Map<String, String> pidsByProviderKey = new HashMap<String, String>();

    /**
     *
     * @param configurationAdmin
     */
    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    /**
     *
     */
    public void start() {
        // do nothing
    }

    /**
     *
     */
    public void stop() {
        for (final JahiaMongoConfig config : mongoConfigs.values()) {
            config.unregister();
        }
        mongoConfigs.clear();
    }

    /**
     *
     * @param pid
     * @param dictionary
     * @throws ConfigurationException
     */
    @Override
    public void updated(final String pid,
                        final Dictionary<String, ?> dictionary) throws ConfigurationException {
        final JahiaMongoConfig mongoConfig;
        if (mongoConfigs.containsKey(pid)) {
            mongoConfig = mongoConfigs.get(pid);
        } else {
            mongoConfig = new JahiaMongoConfig(dictionary);
            mongoConfigs.put(pid, mongoConfig);
            deleteConfig(pidsByProviderKey.put(mongoConfig.getProviderKey(), pid));
        }
        mongoConfig.setContext(context, dictionary);
        flushRelatedCaches();
    }

    /**
     *
     * @param pid
     */
    private void deleteConfig(final String pid) {
        if (pid == null) {
            return;
        }
        try {
            final Configuration cfg = configurationAdmin.getConfiguration(pid);
            if (cfg != null) {
                cfg.delete();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to delete Mongo configuration for pid " + pid, e);
        }
    }

    /**
     *
     * @param pid
     */
    @Override
    public void deleted(final String pid) {
        final JahiaMongoConfig mongoConfig = mongoConfigs.remove(pid);
        final String existingPid = mongoConfig != null ? pidsByProviderKey.get(mongoConfig.getProviderKey()) : null;
        if (existingPid != null && existingPid.equals(pid)) {
            pidsByProviderKey.remove(mongoConfig.getProviderKey());
            mongoConfig.unregister();
            flushRelatedCaches();
        }
    }

    /**
     *
     * @return
     */
    public String getName() {
        return "org.jahia.services.usermanager.mongo";
    }

    /**
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    /**
     *
     * @param providerKey
     * @return
     */
    public String getConfigPID(final String providerKey) {
        return pidsByProviderKey.get(providerKey);
    }

    /**
     *
     */
    private void flushRelatedCaches() {
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaUserManagerService.userPathByUserNameCache",
                true);
        CacheHelper.flushEhcacheByName(MongoCacheManager.MONGO_USERS_CACHE, true);
    }
}
