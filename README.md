# Command line tool for Force.com - BETA

This tool supports the following commands.

	app:apps:           list deployed applications
	app:delete:         delete an application from VMforce
		Usage: delete <name of the application>

	app:push:           push an application to VMforce.
		Usage: push [args] <name of the application to push>
		args:
		-i, --instances	number of instances to deploy (default 1)
		-m, --mem	memory to allocate to the application, in MB (default 512)
		-p, --path	local path to the deployable application(required)

	app:restart:        restart an application
		Usage: restart <name of the application>

	app:start:          start an application
		Usage: start <name of the application>

	app:stop:           stop an application
		Usage: stop <name of the application>

	app:tail:           tail a file within a given application's instance
		Usage: tail [args] <name of the application>
		args:
		-i, --instance	instance on which to tail a file, default:0
		-p, --path	path to file(required)

	banner:             print the banner
	connection:         show the current connection info
	db:clean:           delete all custom objects in the current org
	db:list:            list custom objects
	debug:              turn debug output on/off
		Usage: debug [args] 
		args:
		--off	turn off debug logging to the console
		--on	turn on debug logging to the console

	exit:               exit this shell
	help:               display this help message, or help for a specific command
		Usage: help <command>
	history:            show history of previous commands
	plugin:             add a plugin to the shell
		Usage: plugin [args] <maven artifact id for an artifact in group com.force.cliforce.plugin>
		args:
		-v, --version	maven artifact version for the specified artifact; if unspecified, RELEASE meta-version is used

	require:            exit the shell if a specified version of a plugin is not installed
		Usage: require [args] <maven artifact id for an artifact in group com.force.cliforce.plugin>
		args:
		-v, --version	maven artifact version for the specified artifact; if unspecified, RELEASE meta-version is used

	sh:                 execute the rest of the command on the OS
	template:create:    create a new VMforce maven project from a maven archetype
		Usage: create [args] <artifactId/name of the project to create>
		args:
		-g, --group	groupId of the project to create, defaults to org name(reversed, ie my.org = org.my).artifactId
		-p, --package	root package for classes in project, defaults to groupId
		-t, type	type of project {single|multi}; defaults to single
		-v, --version	version of the project to create, default 1.0-SNAPSHOT

	template:list:      list the available project templates
	unplug:             remove a plugin and it's commands from the shell
	version:            show the current running version of cliforce

# How to use

Clone this project:

	git clone git@github.com:forcedotcom/cliforce.git

Compile the project:

	$ cd cliforce
	$ mvn package

This tool uses a URL format similar to JDBC drivers for connecting to Force.com. The URL format is:

	force://login.salesforce.com;user=scott@acme.com;password=tiger

Hostname is one of the login servers (test.salesforce.com for sandbox, login.salesforce.com for production). 

So, for example you can execute the dbclean task on a sandbox org with user scott@acme.com and password tiger by doing the following:

	$ java -jar boot/target/cliforce-0.0.3-SNAPSHOT-boot.jar

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

#Scripting

This tool is scriptable. You simply need to redirect input from a file.

os>./cliforce < mycliforceScript.cli

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



