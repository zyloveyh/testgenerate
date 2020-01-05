package com.zy;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.COMPILE)
public class GenerateMojo extends AbstractMojo {
    private Log log = getLog();
    /**
     * 需要生成测试的类,逗号分隔
     */
    @Parameter(property = "cuts")
    private String cuts;

    /**
     * 需要生成测试的文件夹
     */
    @Parameter(property = "floder")
    private String floder;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin.artifacts}", required = true, readonly = true)
    private List<Artifact> artifacts;

    @Component
    private ProjectBuilder projectBuilder;

    @Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
    private RepositorySystemSession repoSession;

    private Process process;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
//       String NEWLINE =  java.lang.System.getProperty("line.separator");

        List<String> testCuts = null;
        if (StringUtils.isNotEmpty(cuts)) {
            testCuts = Arrays.asList(cuts.split(","));
        }
        if (StringUtils.isNotEmpty(floder)) {
            File file = new File(floder);
            if (file.exists()) {
                // TODO 将文件下的所有类都加入到  testCuts 集合中
            }
        }
        if (CollectionUtils.isEmpty(testCuts)) {
            log.warn("test Class is null.");
            return;
        }

        //目标:需要生成测试文件的 类,例如:cuts
        String target = null;
        String cp = null;

        try {
            for (String element : project.getCompileClasspathElements()) {
                if (element.endsWith(".jar")) {  // we only target what has been compiled to a folder
                    continue;
                }
                File file = new File(element);
                if (!file.exists()) {
                    //不存在,丢弃
                    continue;
                }

                if (!file.getAbsolutePath().startsWith(project.getBasedir().getAbsolutePath())) {
					/*
						This can happen in multi-module projects when module A has dependency on
						module B. Then, both A and B source folders will end up on compile classpath,
						although we are interested only in A
					 */
                    continue;
                }

                if (target == null) {
                    target = element;
                } else {
                    target = target + File.pathSeparator + element;
                }
            }

            //build the classpath
            Set<String> alreadyAdded = new HashSet<>();
            for (String element : project.getTestClasspathElements()) {
                if (element.toLowerCase().contains("powermock")) {
                    //PowerMock just leads to a lot of troubles, as it includes tools.jar code
                    getLog().warn("Skipping PowerMock dependency at: " + element);
                    continue;
                }
                if (element.toLowerCase().contains("jmockit")) {
                    //JMockit has same issue
                    getLog().warn("Skipping JMockit dependency at: " + element);
                    continue;
                }
                getLog().debug("TEST ELEMENT: " + element);
                cp = addPathIfExists(cp, element, alreadyAdded);
            }
        } catch (DependencyResolutionRequiredException e) {
            getLog().error("Error: " + e.getMessage(), e);
            return;
        }


        String entryPoint = GenerateTest.class.getName();

        List<String> cmd = new ArrayList<>();
        cmd.add(JavaExecCmdUtil.getJavaBinExecutablePath()/*"java"*/);
        cmd.add("-cp");
        cmd.add(cp);
        cmd.add(entryPoint);
        cmd.add("-target");
        cmd.add(target);

        System.out.println(cmd);
        String baseDir = System.getProperty("user.dir");
        File dir = new File(baseDir);
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.directory(dir);
        builder.redirectErrorStream(true);

        try {
            process = builder.start();
            int exitCode = process.waitFor();

        }  catch (IOException e) {
            return ;
        } catch (InterruptedException e) {
            if(process!=null){
                try {
                    //be sure streamers are closed, otherwise process might hang on Windows
                    process.getOutputStream().close();
                    process.getInputStream().close();
                    process.getErrorStream().close();
                } catch (Exception t){
                }
                process.destroy();
            }
            return;
        }
        process = null;
















/*

        //开始分析类
        String s = testCuts.get(0);
        try {
            analyzeClass(Class.forName(s));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log.warn(s + " Class can not be found");
            return;
        }
        //开始生成测试文件字符串

        //开始写入指定位置
*/


    }

    private void analyzeClass(Class clazz) {
        Method[] methods = clazz.getMethods();
        List<Method> methodList = Arrays.asList(methods);
        for (Method method : methodList) {
            List<java.lang.reflect.Parameter> parameters = Arrays.asList(method.getParameters());
            for (java.lang.reflect.Parameter parameter : parameters) {
                Annotation[] annotations = parameter.getAnnotations();
            }

        }
    }

    private String addPathIfExists(String cp, String element, Set<String> alreadyExist) {
        File file = new File(element);
        if (!file.exists()) {
            /*
             * don't add to CP an element that does not exist
             */
            return cp;
        }

        if (alreadyExist.contains(element)) {
            return cp;
        }

        alreadyExist.add(element);

        if (cp == null) {
            cp = element;
        } else {
            cp = cp + File.pathSeparator + element;
        }
        return cp;
    }
}
