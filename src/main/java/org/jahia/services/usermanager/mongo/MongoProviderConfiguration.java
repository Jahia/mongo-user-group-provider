package org.jahia.services.usermanager.mongo;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.modules.external.users.ExternalUserGroupService;
import org.jahia.modules.external.users.UserGroupProviderConfiguration;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.usermanager.mongo.config.UserConfig;
import org.jahia.settings.SettingsBean;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

/**
 * Class to implement specific behaviour for configuration creation/edition/deletion in server settings
 */
public class MongoProviderConfiguration implements UserGroupProviderConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoProviderConfiguration.class);
    private static final long serialVersionUID = 8082529526561969689L;
    public static final String PROP_KEY = "propKey";
    public static final String PROP_VALUE = "propValue";
    public static final String CONFIG_NAME = "configName";
    public static final String MODULES_MONGO_USER_GROUP_PROVIDER_EDIT_JSP =
            "/modules/mongo/userGroupProviderEdit.jsp";
    public static final String MONGO = "mongo";

    private String userGroupProviderClass;
    private ExternalUserGroupService externalUserGroupService;
    private JahiaMongoConfigFactory jahiaMongoConfigFactory;
    private ConfigurationAdmin configurationAdmin;

    /**
     *
     * @return
     */
    @Override
    public String getName() {
        return "Mongo";
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isCreateSupported() {
        return true;
    }

    /**
     *
     * @return
     */
    @Override
    public String getCreateJSP() {
        return MODULES_MONGO_USER_GROUP_PROVIDER_EDIT_JSP;
    }

    /**
     *
     * @param parameters
     * @param flashScope
     * @return
     * @throws Exception
     */
    @Override
    public String create(final Map<String, Object> parameters, final Map<String, Object> flashScope) throws Exception {
        final Properties properties = getProperties(parameters);
        flashScope.put("mongoProperties", properties);

        // config name
        String configName = (String) parameters.get(CONFIG_NAME);
        if (StringUtils.isBlank(configName)) {
            // if we didn't provide a not-blank config name, generate one
            configName = MONGO + System.currentTimeMillis();
        }
        // normalize the name
        configName = JCRContentUtils.generateNodeName(configName);
        flashScope.put(CONFIG_NAME, configName);

        // provider key
        final String providerKey = MONGO + "." + configName;
        configName = jahiaMongoConfigFactory.getName() + "-" + configName + ".cfg";

        // check that we don't already have a provider with that key
        final String pid = jahiaMongoConfigFactory.getConfigPID(providerKey);
        if (pid != null) {
            throw new Exception("An Mongo provider with key '" + providerKey + "' already exists");
        }

        if (!testConnection(properties)) {
            throw new Exception("Connection to the Mongo server impossible");
        }

        final File file = new File(SettingsBean.getInstance().getJahiaModulesDiskPath());
        if (file.exists()) {
            final FileOutputStream out = new FileOutputStream(new File(file, configName));
            try {
                properties.store(out, "");
            } finally {
                IOUtils.closeQuietly(out);
            }
        } else {
            final Configuration configuration = configurationAdmin
                    .createFactoryConfiguration(jahiaMongoConfigFactory.getName());
            properties.put(JahiaMongoConfig.MONGO_PROVIDER_KEY, providerKey);
            configuration.update((Dictionary) properties);
        }
        return providerKey;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isEditSupported() {
        return true;
    }

    /**
     *
     * @return
     */
    @Override
    public String getEditJSP() {
        return MODULES_MONGO_USER_GROUP_PROVIDER_EDIT_JSP;
    }

    /**
     *
     * @param providerKey
     * @param parameters
     * @param flashScope
     * @throws Exception
     */
    @Override
    public void edit(final String providerKey,
                     final Map<String, Object> parameters,
                     final Map<String, Object> flashScope) throws Exception {
        final Properties properties = getProperties(parameters);
        flashScope.put("mongoProperties", properties);
        if (!testConnection(properties)) {
            throw new Exception("Connection to the Mongo server impossible");
        }
        final String configName;
        if (providerKey.equals(MONGO)) {
            configName = jahiaMongoConfigFactory.getName() + "-config.cfg";
        } else if (providerKey.startsWith(MONGO + ".")) {
            configName = jahiaMongoConfigFactory.getName() + "-" + providerKey.substring((MONGO + ".").length())
                    + ".cfg";
        } else {
            throw new Exception("Wrong Mongo provider key: " + providerKey);
        }
        final File file = new File(SettingsBean.getInstance().getJahiaModulesDiskPath(), configName);
        if (file.exists()) {
            final FileOutputStream out = new FileOutputStream(file);
            try {
                properties.store(out, "");
            } finally {
                IOUtils.closeQuietly(out);
            }
        } else {
            final String pid = jahiaMongoConfigFactory.getConfigPID(providerKey);
            if (pid == null) {
                throw new Exception("Cannot find Mongo provider " + providerKey);
            }
            final Configuration configuration = configurationAdmin.getConfiguration(pid);
            properties.put(JahiaMongoConfig.MONGO_PROVIDER_KEY, providerKey);
            configuration.update((Dictionary) properties);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isDeleteSupported() {
        return true;
    }

    /**
     *
     * @param providerKey
     * @param flashScope
     * @throws Exception
     */
    @Override
    public void delete(final String providerKey,
                       final Map<String, Object> flashScope) throws Exception {
        final String configName;
        if (providerKey.equals(MONGO)) {
            configName = jahiaMongoConfigFactory.getName() + "-config.cfg";
        } else if (providerKey.startsWith(MONGO + ".")) {
            configName = jahiaMongoConfigFactory.getName() + "-" + providerKey.substring((MONGO + ".").length())
                    + ".cfg";
        } else {
            throw new Exception("Wrong Mongo provider key: " + providerKey);
        }
        final File file = new File(SettingsBean.getInstance().getJahiaModulesDiskPath(), configName);
        if (file.exists()) {
            file.delete();
        } else {
            final String pid = jahiaMongoConfigFactory.getConfigPID(providerKey);
            if (pid == null) {
                throw new Exception("Cannot find Mongo provider " + providerKey);
            }
            final Configuration configuration = configurationAdmin.getConfiguration(pid);
            configuration.delete();
        }
    }

    /**
     *
     * @param properties
     * @return
     * @throws Exception
     */
    private boolean testConnection(final Properties properties) throws Exception {
        final UserConfig userConfig = new UserConfig();
        BeanUtils.populate(userConfig, properties);
        final MongoClient mongoClient = new MongoClient(userConfig.getHost());
        final MongoDatabase mongoDatabase = mongoClient.getDatabase(userConfig.getDatabase());
        return mongoDatabase != null;
    }

    /**
     *
     */
    public void init() {
        externalUserGroupService.setConfiguration(userGroupProviderClass, this);
    }

    /**
     *
     * @param userGroupProviderClass
     */
    public void setUserGroupProviderClass(final String userGroupProviderClass) {
        this.userGroupProviderClass = userGroupProviderClass;
    }

    /**
     *
     * @param externalUserGroupService
     */
    public void setExternalUserGroupService(final ExternalUserGroupService externalUserGroupService) {
        this.externalUserGroupService = externalUserGroupService;
    }

    /**
     *
     * @param jahiaMongoConfigFactory
     */
    public void setJahiaMongoConfigFactory(final JahiaMongoConfigFactory jahiaMongoConfigFactory) {
        this.jahiaMongoConfigFactory = jahiaMongoConfigFactory;
    }

    /**
     *
     * @param configurationAdmin
     */
    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    /**
     *
     * @param parameters
     * @return
     * @throws Exception
     */
    private Properties getProperties(final Map<String, Object> parameters) throws Exception {
        final String[] propKeys;
        final String[] propValues;
        if (parameters.get(PROP_KEY) instanceof String) {
            propKeys = new String[]{(String) parameters.get(PROP_KEY)};
            propValues = new String[]{(String) parameters.get(PROP_VALUE)};
        } else {
            propKeys = (String[]) parameters.get(PROP_KEY);
            propValues = (String[]) parameters.get(PROP_VALUE);
        }
        final Properties properties = new Properties();
        if (propKeys != null) {
            for (int i = 0; i < propKeys.length; i++) {
                final String propValue = propValues[i];
                if (StringUtils.isNotBlank(propValue)) {
                    properties.put(propKeys[i], propValue);
                }
            }
        }
        for (final Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getKey().startsWith(PROP_VALUE + ".")) {
                final String key = StringUtils.substringAfter(entry.getKey(), PROP_VALUE + ".");
                if (StringUtils.isNotBlank((String) entry.getValue())) {
                    properties.put(key, entry.getValue());
                }
            }
        }
        if (parameters.isEmpty()) {
            throw new Exception("No property has been set");
        }
        return properties;
    }
}
