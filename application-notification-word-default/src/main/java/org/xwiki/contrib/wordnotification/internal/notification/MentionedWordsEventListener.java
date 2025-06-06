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
package org.xwiki.contrib.wordnotification.internal.notification;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.MentionedWordsEvent;
import org.xwiki.contrib.wordnotification.RemovedWordsEvent;
import org.xwiki.contrib.wordnotification.WordsAnalysisResults;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listener of {@link MentionedWordsEvent} and {@link RemovedWordsEvent} in charge of triggering actual notifications
 * to the appropriate dedicated users.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(MentionedWordsEventListener.NAME)
@Singleton
public class MentionedWordsEventListener extends AbstractLocalEventListener
{
    static final String NAME = "MentionedWordsEventListener";

    static final String NOTIFIER_SOURCE = "org.xwiki.contrib.wordnotification:application-notification-word-default";

    @Inject
    private Logger logger;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    /**
     * Default constructor.
     */
    public MentionedWordsEventListener()
    {
        super(NAME, List.of(new MentionedWordsEvent(), new RemovedWordsEvent()));
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        boolean isRemoval = (event instanceof RemovedWordsEvent);
        if (data instanceof WordsAnalysisResults) {
            this.notifyAbout((WordsAnalysisResults) data, null, isRemoval);
        } else if (data instanceof Pair) {
            Pair<WordsAnalysisResults, WordsAnalysisResults> results =
                (Pair<WordsAnalysisResults, WordsAnalysisResults>) data;
            this.notifyAbout(results.getRight(), results.getLeft(), isRemoval);
        } else {
            this.logger.error("Cannot process the following data class for mentioned words event: [{}]",
                data.getClass());
        }
    }

    private void notifyAbout(WordsAnalysisResults currentResult, WordsAnalysisResults previousResult, boolean isRemoval)
    {
        long newOccurrences = currentResult.getOccurrences();
        boolean isNew = false;
        long oldOccurrences;
        if (previousResult == null) {
            oldOccurrences = 0;
            isNew = true;
        } else {
            oldOccurrences = previousResult.getOccurrences();
        }

        DocumentVersionReference reference = currentResult.getReference();
        try {
            XWikiDocument document =
                this.documentRevisionProvider.getRevision(reference, reference.getVersion().toString());
            if (document != null) {
                WordsQuery query = currentResult.getQuery();
                String userTarget = this.userReferenceSerializer.serialize(query.getUserReference());
                AbstractMentionedWordsRecordableEvent event;

                UserReference author = document.getAuthors().getOriginalMetadataAuthor();
                if (isRemoval) {
                    event = new RemovedWordsRecordableEvent(Set.of(userTarget), newOccurrences, oldOccurrences,
                        query.getQuery(), author);
                } else {
                    event = new MentionedWordsRecordableEvent(Set.of(userTarget), newOccurrences, oldOccurrences,
                        query.getQuery(), author);
                }
                event.setNew(isNew);

                this.observationManager.notify(event, NOTIFIER_SOURCE, document);
            } else {
                this.logger.warn("Cannot notify about [{}] as it cannot be retrieved anymore.", reference);
            }
        } catch (XWikiException e) {
            this.logger.error("Error when trying to load document with reference [{}]. Root cause: [{}]",
                reference, ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
