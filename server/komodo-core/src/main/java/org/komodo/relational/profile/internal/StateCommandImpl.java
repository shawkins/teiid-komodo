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

import java.util.HashMap;
import java.util.Map;

import org.komodo.core.KomodoLexicon;
import org.komodo.core.internal.repository.Repository;
import org.komodo.core.repository.KomodoObject;
import org.komodo.core.repository.ObjectImpl;
import org.komodo.core.repository.PropertyValueType;
import org.komodo.relational.dataservice.StateCommand;
import org.komodo.relational.internal.RelationalChildRestrictedObject;
import org.komodo.relational.internal.TypeResolver;
import org.komodo.spi.KException;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.UnitOfWork;

/**
 * An implementation of a view editor state object.
 */
public class StateCommandImpl extends RelationalChildRestrictedObject implements StateCommand {
	
    /**
     * The resolver of a {@link StateCommand}.
     */
    public static final TypeResolver<StateCommandImpl> RESOLVER = new TypeResolver<StateCommandImpl>() {

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
        public Class<StateCommandImpl> owningClass() {
            return StateCommandImpl.class;
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolvable(org.komodo.core.repository.KomodoObject)
         */
        @Override
        public boolean resolvable(final KomodoObject kobject) throws KException {
            return ObjectImpl.validateType(kobject, KomodoLexicon.StateCommand.NODE_TYPE);
        }

        /**
         * {@inheritDoc}
         *
         * @see org.komodo.relational.internal.TypeResolver#resolve(org.komodo.core.repository.KomodoObject)
         */
        @Override
        public StateCommandImpl resolve(final KomodoObject kobject) throws KException {
            if (kobject.getTypeId() == StateCommand.TYPE_ID) {
                return (StateCommandImpl)kobject;
            }

            return new StateCommandImpl(kobject.getTransaction(), kobject.getRepository(), kobject.getAbsolutePath());
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
    public StateCommandImpl(final UnitOfWork uow, final Repository repository, final String path) throws KException {
        super(uow, repository, path);
    }

    @Override
    public KomodoType getTypeIdentifier() {
        return StateCommand.IDENTIFIER;
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

    @Override
    public String getId() throws KException {
        return getObjectProperty(getTransaction(), PropertyValueType.STRING, "getId",
                                                                     KomodoLexicon.StateCommand.ID);
    }

    @Override
    public void setId(String id) throws Exception {
        setObjectProperty(getTransaction(), "setId",
                                                                      KomodoLexicon.StateCommand.ID, id);
    }

    @Override
    public Map<String, String> getArguments() throws KException {
        Map<String, String> args = new HashMap<>();

        String[] propertyNames = getPropertyNames();
        if (propertyNames == null)
            return args;

        for (String propertyName : propertyNames) {
            if (! propertyName.startsWith(KomodoLexicon.StateCommand.ARGS_PREFIX))
                continue;

            String value = getObjectProperty(getTransaction(), PropertyValueType.STRING, "getArgument", propertyName);
            String name = propertyName.replace(
                                                                   KomodoLexicon.StateCommand.ARGS_PREFIX, EMPTY_STRING);
            args.put(name,  value);
        }

        return args;
    }

    @Override
    public void setArguments(Map<String, String> arguments) throws KException {
        if (arguments == null)
            arguments = new HashMap<>();

        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            String name = KomodoLexicon.StateCommand.ARGS_PREFIX + entry.getKey();
            String value = entry.getValue();
            setObjectProperty(getTransaction(), "setArgument", name, value); //$NON-NLS-1$
        }
    }
}
