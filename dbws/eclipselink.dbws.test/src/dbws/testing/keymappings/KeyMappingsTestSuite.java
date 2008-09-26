/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Mike Norman - May 2008, created DBWS test package
 ******************************************************************************/

package dbws.testing.keymappings;

// javase imports
import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
// Java extension imports

// JUnit imports
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// EclipseLink imports
import org.eclipse.persistence.dbws.DBWSModel;
import org.eclipse.persistence.dbws.DBWSModelProject;
import org.eclipse.persistence.internal.databaseaccess.Platform;
import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.internal.sessions.factories.EclipseLinkObjectPersistenceRuntimeXMLProject;
import org.eclipse.persistence.internal.xr.BaseEntity;
import org.eclipse.persistence.internal.xr.BaseEntityClassLoader;
import org.eclipse.persistence.internal.xr.Invocation;
import org.eclipse.persistence.internal.xr.Operation;
import org.eclipse.persistence.internal.xr.ProjectHelper;
import org.eclipse.persistence.internal.xr.XRServiceAdapter;
import org.eclipse.persistence.internal.xr.XRServiceFactory;
import org.eclipse.persistence.internal.xr.XRServiceModel;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.oxm.XMLUnmarshaller;
import org.eclipse.persistence.platform.database.MySQLPlatform;
import org.eclipse.persistence.platform.xml.XMLComparer;
import org.eclipse.persistence.platform.xml.XMLParser;
import org.eclipse.persistence.platform.xml.XMLPlatform;
import org.eclipse.persistence.platform.xml.XMLPlatformFactory;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.sessions.DatasourceLogin;
import org.eclipse.persistence.sessions.Project;

// domain-specific (testing) imports
import dbws.testing.RootHelper;
import static dbws.testing.DBWSTestHelper.DATABASE_DRIVER_KEY;
import static dbws.testing.DBWSTestHelper.DATABASE_PASSWORD_KEY;
import static dbws.testing.DBWSTestHelper.DATABASE_URL_KEY;
import static dbws.testing.DBWSTestHelper.DATABASE_USERNAME_KEY;

public class KeyMappingsTestSuite {

