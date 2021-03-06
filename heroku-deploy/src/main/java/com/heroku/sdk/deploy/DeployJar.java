package com.heroku.sdk.deploy;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * An easy way to deploy an executable Jar file
 */
public class DeployJar extends App {

  protected File jarFile;

  protected String jarOpts;

  public DeployJar(String name, File jarFile, String jarOpts) throws IOException {
    super(name);
    this.jarFile = jarFile;
    this.jarOpts = jarOpts;
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, String jdkVersion, String stack, String slugFileName) throws Exception {
    includedFiles.add(jarFile);
    super.deploy(includedFiles, configVars, jdkVersion, stack, defaultProcTypes(), slugFileName);
  }

  public void deploy(List<File> includedFiles, Map<String,String> configVars, URL jdkUrl, String stack, String slugFileName) throws Exception {
    includedFiles.add(jarFile);
    super.deploy(includedFiles, configVars, jdkUrl, stack, defaultProcTypes(), slugFileName);
  }

  protected Map<String,String> defaultProcTypes() {
    Map<String,String> processTypes = new HashMap<>();
    processTypes.put("web", "java $JAVA_OPTS -jar " + relativize(jarFile) + " " + jarOpts + " $JAR_OPTS");
    processTypes.putAll(getProcfile());
    return processTypes;
  }

  private static List<File> includesToList(String includes) {
    List<String> includeStrings = Arrays.asList(includes.split(File.pathSeparator));

    List<File> includeFiles = new ArrayList<>(includeStrings.size());
    for (String includeString : includeStrings) {
      if (!includeString.isEmpty()) {
        includeFiles.add(new File(includeString));
      }
    }

    return includeFiles;
  }

  protected Map<String, String> getProcfile() {
    Map<String, String> procTypes = new HashMap<String, String>();

    File procfile = new File("Procfile");
    if (procfile.exists()) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(procfile));
        String line = reader.readLine();
        while (line != null) {
          if (line.contains(":")) {
            Integer colon = line.indexOf(":");
            String key = line.substring(0, colon);
            String value = line.substring(colon + 1);
            procTypes.put(key.trim(), value.trim());
          }
          line = reader.readLine();
        }
      } catch (Exception e) {
        logDebug(e.getMessage());
      }
    }

    return procTypes;
  }

  @Override
  public void logInfo(String message) { System.out.println(message); }

  public static void main(String[] args) throws Exception {
    String jarFile = System.getProperty("heroku.jarFile", null);
    String jarOpts = System.getProperty("heroku.jarOpts", "");
    String appName = System.getProperty("heroku.appName", null);
    String jdkVersion = System.getProperty("heroku.jdkVersion", null);
    String jdkUrl = System.getProperty("heroku.jdkUrl", null);
    String stack = System.getProperty("heroku.stack", "cedar-14");
    List<File> includes = includesToList(System.getProperty("heroku.includes", ""));

    String slugFileName = System.getProperty("heroku.slugFileName", "slug.tgz");

    if (jarFile == null) {
      throw new IllegalArgumentException("Path to WAR file must be provided with heroku.warFile system property!");
    }
    if (appName == null) {
      throw new IllegalArgumentException("Heroku app name must be provided with heroku.appName system property!");
    }

    (new DeployJar(appName, new File(jarFile), jarOpts)).
        deploy(includes, new HashMap<String, String>(), jdkUrl == null ? jdkVersion : jdkUrl, stack, slugFileName);
  }
}
