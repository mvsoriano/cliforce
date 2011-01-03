# Some Java tools for Force.com

Right now only one tool is here: It deletes all custom objects (entities) from your org. This is useful if you are using your org as a database and are using the VMforce JPA provider to auto-create entities.

Note that Force.com currently doesn't allow you to completely erase entities via the API. So this script will put them in the recycle bin. If you run the script on every build, you will eventually build up quite a recycle bin, so it's recommended that you only run it when you need to.

# How to use

Clone this project:

	git clone git@github.com:jesperfj/force-tools.git

Compile the project:

	$ cd force-tools/DBTool
	$ mvn package

This tool uses a URL type format for connecting to Force.com, similar to JDBC drivers. The URL format looks like this

	force://login.salesforce.com;user=scott@acme.com;password=tiger

Hostname can be one of the login servers (e.g. test.salesforce.com for sandbox). You can set this URL in 3 different ways:

* In the FORCE_URL environment variable
* In the force.url Java system property
* Stored in the file ~/.force_url (where ~ means your home directory)

Each take precedence in the order listed here, e.g. if FORCE_URL is specified, everything else is ignored. So you can execute the dbclean task on a sandbox org with user scott@acme.com and password tiger by doing the following:

	$ export FORCE_URL=force://test.salesforce.com;user=scott@acme.com;password=tiger
	$ java -jar target/forcecli-0.0.1-SNAPSHOT-jar-with-dependencies.jar dbclean

Which will produce something like this:

	Connected to org 00Dx00000000XXXXXX
	Preparing to delete Producer__c
	Preparing to delete Wine__c
	Operation succeeded.


