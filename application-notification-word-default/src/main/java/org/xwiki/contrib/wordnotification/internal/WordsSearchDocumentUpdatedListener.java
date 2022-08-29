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
package org.xwiki.contrib.wordnotification.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.TaskManager;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listener responsible to create the analysis task whenever a document is updated.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(WordsSearchDocumentUpdatedListener.NAME)
@Singleton
public class WordsSearchDocumentUpdatedListener extends AbstractLocalEventListener
{
    static final String NAME = "WordsSearchDocumentUpdatedListener";

    @Inject
    private TaskManager taskManager;

    /**
     * Default constructor.
     */
    public WordsSearchDocumentUpdatedListener()
    {
        super(NAME, List.of(new DocumentUpdatedEvent(), new DocumentCreatedEvent()));
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        this.taskManager.addTask(doc.getDocumentReference().getWikiReference().getName(), doc.getId(), doc.getVersion(),
            WordsSearchTaskConsumer.WORDS_SEARCH_TASK_HINT);
    }
}
