#Setting up to run the cliforce tests

Since cliforce is run off of a set of files in the user's ~/.force/ directory, you need to do some setup before being able to
run the tests.

There are 2 maven properties defined in the root pom.xml

    ${positive.test.user.home} defaults to /tmp/positive

    ${negative.test.user.home} defaults to /tmp/negative

So before being able to run tests that use cliforce logins and connections, you need to set up these directories on your local machine

From the root of cliforce Run.

    mvn clean install -DupdateReleaseInfo -DskipTests
    java -Duser.home=/tmp/positive -jar boot/target/cliforce-boot-0.0.4-SNAPSHOT-boot.jar

    #Note that if you will set ${positive.test.user.home} to something other than /tmp/positive via maven
    #you should use that value in the -Duser.home param above.

This will bring up cliforce, running against the (currently empty) set of files in ${positive.test.user.home}.

Run the following commands, and create a valid login, and a valid connection named test that hit the servers you are going to want to
test against.

    force> login
    Please log in
    Target login server [api.alpha.vmforce.com]:
    Login server: api.alpha.vmforce.com
    Username:<youruser@your.org>
    Password:**********************************
    Login successful.
    force> connection:add test force://<yourtesthost>;user=<yourtestdbuser>;password=<yourpassword>


Now run

    mkdir /tmp/negative/.force
    cp -r /tmp/positive/.force/* /tmp/negative/.force
    #Note that if you will set ${negative.test.user.home} to something other than /tmp/negative via maven
    #you should use that value in the -Duser.home param above.
    Go in and edit the users and passwords in cliforce_login and cliforce_urls to be incorrect, so that you get an error when logging in or connecting.

Assuming that the login and connection are set up correctly for positive and incorrectly for negative, you can now run the tests.

From the root of cliforce run..

    mvn clean install

The tests should run...and pass.

##Minor annoyance...running tests outside maven.

If you are going to run tests outside maven, from your ide, you will need to configure the appropriate system properties.
Setup your test runner to have

    -Dpositive.test.user.home=/tmp/positive -Dnegative.test.user.home=/tmp/negative
    ##or whatever your overriden values for these directories are






