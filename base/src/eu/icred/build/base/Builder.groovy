package eu.icred.build.base;

import java.nio.file.Files

import eu.icred.build.plugin.PluginBuilder;

public class Builder {

    AntBuilder ant = new AntBuilder()
    File jarFile
    File exeFile
    String version
    File baseDir
    File outputDir
    File exportDir

    public Builder(File baseDir, File exportDir, String version) {
        this.baseDir = baseDir
        this.version = version
        this.exportDir = exportDir

        outputDir = new File(exportDir, version)
    }

    public void build() {

        File workingDir = new File(System.getProperty("user.dir"));
        Properties buildProp = getBuildProp(workingDir)
        if(buildProp.isEmpty()) {
            ant.echo "Error: missing build.prop"
            System.exit(0)
        }

        ant.delete(dir:outputDir)
        ant.mkdir(dir:outputDir)

        File modelDir = new File(baseDir, 'model')
        File coreDir = new File(baseDir, 'core')
        File uiDir = new File(baseDir, 'ui')

        File libsDir = new File(outputDir, "libs")
        ant.mkdir(dir:libsDir)
        File pluginsDir = new File(outputDir, "plugins")
        ant.mkdir(dir:pluginsDir)

        jarFile = new File(libsDir, "icred-${version}.jar")
        exeFile = new File(outputDir, "icred-${version}.exe")


        ant.echo "build $jarFile"

        File tempDir = Files.createTempDirectory("icred-${version}-"+Long.toString(System.nanoTime())).toFile()
        File binDir = new File(tempDir, 'bin')
        File tempLibsDir = new File(tempDir, 'libs')
        ant.mkdir(dir:tempLibsDir)

        new File(modelDir, 'libs').eachFileMatch({it.endsWith('.jar')}) {
            ant.copy(file:it, todir:tempLibsDir, overwrite:'Yes')
        }
        new File(coreDir, 'libs').eachFileMatch({it.endsWith('.jar')}) {
            ant.copy(file:it, todir:tempLibsDir, overwrite:'Yes')
        }
        new File(uiDir, 'libs').eachFileMatch({it.endsWith('.jar')}) {
            ant.copy(file:it, todir:tempLibsDir, overwrite:'Yes')
        }

        ant.mkdir(dir:binDir)

        try {
            ant.echo "compile java classes"
            ant.javac(destdir:binDir, source:"1.7", target:"1.7", debug:"on", fork:'yes') {
                src {
                    pathelement(path:new File(baseDir, 'model'))
                    pathelement(path:new File(baseDir, 'core'))
                    pathelement(path:new File(baseDir, 'ui'))
                }
                classpath {
                    fileset(dir:tempLibsDir, includes: "*.jar")

                    pathelement(path:binDir.canonicalPath)
                }
            }

            ant.echo "build jar"
            ant.jar( destfile: jarFile, compress: true ) {
                fileset( dir: binDir, includes: '**' )

                manifest {
                    // attribute( name: 'PluginClass', value: pluginClass )
                }
            }

            ant.echo "move basic libs"
            tempLibsDir.eachFile {
                ant.copy(file:it, todir:libsDir, overwrite:'Yes')
            }

            ant.echo "build exe"

            File SKELETON_ROOT = new File('./res/sceletons')
            File JSMOOTH_ANT = new File('./lib/jsmoothgen-ant.jar')

            File jSmoothConfigFile = new File(tempDir, 'build.jsmooth')
            jSmoothConfigFile.text = ''
            jSmoothConfigFile << '<?xml version="1.0" encoding="ISO-8859-1"?>' + "\r\n"
            jSmoothConfigFile << '<jsmoothproject>' + "\r\n"
            jSmoothConfigFile << '  <JVMSearchPath>jdkpath</JVMSearchPath>' + "\r\n"
            jSmoothConfigFile << '  <JVMSearchPath>javahome</JVMSearchPath>' + "\r\n"
            jSmoothConfigFile << '  <JVMSearchPath>registry</JVMSearchPath>' + "\r\n"
            jSmoothConfigFile << '  <JVMSearchPath>jrepath</JVMSearchPath>' + "\r\n"
            jSmoothConfigFile << '  <JVMSearchPath>exepath</JVMSearchPath>' + "\r\n"
            jSmoothConfigFile << '  <JVMSearchPath>jview</JVMSearchPath>' + "\r\n"
            jSmoothConfigFile << '  <currentDirectory>${EXECUTABLEPATH}</currentDirectory>' + "\r\n"
            jSmoothConfigFile << '  <embeddedJar>false</embeddedJar>' + "\r\n"
            jSmoothConfigFile << '  <executableName>'+exeFile.name+'</executableName>' + "\r\n"
            jSmoothConfigFile << '  <initialMemoryHeap>-1</initialMemoryHeap>' + "\r\n"
            //            jSmoothConfigFile << '  <jarLocation>'+new File(/D:\workspace\eclipse\icred\build\base\dummy.jar/).canonicalPath+'</jarLocation>' + "\r\n"

            
            jSmoothConfigFile << '  <classPath>libs/'+jarFile.name+'</classPath>' + "\r\n"
            libsDir.eachFile { File libFile ->
                jSmoothConfigFile << '  <classPath>libs/'+libFile.name+'</classPath>'
            }

            jSmoothConfigFile << '  <mainClassName>'+buildProp.getProperty('mainClassName')+'</mainClassName>' + "\r\n"
            jSmoothConfigFile << '  <maximumMemoryHeap>-1</maximumMemoryHeap>' + "\r\n"
            jSmoothConfigFile << '  <maximumVersion></maximumVersion>' + "\r\n"
            jSmoothConfigFile << '  <minimumVersion></minimumVersion>' + "\r\n"
            jSmoothConfigFile << '  <skeletonName>Console Wrapper</skeletonName>' + "\r\n"
            //            jSmoothConfigFile << '  <skeletonName>Windowed Wrapper</skeletonName>' + "\r\n"
            jSmoothConfigFile << '  <skeletonProperties>' + "\r\n"
            jSmoothConfigFile << '    <key>Message</key>' + "\r\n"
            jSmoothConfigFile << '    <value>This program needs Java to run.' + "\r\n" + 'Please download it at http://www.java.com</value>' + "\r\n"
            jSmoothConfigFile << '  </skeletonProperties>' + "\r\n"

            jSmoothConfigFile << '  <skeletonProperties>' + "\r\n"
            jSmoothConfigFile << '    <key>PressKey</key>' + "\r\n"
            jSmoothConfigFile << '    <value>0</value>' + "\r\n"
            jSmoothConfigFile << '  </skeletonProperties>' + "\r\n"
            jSmoothConfigFile << '  <skeletonProperties>' + "\r\n"
            jSmoothConfigFile << '    <key>Debug</key>' + "\r\n"
            jSmoothConfigFile << '    <value>0</value>' + "\r\n"
            jSmoothConfigFile << '  </skeletonProperties>' + "\r\n"

            //            jSmoothConfigFile << '<skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<key>URL</key>' + "\r\n"
            //            jSmoothConfigFile << '<value>http://www.java.com</value>' + "\r\n"
            //            jSmoothConfigFile << '</skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<key>SingleProcess</key>' + "\r\n"
            //            jSmoothConfigFile << '<value>1</value>' + "\r\n"
            //            jSmoothConfigFile << '</skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<key>SingleInstance</key>' + "\r\n"
            //            jSmoothConfigFile << '<value>0</value>' + "\r\n"
            //            jSmoothConfigFile << '</skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<key>JniSmooth</key>' + "\r\n"
            //            jSmoothConfigFile << '<value>0</value>' + "\r\n"
            //            jSmoothConfigFile << '</skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<skeletonProperties>' + "\r\n"
            //            jSmoothConfigFile << '<key>Debug</key>' + "\r\n"
            //            jSmoothConfigFile << '<value>0</value>' + "\r\n"
            //            jSmoothConfigFile << '</skeletonProperties>' + "\r\n"

            jSmoothConfigFile << '</jsmoothproject>' + "\r\n"

            ant.taskdef(name:'jsmooth', classname:'net.charabia.jsmoothgen.ant.JSmoothGen', classpath:JSMOOTH_ANT)
            ant.jsmooth(project:jSmoothConfigFile.canonicalPath, skeletonroot:SKELETON_ROOT)
            ant.move(file:new File(tempDir, exeFile.name), todir:outputDir)
            
            new File(outputDir, 'open.cmd').text = '@cmd'
            new File(outputDir, 'start icred.cmd').text = '''@echo off
setLocal EnableDelayedExpansion
SET CLASSPATH=""
FOR /R ./''' + tempLibsDir.name + ''' %%a in (*.jar) DO CALL :AddToPath %%a

java -cp %CLASSPATH% ''' + buildProp.getProperty('mainClassName') + '''

:AddToPath
SET CLASSPATH=%1;%CLASSPATH%
GOTO :EOF'''

            ant.echo "build plugins"
            getArrayProperty(buildProp, 'includedPluginPath').each { String pluginPath ->
                File pluginDir = new File(pluginPath)

                PluginBuilder pBuilder = new PluginBuilder(pluginDir)
                pBuilder.ant = ant
                pBuilder.buildPlugin()

                ant.copy(file:pBuilder.jarFile, todir:pluginsDir, overwrite:'Yes')
            }

            ant.echo "build completed"
        } finally {
            //            ['explorer', tempDir].execute()
            //            System.in.read()
            ant.delete(dir:tempDir)
        }
    }

    public static void main(String[] args) {
        if(args.size() < 3) {
            println "missing args: path to base dir, export dir, version"
            System.exit(0)
        }

        new Builder(new File(args[0]), new File(args[1]), args[2]).build();
    }

    private Properties getBuildProp(File projectDir) {
        Properties properties

        File propFile = new File(projectDir, "build.prop");
        if (propFile != null) {
            properties = new Properties();
            BufferedInputStream stream = null;
            try {
                stream = new BufferedInputStream(new FileInputStream(propFile));
                properties.load(stream);
            } catch (IOException e) {
            } finally {
                try {
                    stream.close();
                } catch (Throwable t) {
                }
            }
        }

        return properties
    }

    private static List<String> getArrayProperty(Properties prop, String key) {
        List<String> result = new ArrayList<String>()
        long curId = 0

        String curKey = "${key}.${curId++}"
        String curValue = prop.getProperty(curKey)
        while(curValue != null) {
            result.add(curValue)

            curKey = "${key}.${curId++}"
            curValue = prop.getProperty(curKey)
        }

        return result
    }
}

