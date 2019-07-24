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
package org.komodo.relational.dataservice;

import org.komodo.relational.RelationalObject;
import org.komodo.spi.KException;
import org.komodo.spi.StringConstants;
import org.komodo.spi.repository.KomodoType;

/**
 * Represents the configuration of a view editor state
 */
public interface ViewEditorState extends RelationalObject, StringConstants {

    /**
     * The type identifier.
     */
    int TYPE_ID = ViewEditorState.class.hashCode();

    /**
     * Identifier of this object
     */
    KomodoType IDENTIFIER = KomodoType.VIEW_EDITOR_STATE;

    /**
     * @return the new command
     * @throws KException
     *         if an error occurs
     */
    StateCommandAggregate addCommand() throws KException;

    /**
     * @return the state commands
     * @throws KException
     *         if an error occurs
     */
    StateCommandAggregate[] getCommands() throws KException;
    
    /**
     * @return the view definition
     * @throws KException
     *         if an error occurs
     */
    ViewDefinition setViewDefinition() throws KException;
    
    /**
     * @return the view definition
     * @throws KException
     *         if an error occurs
     */
    ViewDefinition getViewDefinition() throws KException;
}
