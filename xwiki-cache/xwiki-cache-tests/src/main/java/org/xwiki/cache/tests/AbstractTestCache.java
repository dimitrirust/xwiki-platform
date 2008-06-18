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
package org.xwiki.cache.tests;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;

import com.xpn.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Base class for testing cache component implementation.
 * 
 * @version $Id: $
 */
public abstract class AbstractTestCache extends AbstractXWikiComponentTestCase implements ApplicationContext
{
    /**
     * The first key.
     */
    private static final String KEY = "key";

    /**
     * The second key.
     */
    private static final String KEY2 = "key2";

    /**
     * The value of the first key.
     */
    private static final String VALUE = "value";

    /**
     * The value of the second key.
     */
    private static final int VALUE2 = 2;

    /**
     * The role hint of the cache component implementation to test.
     */
    protected String roleHint;

    /**
     * The container.
     */
    private Container container;

    /**
     * @param roleHint the role hint of the cache component implementation to test.
     */
    protected AbstractTestCache(String roleHint)
    {
        this.roleHint = roleHint;
    }

    /**
     * @return the component manager to get a cache component.
     * @throws Exception error when initializing component manager.
     */
    public ComponentManager getComponentManager() throws Exception
    {
        ComponentManager cm = getComponentManager();

        if (this.container == null) {
            // Initialize the Container
            Container c = (Container) cm.lookup(Container.ROLE);
            c.setApplicationContext(this);
        }

        return cm;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getResourceAsStream(java.lang.String)
     */
    public InputStream getResourceAsStream(String resourceName)
    {
        return getClass().getResourceAsStream(resourceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.container.ApplicationContext#getResource(java.lang.String)
     */
    public URL getResource(String resourceName) throws MalformedURLException
    {
        return getClass().getResource(resourceName);
    }

    /**
     * @return a instance of the cache factory.
     * @throws Exception error when searching for cache factory component.
     */
    public CacheFactory getCacheFactory() throws Exception
    {
        CacheFactory factory = (CacheFactory) getComponentManager().lookup(CacheFactory.ROLE, this.roleHint);

        assertNotNull(factory);

        return factory;
    }

    // ///////////////////////////////////////////////////////::
    // Tests

    /**
     * Validate factory initialization.
     * 
     * @throws Exception error.
     */
    public void testGetFactory() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        CacheFactory factory2 = getCacheFactory();

        assertSame(factory, factory2);
    }

    /**
     * Validate some basic cache use case without any constraints.
     * 
     * @throws Exception error.
     */
    public void testCreateAndDestroyCacheSimple() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        assertNotNull(cache);

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        assertEquals(VALUE, cache.get(KEY));
        assertEquals(VALUE2, cache.get(KEY2));

        cache.dispose();
    }

    /**
     * Validate {@link Cache#remove(String)}.
     * 
     * @throws Exception error.
     */
    public void testRemove() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        cache.remove(KEY);

        assertNull(cache.get(KEY));
        assertEquals(VALUE2, cache.get(KEY2));
    }

    /**
     * Validate {@link Cache#removeAll()}.
     * 
     * @throws Exception error.
     */
    public void testRemoveAll() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        cache.set(KEY, VALUE);
        cache.set(KEY2, VALUE2);

        cache.removeAll();

        assertNull(cache.get(KEY));
        assertNull(cache.get(KEY2));
    }

    /**
     * Validate event management.
     * 
     * @throws Exception error.
     */
    public void testEvents() throws Exception
    {
        CacheFactory factory = getCacheFactory();

        Cache<Object> cache = factory.newCache(new CacheConfiguration());

        CacheEntryListenerTest eventListener = new CacheEntryListenerTest();

        cache.addCacheEntryListener(eventListener);

        cache.set(KEY, VALUE);

        assertNotNull(eventListener.getAddedEvent());
        assertSame(cache, eventListener.getAddedEvent().getCache());
        assertEquals(KEY, eventListener.getAddedEvent().getEntry().getKey());
        assertEquals(VALUE, eventListener.getAddedEvent().getEntry().getValue());

        cache.set(KEY, VALUE2);

        assertNotNull(eventListener.getModifiedEvent());
        assertSame(cache, eventListener.getModifiedEvent().getCache());
        assertEquals(KEY, eventListener.getModifiedEvent().getEntry().getKey());
        assertEquals(VALUE2, eventListener.getModifiedEvent().getEntry().getValue());

        cache.remove(KEY);
        cache.get(KEY);

        assertNotNull(eventListener.getModifiedEvent());
        assertSame(cache, eventListener.getModifiedEvent().getCache());
        assertEquals(KEY, eventListener.getModifiedEvent().getEntry().getKey());
        assertEquals(VALUE2, eventListener.getModifiedEvent().getEntry().getValue());
    }
}

/**
 * Class used to test cache event management.
 * 
 * @version $Id: $
 */
class CacheEntryListenerTest implements CacheEntryListener<Object>
{
    /**
     * Event object received with last insertion.
     */
    private CacheEntryEvent<Object> addedEvent;

    /**
     * Event object received with last modification.
     */
    private CacheEntryEvent<Object> modifiedEvent;

    /**
     * Event object received with last remove.
     */
    private CacheEntryEvent<Object> removedEvent;

    /**
     * @return event object received with last insertion.
     */
    public CacheEntryEvent<Object> getAddedEvent()
    {
        return addedEvent;
    }

    /**
     * @return event object received with last modification.
     */
    public CacheEntryEvent<Object> getModifiedEvent()
    {
        return modifiedEvent;
    }

    /**
     * @return event object received with last remove.
     */
    public CacheEntryEvent<Object> getRemovedEvent()
    {
        return removedEvent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryAdded(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryAdded(CacheEntryEvent<Object> event)
    {
        this.addedEvent = event;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryModified(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryModified(CacheEntryEvent<Object> event)
    {
        this.modifiedEvent = event;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.event.CacheEntryListener#cacheEntryRemoved(org.xwiki.cache.event.CacheEntryEvent)
     */
    public void cacheEntryRemoved(CacheEntryEvent<Object> event)
    {
        this.removedEvent = event;
    }
}
