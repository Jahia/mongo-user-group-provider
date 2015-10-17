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
package org.jahia.services.usermanager.mongo.config;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;

/**
 * User specific config provide by the org.jahia.services.usermanager.mongo config file
 * @author kevan
 */
public class UserConfig extends AbstractConfig {
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    private String uidSearchName;
    private String uidSearchAttribute = EMAIL;

    public UserConfig() {
        super();
    }

    public void handleDefaults() {
        if (getSearchWildcardsAttributes().isEmpty()) {
            setSearchWildcardsAttributes(Sets.newHashSet(EMAIL, FIRST_NAME, LAST_NAME, EMAIL));
        }
        if (getAttributesMapper().isEmpty()) {
            getAttributesMapper().put(USERNAME, uidSearchAttribute);
            getAttributesMapper().put(PASSWORD, PASSWORD);
            getAttributesMapper().put("j:firstName", FIRST_NAME);
            getAttributesMapper().put("j:lastName", LAST_NAME);
            getAttributesMapper().put("j:email", EMAIL);
            getAttributesMapper().put("j:organization", "company");
        }
    }

    public boolean isMinimalSettingsOk() {
        return StringUtils.isNotEmpty(getUidSearchName());
    }

    public String getUidSearchName() {
        return uidSearchName;
    }

    public void setUidSearchName(final String uidSearchName) {
        this.uidSearchName = uidSearchName;
    }

    public String getUidSearchAttribute() {
        return uidSearchAttribute;
    }

    public void setUidSearchAttribute(final String uidSearchAttribute) {
        this.uidSearchAttribute = uidSearchAttribute;
    }
}
