---

layout: doc
title: cliforce

---
# cliforce

[TODO: Question @Jesper] All cliforce doc on this page currently. Maybe break it into following sub-pages?
cliforce
Managing Connections
Reading Schema and Data (db:describe and jpa:query)
Generating Schema (jpa:populate and jpa:clean)
Generating Code (codegen:jpaClass)
Plugins (not sure on level of details needed)

You can use the extensible cliforce command-line tool for a variety of tasks when creating and developing Java applications.

## Installing cliforce
For instructions on installing cliforce, see the [quick start](quick-start#installcliforce).

After installing cliforce, start it up and run the help command at the `force>` prompt to see all the available options.

    cliforce
    force> help
    
The cliforce installation creates a .force directory to store login, connection, and history information. By default, this directory is created under your home directory. To override this location, set the Java cliforce.home system property when starting cliforce. For Linux:

    java -Dcliforce.home=/path/to/different/home -jar cliforce.jar

This creates a .force directory under /path/to/different/home instead of your home directory.

cliforce creates and uses a local Maven repository to download and store resolved dependencies. By default, this directory is assumed to be ~/.m2/repository. If you set cliforce.home, the repository is created in .m2/repository under the cliforce.home directory instead. To override the location of the local Maven repository, set the Java maven.repo system property.

To set an alternate cliforce.home but still use the standard location of the local Maven repository on Linux:

    java -Dcliforce.home=/path/to/different/home -Dmaven.repo=/path/to/user/home/.m2/repository -jar cliforce-boot.jar

## Creating a Java Project from a Template
You can use cliforce to quickly create a new application project. For example, to create a project called "hellocloud", run the following command:

    cliforce template:create hellocloud

This creates a new `hellocloud` directory with a basic Spring MVC application project structure. For more information, see the [quick-start](quick-start#createProjectTemplate).

Note: If you create a new project in a directory that contains a pom.xml file, the new project is added as a submodule. 

## Connecting to Force.com
There are several commands for adding and managing connections to Force.com. A connection consists of a service endpoint location and credentials used by a client application to access Database.com. Credentials can consist of a username and password pair, an OAuth key and secret pair, or a combination of both pairs. Read more about connection URLs [here](connection-url).

You can manage multiple connections with cliforce and it's easy to switch between connections. You associate a name with each connection. You may want to simultaneously manage connections for development, staging, and production environments.

### Adding a Connection
Use connection:add to add a Force.com connection. A connection corresponds to a currently logged-in user. Subsequent commands are executed for this user. To add a connection, run:

    connection:add -h login.salesforce.com -n devEnv -u user@salesforcedoc.org -p samplePassword -t gVzTdcXM8MguTnkH3TG6WyJf
    
See the help command for a definition of each argument.

### Setting a Connection
Use connection:set to set the current connection. To set the connection to a development org with a connection name of devEnv:

    connection:set devEnv

connection:default:    set the current and default connection for cliforce. Usage connection:default <connectionName>

### Setting a Default Connection
Use connection:default to set the default connection. The default connection is the connection used if you exit cliforce and then start it again. Setting a default connection also sets the connection as the current connection. To set the default connection to a connection named of devEnv:

    connection:set devEnv

### Showing the currently selected connection
Use connection:current to show the currently selected connection. If you're working with multiple connections, it's useful to check your current connection before you make any changes to ensure that you're connecting to the intended organization. For example:

    connection:current
    
### Listing connections
Use connection:list to list the connections managed by cliforce. For example:

    connection:list

### Testing a Connection
Use connection:test to test that a connection is valid. To test a connection named devEnv:

    connection:test devEnv

### Renaming a Connection
Use connection:rename to rename a connection. To rename a connection named devEnv to stagingEnv:

    connection:rename devEnv stagingEnv

### Removing a Connection
Use connection:remove to remove a connection. To remove a connection named devEnv:

    connection:remove devEnv

## Describing Force.com Entities
Use db:describe to describe the Force.com schema for the current connection. There are multiple options that allow you to describe a single entity, a named subset of entities, all the custom entities, or all the standard entities.

To describe one or more entities, add the list of entities as arguments. For example:

    db:describe Account
    
To describe the Case standard entity and a Merchandise__c custom entity:

    db:describe Case Merchandise__c
        
The result indicates the CRUD permissions on the entity or entities for the user associated with the current connection. If the entity is READABLE, the user can query the entity.

Use the -v flag to to return verbose information about the entity. Instead of just the CRUD permissions, the verbose results include information for each field in the entity. The information for each field includes the field name and label, field type, whether the field is required, and whether it has a default value. For example, to get verbose information about the User entity:

    db:describe -v User

Use the -a flag to describe all the Force.com schema for the current connection

Use the -s flag to describe all the standard entities (objects) for the current connection

Use the -c flag to describe all the custom entities (objects) for the current connection

<a name="codegen"></a>
## Generating Java Classes from Force.com Entities
Use codegen:jpaClass to generate Java classes from your Force.com schema. For example, to generate a Java class for the Account entity on Windows:

    codegen:jpaClass --projectDir C:\force\codegen -p com.force.docsample Account
    
The --projectDir flag sets the root directory for the project. If this flag is not set, the current working directory is the default.  

The -d flag sets the relative path within the project for the generated Java classes. If this flag is not set, the default directory is src/main/java.

The -p flag sets the package name for the generated Java classes. If this flag is not set, the default package name is com.*normalizedCompanyName*.model, where *normalizedCompanyName* is your company (organization) name with any periods stripped out.

The -a flag generates Java classes for all the Force.com schema in your organization. Alternatively, you can specify a space-separated list of entities on the command line if you want to generate classes for a subset of your schema.

Each generated class is read-only schema.  This means that the JPA provider never attempts to persist the class as metadata schema in Force.com. Read-only schema is denoted in the generated code by the @com.force.sdk.jpa.annotation.CustomObject(readOnlySchema=true) Force.com custom annotation.

If an entity has a custom field with the same name as a standard field, the custom field has "Custom" appended to the field name in the generated Java class.

If your organization has a namespace, the namespace precedes each entity name in the generated Java classes.

To generate Force.com schema from Java classes, see [Creating Force.com Schema](#creatingSchema).

### Transitive Closure Generation
To ensure that the generated code compiles, the code generator discovers all schema relationships during code generation and generates those relationships as JPA Java classes. This is called transitive closure. For example, the standard Contact entity has a relationship field to the Account entity. If you generate a Java class for the Contact entity, the code generator generates both Contact and Account classes, as well as classes for any other relationships for Contact. 

Every time you run codegen:jpaClass, a Java class is generated for the User entity as this is a central entity that is referenced by many other entities. Any entities associated with reference fields in the User entity are also generated. Therefore, several classes are generated even if you only specify one entity on the command line.

### Extending Generated Classes
If you wish to create new schema, you should sub-class instead of editing any of the generated files. With this approach, you can safely regenerate code again later without blowing away your extensions.

You can extend a generated class to add a custom field. For example:

    public class AccountCustom extends Account {
        
        public String myCustomField;
        ...
        public String getMyCustomField() {
            return this.myCustomField;
        }
        
        public void setMyCustomField(String myCustomField) {
            this.myCustomField = myCustomField;
        }
        ...
    }

Another reason to extend generated code is to change the [fetch type](jpa-queries#eagerVsLazy) for a field. All generated relationship fields are marked as lazily fetched JPA fields. A lazily-loaded field is not loaded or cached for JPQL queries unless the field is specifically named in the SELECT clause. However, you can override this behavior by sub-classing to make the field eagerly loaded. for example, to make the account field in Contact eagerly loaded:

    public class ContactExt extends Contact {
        ...
        @Override
        @ManyToOne
        @Basic(fetch=FetchType.EAGER) // Force the account field to be eagerly fetched
        @Column(name="AccountId")
        public Account getAccount() {
            return this.account;
        }
        
        @Override
        public void setAccount(Account account) {
            this.account = account;
        }
        ...
    }    

Note that the Force.com JPA provider requires that you include both the getter and setter when overriding a field definition. Also, you must copy all field annotations from the generated Contact class; otherwise, those annotations are lost.

<a name="creatingSchema"></a>
## Creating Force.com Schema
Use jpa:populate to create Force.com schema for all writable JPA entities and fields for the persistence unit of an application. An entity is writable if it's annotated with @CustomObject(readOnlySchema = true).

You must set the -a, -g, and -v flags to specify the Maven coordinates used to uniquely identify a deployment artifact (jar or war) that has been installed in your Maven repository using the mvn:install command.

The -a flag specifies the artifactId, which is a unique identifier under groupId that represents a single project.

The -g flag specifies the groupId for the project. The convention for groupId is that it begins with the reverse domain name of your company. For example, the groupId for a project from docsample.org would start with org.docsample.

The -v flag specifies a release version of a project.

The optional -u flag specifies a persistent-unit name. This is useful if the deployment artifact contains more than one persistence unit. If you don't specify a -u flag and the deployment artifact includes more than one persistence.xml, you are prompted to select one from the list. If the selected persistence.xml has more than one persistence unit, you are prompted to select a persistence unit from the list. 

If you want to model schema in a Java class and persist it to Force.com, see [Generating Java Classes from Force.com Entities](#codegen) for best practices for using annotations. You can create a Java class to represent a custom Force.com entity by sub-classing **BaseForceCustomObject**, which is the base class for custom Force.com entities. **BaseForceCustomObject** is in the com.force.sdk.jpa.model package in the Database.com Java SDK.

## Deleting Force.com Schema
Use jpa:clean to delete all writable JPA entities in the persistence unit of an application. An entity is writable if it's annotated with @CustomObject(readOnlySchema = true). The primary reason for cleaning your schema is to remove customizations during testing to start with a consistent set of schema.

You must set the -a, -g, and -v flags to specify the Maven coordinates used to uniquely identify a deployment artifact (jar or war) that has been installed in your Maven repository using the mvn:install command. For descriptions of these flags and the optional -u flag, see [Creating Force.com Schema](#creatingSchema).

The optional -f flag enables the Force.com JPA provider to bypass the Recycle bin. Instead, deleted schema becomes immediately eligible for deletion. This flag is only available if the force.deleteSchema and force.purgeOnDeleteSchema properties are set to true in your persistence unit. For more information, see [Schema Deletion Properties](jpa-config-persistence#schemaDeleteProps).


## Querying Force.com Entities
Use jpa:query to run JPQL or SOQL queries against JPA entities in the persistence unit of an application. 
    
You must set the -a, -g, and -v flags to specify the Maven coordinates used to uniquely identify a deployment artifact (jar) that has been installed in your Maven repository using the mvn:install command. For descriptions of these flags and the optional -u flag, see [Creating Force.com Schema](#creatingSchema).

You need to reference a deployment artifact as cliforce uses the compiled code to unmarshall the query results.

[TODO: @Jesper] should we change the prompt to query> as it supports JPQL and SOQL
When you run jpa_query with the required flags, cliforce presents a jpql> prompt. Enter your query at the command prompt.

Here is a sample JPQL query:

    jpql>select o from Account o

For more information on JPQL, see [Querying with JPQL](jpa-queries).

To run a SOQL query, prepend soql: to your query. For example:

    jpql>soql:select id, name from Account

For more information on SOQL, see [Querying with SOQL](jpa-queries-soql).    
   
[TODO @Jesper] do we need more details here or is pointer enough?
## Plugins
Writing a plugin enables you to add commands that execute your own Java code, which is passed a CommandContext as a parameter. This context gives you access to MetadataConnection, PartnerConnection, and BulkConnection connections.

Use the plugin command to add a new plugin.

For more details about plugins, see [Writing plugins](https://github.com/forcedotcom/cliforce/blob/master/PLUGINS.md).
