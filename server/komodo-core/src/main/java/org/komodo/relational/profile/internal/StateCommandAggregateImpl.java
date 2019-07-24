/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.komodo.relational.profile.internal;

import java.util.Map;

import org.komodo.core.KomodoLexicon;
import org.komodo.core.internal.repository.Repository;
import org.komodo.core.repository.KomodoObject;
import org.komodo.core.repository.ObjectImpl;
import org.komodo.relational.dataservice.StateCommand;
import org.komodo.relational.dataservice.StateCommandAggregate;
import org.komodo.relational.internal.RelationalModelFactory;
import org.komodo.relational.internal.RelationalObjectImpl;
import org.komodo.relational.internal.TypeResolver;
import org.komodo.spi.KException;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.UnitOfWork;
import org.komodo.spi.repository.UnitOfWork.State;
import org.komodo.utils.ArgCheck;

/**
 * An implementation of a view editor state object.
 */
public class StateCommandAggregateImpl extends RelationalObjectImpl implements StateCommandAggregate {
	
    /**
     * The resolver of a {@link StateCommandAggregate}.
     */
    public static final TypeResolver<StateCommandAggregateImpl> RESOLVER = new TypeResolver<StateCommandAggregateImpl>() {

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#identifier()
         */
        @Override
        public KomodoType identifier() {
            return IDENTIFIER;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#owningClass()
         */
        @Override
        public Class<StateCommandAggregateImpl> owningClass() {
            return StateCommandAggregateImpl.class;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolvable(org.komodo.core.repository.KomodoObject)
         */
        @Override
        public boolean resolvable(final KomodoObject kobject) throws KException {
            return ObjectImpl.validateType(kobject, KomodoLexicon.StateCommandAggregate.NODE_TYPE);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolve(org.komodo.core.repository.KomodoObject)
         */
        @Override
        public StateCommandAggregateImpl resolve(final KomodoObject kobject) throws KException {
            if (kobject.getTypeId() == StateCommandAggregate.TYPE_ID) {
                return (StateCommandAggregateImpl)kobject;
            }

            return new StateCommandAggregateImpl(kobject.getTransaction(), kobject.getRepository(), kobject.getAbsolutePath());
        }

    };

    /**
     * @param uow
     *        the transaction (cannot be <code>null</code> or have a state that is not {@link State#NOT_STARTED})
     * @param repository
     *        the repository where the relational object exists (cannot be <code>null</code>)
     * @param path
     *        the path (cannot be empty)
     * @throws KException
     *         if an error occurs
     */
    public StateCommandAggregateImpl(final UnitOfWork uow, final Repository repository, final String path) throws KException {
        super(uow, repository, path);
    }

    @Override
    public KomodoType getTypeIdentifier() {
        return StateCommandAggregate.IDENTIFIER;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.core.repository.KomodoObject#getTypeId()
     */
    @Override
    public int getTypeId() {
        return TYPE_ID;
    }

    private StateCommand getStateCommand(UnitOfWork transaction, String stateCmdType) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
    
        KomodoObject stateCmdObject = null;

        if (hasChild(stateCmdType)) {
            stateCmdObject = getChild(stateCmdType, KomodoLexicon.StateCommand.NODE_TYPE);
        }
    
        if (stateCmdObject == null)
            return null;
    
        return StateCommandImpl.RESOLVER.resolve(stateCmdObject);
    }

    private StateCommand setStateCommand(UnitOfWork transaction, String stateCmdType,
                                                                 String commandId, Map<String, String> arguments) throws KException {
        return RelationalModelFactory.createStateCommand(transaction, getRepository(), this,
                                                                                                                 stateCmdType, commandId, arguments);
    }

    @Override
    public StateCommand getUndo() throws KException {
        return getStateCommand(getTransaction(), KomodoLexicon.StateCommandAggregate.UNDO);
    }

    @Override
    public StateCommand setUndo(String commandId, Map<String, String> arguments) throws Exception {
        return setStateCommand(getTransaction(), KomodoLexicon.StateCommandAggregate.UNDO,
                                                               commandId, arguments);
    }

    @Override
    public StateCommand getRedo() throws KException {
        return getStateCommand(getTransaction(), KomodoLexicon.StateCommandAggregate.REDO);
    }

    @Override
    public StateCommand setRedo(String commandId, Map<String, String> arguments) throws Exception {
        return setStateCommand(getTransaction(), KomodoLexicon.StateCommandAggregate.REDO,
                                                               commandId, arguments);
    }
}
