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
package org.xwiki.wikistream.instance.internal.output;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wikistream.instance.internal.InstanceUtils;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;

/**
 * @version $Id$
 * @since 5.2
 */
@Component
@Named(DocumentOutputInstanceWikiStreamFactory.ROLEHINT)
@Singleton
public class DocumentOutputInstanceWikiStreamFactory extends
    AbstractBeanOutputInstanceWikiStreamFactory<DocumentOutputProperties, XWikiDocumentFilter>
{
    /**
     * The id of this {@link org.xwiki.wikistream.instance.output.OutputInstanceWikiStreamFactory}.
     */
    public static final String ID = "documents";

    /**
     * Tje role hint of this {@link org.xwiki.wikistream.output.OutputWikiStreamFactory}.
     */
    public static final String ROLEHINT = InstanceUtils.ROLEHINT + '+' + ID;

    /**
     * The default constructor.
     */
    public DocumentOutputInstanceWikiStreamFactory()
    {
        super(ID);
    }
}
