package org.jahia.services.usermanager.mongo.config;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Abstract config provide by the org.jahia.services.usermanager.mongo config file
 * @author kevan
 */
public abstract class AbstractConfig {
    private String host;
    private String port;
    private String dbUsername;
    private String dbPassword;
    private String database;
    private String collection;
    private String targetSite;

    private long searchCountlimit = 100;
    private Set<String> searchWildcardsAttributes = Sets.newHashSet();
    private Map<String, String> attributesMapper = Maps.newHashMap();

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(final String port) {
        this.port = port;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(final String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(final String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(final String database) {
        this.database = database;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(final String collection) {
        this.collection = collection;
    }

    public String getTargetSite() {
        return targetSite;
    }

    public void setTargetSite(final String targetSite) {
        this.targetSite = targetSite;
    }

    public long getSearchCountlimit() {
        return searchCountlimit;
    }

    public void setSearchCountlimit(final long searchCountlimit) {
        this.searchCountlimit = searchCountlimit;
    }

    public Set<String> getSearchWildcardsAttributes() {
        return searchWildcardsAttributes;
    }

    public void setSearchWildcardsAttributes(final Set<String> searchWildcardsAttributes) {
        this.searchWildcardsAttributes = searchWildcardsAttributes;
    }

    public Map<String, String> getAttributesMapper() {
        return attributesMapper;
    }

    public void setAttributesMapper(final Map<String, String> attributesMapper) {
        this.attributesMapper = attributesMapper;
    }
}
