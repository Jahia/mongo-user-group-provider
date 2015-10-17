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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.usermanager.mongo.communication.MongoTemplateWrapper;
import org.jahia.services.usermanager.mongo.config.AbstractConfig;
import org.jahia.services.usermanager.mongo.config.UserConfig;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Helper class to configure Mongo user and group providers via OSGi Config Admin service.
 */
public class JahiaMongoConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(JahiaMongoConfig.class);
    public static final String MONGO_PROVIDER_KEY = "org.jahia.services.usermanager.mongo.provider.key";

    private String providerKey;
    private MongoUserGroupProvider mongoUserGroupProvider;

    /**
     * Initializes an instance of this class.
     *
     * @param dictionary configuration parameters
     */
    public JahiaMongoConfig(final Dictionary<String, ?> dictionary) {
        providerKey = computeProviderKey(dictionary);
    }

    /**
     * defines or update the context of the provider
     * @param context the Spring application context object
     * @param dictionary configuration parameters
     */
    public void setContext(final ApplicationContext context,
                           final Dictionary<String, ?> dictionary) {
        final Properties userMongoProperties = new Properties();
        final UserConfig userConfig = new UserConfig();
        final Enumeration<String> keys = dictionary.keys();

        String fileName = null;
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            if (Constants.SERVICE_PID.equals(key) ||
                    ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)) {
                continue;
            } else if ("felix.fileinstall.filename".equals(key)) {
                fileName = (String) dictionary.get(key);
                continue;
            }
            final Object value = dictionary.get(key);
            if (key.startsWith("user.")) {
                buildConfig(userMongoProperties, userConfig, key, value, true);
            } else {
                userMongoProperties.put(transformPropKeyToBeanAttr(key), value);
            }
        }
        try {
            // populate config beans
            BeanUtils.populate(userConfig, userMongoProperties);

            // handle defaults values
            userConfig.handleDefaults();

            final String host = userConfig.getHost();
            final String database = userConfig.getDatabase();
            final MongoClient mongoClient = new MongoClient(host);
            final MongoDatabase mongoDatabase = mongoClient.getDatabase(database);

            if (mongoUserGroupProvider == null) {
                mongoUserGroupProvider = (MongoUserGroupProvider) context.getBean("mongoUserGroupProvider");
            } else {
                // Deactivate the provider before reconfiguring it.
                mongoUserGroupProvider.unregister();
            }

            mongoUserGroupProvider.setKey(providerKey);
            mongoUserGroupProvider.setUserConfig(userConfig);
            mongoUserGroupProvider.setMongoTemplateWrapper(new MongoTemplateWrapper(mongoDatabase));

            // Activate (again).
            mongoUserGroupProvider.register();
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Invalid Mongo configuration:" + fileName + ", " +
                    "please refer to the Mongo configuration documentation", e);
        }
    }

    /**
     *
     */
    public void unregister() {
        if (mongoUserGroupProvider != null) {
            unregisterUserProvider();
        }
    }

    /**
     *
     */
    private void unregisterUserProvider() {
        mongoUserGroupProvider.unregister();
        mongoUserGroupProvider = null;
    }

    /**
     *
     * @param dictionary
     * @return
     */
    private String computeProviderKey(final Dictionary<String, ?> dictionary) {
        final String provideKey = (String) dictionary.get(MONGO_PROVIDER_KEY);
        if (provideKey != null) {
            return provideKey;
        }
        final String filename = (String) dictionary.get("felix.fileinstall.filename");
        final String factoryPid = (String) dictionary.get(ConfigurationAdmin.SERVICE_FACTORYPID);
        String confId;
        if (StringUtils.isBlank(filename)) {
            confId = (String) dictionary.get(Constants.SERVICE_PID);
            if (StringUtils.startsWith(confId, factoryPid + ".")) {
                confId = StringUtils.substringAfter(confId, factoryPid + ".");
            }
        } else {
            confId = StringUtils.removeEnd(StringUtils.substringAfter(filename,
                    factoryPid + "-"), ".cfg");
        }
        return (StringUtils.isBlank(confId) || "config".equals(confId)) ? "mongo" : ("mongo." + confId);
    }

    /**
     *
     * @param key
     * @return
     */
    private String transformPropKeyToBeanAttr(final String key) {
        final Iterable<String> upperStrings = Iterables.transform(Arrays.asList(StringUtils.split(key, '.')),
                new Function<String, String>() {
            public String apply(String input) {
                return (input == null) ? null : StringUtils.capitalize(input);
            }
        });
        return StringUtils.uncapitalize(StringUtils.join(upperStrings.iterator(), ""));
    }

    /**
     *
     * @param properties
     * @param config
     * @param key
     * @param value
     * @param isUser
     */
    private void buildConfig(final Properties properties,
                             final AbstractConfig config,
                             final String key,
                             final Object value,
                             final boolean isUser) {
        if (key.contains(".attribute.map")) {
            config.getAttributesMapper().put(StringUtils.substringBetween(key, "user.",
                            ".attribute.map").replace("_", ":"),
                    (String) value);
        } else if (key.contains("search.wildcards.attributes")) {
            if (StringUtils.isNotEmpty((String) value)) {
                for (String wildcardAttr : ((String) value).split(",")) {
                    config.getSearchWildcardsAttributes().add(wildcardAttr.trim());
                }
            }
        } else {
            properties.put(transformPropKeyToBeanAttr(key.substring(isUser ? 5 : 6)), value);
        }
    }

    /**
     *
     * @return
     */
    public String getProviderKey() {
        return providerKey;
    }
}
