# Command line tool for Force.com - BETA

This tool supports the following commands.

       banner:                print the banner
       classpath:             show the classpath for a cliforce plugin, or for cliforce itself.
        Note that the classloader of cliforce is the parent classloader of plugin classloaders
        Usage: classpath [args] <name of the plugin to get the classpath for, or none for the cliforce classpath>
        args:
        -s, --sort	sort the returned list of files on the classpath alphabetically

       codegen:jpaClass:      Generates Force.com JPA enabled Java classes
        Usage: jpaClass [args] <names of Force.com schema objects to generate as JPA classes>
        args:
            --projectDir	root directory for project
        -a, --all	generate all Force.com schema objects as JPA classes
        -d, --destDir	destination directory for generated JPA classes (within project)
        -p, --package	java package name for generated JPA classes

       connection:add:        add a database connection
        Usage: add [args]
        args:
            --notoken	set this flag if security tokens are turned off in your org
        -h, --host	Host to connect to, defaults to vmf01.t.salesforce.com
        -n, --name	name of the connection
        -p, --password	password with which to connect
        -t, --token	security token with which to connect
        -u, --user	username with which to connect, youruser@your.org

       connection:current:    show the currently selected connection
       connection:default:    set the current and default connection for cliforce. Usage connection:default <connectionName>
       connection:list:       list the currently available connections
       connection:remove:     remove a connection from cliforce. Usage connection:remove <connectionName>
       connection:rename:     rename a connection for cliforce. Usage connection:rename <currentName> <newName>
       connection:set:        set the current connection for cliforce. Usage connection:set <connectionName>
       connection:test:       test the current connection. Usage connection:test <connectionName>
       db:describe:           Describes Force.com schema in the current org
        Usage: describe [args] <names of Force.com schema objects to describe>
        args:
        -a, --all	describe all Force.com schema objects
        -c, --custom	describe custom Force.com schema objects
        -s, --standard	describe standard Force.com schema objects
        -v, --verbose	verbosely describe Force.com schema objects

       debug:                 Turns debug output on. Or off with the --off switch
        Usage: debug [args]
        args:
            --off	Turns off debug logging to the console

       env:                   Display the current environment variables
       exit:                  Exit this shell
       help:                  Display this help message, or help for a specific command
        Usage: help <command>
       history:               Show history of previous commands
       jpa:clean:             Deletes schema for all writable JPA entities in the current org
       jpa:populate:          Populate schema for all writable JPA entities in the current org
       jpa:query:             Run JPQL (or SOQL) against the current org
       plugin:                adds a plugin to the shell
        Usage: plugin [args] <maven artifact id for an artifact in group com.force.cliforce.plugin>
        args:
        -v, --version	maven artifact version for the specified artifact, if unspecified RELEASE meta-version is used

       require:               exit the shell if a specified version of a plugin is not installed
        Usage: require [args] <maven artifact id for an artifact in group com.force.cliforce.plugin>
        args:
        -v, --version	maven artifact version for the specified artifact, if unspecified RELEASE meta-version is used

       sh:                    Execute the rest of the command on the OS
       sysprops:              Display the current Java system properties
       template:create:       creates a new forcesdk maven project from a maven archetype
        Usage: create [args] <artifactId/name of the project to create>
        args:
        -d, --dir	directory to create the project in, defaults to current dir.
        -g, --group	groupId of the project to create, defaults to org name(reversed, ie my.org = org.my).artifactId
        -p, --package	root package for classes in project, defaults to groupId
        -v, --version	version of the project to create, default 1.0-SNAPSHOT

       template:list:         list the available project templates
       unplug:                removes a plugin and its commands from the shell
       version:               Show the current running version of cliforce


# How to use

Clone this project:

	git clone git@github.com:forcedotcom/cliforce.git

Compile the project:

	$ cd cliforce
	$ mvn install -DupdateReleaseInfo -DskipTests

This tool uses a URL format similar to JDBC drivers for connecting to Force.com. The URL format is:

	force://login.salesforce.com;user=scott@acme.com;password=tiger

Hostname is one of the login servers (test.salesforce.com for sandbox, login.salesforce.com for production). 

So, for example you can execute the dbclean task on a sandbox org with user scott@acme.com and password tiger by doing the following:

	$ java -jar boot/target/cliforce-22.0.0-SNAPSHOT-boot.jar

You see the force prompt.

    force>

Enter the following command at the prompt.

    force> connection:add test force://test.salesforce.com;user=scott@acme.com;password=tiger

This will add a connection to cliforce, which is persisted in ~/.force_urls, so you only have to perform this step once.

Hit the tab key to see available commands or type help to see the description of the behavior of each command.

To clean out your org, type dbclean at the prompt.

    force> dbclean

This command produces output like this:

	Connected to org 00Dx00000000XXXXXX
	Preparing to delete Producer__c
	Preparing to delete Wine__c
	Operation succeeded.

#Installation

##Unix

Put the cliforce-boot<VERSION>.boot.jar in a directory on your path.
In this example, your ~/bin directory.
Put the line (replacing <VERSION> with your version)

        java -Xmx512M -jar `dirname $0`/cliforce-boot<VERSION>.boot.jar "$@"

in a file called cliforce in your ~/bin directory and run

	$ chmod u+x ~/bin/cliforce

This allows you to launch cliforce in any directory by typing cliforce at the command prompt.

You can also use a really neat trick documented in [this gist](https://gist.github.com/782862), (based on [this gist](https://gist.github.com/782523) by Sam Pullara)
to create a single executable file by combining a bash script and the jar.

##Windows

Create a cliforce.bat batch file:

        set SCRIPT_DIR=%~dp0
        java -Xmx512M -jar "%SCRIPT_DIR%cliforce-boot<VERSION>.boot.jar" %*

(replacing <VERSION> with your version)and put the jar in the same directory as the batch file.
Put cliforce.bat on your path so that you can launch cliforce in any directory by typing cliforce at the command prompt.

##Java System properties you may want to add to your script

cliforce create/uses a directory named .force to store login, connection,plugin and history information. By default this directory is created under the user's home directory.
To override this location, set the java system property cliforce.home

        java -Dcliforce.home=/path/to/different/home -jav cliforce-boot.jar

Note that this will still create a directory called .force under /path/to/different/home


cliforce creates or uses a local maven repository to download and store resolved dependencies. By default this directory is assumed to be .m2/repository under the users's home directory, unless
cliforce.home is set, then the assumption is that the repo would be in .m2/reposotory under cliforce.home. To override this location set the java system property maven.repo

So to set an alternate cliforce.home but to use the local maven repository of the user, you can do the following.

        java -Dcliforce.home=/path/to/different/home -Dmaven.repo=/path/to/user/home/.m2/repository -jar cliforce-boot.jar





#Scripting

This tool is scriptable. You simply need to redirect input from a file.

os> cliforce < mycliforceScript.cli

#Deployment

Due to the way that cliforce tests bootstrapping itself, you must run mvn -DskipTests=true install once when you increment the version before the test will
 pass. You will most likely do this if you are deploying a new version.

        <increment pom version>
        git commit -a -m 'increment version for release'
        git tag -a <version-tag> -m 'version-tag'
        mvn -DskipTests=true install
        mvn deploy
        <increment to next snapshot>
        git commit -a -m 'increment to next snapshot'
        mvn -DskipTests=true install


# License

Copyright (c) 2011, salesforce.com, inc.

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.




