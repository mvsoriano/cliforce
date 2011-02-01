# Command line tool for Force.com - BETA

This tool supports the following commands.

    apps:          Lists deployed applications
    banner:        Prints the banner
    connection:    Shows the current connection info
    dbclean:       Deletes all custom objects in the current org
    debug:         Turns debug output on/off
        Usage: debug [args]
        args:
        --off	Turns off debug logging to the console
        --on	Turns on debug logging to the console

    delete:        Deletes an application from VMforce
        Usage: delete <the name of the application>

    exit:          Exits this shell
    help:          Displays this help message, or help for a specific command
        Usage: help <command>
    history:       Shows history of previous commands
    list:          Lists custom objects
    plugin:        Adds a plugin to the shell
        Usage: plugin [args]
        args:
        -a, --artifact	maven artifact id for an artifact in group com.force.cliforce.plugin(required)
        -v, --version	maven artifact version for the specified artifact. If unspecified, RELEASE meta-version is used.

    push:          Pushes an application to VMforce.
        Usage: push [args]
        args:
        -i, --instances	Number of instances to deploy (default 1)
        -m, --mem	Memory to allocate to the application, in MB (default 512)
        -n, --name	Name of the application to push (required)
        -p, --path	Local path to the deployable application (required)

    restart:       Restarts an application
        Usage: restart <the name of the application>

    sh:            Executes the rest of the command on the OS
    start:         Starts an application
        Usage: start <the name of the application>

    stop:          Stops an application
        Usage: stop <the name of the application>

    unplug:        Removes a plugin and its commands from the shell



# How to use

Clone this project:

	git clone git@github.com:forcedotcom/cliforce.git

Compile the project:

	$ cd cliforce
	$ mvn package

This tool uses a URL format similar to JDBC drivers for connecting to Force.com. The URL format is:

	force://login.salesforce.com;user=scott@acme.com;password=tiger

Hostname is one of the login servers (test.salesforce.com for sandbox, login.salesforce.com for production). You can set this URL in 3 different ways:

* In the FORCE_URL environment variable
* In the force.url Java system property
* Stored in the file ~/.force_url (where ~ means your home directory)

Each takes precedence in the order listed above. For example, if FORCE_URL is specified, everything else is ignored. So you can execute the dbclean task on a sandbox org with user scott@acme.com and password tiger by doing the following:

	$ export FORCE_URL=force://test.salesforce.com;user=scott@acme.com;password=tiger
	$ java -jar target/forcecli-0.0.1-SNAPSHOT-jar-with-dependencies.jar

You see the force prompt.

    force>

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

in a file called cliforce in your ~/bin directory and do

	$ chmod u+x ~/bin/cliforce

This allows you to launch cliforce in any directory by typing cliforce at the command prompt.

You can also use a really neat trick documented in [this gist](https://gist.github.com/782862), (based on [this gist](https://gist.github.com/782523) by Sam Pullara)
to create a single executable file by combining a bash script and the jar.

##Windows

Create a batch file cliforce.bat:

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



