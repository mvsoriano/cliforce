#CLIForce Boot process

CLIForce is a self updating tool, here is how it does that.

## cliforce-boot module and its boot-assembly.xml assembly descriptor

Building the cliforce-boot module with maven produces 2 artifacts.

The first being the standard jar file produced by a maven module with jar packaging.
<<<<<<< HEAD
Something like cliforce-boot-22.0.0-SNAPSHOT.jar
=======
Something like cliforce-boot-21.0.1-SNAPSHOT.jar
>>>>>>> 2193c931da717147685d4a460f9f2b48f5c45cd9

The second is produced with the maven assembly plugin. It takes the classfiles for the cliforce-boot module plus the classfiles for
maven-aether (which is used to resolve dependencies) and its runtime dependencies, and puts them into a jar with the -boot qualifier,
with a MANIFEST Main-Class of com.force.cliforce.boot.Boot.

<<<<<<< HEAD
Something like cliforce-boot-22.0.0-SNAPSHOT-boot.jar. (Notice the -boot between SNAPSHOT and .jar)

When you install cliforce, you are essentially creating a script that runs

    java -jar path/to/cliforce-boot-22.0.0-SNAPSHOT-boot.jar
=======
Something like cliforce-boot-21.0.1-SNAPSHOT-boot.jar. (Notice the -boot between SNAPSHOT and .jar)

When you install cliforce, you are essentially creating a script that runs

    java -jar path/to/cliforce-boot-21.0.1-SNAPSHOT-boot.jar
>>>>>>> 2193c931da717147685d4a460f9f2b48f5c45cd9

The boot class is very simple. It uses the version of DependencyResolver (and maven-aether) that it is packaged up with
to create a classloader from the runtime dependencies of the LATEST release of the main cliforce module, using maven-aether
to download the appropriate jars from maven repositories. It then reflectively loads the CLIForce class and calls its main method,
using the classloader which contains the latest version of CLIForce and its runtime dependencies.

In this way, users will run the latest CLIForce version automatically, subject to standard maven update check timeframes (check once per day).


