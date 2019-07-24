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
 * Represents the configuration of a view definition
 */
public interface ViewDefinition  extends RelationalObject, StringConstants {

    /**
     * The type identifier.
     */
    int TYPE_ID = ViewDefinition.class.hashCode();

    /**
     * Identifier of this object
     */
    KomodoType IDENTIFIER = KomodoType.VIEW_DEFINITION;


    
    /**
     * @param compositionName
     *        the name of the sql composition being added (cannot be empty)
     * @return the new sql composition (never <code>null</code>)
     * @throws KException
     *         if an error occurs
     */
    SqlComposition addSqlComposition(  String compositionName ) throws KException;
    
    /**
     * @param namePatterns
     *        optional name patterns (can be <code>null</code> or empty but cannot have <code>null</code> or empty elements)
     * @return the sql compositions (never <code>null</code> but can be empty)
     * @throws KException
     *         if an error occurs
     */
    SqlComposition[] getSqlCompositions(  final String... namePatterns ) throws KException;

    /**
     * @param sqlCompositionToRemove
     *        the name of the sql composition being removed (cannot be empty)       
     * @throws KException
     *         if an error occurs
     */
    void removeSqlComposition( 
                         final String sqlCompositionToRemove ) throws KException;
    
    
    /**
     * @return the view name
     * @throws KException
     *         if an error occurs   
     */
    String getViewName() throws KException;
    
    /**
     * @param name the view name
     * @throws KException
     *         if an error occurs
     */
    void setViewName(String name) throws KException;
    
    /**
     * @return the description
     * @throws KException
     *         if an error occurs
     */
    String getDescription() throws KException;
    
    /**
     * @param description value of description
     * @throws KException
     *         if an error occurs         
     */
    void setDescription( String description) throws KException;
    
    /**
     * @return the view DDL
     * @throws KException
     *         if an error occurs
     */
    String getDdl() throws KException;
    
    /**
     * @param ddl value of view ddl
     * @throws KException
     *         if an error occurs         
     */
    void setDdl( String ddl) throws KException;

    /**
     * @param complete value for isComplete
     * @throws KException
     *         if an error occurs         
     */
    void setComplete( boolean complete) throws KException;
    
    /**
     * @param transaction
     *        the transaction (cannot be <code>null</code> or have a state that is not {@link State#NOT_STARTED})
     * @return boolean value of isComplete
     * @throws KException
     *         if an error occurs         
     */
    boolean isComplete() throws KException;

    /**
     * @param userDefined value for isUserDefined
     * @throws KException
     *         if an error occurs         
     */
    void setUserDefined( boolean userDefined) throws KException;
    
    /**
     * @return boolean value of isUserDefined
     * @throws KException
     *         if an error occurs         
     */
    boolean isUserDefined() throws KException;
    
    /**
     * @param namePatterns
     *        optional name patterns (can be <code>null</code> or empty but cannot have <code>null</code> or empty elements)
     * @return the string array of source paths
     * @throws KException
     *         if an error occurs     
     */
    String[] getSourcePaths(  final String... namePatterns ) throws KException;
    
    /**
     * @param sourcePathToRemove
     *        the source path being removed (cannot be empty)       
     * @return the source paths
     * @throws KException
     *         if an error occurs
     */
    String[] removeSourcePath( 
                                final String sourcePathToRemove ) throws KException;
    
    /**
     * @param sourcePath 
     *        the name of the source path (cannot be empty)
     * @return the source paths
     * @throws KException
     *         if an error occurs
     */
    String[] addSourcePath(  String sourcePath ) throws KException;

    /**
     * @param columnName
     *        the name of the projected column being added (cannot be empty)
     * @return the new projected column (never <code>null</code>)
     * @throws KException
     *         if an error occurs
     */
    SqlProjectedColumn addProjectedColumn(  String columnName ) throws KException;
    
    /**
     * @param projectedColumnToRemove
     *        the name of the projected column being removed (cannot be empty)       
     * @throws KException
     *         if an error occurs
     */
    void removeProjectedColumn( 
                                final String projectedColumnToRemove ) throws KException;

    /**
     * @param namePatterns
     *        optional name patterns (can be <code>null</code> or empty but cannot have <code>null</code> or empty elements)
     * @return the sql projected columns (never <code>null</code> but can be empty)
     * @throws KException
     *         if an error occurs
     */
    SqlProjectedColumn[] getProjectedColumns(  final String... namePatterns ) throws KException;

}
