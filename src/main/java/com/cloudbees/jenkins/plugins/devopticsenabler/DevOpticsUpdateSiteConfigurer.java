/*
 * The MIT License
 *
 * Copyright (c) 2018, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.devopticsenabler;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.DownloadService;
import hudson.model.UpdateCenter;
import hudson.model.UpdateSite;
import hudson.util.PersistedList;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adds the custom update site.
 */
public final class DevOpticsUpdateSiteConfigurer {

    /**
     * Our logger.
     */
    private static final Logger LOGGER = Logger.getLogger(DevOpticsUpdateSiteConfigurer.class.getName());

    /**
     * The update center URL.
     */
    private static final String UPDATE_CENTER_URL =
            "http://jenkins-updates.cloudbees.com/update-center/devoptics/update-center.json";

    /**
     * The current update center ID.
     */
    private static final String UPDATE_CENTER_ID = "devoptics";

    /** Constructor. */
    public DevOpticsUpdateSiteConfigurer() {
    }

    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED)
    public static void configureUpdateCenter() throws Exception {
        LOGGER.log(Level.FINE, "Checking whether the DevOptics Update center is configured");
        UpdateCenter updateCenter = Jenkins.getInstance().getUpdateCenter();
        boolean changed = false;
        boolean found = false;
        DevOpticsUpdateSite added = null;
        synchronized (updateCenter) {
            PersistedList<UpdateSite> sites = updateCenter.getSites();
            if (sites.isEmpty()) {
                // likely the list has not been loaded yet
                updateCenter.load();
                sites = updateCenter.getSites();
            }
            List<UpdateSite> newList = new ArrayList<>();
            for (UpdateSite site : sites) {
                if (site instanceof DevOpticsUpdateSite) {
                    if (!found && UPDATE_CENTER_ID.equals(site.getId()) && UPDATE_CENTER_URL.equals(site.getUrl())) {
                        // We have found it and is correct, we keep it in the list
                        LOGGER.log(Level.FINEST, "DevOptics Update center already configured");
                        found = true;
                        newList.add(site);
                    } else {
                        // We already have the correct one or it is incorrect. We skip it.
                        LOGGER.log(Level.FINEST, "Removing extra/invalid DevOptics Update center");
                        changed = true;
                    }
                } else {
                    newList.add(site);
                }
            }
            // If we have not found it, we add it
            if (!found) {
                LOGGER.log(Level.INFO, "Adding DevOptics Update center");
                added = new DevOpticsUpdateSite(UPDATE_CENTER_ID, UPDATE_CENTER_URL);
                newList.add(added);
                changed = true;
            }
            // If we have changed anything we update the list
            if (changed) {
                LOGGER.log(Level.FINEST, "Reconfiguring update sites");
                sites.replaceBy(newList);
                if (added != null) {
                    added.updateDirectly(DownloadService.signatureCheck);
                }
            } else {
                LOGGER.log(Level.FINEST, "No update site reconfiguration needed");
            }
        }
    }

}
