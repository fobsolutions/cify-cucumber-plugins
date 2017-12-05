[ ![Download](https://api.bintray.com/packages/fobsolutions/io.cify/cify-cucumber-plugins/images/download.svg) ](https://bintray.com/fobsolutions/io.cify/cify-cucumber-plugins/_latestVersion)

1. <a href="#what">What Is Cify Cucumber Plugins?</a>
2. <a href="#usage">How To Use Cify Cucumber Plugins</a>
3. <a href="#plugins">List of plugins</a>

<a name="what" />
## What Is Cify Cucumber Plugins?

Cify Cucumber Plugins is part of an open source test automation tool called Cify. Cify cucumber plugins project is where users can find various plugins for Cify automation tool.

<a name="usage" />
## How To Use Cify Cucumber Plugins

### Installation
Add Gradle dependency to your project as following:

```
repositories {
    maven {
        url "http://fobsolutions.bintray.com/io.cify"
    }
}

dependencies {
    compile 'io.cify:cify-cucumber-plugins:1.0.0'
}
```

### Adding to Cucumber Plugins

It is possible to add custom plugins to cucumber runners.

#### Via command line

    ./gradlew cucumber -PcucumberPlugins=io.cify.cucumber.plugins.SauceLabsPlugin

#### Via properties file

    cucumberPlugins=io.cify.cucumber.plugins.SauceLabsPlugin

<a name="plugins" />
## List of currently usable plugins

### SauceLabsPlugin

#### Adding to cucumber plugins

Cucumber SauceLabs plugin is used when user adds SauceLabsPlugin to cucumberPlugins parameter. It is responsible for making jobs passed/failed in SauceLabs and also provides out of the box authentication proccess.

To use SauceLabsPlugin add plugin to project like following:

    ./gradlew cucumber -cucumberPlugins=io.cify.cucumber.plugins.SauceLabsPlugin
    
Or via properties file: 
    
    cucumberPlugins=io.cify.cucumber.plugins.SauceLabsPlugin
    
#### Adding username and password

Simply add **SAUCELABS_USERNAME** and **SAUCELABS_ACCESSKEY** to system environment variables

or

send -Dcify.SAUCELABS_USERNAME and -Dcify.SAUCELABS_ACCESSKEY with command line.

### CifyJSONFormatter

CifyJSONFormatter is a extension of default cucumber json formatter which adds device names to feature files in reports.

To use SauceLabsPlugin add plugin to project like following:

    ./gradlew cucumber -PcucumberPlugins=io.cify.cucumber.plugins.CifyJSONFormatter:build/cify/reports/json/
    
Or via properties file: 
    
    cucumberPlugins=io.cify.cucumber.plugins.CifyJSONFormatter:build/cify/reports/json/

[![Analytics](https://ga-beacon.appspot.com/UA-109814182-1/cify-pages)](https://github.com/fobsolutions/cify-pages)
