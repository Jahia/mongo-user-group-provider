package org.jahia.services.usermanager.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jahia.modules.external.users.BaseUserGroupProvider;
import org.jahia.modules.external.users.GroupNotFoundException;
import org.jahia.modules.external.users.Member;
import org.jahia.modules.external.users.UserNotFoundException;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserImpl;
import org.jahia.services.usermanager.mongo.cache.MongoCacheManager;
import org.jahia.services.usermanager.mongo.cache.MongoUserCacheEntry;
import org.jahia.services.usermanager.mongo.communication.BaseMongoActionCallback;
import org.jahia.services.usermanager.mongo.communication.MongoTemplateWrapper;
import org.jahia.services.usermanager.mongo.config.UserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.jcr.RepositoryException;
import javax.naming.NamingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.mongodb.client.model.Filters.in;

/**
 * Implementation of UserGroupProvider for Spring Mongo
 *
 * @author david
 */
public class MongoUserGroupProvider extends BaseUserGroupProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoUserGroupProvider.class);

    private MongoTemplateWrapper mongoTemplateWrapper;
    private UserConfig userConfig;
    private MongoCacheManager mongoCacheManager;

    /**
     *
     * @param name
     * @return
     * @throws UserNotFoundException
     */
    @Override
    public JahiaUser getUser(final String name) throws UserNotFoundException {
        final MongoUserCacheEntry userCacheEntry = getUserCacheEntry(name, true);
        if (userCacheEntry == null) {
            throw new UserNotFoundException("unable to find user " + name + " on provider " + getKey());
        } else {
            return userCacheEntry.getUser();
        }
    }

    /**
     *
     * @param name
     * @return
     * @throws GroupNotFoundException
     */
    @Override
    public JahiaGroup getGroup(final String name) throws GroupNotFoundException {
        return null;
    }

    /**
     *
     * @param groupName
     * @return
     */
    @Override
    public List<Member> getGroupMembers(final String groupName) {
        return Collections.emptyList();
    }

    /**
     *
     * @param member
     * @return
     */
    @Override
    public List<String> getMembership(final Member member) {
        return Collections.emptyList();
    }

    /**
     *
     * @param searchCriteria
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public List<String> searchUsers(final Properties searchCriteria,
                                    final long offset,
                                    final long limit) {
        if (searchCriteria.size() == 1) {
            try {
                final String username;
                if (searchCriteria.containsKey("*")) {
                    username = searchCriteria.getProperty("*");
                } else {
                    username = searchCriteria.getProperty(UserConfig.USERNAME);
                }
                final JahiaUser user = getUser(username);
                return Collections.singletonList(user.getUsername());
            } catch (UserNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return Collections.emptyList();
    }

    /**
     *
     * @param searchCriteria
     * @param offset
     * @param limit
     * @return
     */
    @Override
    public List<String> searchGroups(final Properties searchCriteria,
                                     final long offset,
                                     final long limit) {
        return Collections.emptyList();
    }

    /**
     *
     * @param userName
     * @param userPassword
     * @return
     */
    @Override
    public boolean verifyPassword(final String userName,
                                  final String userPassword) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Verify password for {}", userName);
        }
        final MongoUserCacheEntry userCacheEntry = getUserCacheEntry(userName, true);
        if (userCacheEntry != null) {
            final long startTime = System.currentTimeMillis();
            final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            final String hashedPassword = passwordEncoder.encode(userPassword);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Password verified for {} in {} ms", userName, System.currentTimeMillis() - startTime);
            }
            return true;
            //hashedPassword.equals(userCacheEntry.getUser().getProperties().getProperty(UserConfig.PASSWORD));
        }
        return false;
    }

    /**
     *
     * @return
     * @throws RepositoryException
     */
    @Override
    public boolean isAvailable() throws RepositoryException {
        // do a simple search on users to check the availability
        final long startTime = System.currentTimeMillis();
        final Exception[] exception = new Exception[1];
        final boolean available = mongoTemplateWrapper.execute(
                new BaseMongoActionCallback<Boolean>(getExternalUserGroupService(), getKey()) {
                    @Override
                    public Boolean doInMongo(final MongoDatabase mongoDatabase) {
                        return mongoDatabase.getCollection(userConfig.getCollection()) != null;
                    }

                    @Override
                    public Boolean onError(final Exception e) {
                        super.onError(e);
                        exception[0] = e;
                        return false;
                    }
                });
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Is available in {} ms", System.currentTimeMillis() - startTime);
        }

        if (!available) {
            // throw an exception instead of return false to display a custom message with the
            // org.jahia.services.usermanager.mongo server url.
            throw new RepositoryException("Mongo Server '"
                    + userConfig.getHost()
                    + "' is not reachable", exception[0]);
        } else {
            return true;
        }
    }

    /**
     *
     * @return
     */
    @Override
    protected String getSiteKey() {
        return userConfig.getTargetSite();
    }

    /**
     *
     * @return
     */
    @Override
    public boolean supportsGroups() {
        return false;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "MongoUserGroupProvider{" + "getKey()='" + getKey() + '\'' + '}';
    }

    /**
     *
     * @param userConfig
     */
    public void setUserConfig(final UserConfig userConfig) {
        this.userConfig = userConfig;
    }

    /**
     *
     * @param mongoTemplateWrapper
     */
    public void setMongoTemplateWrapper(final MongoTemplateWrapper mongoTemplateWrapper) {
        this.mongoTemplateWrapper = mongoTemplateWrapper;
    }

    /**
     *
     * @param mongoCacheManager
     */
    public void setMongoCacheManager(final MongoCacheManager mongoCacheManager) {
        this.mongoCacheManager = mongoCacheManager;
    }

    /**
     *
     * @param searchCriteria
     * @param isDynamics
     * @return
     */
    private List<String> searchGroups(final Properties searchCriteria,
                                      final boolean isDynamics) {
        return Collections.emptyList();
    }

    /**
     * Retrieve the cache entry for a given username, if not found create a new one, and cache it if the param "cache"
     * set to true
     *
     * @param userName
     * @param cache
     * @return
     */
    private MongoUserCacheEntry getUserCacheEntry(final String userName,
                                                  final boolean cache) {
        MongoUserCacheEntry userCacheEntry = mongoCacheManager.getUserCacheEntryByName(getKey(), userName);
        if (userCacheEntry != null) {
            return userCacheEntry;
        } else {
            final long startTime = System.currentTimeMillis();
            userCacheEntry = mongoTemplateWrapper.execute(
                    new BaseMongoActionCallback<MongoUserCacheEntry>(getExternalUserGroupService(), getKey()) {
                        @Override
                        public MongoUserCacheEntry doInMongo(final MongoDatabase mongoDatabase) {
                            final MongoCollection<Document> userCollection = mongoDatabase.getCollection(
                                    userConfig.getCollection());
                            //Remove the * from the value before we use it to do a search in mongo.
                            final Document document = userCollection.find(in(userConfig.getUidSearchAttribute(),
                                    userName.replace("*", ""))).first();
                            if (document != null) {
                                return attributesToUserCacheEntry(document);
                            }
                            return null;
                        }
                    });
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Get user {} in {} ms", userName, System.currentTimeMillis() - startTime);
            }
            if (userCacheEntry != null && cache) {
                mongoCacheManager.cacheUser(getKey(), userCacheEntry);
            }
            return userCacheEntry;
        }
    }

    /**
     * Populate the given cache entry or create new one if the given is null with the Mongo attributes
     *
     * @param document
     * @return
     * @throws NamingException
     */
    private MongoUserCacheEntry attributesToUserCacheEntry(final Document document) {
        final String userId = (String) document.get(userConfig.getUidSearchAttribute());
        final JahiaUser jahiaUser = new JahiaUserImpl(userId, null,
                attributesToJahiaProperties(document), getKey(), null);
        final MongoUserCacheEntry cacheEntry = new MongoUserCacheEntry(userId);
        cacheEntry.setUser(jahiaUser);
        return cacheEntry;
    }

    /**
     *
     * @param document
     * @return
     */
    private Properties attributesToJahiaProperties(final Document document) {
        final Properties props = new Properties();
        final Map<String, String> attributesMapper = userConfig.getAttributesMapper();
        for (final String propertyKey : attributesMapper.keySet()) {
            final Object mongoAttribute = document.get(attributesMapper.get(propertyKey));
            if (mongoAttribute != null) {
                props.put(propertyKey, mongoAttribute);
            }
        }
        return props;
    }
}
