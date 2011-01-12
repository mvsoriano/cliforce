# Command line tool for Force.com

This tool supports the following commands.

        apps:			 lists deployed apps
        connection:	     Show the current connection info:
        dbclean:		 Deletes all custom objects in the current org
        exit:			 Exit this shell
        help:			 Display this help message
        history:		 Show history of previous commands
        list:			 list custom objects
        plugin:			 adds a plugin to the shell
            Usage: plugin
            <main>	the maven artifact to load plugins from, syntax: plugin mavengroup:artifact:version

        push:			 push an application to VMForce.
            Usage: push
            -p, --path	Local path to the deployable app
            -i, --instances	Number of instances to deploy (default 1)
            -n, --name	Name of the Application to push
            -m, --mem	Memory to allocate to the app, in MB (default 256)

        restart:			 restart an application
        start:			 start an application
        stop:			 Stop an application
        unplug:			 removes a plugin from the shell

# How to use

Clone this project:

	git clone git@github.com:jesperfj/cliforce.git

Compile the project:

	$ cd cliforce
	$ mvn package

This tool uses a URL type format for connecting to Force.com, similar to JDBC drivers. The URL format looks like this

	force://login.salesforce.com;user=scott@acme.com;password=tiger

Hostname can be one of the login servers (e.g. test.salesforce.com for sandbox). You can set this URL in 3 different ways:

* In the FORCE_URL environment variable
* In the force.url Java system property
* Stored in the file ~/.force_url (where ~ means your home directory)

Each take precedence in the order listed here, e.g. if FORCE_URL is specified, everything else is ignored. So you can execute the dbclean task on a sandbox org with user scott@acme.com and password tiger by doing the following:

	$ export FORCE_URL=force://test.salesforce.com;user=scott@acme.com;password=tiger
	$ java -jar target/forcecli-0.0.1-SNAPSHOT-jar-with-dependencies.jar

You will be taken to the force prompt.

    force>

Hit the tab key to see available commands or type help to see the description of the behavior of each command.

If you wish to clean out your org, type dbclean at the prompt.

    force> dbclean

Which will produce something like this:

	Connected to org 00Dx00000000XXXXXX
	Preparing to delete Producer__c
	Preparing to delete Wine__c
	Operation succeeded.

#Installation

##Unix

Put the cliforce-<VERSION>.boot.jar in a directory on your path.
In this example, your ~/bin directory.
Put the line (replacing <VERSION> with your version)

        java -Xmx512M -jar `dirname $0`/cliforce-<VERSION>.boot.jar "$@"

in a file called cliforce in your ~/bin directory and do

$ chmod u+x ~/bin/cliforce

This allows you to launch cliforce in any directory by typing cliforce at the command prompt.

##Windows

Create a batch file cliforce.bat:

        set SCRIPT_DIR=%~dp0
        java -Xmx512M -jar "%SCRIPT_DIR%cliforce-<VERSION>.boot.jar" %*

(replacing <VERSION> with your version)and put the jar in the same directory as the batch file.
Put cliforce.bat on your path so that you can launch cliforce in any directory by typing cliforce at the command prompt.

#Scripting

This tool is scriptable, you simply need to redirect input from a file

os>./cliforce < mycliforceScript.cli


