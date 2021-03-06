/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.wiki.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.WikiDescriptor;
import org.xwiki.wiki.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Used to refresh the Wiki Descriptor Cache.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Component
@Named("wikidescriptor")
@Singleton
public class WikiDescriptorListener implements EventListener
{
    /**
     * Relative reference to the XWiki.XWikiServerClass containing wiki descriptor metadata.
     */
    static final EntityReference SERVER_CLASS = new EntityReference("XWikiServerClass", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    @Inject
    private WikiDescriptorBuilder builder;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public String getName()
    {
        return "wikidescriptor";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(
            new DocumentCreatedEvent(),
            new DocumentUpdatedEvent(),
            new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        // If the document is deleted then check the original document to see if it had XWiki Server objects and if
        // so then unregister them
        if (event instanceof DocumentDeletedEvent || event instanceof DocumentUpdatedEvent) {
            removeExistingDescriptor(document.getOriginalDocument());
        }

        // Register the new XWiki Server objects if any
        List<BaseObject> serverClassObjects = document.getXObjects(SERVER_CLASS);
        if (serverClassObjects != null && !serverClassObjects.isEmpty()) {
            WikiDescriptor descriptor = this.builder.build(serverClassObjects, document);
            if (descriptor != null) {
                this.wikiDescriptorManager.set(descriptor);
            }
        }
    }

    private void removeExistingDescriptor(XWikiDocument document)
    {
        List<BaseObject> existingServerClassObjects = document.getXObjects(SERVER_CLASS);
        if (existingServerClassObjects != null && !existingServerClassObjects.isEmpty()) {
            WikiDescriptor existingDescriptor =
                this.builder.build(existingServerClassObjects, document);
            if (existingDescriptor != null) {
                this.wikiDescriptorManager.remove(existingDescriptor);
            }
        }
    }
}
