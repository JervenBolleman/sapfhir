/**
 * Copyright (c) 2024, SIB Swiss Institute of Bioinformatics
 * and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package swiss.sib.swissprot.sapfhir.statements;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Only to be used when we know that the values are good.
 * 
 * @param subject   may not be null
 * @param predicate may not be null
 * @param object    may not be null
 */
record UnsafeStatement(Resource subject, IRI predicate, Value object) implements Statement {

	private static final long serialVersionUID = 1L;

	@Override
	public Resource getSubject() {
		return subject;
	}

	@Override
	public IRI getPredicate() {
		return predicate;
	}

	@Override
	public Value getObject() {
		return object;
	}

	@Override
	public Resource getContext() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof Statement)) {
			return false;
		}

		Statement that = (Statement) o;

		return subject.equals(that.getSubject()) && predicate.equals(that.getPredicate())
				&& object.equals(that.getObject()) && that.getContext() == null;
	}

	@Override
	public int hashCode() {
		int result = 31 + subject.hashCode();
		result = 31 * result + predicate.hashCode();
		result = 31 * result + object.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "(" + subject + ", " + predicate + ", " + object + ") [" + null + "]";
	}

}