    static final String KEYMAPPINGS_SCHEMA =
        "<?xml version='1.0' encoding='UTF-8'?>" +
        "<xsd:schema targetNamespace=\"urn:keymappings\" xmlns=\"urn:keymappings\" elementFormDefault=\"qualified\"\n" +
        "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
        "  >\n" +
        "  <xsd:complexType name=\"phone\">\n" +
        "    <xsd:sequence>\n" +
        "      <xsd:element name=\"area-code\" type=\"xsd:string\" />\n" +
        "      <xsd:element name=\"phone-number\" type=\"xsd:string\" />\n" +
        "      <xsd:element name=\"type\" type=\"xsd:string\" />\n" +
        "    </xsd:sequence>\n" +
        "    <xsd:attribute name=\"phone-id\" type=\"xsd:int\" use=\"required\" />\n" +
        "    <xsd:attribute name=\"owner-ref-id\" type=\"xsd:int\" use=\"required\" />\n" +
        "  </xsd:complexType>\n" +
        "  <xsd:element name=\"phone\" type=\"phone\"/>\n" +
        "  <xsd:complexType name=\"address\">\n" +
        "    <xsd:sequence>\n" +
        "      <xsd:element name=\"street\" type=\"xsd:string\" />\n" +
        "      <xsd:element name=\"city\" type=\"xsd:string\" />\n" +
        "      <xsd:element name=\"province\" type=\"xsd:string\" />\n" +
        "    </xsd:sequence>\n" +
        "    <xsd:attribute name=\"address-id\" type=\"xsd:int\" use=\"required\" />\n" +
        "  </xsd:complexType>\n" +
        "  <xsd:element name=\"address\" type=\"address\"/>\n" +
        "  <xsd:complexType name=\"employee\">\n" +
        "    <xsd:sequence>\n" +
        "      <xsd:element name=\"first-name\" type=\"xsd:string\" />\n" +
        "      <xsd:element name=\"last-name\" type=\"xsd:string\" />\n" +
        "      <xsd:element name=\"address\" type=\"address\" minOccurs=\"0\" />\n" +
        "      <xsd:element name=\"phones\">\n" +
        "        <xsd:complexType>\n" +
        "          <xsd:sequence>\n" +
        "            <xsd:element maxOccurs=\"unbounded\" name=\"phone-ref\">\n" +
        "              <xsd:complexType>\n" +
        "                <xsd:attribute name=\"phone-id\" type=\"xsd:int\" use=\"required\" />\n" +
        "              </xsd:complexType>\n" +
        "            </xsd:element>\n" +
        "          </xsd:sequence>\n" +
        "        </xsd:complexType>\n" +
        "      </xsd:element>\n" +
        "    </xsd:sequence>\n" +
        "    <xsd:attribute name=\"employee-id\" type=\"xsd:int\" use=\"required\" />\n" +
        "    <xsd:attribute name=\"address-ref-id\" type=\"xsd:int\" use=\"required\" />\n" +
        "  </xsd:complexType>\n" +
        "  <xsd:element name=\"employee\" type=\"employee\"/>\n" +
        "</xsd:schema>";
    static final String KEYMAPPINGS_DBWS =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<dbws\n" +
        "  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n" +
        "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "  xmlns:ns1=\"urn:keymappings\"\n" +
        "  >\n" +
        "  <name>keymappings</name>\n" +
        "  <query>\n" +
        "    <name>getAllEmployees</name>\n" +
        "    <result isCollection=\"true\">\n" +
        "      <type>ns1:employee</type>\n" +
        "    </result>\n" +
        "    <sql><![CDATA[select * from XR_KEYMAP_EMPLOYEE]]></sql>\n" +
        "  </query>\n" +
        "</dbws>\n";
    static final String KEYMAPPINGS_OR_PROJECT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<object-persistence version=\"Eclipse Persistence Services - @VERSION@ (Build @BUILD_NUMBER@)\"\n" +
        "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "   xmlns=\"http://www.eclipse.org/eclipselink/xsds/persistence\"\n" +
        "   >\n" +
        "   <name>keymappings</name>\n" +
        "   <class-mapping-descriptors>\n" +
        "      <class-mapping-descriptor xsi:type=\"relational-class-mapping-descriptor\">\n" +
        "         <class>dbws.testing.keymappings.Address</class>\n" +
        "         <alias>address</alias>\n" +
        "         <primary-key>\n" +
        "            <field table=\"XR_KEYMAP_ADDRESS\" name=\"ADDRESS_ID\" xsi:type=\"column\"/>\n" +
        "         </primary-key>\n" +
        "         <events xsi:type=\"event-policy\"/>\n" +
        "         <querying xsi:type=\"query-policy\"/>\n" +
        "         <attribute-mappings>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>addressId</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_ADDRESS\" name=\"ADDRESS_ID\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>street</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_ADDRESS\" name=\"STREET\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>city</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_ADDRESS\" name=\"CITY\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>province</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_ADDRESS\" name=\"PROVINCE\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "         </attribute-mappings>\n" +
        "         <descriptor-type>independent</descriptor-type>\n" +
        "         <instantiation/>\n" +
        "         <copying xsi:type=\"instantiation-copy-policy\"/>\n" +
        "         <change-policy xsi:type=\"deferred-detection-change-policy\"/>\n" +
        "         <tables>\n" +
        "            <table name=\"XR_KEYMAP_ADDRESS\"/>\n" +
        "         </tables>\n" +
        "      </class-mapping-descriptor>\n" +
        "      <class-mapping-descriptor xsi:type=\"relational-class-mapping-descriptor\">\n" +
        "         <class>dbws.testing.keymappings.Employee</class>\n" +
        "         <alias>employee</alias>\n" +
        "         <primary-key>\n" +
        "            <field table=\"XR_KEYMAP_EMPLOYEE\" name=\"EMP_ID\" xsi:type=\"column\"/>\n" +
        "         </primary-key>\n" +
        "         <events xsi:type=\"event-policy\"/>\n" +
        "         <querying xsi:type=\"query-policy\">\n" +
        "         </querying>\n" +
        "         <attribute-mappings>\n" +
        "            <attribute-mapping xsi:type=\"one-to-one-mapping\">\n" +
        "               <attribute-name>address</attribute-name>\n" +
        "               <reference-class>dbws.testing.keymappings.Address</reference-class>\n" +
        "               <foreign-key>\n" +
        "                  <field-reference>\n" +
        "                     <source-field table=\"XR_KEYMAP_EMPLOYEE\" name=\"ADDR_ID\" xsi:type=\"column\"/>\n" +
        "                     <target-field table=\"XR_KEYMAP_ADDRESS\" name=\"ADDRESS_ID\" xsi:type=\"column\"/>\n" +
        "                  </field-reference>\n" +
        "               </foreign-key>\n" +
        "               <foreign-key-fields>\n" +
        "                  <field table=\"XR_KEYMAP_EMPLOYEE\" name=\"ADDR_ID\" xsi:type=\"column\"/>\n" +
        "               </foreign-key-fields>\n" +
        "               <selection-query xsi:type=\"read-object-query\"/>\n" +
        "               <join-fetch>inner-join</join-fetch>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>employeeId</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_EMPLOYEE\" name=\"EMP_ID\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>firstName</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_EMPLOYEE\" name=\"F_NAME\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>lastName</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_EMPLOYEE\" name=\"L_NAME\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"one-to-many-mapping\">\n" +
        "               <attribute-name>phones</attribute-name>\n" +
        "               <reference-class>dbws.testing.keymappings.Phone</reference-class>\n" +
        "               <target-foreign-key>\n" +
        "                  <field-reference>\n" +
        "                     <source-field table=\"XR_KEYMAP_PHONE\" name=\"OWNER_ID\" xsi:type=\"column\"/>\n" +
        "                     <target-field table=\"XR_KEYMAP_EMPLOYEE\" name=\"EMP_ID\" xsi:type=\"column\"/>\n" +
        "                  </field-reference>\n" +
        "               </target-foreign-key>\n" +
        "               <container xsi:type=\"list-container-policy\">\n" +
        "                  <collection-type>org.eclipse.persistence.indirection.IndirectList</collection-type>\n" +
        "               </container>\n" +
        "               <indirection xsi:type=\"transparent-collection-indirection-policy\"/>\n" +
        "               <selection-query xsi:type=\"read-all-query\">\n" +
        "                  <container xsi:type=\"list-container-policy\">\n" +
        "                     <collection-type>org.eclipse.persistence.indirection.IndirectList</collection-type>\n" +
        "                  </container>\n" +
        "               </selection-query>\n" +
        "            </attribute-mapping>\n" +
        "         </attribute-mappings>\n" +
        "         <descriptor-type>independent</descriptor-type>\n" +
        "         <instantiation/>\n" +
        "         <copying xsi:type=\"instantiation-copy-policy\"/>\n" +
        "         <change-policy xsi:type=\"deferred-detection-change-policy\"/>\n" +
        "         <tables>\n" +
        "            <table name=\"XR_KEYMAP_EMPLOYEE\"/>\n" +
        "         </tables>\n" +
        "      </class-mapping-descriptor>\n" +
        "      <class-mapping-descriptor xsi:type=\"relational-class-mapping-descriptor\">\n" +
        "         <class>dbws.testing.keymappings.Phone</class>\n" +
        "         <alias>phone</alias>\n" +
        "         <primary-key>\n" +
        "            <field table=\"XR_KEYMAP_PHONE\" name=\"PHONE_ID\" xsi:type=\"column\"/>\n" +
        "         </primary-key>\n" +
        "         <events xsi:type=\"event-policy\"/>\n" +
        "         <querying xsi:type=\"query-policy\"/>\n" +
        "         <attribute-mappings>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>phoneId</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_PHONE\" name=\"PHONE_ID\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>areaCode</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_PHONE\" name=\"AREA_CODE\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>phonenumber</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_PHONE\" name=\"P_NUMBER\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"direct-mapping\">\n" +
        "               <attribute-name>type</attribute-name>\n" +
        "               <field table=\"XR_KEYMAP_PHONE\" name=\"TYPE\" xsi:type=\"column\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"one-to-one-mapping\">\n" +
        "               <attribute-name>owner</attribute-name>\n" +
        "               <reference-class>dbws.testing.keymappings.Employee</reference-class>\n" +
        "               <private-owned>false</private-owned>\n" +
        "               <foreign-key>\n" +
        "                  <field-reference>\n" +
        "                     <source-field table=\"XR_KEYMAP_PHONE\" name=\"OWNER_ID\" xsi:type=\"column\"/>\n" +
        "                     <target-field table=\"XR_KEYMAP_EMPLOYEE\" name=\"EMP_ID\" xsi:type=\"column\"/>\n" +
        "                  </field-reference>\n" +
        "               </foreign-key>\n" +
        "               <foreign-key-fields>\n" +
        "                  <field table=\"XR_KEYMAP_PHONE\" name=\"OWNER_ID\" xsi:type=\"column\"/>\n" +
        "               </foreign-key-fields>\n" +
        "               <batch-reading>true</batch-reading>\n" +
        "               <selection-query xsi:type=\"read-object-query\"/>\n" +
        "            </attribute-mapping>\n" +
        "         </attribute-mappings>\n" +
        "         <descriptor-type>independent</descriptor-type>\n" +
        "         <instantiation/>\n" +
        "         <copying xsi:type=\"instantiation-copy-policy\"/>\n" +
        "         <change-policy xsi:type=\"deferred-detection-change-policy\"/>\n" +
        "         <tables>\n" +
        "            <table name=\"XR_KEYMAP_PHONE\"/>\n" +
        "         </tables>\n" +
        "      </class-mapping-descriptor>\n" +
        "   </class-mapping-descriptors>\n" +
        "   <login xsi:type=\"database-login\">\n" +
        "      <bind-all-parameters>true</bind-all-parameters>\n" +
        "   </login>\n" +
        "</object-persistence>";
    static final String KEYMAPPINGS_OX_PROJECT =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<object-persistence version=\"Eclipse Persistence Services - @VERSION@ (Build @BUILD_NUMBER@)\"\n" +
        "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "   xmlns=\"http://www.eclipse.org/eclipselink/xsds/persistence\"\n" +
        "   >\n" +
        "   <name>keymappings</name>\n" +
        "   <class-mapping-descriptors>\n" +
        "      <class-mapping-descriptor xsi:type=\"xml-class-mapping-descriptor\">\n" +
        "         <class>dbws.testing.keymappings.Phone</class>\n" +
        "         <alias>phone</alias>\n" +
        "         <primary-key>\n" +
        "            <field name=\"@phone-id\" xsi:type=\"column\"/>\n" +
        "         </primary-key>\n" +
        "         <events xsi:type=\"event-policy\"/>\n" +
        "         <querying xsi:type=\"query-policy\"/>\n" +
        "         <attribute-mappings>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>phoneId</attribute-name>\n" +
        "               <field name=\"@phone-id\" xsi:type=\"node\">\n" +
        "               </field>\n" +
        "               <converter xsi:type=\"type-conversion-converter\">\n" +
        "                  <object-class>java.lang.Integer</object-class>\n" +
        "                  <data-class>java.lang.String</data-class>\n" +
        "               </converter>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>areaCode</attribute-name>\n" +
        "               <field name=\"ns1:area-code/text()\" xsi:type=\"node\">\n" +
        "                  <schema-type>{http://www.w3.org/2001/XMLSchema}string</schema-type>\n" +
        "               </field>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>phonenumber</attribute-name>\n" +
        "               <field name=\"ns1:phone-number/text()\" xsi:type=\"node\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>type</attribute-name>\n" +
        "               <field name=\"ns1:type/text()\" xsi:type=\"node\"/>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-object-reference-mapping\">\n" +
        "               <attribute-name>owner</attribute-name>\n" +
        "               <reference-class>dbws.testing.keymappings.Employee</reference-class>\n" +
        "               <source-to-target-key-field-association>\n" +
        "                  <field-reference>\n" +
        "                     <source-field name=\"@owner-ref-id\" xsi:type=\"node\"/>\n" +
        "                     <target-field name=\"@employee-id\" xsi:type=\"node\"/>\n" +
        "                  </field-reference>\n" +
        "               </source-to-target-key-field-association>\n" +
        "               <source-to-target-key-fields>\n" +
        "                  <field name=\"@owner-ref-id\" xsi:type=\"node\"/>\n" +
        "               </source-to-target-key-fields>\n" +
        "            </attribute-mapping>\n" +
        "         </attribute-mappings>\n" +
        "         <descriptor-type>aggregate</descriptor-type>\n" +
        "         <default-root-element>ns1:phone</default-root-element>\n" +
        "         <default-root-element-field name=\"ns1:phone\" xsi:type=\"node\"/>\n" +
        "         <namespace-resolver>\n" +
        "            <namespaces>\n" +
        "               <namespace>\n" +
        "                  <prefix>xsd</prefix>\n" +
        "                  <namespace-uri>http://www.w3.org/2001/XMLSchema</namespace-uri>\n" +
        "               </namespace>\n" +
        "               <namespace>\n" +
        "                  <prefix>ns1</prefix>\n" +
        "                  <namespace-uri>urn:keymappings</namespace-uri>\n" +
        "               </namespace>\n" +
        "               <namespace>\n" +
        "                  <prefix>xsi</prefix>\n" +
        "                  <namespace-uri>http://www.w3.org/2001/XMLSchema-instance</namespace-uri>\n" +
        "               </namespace>\n" +
        "            </namespaces>\n" +
        "         </namespace-resolver>\n" +
        "      </class-mapping-descriptor>\n" +
        "      <class-mapping-descriptor xsi:type=\"xml-class-mapping-descriptor\">\n" +
        "         <class>dbws.testing.keymappings.Address</class>\n" +
        "         <alias>address</alias>\n" +
        "         <primary-key>\n" +
        "            <field name=\"@address-id\" xsi:type=\"column\"/>\n" +
        "         </primary-key>\n" +
        "         <events xsi:type=\"event-policy\"/>\n" +
        "         <querying xsi:type=\"query-policy\"/>\n" +
        "         <attribute-mappings>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>addressId</attribute-name>\n" +
        "               <field name=\"@address-id\" xsi:type=\"node\">\n" +
        "               </field>\n" +
        "               <converter xsi:type=\"type-conversion-converter\">\n" +
        "                  <object-class>java.lang.Integer</object-class>\n" +
        "                  <data-class>java.lang.String</data-class>\n" +
        "               </converter>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>street</attribute-name>\n" +
        "               <field name=\"ns1:street/text()\" xsi:type=\"node\">\n" +
        "                  <schema-type>{http://www.w3.org/2001/XMLSchema}string</schema-type>\n" +
        "               </field>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>city</attribute-name>\n" +
        "               <field name=\"ns1:city/text()\" xsi:type=\"node\">\n" +
        "                  <schema-type>{http://www.w3.org/2001/XMLSchema}string</schema-type>\n" +
        "               </field>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>province</attribute-name>\n" +
        "               <field name=\"ns1:province/text()\" xsi:type=\"node\">\n" +
        "                  <schema-type>{http://www.w3.org/2001/XMLSchema}string</schema-type>\n" +
        "               </field>\n" +
        "            </attribute-mapping>\n" +
        "         </attribute-mappings>\n" +
        "         <descriptor-type>aggregate</descriptor-type>\n" +
        "         <default-root-element>ns1:address</default-root-element>\n" +
        "         <default-root-element-field name=\"ns1:address\" xsi:type=\"node\"/>\n" +
        "         <namespace-resolver>\n" +
        "            <namespaces>\n" +
        "               <namespace>\n" +
        "                  <prefix>xsd</prefix>\n" +
        "                  <namespace-uri>http://www.w3.org/2001/XMLSchema</namespace-uri>\n" +
        "               </namespace>\n" +
        "               <namespace>\n" +
        "                  <prefix>ns1</prefix>\n" +
        "                  <namespace-uri>urn:keymappings</namespace-uri>\n" +
        "               </namespace>\n" +
        "               <namespace>\n" +
        "                  <prefix>xsi</prefix>\n" +
        "                  <namespace-uri>http://www.w3.org/2001/XMLSchema-instance</namespace-uri>\n" +
        "               </namespace>\n" +
        "            </namespaces>\n" +
        "         </namespace-resolver>\n" +
        "      </class-mapping-descriptor>\n" +
        "      <class-mapping-descriptor xsi:type=\"xml-class-mapping-descriptor\">\n" +
        "         <class>dbws.testing.keymappings.Employee</class>\n" +
        "         <alias>employee</alias>\n" +
        "         <primary-key>\n" +
        "            <field name=\"@employee-id\" xsi:type=\"column\"/>\n" +
        "         </primary-key>\n" +
        "         <events xsi:type=\"event-policy\"/>\n" +
        "         <querying xsi:type=\"query-policy\"/>\n" +
        "         <attribute-mappings>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>employeeId</attribute-name>\n" +
        "               <field name=\"@employee-id\" xsi:type=\"node\">\n" +
        "               </field>\n" +
        "               <converter xsi:type=\"type-conversion-converter\">\n" +
        "                  <object-class>java.lang.Integer</object-class>\n" +
        "                  <data-class>java.lang.String</data-class>\n" +
        "               </converter>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>firstName</attribute-name>\n" +
        "               <field name=\"ns1:first-name/text()\" xsi:type=\"node\">\n" +
        "                  <schema-type>{http://www.w3.org/2001/XMLSchema}string</schema-type>\n" +
        "               </field>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-direct-mapping\">\n" +
        "               <attribute-name>lastName</attribute-name>\n" +
        "               <field name=\"ns1:last-name/text()\" xsi:type=\"node\">\n" +
        "                  <schema-type>{http://www.w3.org/2001/XMLSchema}string</schema-type>\n" +
        "               </field>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-object-reference-mapping\">\n" +
        "               <attribute-name>address</attribute-name>\n" +
        "               <reference-class>dbws.testing.keymappings.Address</reference-class>\n" +
        "               <source-to-target-key-field-association>\n" +
        "                  <field-reference>\n" +
        "                     <source-field name=\"@address-ref-id\" xsi:type=\"node\"/>\n" +
        "                     <target-field name=\"@address-id\" xsi:type=\"node\"/>\n" +
        "                  </field-reference>\n" +
        "               </source-to-target-key-field-association>\n" +
        "               <source-to-target-key-fields>\n" +
        "                  <field name=\"@address-ref-id\" xsi:type=\"node\"/>\n" +
        "               </source-to-target-key-fields>\n" +
        "            </attribute-mapping>\n" +
        "            <attribute-mapping xsi:type=\"xml-collection-reference-mapping\">\n" +
        "               <attribute-name>phones</attribute-name>\n" +
        "               <reference-class>dbws.testing.keymappings.Phone</reference-class>\n" +
        "               <source-to-target-key-field-association>\n" +
        "                  <field-reference>\n" +
        "                     <source-field name=\"ns1:phones/ns1:phone-ref/@phone-id\" xsi:type=\"node\"/>\n" +
        "                     <target-field name=\"@phone-id\" xsi:type=\"node\"/>\n" +
        "                  </field-reference>\n" +
        "               </source-to-target-key-field-association>\n" +
        "               <source-to-target-key-fields>\n" +
        "                  <field name=\"ns1:phones/ns1:phone-ref/@phone-id\" xsi:type=\"node\"/>\n" +
        "               </source-to-target-key-fields>\n" +
        "               <containerpolicy xsi:type=\"list-container-policy\">\n" +
        "                  <collection-type>java.util.Vector</collection-type>\n" +
        "               </containerpolicy>\n" +
        "               <uses-single-node>false</uses-single-node>\n" +
        "            </attribute-mapping>\n" +
        "         </attribute-mappings>\n" +
        "         <descriptor-type>aggregate</descriptor-type>\n" +
        "         <default-root-element>ns1:employee</default-root-element>\n" +
        "         <default-root-element-field name=\"ns1:employee\" xsi:type=\"node\"/>\n" +
        "         <namespace-resolver>\n" +
        "            <namespaces>\n" +
        "               <namespace>\n" +
        "                  <prefix>xsd</prefix>\n" +
        "                  <namespace-uri>http://www.w3.org/2001/XMLSchema</namespace-uri>\n" +
        "               </namespace>\n" +
        "               <namespace>\n" +
        "                  <prefix>ns1</prefix>\n" +
        "                  <namespace-uri>urn:keymappings</namespace-uri>\n" +
        "               </namespace>\n" +
        "               <namespace>\n" +
        "                  <prefix>xsi</prefix>\n" +
        "                  <namespace-uri>http://www.w3.org/2001/XMLSchema-instance</namespace-uri>\n" +
        "               </namespace>\n" +
        "            </namespaces>\n" +
        "         </namespace-resolver>\n" +
        "      </class-mapping-descriptor>\n" +
        "      <class-mapping-descriptor xsi:type=\"xml-class-mapping-descriptor\">\n" +
        "         <class>dbws.testing.RootHelper</class>\n" +
        "         <alias>RootHelper</alias>\n" +
        "         <events xsi:type=\"event-policy\"/>\n" +
        "         <querying xsi:type=\"query-policy\"/>\n" +
        "         <attribute-mappings>\n" +
        "            <attribute-mapping xsi:type=\"xml-any-collection-mapping\">\n" +
        "               <attribute-name>roots</attribute-name>\n" +
        "               <container xsi:type=\"list-container-policy\">\n" +
        "                  <collection-type>java.util.Vector</collection-type>\n" +
        "               </container>\n" +
        "               <keep-as-element-policy>KEEP_NONE_AS_ELEMENT</keep-as-element-policy>\n" +
        "            </attribute-mapping>\n" +
        "         </attribute-mappings>\n" +
        "         <descriptor-type>aggregate</descriptor-type>\n" +
        "         <instantiation/>\n" +
        "         <copying xsi:type=\"instantiation-copy-policy\"/>\n" +
        "         <change-policy xsi:type=\"deferred-detection-change-policy\"/>\n" +
        "         <default-root-element>ns1:employee-address-phone-system</default-root-element>\n" +
        "         <default-root-element-field name=\"ns1:employee-address-phone-system\" xsi:type=\"node\"/>\n" +
        "         <namespace-resolver>\n" +
        "            <namespaces>\n" +
        "               <namespace>\n" +
        "                  <prefix>xsd</prefix>\n" +
        "                  <namespace-uri>http://www.w3.org/2001/XMLSchema</namespace-uri>\n" +
        "               </namespace>\n" +
        "               <namespace>\n" +
        "                  <prefix>ns1</prefix>\n" +
        "                  <namespace-uri>urn:keymappings</namespace-uri>\n" +
        "               </namespace>\n" +
        "               <namespace>\n" +
        "                  <prefix>xsi</prefix>\n" +
        "                  <namespace-uri>http://www.w3.org/2001/XMLSchema-instance</namespace-uri>\n" +
        "               </namespace>\n" +
        "            </namespaces>\n" +
        "         </namespace-resolver>\n" +
        "      </class-mapping-descriptor>\n" +
        "   </class-mapping-descriptors>\n" +
        "   <login xsi:type=\"xml-login\">\n" +
        "      <platform-class>org.eclipse.persistence.oxm.platform.SAXPlatform</platform-class>\n" +
        "   </login>\n" +
        "</object-persistence>";

