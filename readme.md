# What is this? #

**JMeter for Appian** is a tool for Performance testing Appian using JMeter.

## Overview ##

Using this plugin JMeter provides an "easy" way record and replay Appian processes.

## Usage and Contributions ##

To use JMeter on your project, please follow the steps identified below for setting up the testing framework and creating/executing test cases.

We ask that whenever possible, you contribute back to the repository and the Appian PS community by 
* adding any missing parameterization that's necessary
* fixing issues and defects
* implementing enhancements

As we work and contribute to make this tool better, we will take the greatest care to ensure that the next versions are backwards compatible. Rest assure that whatever changes are released in the future are NOT going to break your test cases.

If your team cannot directly enhance the framework, please make sure to contact the Appian CoE on Home or over email in order to provide your feedback. 

**We can help only if we know where the problems are!**

## Installation ##
#### Installation ###

1. Download the newest version of [JMeter 3.0](http://apache.mirror.serversaustralia.com.au//jmeter/binaries/apache-jmeter-3.0.zip).
1. Unzip the release package and put the contents into JMETER_HOME which contains the following:
 1. `ApacheJMeter_http.jar` replaces the OOTB version with a few changes
 1. `jmeter-for-appian-X.X.jar` contains Appian specific functionality
 1. `jmeter.properties` turns on cookie settings
1. Download the example template file `template.jmx` from the release.

#### Recording Your First JMeter Test ####

1. Start JMeter if it isn't already running:
 1. In a command prompt navigate to `JMETER_HOME\bin`.
 1. Run `jmeter.bat`.
1. Open template.jmx included in the release.
1. Update the following in JMeter:
 1. User Defined Variables (Test Plan and WorkBench) - change **BASE_URL** value to the correct site URL
 1. HTTP Request Defaults (Test Plan and WorkBench) - change **Server Name or IP** to the correct site URL.
 1. HTTP(S) Test Script Recorder - change **URL Patterns to Include** to correct site URL.  All periods in the middle of the URL must be escaped with a backslash '\'. Leave the .* at the beginning and end of URL.
1. Start recording - click **Start** on HTTP(S) Test Script Recorder
1. Install JMeter certificate
 1. Right click `JMETER_HOME\bin\ApacheJMeterTemporaryRootCA.crt` and click Install Certificate
 1. Follow Next > Place all certificates in the following store > **Trusted Root Certification Authorities** > Next > Finish > Yes > OK  
 1. Configure browser to run through a proxy - Settings > Advanced > Change proxy settings<br />
 LAN settings > Use a proxy server for your LAN<br />
 **Address**: localhost<br />
 **Port**: 8090
 1. At this point samples should be recorded if you navigate to the site URL.
1. Take the following actions:
 1. Navigate to the login page.
 1. Login
1. Copy all of the created samples in the **Recording Controller** to the **Login (Test Fragment)**.
1. Take the following actions:
 1. Navigate to the Actions tab.
 1. Click on an action.
 1. Complete the action.
1. Stop Recording
1. Copy all of the created samples in the **Recording Controller** to the **Action (Test Fragment)**.
1. You can now run the performance test again by clicking the green run arrow and view the results.
 
## Development Environment Setup ##

#### Setup Maven ####
1. Download [Maven](http://maven.apache.org/)
1. Configure `M2_HOME` environment variable to point to your Maven installation directory.
1. Add `%M2_HOME%\bin` to PATH environment variable
1. Add Git to PATH environment variable

#### Clone the GitHub Repo ####
1. Clone the project repo to your local computer using `git clone git@github.com:appianps/ps-ext-JMeterForAppian.git`

#### Eclipse Setup ####
1. Setup the `M2_REPO` variable in Eclipse by running: `mvn eclipse:configure-workspace -Declipse.workspace="<your-eclipse-workspace>"`.
  * If you don't know your eclipse workspace, open your Eclipse preferences and go to General > Startup and Shutdown > Workspaces.
1. Cleanup any existing Eclipse project files `mvn eclipse:clean`
1. Generate new Eclipse project files by running the following from the root of the Labs repo working directory: `mvn eclipse:eclipse`.
1. Open Eclipse
1. Import the projects using File > Import > General > Existing Projects into Workspace.
1. Select the repo folder and import the project.

## Development ##

## Building ##
#### Version Selection Methodology ####
* Update **MAJOR** version when you make incompatible API changes, e.g. removing methods, changing method names, or anything that would require updates to existing test cases.
* Update **MINOR** version when you add functionality in a backwards-compatible manner, e.g. adding new methods, updating drivers or jars.
* Update **PATCH** version when you make backwards-compatible bug fixes, e.g. fixing existing methods.

#### Local ####
1. Run `mvn verify` to run integration tests.
1. Run `mvn clean package` (add `-DskipTests=true` to skip unit tests).
1. Final JAR and Package are placed in the `/target/` folder.

#### Release ####
A new release can be prepared by completing the following steps:

1. Run `mvn release:clean`.
1. Run `mvn release:prepare -DpushChanges=false` add `-DdryRun=true` to perform the Maven release procedure without commiting changes to the local repository. Add `-Darguments="-DskipTests"` to skip integration tests. <br>**This step will automatically do the following:**
 * Updates the pom.xml files with the release version number
 * Commits the updated pom.xml files to the local git repository
 * Runs all unit & integration tests and build the final packages
 * Updates the pom.xml files with the next development version number
 * Commits the updated pom.xml files to the local git repository
1. Push generated commits to GitHub.
1. Create new release on https://github.com/appianps/ps-ext-AutomatedTestFramework/releases.

As `mvn release:prepare` only commits the updated pom.xml files locally, they need to be pushed to GitHub the same as any other commit.
The final release JAR's are placed in the `/target/` folder of each module.

#### Create New Installation Package ####
1. Run `mvn package`
1. Upload new package to both the GitHub release and Share Component.
1. Upload template.jmx to both the GitHub release and Share Component.

If wanting to rebuild the customizations to the JMeter core components (to keep up with JMeter updates)

1. ApacheJMeter_http.jar and ApacheJMeter_components.jar
 1. On the jmeter branch merge the most recent changes from jmeter trunk
 1. run `ant`
 1. Copy the resulting ApacheJMeter_http.jar and ApacheJMeter_components.jar from the lib/ext folder to the resources folder of the master branch.
