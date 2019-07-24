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
package org.komodo.relational.workspace;

import java.util.ArrayList;
import java.util.List;

import org.komodo.core.KEvent;
import org.komodo.core.KObserver;
import org.komodo.core.KomodoLexicon;
import org.komodo.core.internal.repository.Repository;
import org.komodo.core.internal.repository.Repository.State;
import org.komodo.core.repository.KomodoObject;
import org.komodo.core.repository.RepositoryImpl;
import org.komodo.relational.Messages;
import org.komodo.relational.Messages.Relational;
import org.komodo.relational.RelationalObject;
import org.komodo.relational.WorkspaceManager;
import org.komodo.relational.dataservice.Dataservice;
import org.komodo.relational.dataservice.internal.DataserviceImpl;
import org.komodo.relational.internal.AdapterFactory;
import org.komodo.relational.internal.RelationalModelFactory;
import org.komodo.relational.internal.RelationalObjectImpl;
import org.komodo.relational.profile.internal.ViewEditorStateImpl;
import org.komodo.spi.KException;
import org.komodo.spi.StringConstants;
import org.komodo.spi.SystemConstants;
import org.komodo.spi.repository.KomodoType;
import org.komodo.spi.repository.UnitOfWork;
import org.komodo.utils.ArgCheck;
import org.komodo.utils.KLog;
import org.komodo.utils.KeyInValueMap;
import org.komodo.utils.KeyInValueMap.KeyFromValueAdapter;
import org.komodo.utils.StringUtils;
import org.teiid.modeshape.sequencer.dataservice.lexicon.DataVirtLexicon;

/**
 *
 */
public class WorkspaceManagerImpl extends RelationalObjectImpl implements RelationalObject, WorkspaceManager {
	
    /**
     * An empty array of Dataservices.
     */
    final static DataserviceImpl[] NO_DATASERVICES = new DataserviceImpl[ 0 ];
    
	/**
     * An empty array of view editor states.
     */
    final static ViewEditorStateImpl[] NO_VIEW_EDITOR_STATES = new ViewEditorStateImpl[0];
	
	static final Filter[] NO_FILTERS = new Filter[0];
	
	protected static final KLog LOGGER = KLog.getLogger();

    /**
     * The allowed child types.
     */
    private static final KomodoType[] CHILD_TYPES = new KomodoType[] { Dataservice.IDENTIFIER };

    /**
     * The type identifier.
     */
    public static final int TYPE_ID = WorkspaceManagerImpl.class.hashCode();

    // @formatter:off
    private static final String FIND_ALL_QUERY_PATTERN = "SELECT [jcr:path] FROM [%s]" //$NON-NLS-1$
                                                         + " WHERE ISDESCENDANTNODE('%s')" //$NON-NLS-1$
                                                         + " ORDER BY [jcr:path] ASC"; //$NON-NLS-1$

    private static final String FIND_MATCHING_QUERY_PATTERN = "SELECT [jcr:path] FROM [%s]"  //$NON-NLS-1$
                                                              + " WHERE ISDESCENDANTNODE('%s')" //$NON-NLS-1$
                                                              + " AND [jcr:name] LIKE '%s'" //$NON-NLS-1$
                                                              + " ORDER BY [jcr:path] ASC"; //$NON-NLS-1$
    // @formatter:on

    private static class CacheKey {
        private final Repository.Id repoId;

        private final String user;

        public CacheKey(Repository.Id repoId, String user) {
            this.repoId = repoId;
            this.user = user;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((repoId == null) ? 0 : repoId.hashCode());
            result = prime * result + ((user == null) ? 0 : user.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheKey other = (CacheKey)obj;
            if (repoId == null) {
                if (other.repoId != null)
                    return false;
            } else if (!repoId.equals(other.repoId))
                return false;
            if (user == null) {
                if (other.user != null)
                    return false;
            } else if (!user.equals(other.user))
                return false;
            return true;
        }
    }

    private static class WskpMgrAdapter implements KeyFromValueAdapter< CacheKey, WorkspaceManagerImpl > {

        @Override
        public CacheKey getKey( WorkspaceManagerImpl value ) {
            Repository repository = value.getRepository();
            String user = value.getOwner();

            return new CacheKey(repository.getId(), user);
        }
    }

