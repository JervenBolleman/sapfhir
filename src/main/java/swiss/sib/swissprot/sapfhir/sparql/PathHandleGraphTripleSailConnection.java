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
import java.util.Iterator;
import java.util.List;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.BindingAssigner;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.FilterOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractSailConnection;
import swiss.sib.swissprot.handlegraph4jrdf.FALDO;
import swiss.sib.swissprot.handlegraph4jrdf.VG;

/**
 *
 * @author jbollema
 * @param <P> the type of PathHandle
 * @param <S> the type of StepHandle
 * @param <E> the type of EdgeHandle
 * @param <N> the type of NodeHandle
 */
public class PathHandleGraphTripleSailConnection<P extends PathHandle, S extends StepHandle, N extends NodeHandle, E extends EdgeHandle<N>> extends AbstractSailConnection {

    private final PathHandleGraphSail<P, S, N, E> phg;
    private final SPARQLServiceResolver fd;

    public PathHandleGraphTripleSailConnection(PathHandleGraphSail<P, S, N, E> phg) {
        super(phg);
        this.phg = phg;
        this.fd = new SPARQLServiceResolver();
    }

    @Override
    protected void closeInternal() throws SailException {
        //Does nothing
    }

    @Override
    protected CloseableIteration<? extends BindingSet, QueryEvaluationException> evaluateInternal(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings, boolean includeInferred) throws SailException {
        try {

            PathHandleGraphTripleSource<P, S, N, E> tripleSource = new PathHandleGraphTripleSource<>(phg);
            EvaluationStrategy strategy = new StrictEvaluationStrategy(tripleSource, fd);
            tupleExpr = tupleExpr.clone();
            new BindingAssigner().optimize(tupleExpr, dataset, bindings);

            new FilterOptimizer().optimize(tupleExpr, dataset, bindings);
            return strategy.evaluate(tupleExpr, EmptyBindingSet.getInstance());
        } catch (QueryEvaluationException e) {
            throw new SailException(e);
        }
    }

    @Override
    protected CloseableIteration<? extends Resource, SailException> getContextIDsInternal() throws SailException {
        return new EmptyIteration<>();
    }

    @Override
    protected CloseableIteration<? extends Statement, SailException> getStatementsInternal(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts) throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected long sizeInternal(Resource... contexts) throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void startTransactionInternal() throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void commitInternal() throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void rollbackInternal() throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void clearInternal(Resource... contexts) throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected CloseableIteration<? extends Namespace, SailException> getNamespacesInternal() throws SailException {
        Iterator<SimpleNamespace> ns = List.of(new SimpleNamespace(FALDO.PREFIX, FALDO.NAMESPACE),
                new SimpleNamespace(VG.PREFIX, VG.NAMESPACE),
                new SimpleNamespace(RDFS.PREFIX, RDFS.NAMESPACE))
                .iterator();
        return new CloseableIteratorIteration<>(ns);
    }

    @Override
    protected String getNamespaceInternal(String prefix) throws SailException {
        switch (prefix) {
            case FALDO.PREFIX:
                return FALDO.NAMESPACE;
            case VG.PREFIX:
                return VG.NAMESPACE;
            default:
                return null;
        }
    }

    @Override
    protected void setNamespaceInternal(String prefix, String name) throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void removeNamespaceInternal(String prefix) throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void clearNamespacesInternal() throws SailException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