    // test fixtures
    public static XMLComparer comparer = new XMLComparer();
    public static XMLPlatform xmlPlatform = XMLPlatformFactory.getInstance().getXMLPlatform();
    public static XMLParser xmlParser = xmlPlatform.newXMLParser();
    public static XRServiceAdapter xrService = null;
    @BeforeClass
    public static void setUp() {
        final String username = System.getProperty(DATABASE_USERNAME_KEY);
        if (username == null) {
            fail("error retrieving database username");
        }
        final String password = System.getProperty(DATABASE_PASSWORD_KEY);
        if (password == null) {
            fail("error retrieving database password");
        }
        final String url = System.getProperty(DATABASE_URL_KEY);
        if (url == null) {
            fail("error retrieving database url");
        }
        final String driver = System.getProperty(DATABASE_DRIVER_KEY);
        if (driver == null) {
            fail("error retrieving database driver");
        }

        XRServiceFactory factory = new XRServiceFactory() {
            @Override
            public XRServiceAdapter buildService(XRServiceModel xrServiceModel) {
                parentClassLoader = this.getClass().getClassLoader();
                xrSchemaStream = new ByteArrayInputStream(KEYMAPPINGS_SCHEMA.getBytes());
                return super.buildService(xrServiceModel);
            }
            @Override
            public void buildSessions() {
                BaseEntityClassLoader becl = new BaseEntityClassLoader(parentClassLoader);
                XMLContext context = new XMLContext(
                    new EclipseLinkObjectPersistenceRuntimeXMLProject(),becl);
                XMLUnmarshaller unmarshaller = context.createUnmarshaller();
                Project orProject = (Project)unmarshaller.unmarshal(
                    new StringReader(KEYMAPPINGS_OR_PROJECT));
                DatasourceLogin login = new DatabaseLogin();
                login.setUserName(username);
                login.setPassword(password);
                ((DatabaseLogin)login).setConnectionString(url);
                ((DatabaseLogin)login).setDriverClassName(driver);
                Platform platform = new MySQLPlatform();
                ConversionManager conversionManager = platform.getConversionManager();
                if (conversionManager != null) {
                    conversionManager.setLoader(becl);
                }
                login.setDatasourcePlatform(platform);
                ((DatabaseLogin)login).bindAllParameters();
                orProject.setDatasourceLogin(login);
                Project oxProject = (Project)unmarshaller.unmarshal(
                    new StringReader(KEYMAPPINGS_OX_PROJECT));
                login = (DatasourceLogin)oxProject.getDatasourceLogin();
                if (login != null) {
                    platform = login.getDatasourcePlatform();
                    if (platform != null) {
                        conversionManager = platform.getConversionManager();
                        if (conversionManager != null) {
                            conversionManager.setLoader(becl);
                        }
                    }
                }
                ProjectHelper.fixOROXAccessors(orProject, oxProject);
                xrService.setORSession(orProject.createDatabaseSession());
                xrService.getORSession().dontLogMessages();
                xrService.setXMLContext(new XMLContext(oxProject));
                xrService.setOXSession(xrService.getXMLContext().getSession(0));
            }
        };
        XMLContext context = new XMLContext(new DBWSModelProject());
        XMLUnmarshaller unmarshaller = context.createUnmarshaller();
        DBWSModel model = (DBWSModel)unmarshaller.unmarshal(new StringReader(KEYMAPPINGS_DBWS));
        xrService = factory.buildService(model);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getAllEmployees() {
        Invocation invocation = new Invocation("getAllEmployees");
        Operation op = xrService.getOperation(invocation.getName());
        Object result = op.invoke(xrService, invocation);
        assertNotNull("result is null", result);
        Vector<BaseEntity> resultVector = (Vector<BaseEntity>)result;
        RootHelper rootHelper = new RootHelper();
        for (BaseEntity employee : resultVector) {
          rootHelper.roots.add(employee);
          rootHelper.roots.add(employee.get(0)); // address
          Vector<BaseEntity> phones = (Vector<BaseEntity>)employee.get(4); // phones
          phones.size(); // trigger IndirectList
          for (BaseEntity phone : phones) {
            rootHelper.roots.add(phone);
          }
        }
        Document doc = xmlPlatform.createDocument();
        XMLMarshaller marshaller = xrService.getXMLContext().createMarshaller();
        marshaller.marshal(rootHelper, doc);
        Document controlDoc = xmlParser.parse(new StringReader(EMPLOYEE_COLLECTION_XML));
        assertTrue("control document not same as XRService instance document",
            comparer.isNodeEqual(controlDoc, doc));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void buildEmployees() {
        XMLUnmarshaller unMarshaller = xrService.getXMLContext().createUnmarshaller();
        Reader reader = new StringReader(EMPLOYEE_COLLECTION_XML);
        InputSource inputSource = new InputSource(reader);
        Object result = unMarshaller.unmarshal(inputSource);
        assertNotNull("result is null", result);
        RootHelper rootHelper = (RootHelper)result;
        BaseEntity employee1 = (BaseEntity)rootHelper.roots.firstElement();
        assertNotNull("employee1 address is null", employee1.get(0));
        assertTrue("employee1 __pk incorrent", Integer.valueOf(1).equals(employee1.get(1)));
        assertTrue("employee1 first name incorrent", "Mike".equals(employee1.get(2)));
        assertTrue("employee1 last name incorrent", "Norman".equals(employee1.get(3)));
        Vector<BaseEntity> phones = (Vector<BaseEntity>)employee1.get(4); // phones
        assertTrue("employee1 has wrong number of phones", phones.size() == 2);
    }

    public static final String EMPLOYEE_COLLECTION_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<ns1:employee-address-phone-system xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns1=\"urn:keymappings\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
           "<ns1:employee employee-id=\"1\" address-ref-id=\"1\">" +
              "<ns1:first-name>Mike</ns1:first-name>" +
              "<ns1:last-name>Norman</ns1:last-name>" +
              "<ns1:phones>" +
                 "<ns1:phone-ref phone-id=\"1\"/>" +
                 "<ns1:phone-ref phone-id=\"2\"/>" +
              "</ns1:phones>" +
           "</ns1:employee>" +
           "<ns1:address address-id=\"1\">" +
              "<ns1:street>20 Pinetrail Cres.</ns1:street>" +
              "<ns1:city>Nepean</ns1:city>" +
              "<ns1:province>Ont</ns1:province>" +
           "</ns1:address>" +
           "<ns1:phone phone-id=\"1\" owner-ref-id=\"1\">" +
              "<ns1:area-code>613</ns1:area-code>" +
              "<ns1:phone-number>2281808</ns1:phone-number>" +
              "<ns1:type>Home</ns1:type>" +
           "</ns1:phone>" +
           "<ns1:phone phone-id=\"2\" owner-ref-id=\"1\">" +
              "<ns1:area-code>613</ns1:area-code>" +
              "<ns1:phone-number>2884638</ns1:phone-number>" +
              "<ns1:type>Work</ns1:type>" +
           "</ns1:phone>" +
           "<ns1:employee employee-id=\"2\" address-ref-id=\"2\">" +
              "<ns1:first-name>Rick</ns1:first-name>" +
              "<ns1:last-name>Barkhouse</ns1:last-name>" +
              "<ns1:phones>" +
                 "<ns1:phone-ref phone-id=\"3\"/>" +
                 "<ns1:phone-ref phone-id=\"4\"/>" +
              "</ns1:phones>" +
           "</ns1:employee>" +
           "<ns1:address address-id=\"2\">" +
              "<ns1:street>Davis Side Rd.</ns1:street>" +
              "<ns1:city>Carleton Place</ns1:city>" +
              "<ns1:province>Ont</ns1:province>" +
           "</ns1:address>" +
           "<ns1:phone phone-id=\"3\" owner-ref-id=\"2\">" +
              "<ns1:area-code>613</ns1:area-code>" +
              "<ns1:phone-number>2832684</ns1:phone-number>" +
              "<ns1:type>Home</ns1:type>" +
           "</ns1:phone>" +
           "<ns1:phone phone-id=\"4\" owner-ref-id=\"2\">" +
              "<ns1:area-code>613</ns1:area-code>" +
              "<ns1:phone-number>2884613</ns1:phone-number>" +
              "<ns1:type>Work</ns1:type>" +
           "</ns1:phone>" +
        "</ns1:employee-address-phone-system>";
}