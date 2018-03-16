package eu.icred.build.plugin

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.codehaus.groovy.antlr.treewalker.TraversalHelper;

import eu.icred.plugin.PluginInstaller;

class PluginBuilder {
    AntBuilder ant = new AntBuilder()

    File projectDir
    File mainSrcDir
    File jarFile
    String pluginClass
    String pluginVersion
    String pluginId

    public PluginBuilder(File projectDir) {
        this.projectDir = projectDir
    }

    public buildPlugin() {

        File projectFile = new File(projectDir, '.project')
        if(!projectFile.exists()) {
            ant.echo "Error: '$projectDir' is no plugin/eclipse project"
            System.exit(0)
        }

        mainSrcDir = traversalFile(projectDir, 'src', 'main')
        if(!mainSrcDir.exists()) {
            ant.echo "Error: '$mainSrcDir' does not exists"
            System.exit(0)
        }

        Properties buildProp = getBuildProp(projectDir)
        if(buildProp.isEmpty()) {
            ant.echo "Error: missing build.prop"
            System.exit(0)
        }
        pluginClass = buildProp.getProperty('PluginClass');
        pluginVersion = getInfoByPluginClass('getPluginVersion')
        pluginId = getInfoByPluginClass('getPluginId')

        //        def GROOVY_HOME = new File(System.getenv('GROOVY_HOME'))
        //        if (!GROOVY_HOME.canRead()) {
        //            ant.echo( "Missing environment variable GROOVY_HOME" )
        //            System.exit(0)
        //        }

        File baseSrcDir = traversalFile(new File(buildProp.getProperty('baseCoreProjectPath').trim()), 'src', 'main')
        File installerClassSrc = traversalFile(baseSrcDir, *PluginInstaller.class.package.name.split("[.]"), PluginInstaller.class.simpleName + '.java')
        File installerClassTemp = traversalFile(mainSrcDir, *PluginInstaller.class.package.name.split("[.]"), PluginInstaller.class.simpleName +  '.java')
        ant.copy(file:installerClassSrc, todir:traversalFile(mainSrcDir, *PluginInstaller.class.package.name.split("[.]")), overwrite:'Yes')

        jarFile = new File(projectDir, "${pluginId}-${pluginVersion}.jar")
        ant.echo "build $jarFile"

        File tempDir = Files.createTempDirectory("${pluginId}-${pluginVersion}-"+Long.toString(System.nanoTime())).toFile()
        File binDir = new File(tempDir, 'bin')
        File libsDir = new File(projectDir, "libs")
        ant.mkdir(dir:binDir)

        try {
            ant.javac(srcdir:mainSrcDir, destdir:binDir, source:"1.7", target:"1.7", debug:"on", fork:'yes') {
                classpath {
                    fileset(dir:libsDir, includes: "*.jar")

                    pathelement(path:new File(buildProp.getProperty('baseCoreProjectPath').trim(), 'bin').canonicalPath)
                    pathelement(path:new File(buildProp.getProperty('baseModelProjectPath').trim(), 'bin').canonicalPath)
                    pathelement(path:new File(buildProp.getProperty('baseUiProjectPath').trim(), 'bin').canonicalPath)
                    pathelement(path:binDir.canonicalPath)
                }
            }

            ant.delete(file: installerClassTemp)

            
            File libsBinDir = new File(tempDir, 'libsBin')
            libsDir.eachFile { File libFile ->
                ant.unzip(src:libFile, dest: libsBinDir, overwrite:true)
            }
            
            ant.jar( destfile: jarFile, compress: true ) {
                fileset( dir: binDir, includes: '**' )
                fileset( dir: libsBinDir, includes: '**')

                manifest {
//                    attribute(name:'Main-Class', value:PluginInstaller.class.name)
                    attribute( name: 'PluginClass', value: pluginClass )
                }
            }

            ant.echo "build completed"
        } finally {
            ant.delete(dir:tempDir)
        }
    }




    private String getInfoByPluginClass(String methodName) {
        String[] pluginClassPath = pluginClass.split(/\./)
        File pluginClassFile = new File(traversalFile(mainSrcDir, *pluginClassPath[0..-2]), pluginClassPath[-1]+'.java')

        String info
        boolean isInVersionMethod = false
        pluginClassFile.eachLine { String line ->
            if(isInVersionMethod && line.contains("return")) {
                info = line[(line.indexOf('"')+1)..(line.lastIndexOf('"')-1)]
            }

            if(line.contains('public String '+methodName+'() {')) {
                isInVersionMethod = true
            }
            if(line.contains('}')) {
                isInVersionMethod = false
            }
        }
        return info
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

    static main(args) {
        if(args.size() == 0) {
            println "missing arg: path to project dir"
            System.exit(0)
        }

        args.each { new PluginBuilder(new File(it)).buildPlugin() }
    }

    private static File traversalFile(File base, String... nodes) {
        File curFile = base;
        for (String curNode : nodes) {
            if (curNode.equals("..")) {
                curFile = curFile.getParentFile();
            } else if (curNode.equals(".")) {
                // nothing to do
            } else {
                curFile = new File(curFile, curNode);
            }
        }
        return curFile;
    }
}