    private static KeyFromValueAdapter< CacheKey, WorkspaceManagerImpl > adapter = new WskpMgrAdapter();

    private static KeyInValueMap< CacheKey, WorkspaceManagerImpl > instances = 
                                                                        new KeyInValueMap< CacheKey, WorkspaceManagerImpl >(adapter);

    private final String owner;
    
    private AdapterFactory adapterFactory = new AdapterFactory( );
    

    /**
     * @param repository
     *        the repository whose workspace manager is being requested (cannot be <code>null</code>)
     * @param transaction
     *        the transaction containing the user name of the owner of this workspace manager
     *        (if <code>null</code> then this manager is owner by the system user and has the workspace root as its path)
     *
     * @return the singleton instance for the given repository (never <code>null</code>)
     * @throws KException
     *         if there is an error obtaining the workspace manager
     */
    public static WorkspaceManagerImpl getInstance( Repository repository, UnitOfWork transaction) throws KException {
        boolean txNotProvided = transaction == null;

        if (txNotProvided) {
			transaction = repository.createTransaction(SystemConstants.SYSTEM_USER, "createWorkspaceManager", false, null, //$NON-NLS-1$
					SystemConstants.SYSTEM_USER);
        }

        WorkspaceManagerImpl instance = instances.get(new CacheKey(repository.getId(), transaction.getRepositoryUser()));

        if ( instance == null ) {
            // We must create a transaction here so that it can be passed on to the constructor. Since the
            // node associated with the WorkspaceManager always exists we don't have to create it.
            instance = new WorkspaceManagerImpl(repository, transaction);

            if (txNotProvided)
                transaction.commit();

            instances.add( instance );
        }

        return instance;
    }

    /**
     * @return the owner of this workspace manager
     */
    public String getOwner() {
        return this.owner;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.core.repository.ObjectImpl#getChildTypes()
     */
    @Override
    public KomodoType[] getChildTypes() {
        return CHILD_TYPES;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.RelationalObject#getFilters()
     */
    @Override
    public Filter[] getFilters() {
        return NO_FILTERS;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.core.repository.ObjectImpl#getTypeId()
     */
    @Override
    public int getTypeId() {
        return TYPE_ID;
    }

    @Override
    public KomodoType getTypeIdentifier() {
        return KomodoType.WORKSPACE;
    }

    /**
     * Primarily used in tests to remove the workspace manager instance from the instances cache.
     *
     * @param repository remove instance with given repository
     */
    public static void uncacheInstance(final Repository repository, final String owner) {
        if (repository == null)
            return;

        instances.remove(new CacheKey(repository.getId(), owner));
    }

    private WorkspaceManagerImpl(Repository repository, UnitOfWork uow ) throws KException {
    	super(uow, repository, RepositoryImpl.komodoWorkspacePath(uow), 0 );
        this.owner = uow.getRepositoryUser();

        repository.addObserver(new KObserver() {

            @Override
            public void eventOccurred(KEvent<?> event) {
                // Disposal observer
                if (getRepository() == null || State.NOT_REACHABLE == getRepository().getState() || !(getRepository().ping())) {
                    instances.remove(adapter.getKey(WorkspaceManagerImpl.this));
                }
            }

            @Override
            public void errorOccurred(Throwable e) {
                // Nothing to do
            }
        });
    }

    @Override
    public Dataservice createDataservice(String serviceName) throws KException {
    	return createDataservice(getTransaction(), null, serviceName);
    }
    
    /**
     * @param uow
     *        the transaction (cannot be <code>null</code> or have a state that is not
     *        {@link org.komodo.spi.repository.UnitOfWork.State#NOT_STARTED})
     * @param parent
     *        the parent of the dataservice object being created (can be <code>null</code>)
     * @param serviceName
     *        the name of the dataservice to create (cannot be empty)
     * @return the Dataservice object (never <code>null</code>)
     * @throws KException
     *         if an error occurs
     */
    public DataserviceImpl createDataservice( final UnitOfWork uow,
                                        final KomodoObject parent,
                                        final String serviceName ) throws KException {
        final String path = ( ( parent == null ) ? getRepository().komodoWorkspace( uow ).getAbsolutePath()
                                                 : parent.getAbsolutePath() );
         return RelationalModelFactory.createDataservice( uow, getRepository(), path, serviceName );
    }

    /**
     * @param transaction
     *        the transaction (cannot be <code>null</code> or have a state that is not
     *        {@link org.komodo.spi.repository.UnitOfWork.State#NOT_STARTED})
     * @param kobjects
     *        the object(s) being deleted (cannot be <code>null</code>, empty, or have a <code>null</code> element)
     * @throws KException
     *         if an error occurs or if an object does not exist
     */
    public void delete( final UnitOfWork transaction,
                        final KomodoObject... kobjects ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == org.komodo.spi.repository.UnitOfWork.State.NOT_STARTED ),
                         "transaction state is not NOT_STARTED" ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( kobjects, "kobjects" ); //$NON-NLS-1$

        for ( final KomodoObject kobject : kobjects ) {
            ArgCheck.isNotNull( kobject, "kobject" ); //$NON-NLS-1$
            validateWorkspaceMember( transaction, kobject );
            kobject.remove( transaction );
        }
    }
    
    @Override
    public void deleteDataservice(Dataservice dataservice) throws KException {
    	delete(getTransaction(), (DataserviceImpl)dataservice);
    }
    
    /**
     * @param transaction
     *        the transaction (cannot be <code>null</code> and must have a state of
     *        {@link org.komodo.spi.repository.UnitOfWork.State#NOT_STARTED})
     * @param type
     *        the lexicon node type name of objects being found (cannot be empty)
     * @param parentPath
     *        the parent path whose children recursively will be checked (can be empty if searching from the workspace root)
     * @param namePattern
     *        the regex used to match object names (can be empty if all objects of the given type are being requested)
     * @param includeSubTypes
     *        determines whether sub types are included in the return
     * @return the paths of all the objects under the specified parent path with the specified type (never <code>null</code> but
     *         can be empty)
     * @throws KException
     *         if an error occurs
     */
    public String[] findByType( final UnitOfWork transaction,
                                final String type,
                                String parentPath,
                                final String namePattern,
                                boolean includeSubTypes) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == org.komodo.spi.repository.UnitOfWork.State.NOT_STARTED ),
                         "transaction state must be NOT_STARTED and was " + transaction.getState() ); //$NON-NLS-1$
        ArgCheck.isNotEmpty( type, "type" ); //$NON-NLS-1$

