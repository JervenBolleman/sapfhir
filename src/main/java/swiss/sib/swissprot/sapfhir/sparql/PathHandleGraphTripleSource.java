/*
 * Copyright (C) 2020 SIB Swiss Institute of Bioinformatics.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package swiss.sib.swissprot.sapfhir.sparql;

import io.github.vgteam.handlegraph4j.EdgeHandle;
import io.github.vgteam.handlegraph4j.NodeHandle;
import io.github.vgteam.handlegraph4j.PathHandle;
import io.github.vgteam.handlegraph4j.StepHandle;
import io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.filter;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.flatMap;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.from;
import static io.github.vgteam.handlegraph4j.iterators.AutoClosedIterator.map;
import swiss.sib.swissprot.sapfhir.statements.NodeRelatedStatementProvider;
import swiss.sib.swissprot.sapfhir.statements.PathRelatedStatementProvider;
import swiss.sib.swissprot.sapfhir.statements.StatementProvider;
import swiss.sib.swissprot.sapfhir.statements.StepPositionStatementProvider;
import swiss.sib.swissprot.sapfhir.statements.StepRelatedStatementProvider;
import swiss.sib.swissprot.sapfhir.values.HandleGraphValueFactory;
import java.util.List;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;

/**
 *
 * @author jbollema
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class PathHandleGraphTripleSource<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>>
        implements TripleSource {

    private final HandleGraphValueFactory<P, S, N, E> vf;
    private final List<StatementProvider> statementProviders;

    public PathHandleGraphTripleSource(PathHandleGraphSail<P, S, N, E> sail) {
        this.vf = new HandleGraphValueFactory<>(sail);
        this.statementProviders = List.of(
                new StepPositionStatementProvider<>(sail, vf),
                new NodeRelatedStatementProvider<>(sail, vf),
                new StepRelatedStatementProvider<>(sail, vf),
                new PathRelatedStatementProvider<>(sail, vf));
    }

    @Override
    public CloseableIteration<? extends Statement, QueryEvaluationException>
            getStatements(Resource subject,
                    IRI predicate,
                    Value object,
                    Resource... rsrcs)
            throws QueryEvaluationException {
        //We know that we don't have any blanknodes
        if (subject instanceof BNode || object instanceof BNode) {
            return new EmptyIteration<>();
        }
        var from = from(statementProviders.iterator());
        var subs = filter(from, sp -> sp.subjectMightReturnValues(subject));
        var preds = filter(subs, sp -> sp.predicateMightReturnValues(predicate));
        var obs = filter(preds, sp -> sp.objectMightReturnValues(object));

        var generateStatements = map(obs, ps -> statements(ps, subject, predicate, object));
        return new CloseableIterationFromAutoClosedIterator(flatMap(generateStatements));
    }

    private static AutoClosedIterator<Statement> statements(StatementProvider ps,
            Resource subject, IRI predicate, Value object) {
        return ps.getStatements(subject, predicate, object);
    }

    @Override
    public HandleGraphValueFactory getValueFactory() {
        return vf;
    }

    public double estimateCardinality(Resource subj, IRI pred, Value obj) {
        double estimate = 0;
        for (StatementProvider sp : statementProviders) {
            estimate = Math.max(estimate, sp.estimateCardinality(subj, pred, obj));
        }
        return estimate;
    }

    private static class CloseableIterationFromAutoClosedIterator
            implements CloseableIteration<Statement, QueryEvaluationException> {

        private final AutoClosedIterator<Statement> providedAsIter;
        private Statement last;

        public CloseableIterationFromAutoClosedIterator(AutoClosedIterator<Statement> providedAsIter) {
            this.providedAsIter = providedAsIter;
        }

        @Override
        public void close() throws QueryEvaluationException {
            providedAsIter.close();
        }

        @Override
        public boolean hasNext() throws QueryEvaluationException {
            return providedAsIter.hasNext();
        }

        @Override
        public Statement next() throws QueryEvaluationException {
            Statement next = providedAsIter.next();
            assert next != null : "null after" + last;
            last = next;
            return next;
        }

        @Override
        public void remove() throws QueryEvaluationException {
            providedAsIter.remove();
        }
    }
}
