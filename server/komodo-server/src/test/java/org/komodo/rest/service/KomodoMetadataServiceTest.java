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
 
package org.komodo.rest.service;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import org.junit.Test;
import org.komodo.metadata.internal.TeiidDataSourceImpl;
import org.komodo.metadata.runtime.TeiidDataSource;
import org.teiid.adminapi.impl.VDBMetaData;
import org.teiid.adminapi.impl.VDBMetadataParser;

@SuppressWarnings({ "javadoc", "nls" })
public class KomodoMetadataServiceTest {
	
	@Test
	public void testSourceVdbGeneration() throws Exception {
		Properties properties = new Properties();
		properties.setProperty(TeiidDataSource.DATASOURCE_JNDINAME, "something");
		properties.setProperty(TeiidDataSource.DATASOURCE_DRIVERNAME, "type");
		TeiidDataSourceImpl tds = new TeiidDataSourceImpl("source", properties);
		VDBMetaData vdb = KomodoMetadataService.generateSourceVdb(tds, "vdb");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		VDBMetadataParser.marshell(vdb, baos);
		
		String s = new String(baos.toByteArray(), "UTF-8");
		assertEquals(
				"<?xml version=\"1.0\" ?><vdb name=\"vdb\" version=\"1\"><description>Vdb for source Data Source:	source\n"
						+ "Type: 		type</description><connection-type>BY_VERSION</connection-type>"
						+ "<model name=\"sourceschemamodel\" type=\"PHYSICAL\" visible=\"true\">"
						+ "<property name=\"importer.TableTypes\" value=\"TABLE,VIEW\"></property>"
						+ "<property name=\"importer.UseQualifiedName\" value=\"true\"></property>"
						+ "<property name=\"importer.UseCatalogName\" value=\"false\"></property>"
						+ "<property name=\"importer.UseFullSchemaName\" value=\"false\"></property>"
						+ "<source name=\"source\" translator-name=\"type\" connection-jndi-name=\"something\"></source></model></vdb>",
				s);
	}
}