        if ( StringUtils.isBlank( parentPath ) ) {
            parentPath = RepositoryImpl.komodoWorkspacePath(transaction);
        }

        try {
            String queryText = null;

            if ( StringUtils.isBlank( namePattern ) ) {
                queryText = String.format( FIND_ALL_QUERY_PATTERN, type, parentPath );
            } else {
                queryText = String.format( FIND_MATCHING_QUERY_PATTERN, type, parentPath, namePattern );
            }

            final List< KomodoObject > kObjs = getRepository().query( transaction, queryText );
            List< KomodoObject > results = new ArrayList< > ();
            for( final KomodoObject kObj : kObjs ) {
                if(includeSubTypes) {
                    results.add(kObj);
                } else if ( type.equals(kObj.getPrimaryType().getName()) ) {
                    results.add(kObj);
                }
            }

            final int numPaths = results.size();

            if ( numPaths == 0 ) {
                return StringConstants.EMPTY_ARRAY;
            }

            final String[] result = new String[ numPaths ];
            int i = 0;

            for ( final KomodoObject kObject : results ) {
                result[ i++ ] = kObject.getAbsolutePath();
            }

            return result;
        } catch ( final Exception e ) {
            throw handleError( e );
        }
    }

    /**
     * @param searchPattern pattern to match.  If blank, will match all. 
     * @param transaction
     *        the transaction (cannot be <code>null</code> or have a state that is not
     *        {@link org.komodo.spi.repository.UnitOfWork.State#NOT_STARTED})
     * @return {@link Dataservice}s in the workspace
     * @throws KException
     *         if an error occurs
     */
    public DataserviceImpl[] findDataservices( String searchPattern ) throws KException {
        ArgCheck.isNotNull( getTransaction(), "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( getTransaction().getState() == org.komodo.spi.repository.UnitOfWork.State.NOT_STARTED ),
                         "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final String[] paths = findByType(getTransaction(), DataVirtLexicon.DataService.NODE_TYPE, null, searchPattern, false);
        DataserviceImpl[] result = null;

        if (paths.length == 0) {
            result = NO_DATASERVICES;
        } else {
            result = new DataserviceImpl[paths.length];
            int i = 0;

            for (final String path : paths) {
                result[i++] = new DataserviceImpl(getTransaction(), getRepository(), path);
            }
        }

        LOGGER.debug("getDataservices:found '{0}' DataServices using pattern '{1}'", result.length, //$NON-NLS-1$
                searchPattern);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.core.repository.ObjectImpl#remove(org.komodo.spi.repository.Repository.UnitOfWork)
     */
    @Override
    public void remove( final UnitOfWork transaction ) {
        throw new UnsupportedOperationException( Messages.getString( Relational.REMOVE_NOT_ALLOWED, getAbsolutePath() ) );
    }

    /**
     * <strong><em>Rename is not allowed!!</em></strong>
     *
     * @see org.komodo.core.repository.KomodoObject#rename(org.komodo.spi.repository.Repository.UnitOfWork, java.lang.String)
     * @throws UnsupportedOperationException if called
     */
    @Override
    public final void rename( final UnitOfWork transaction,
                              final String newName ) throws UnsupportedOperationException {
        throw new UnsupportedOperationException( Messages.getString( Relational.RENAME_NOT_ALLOWED, getAbsolutePath() ) );
    }

    /**
     * Attempts to adapt the given object to a relational model typed class.
     * If the object is not an instance of {@link KomodoObject} then null is
     * returned.
     *
     * The type id of the {@link KomodoObject} is extracted and the correct
     * relational model object created. If the latter is not assignable from the
     * given adapted class then it is concluded the adaption should fail and
     * null is returned, otherwise the new object is returned.
     *
     * @param <T>
     *        the desired outcome class
     * @param transaction
     *        the transaction (cannot be <code>null</code> or have a state that is not
     *        {@link org.komodo.spi.repository.UnitOfWork.State#NOT_STARTED})
     * @param object
     *        the object being resolved
     * @param resolvedClass
     *        the class the object should be resolved to (cannot be <code>null</code>)
     * @return the strong typed object of the desired type (can be <code>null</code> if not resolvable)
     * @throws KException
     *         if a resolver could not be found or if an error occurred
     */
    public <T> T resolve( final UnitOfWork transaction,
                                                 final Object object,
                                                 final Class<T> resolvedClass ) throws KException {
        ArgCheck.isNotNull( transaction, "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( transaction.getState() == org.komodo.spi.repository.UnitOfWork.State.NOT_STARTED ),
                         "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        T kobject = adapterFactory.adapt(transaction, object, resolvedClass);
        return kobject;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.komodo.relational.RelationalObject#setFilters(org.komodo.relational.RelationalObject.Filter[])
     */
    @Override
    public void setFilters( Filter[] newFilters ) {
        // filters not allowed
    }

    private void validateWorkspaceMember( final UnitOfWork uow,
                                          final KomodoObject kobject ) throws KException {
		if (!getRepository().equals(repository)) {
            throw new KException(Messages.getString(Relational.OBJECT_BEING_DELETED_HAS_WRONG_REPOSITORY,
                                                    kobject.getAbsolutePath(),
                                                    repository.getId().getUrl(),
                                                    getRepository().getId().getUrl()));
        }

        if (!kobject.getAbsolutePath().startsWith(getRepository().komodoWorkspace(uow).getAbsolutePath())) {
            throw new KException(Messages.getString(Relational.OBJECT_BEING_DELETED_HAS_NULL_PARENT, kobject.getAbsolutePath()));
        }
    }
    
    @Override
    public Dataservice findDataservice(String dataserviceName) throws KException {
        if (! hasChild( dataserviceName, DataVirtLexicon.DataService.NODE_TYPE ) ) {
            return null;
        }

        final KomodoObject kobject = getChild( dataserviceName, DataVirtLexicon.DataService.NODE_TYPE );
        final Dataservice dataservice = resolve( getTransaction(), kobject, Dataservice.class );

        LOGGER.debug( "Dataservice '{0}' was found", dataserviceName ); //$NON-NLS-1$
        return dataservice;
    }
    
	@Override
	public String findSchema(String name) throws KException {
		if (hasChild(name, KomodoLexicon.Schema.NODE_TYPE)) {
			KomodoObject object = getChild(name, KomodoLexicon.Schema.NODE_TYPE);
			return object.getProperty(KomodoLexicon.Schema.RENDITION).getStringValue(getTransaction());
		}
		return null;
	}

	@Override
	public boolean deleteSchema(String name) throws KException {
		if (hasChild(name, KomodoLexicon.Schema.NODE_TYPE)) {
			KomodoObject object = getChild(name, KomodoLexicon.Schema.NODE_TYPE);
			object.remove(getTransaction());
			return true;
		}
		return false;
	}

	@Override
	public void saveSchema(String name, String contents) throws KException {
		KomodoObject object = this.addChild(getTransaction(), name, KomodoLexicon.Schema.NODE_TYPE);
		object.setProperty(KomodoLexicon.Schema.RENDITION, contents);
	}
	
    @Override
    public ViewEditorStateImpl addViewEditorState(String stateId) throws KException {
        // first delete if already exists
        if ( getViewEditorState( stateId ) != null ) {
            removeViewEditorState( stateId );
        }

        return RelationalModelFactory.createViewEditorState( getTransaction(), getRepository(), getRepository().komodoProfile(getTransaction()), stateId );
    }

    private KomodoObject getViewEditorStatesGroupingNode() {
        try {
        	KomodoObject userProfileObj = getRepository().komodoProfile(getTransaction());
        	
            final KomodoObject[] groupings = userProfileObj.getRawChildren( getTransaction(), KomodoLexicon.Profile.VIEW_EDITOR_STATES );

            if ( groupings.length == 0 ) {
                return null;
            }

            return groupings[ 0 ];
        } catch ( final KException e ) {
            return null;
        }
    }

    @Override
    public ViewEditorStateImpl[] getViewEditorStates(String... namePatterns) throws KException {
        ArgCheck.isNotNull( getTransaction(), "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( getTransaction().getState() == org.komodo.spi.repository.UnitOfWork.State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final KomodoObject grouping = getViewEditorStatesGroupingNode( );

        if ( grouping != null ) {
            final List< ViewEditorStateImpl > temp = new ArrayList<>();

            for ( final KomodoObject kobject : grouping.getChildren( namePatterns ) ) {
                final ViewEditorStateImpl gitRepo = new ViewEditorStateImpl( getTransaction(), getRepository(), kobject.getAbsolutePath() );
                temp.add( gitRepo );
            }

            return temp.toArray( new ViewEditorStateImpl[ temp.size() ] );
        }

        return NO_VIEW_EDITOR_STATES;
    }
    
    @Override
    public ViewEditorStateImpl getViewEditorState(String name) throws KException {
        ArgCheck.isNotNull( getTransaction(), "transaction" ); //$NON-NLS-1$
        ArgCheck.isTrue( ( getTransaction().getState() == org.komodo.spi.repository.UnitOfWork.State.NOT_STARTED ), "transaction state is not NOT_STARTED" ); //$NON-NLS-1$

        final KomodoObject grouping = getViewEditorStatesGroupingNode();

        if ( grouping != null && grouping.hasChild( name ) ) {
        	KomodoObject kobject = grouping.getChild( name );
            return new ViewEditorStateImpl( getTransaction(), getRepository(), kobject.getAbsolutePath() );
        }

        return null;
    }

    @Override
    public void removeViewEditorState(String viewEditorStateId) throws KException {
        ArgCheck.isNotNull(getTransaction(), "transaction"); //$NON-NLS-1$
        ArgCheck.isTrue((getTransaction().getState() == org.komodo.spi.repository.UnitOfWork.State.NOT_STARTED), "transaction state is not NOT_STARTED"); //$NON-NLS-1$
        ArgCheck.isNotEmpty(viewEditorStateId, "viewEditorStateId"); //$NON-NLS-1$

        ViewEditorStateImpl state = getViewEditorState(viewEditorStateId);

        if (state == null) {
            throw new KException(Messages.getString(Relational.VIEW_EDITOR_STATE_NOT_FOUND_TO_REMOVE, viewEditorStateId));
        }

        // remove first occurrence
        state.remove(getTransaction());
    }

}
