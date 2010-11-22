# Some Java tools for Force.com

Right now only one tool is here: It deletes all custom objects (entities) from your org. This is useful if you are using your org as a database and are using the VMforce JPA provider to auto-create entities.

Note that Force.com currently doesn't allow you to completely erase entities via the API. So this script will put them in the recycle bin. If you run the script on every build, you will eventually build up quite a recycle bin, so it's recommended that you only run it when you need to.

# How to use

Clone this project:

	git clone git@github.com:jesperfj/force-tools.git

Create a config file called `.force_config` in your home directory with connection parameters for your org. It should look something like this:

	# Connection configuration for your Force.com Java app
	force.endPoint = https://login.salesforce.com
	force.apiVersion = 20.0
	force.userName = my_user@my_domain
	force.password = my_password

Compile the project:

	$ cd DBTools
	$ mvn package

This should create a complete jar-with-dependencies in the target directory allowing you to run the script with:

	$ java -jar target/DBTool-0.0.1-SNAPSHOT-jar-with-dependencies.jar

Which will produce something like this:

	Connected to org 00Dx00000000XXXXXX
	Preparing to delete Producer__c
	Preparing to delete Wine__c
	Operation succeeded.

If you look at the code, you can tell there is ambition to expand this tool to do other things. DBTool is the main class that sets you up to do API stuff and the DBClean class contains the clean code. The plan is to add command line switches and more commands.


