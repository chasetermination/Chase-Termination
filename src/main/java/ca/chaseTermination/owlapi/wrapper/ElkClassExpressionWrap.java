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

import org.semanticweb.elk.owl.interfaces.ElkClassExpression;
import org.semanticweb.elk.owl.visitors.ElkClassExpressionVisitor;
import org.semanticweb.elk.owl.visitors.ElkObjectVisitor;
import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * Implements the {@link ElkClassExpression} interface by wrapping instances of
 * {@link OWLClassExpression}
 * 
 * @author Yevgeny Kazakov
 * 
 * @param <T>
 *            the type of the wrapped object
 */
public abstract class ElkClassExpressionWrap<T extends OWLClassExpression>
		extends ElkObjectWrap<T> implements ElkClassExpression {

	ElkClassExpressionWrap(T owlClassExpression) {
		super(owlClassExpression);
	}

	@Override
	public abstract <O> O accept(ElkClassExpressionVisitor<O> visitor);

	@Override
	public <O> O accept(ElkObjectVisitor<O> visitor) {
		return accept((ElkClassExpressionVisitor<O>) visitor);
	}
}
