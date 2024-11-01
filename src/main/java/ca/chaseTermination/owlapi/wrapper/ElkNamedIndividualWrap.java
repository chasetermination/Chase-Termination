/*
 * #%L
 * ELK OWL API Binding
 * 
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2011 Department of Computer Science, University of Oxford
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.chaseTermination.owlapi.wrapper;

import org.semanticweb.elk.owl.interfaces.ElkNamedIndividual;
import org.semanticweb.elk.owl.visitors.ElkEntityVisitor;
import org.semanticweb.elk.owl.visitors.ElkIndividualVisitor;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

/**
 * Implements the {@link ElkNamedIndividual} interface by wrapping instances of
 * {@link OWLNamedIndividual}
 * 
 * @author Yevgeny Kazakov
 * 
 * @param <T>
 *            the type of the wrapped object
 */
public class ElkNamedIndividualWrap<T extends OWLNamedIndividual> extends
        ElkEntityWrap<T> implements ElkNamedIndividual {

	public ElkNamedIndividualWrap(T owlNamedIndividual) {
		super(owlNamedIndividual);
	}

	@Override
	public <O> O accept(ElkEntityVisitor<O> visitor) {
		return visitor.visit(this);
	}

	@Override
	public <O> O accept(ElkIndividualVisitor<O> visitor) {
		return visitor.visit(this);
	}

}
