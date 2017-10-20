package org.jenkinsci.plugins.vstest_runner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.annotation.PostConstruct;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.EnvironmentContributingAction;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 *
 * @author Yasuyuki Saito
 */
public class VsTestBuilder extends Builder {

    /**
     * Platform:x86
     */
    private static final String PLATFORM_X86 = "x86";

    /**
     * Platform:x64
     */
    private static final String PLATFORM_X64 = "x64";

    /**
     * Platform:ARM
     */
    private static final String PLATFORM_ARM = "ARM";

    /**
     * Platform:Other
     */
    private static final String PLATFORM_OTHER = "Other";

    /**
     * .NET Framework 3.5
     */
    private static final String FRAMEWORK_35 = "framework35";

    /**
     * .NET Framework 4.0
     */
    private static final String FRAMEWORK_40 = "framework40";

    /**
     * .NET Framework 4.5
     */
    private static final String FRAMEWORK_45 = "framework45";

    /**
     * .NET Framework Other
     */
    private static final String FRAMEWORK_OTHER = "Other";

    /**
     * Logger TRX
     */
    private static final String LOGGER_TRX = "trx";

    /**
     * Logger Other
     */
    private static final String LOGGER_OTHER = "Other";

    String vsTestName, testFiles, settings, tests, testCaseFilter, platform, otherPlatform,
           framework, otherFramework, logger, otherLogger, cmdLineArgs;
    boolean enablecodecoverage, inIsolation, useVsixExtensions, useVs2017Plus, failBuild;

    @PostConstruct
    public void SetOthers() {
    	//can't add these lines into the setters.
    	//order isn't guaranteed AFAIK and logic may change
    	otherPlatform = PLATFORM_OTHER.equals(platform) ? otherPlatform : "";
    	otherFramework = FRAMEWORK_OTHER.equals(framework) ? otherFramework : "";
    	otherLogger = LOGGER_OTHER.equals(logger) ? otherLogger : "";
    }
    
    @DataBoundConstructor
    public VsTestBuilder() {
        
    }

    public String getVsTestName() {
        return vsTestName;
    }

    public String getTestFiles() {
        return testFiles;
    }

    public String getSettings() {
        return settings;
    }

    public String getTests() {
        return tests;
    }

    public boolean isEnablecodecoverage() {
        return enablecodecoverage;
    }

    public boolean isInIsolation() {
        return inIsolation;
    }

    public boolean isUseVsixExtensions() {
        return useVsixExtensions;
    }

    public boolean isUseVs2017Plus() {
    	return useVs2017Plus;
    }
    
    public String getPlatform() {
        return platform;
    }

    public String getOtherPlatform() {
        return otherPlatform;
    }

    public String getFramework() {
        return framework;
    }

    public String getOtherFramework() {
        return otherFramework;
    }

    public String getTestCaseFilter() {
        return testCaseFilter;
    }

    public String getLogger() {
        return logger;
    }

    public String getOtherLogger() {
        return otherLogger;
    }

    public String getCmdLineArgs() {
        return cmdLineArgs;
    }

    public boolean isFailBuild() {
        return failBuild;
    }

    @DataBoundSetter
    public void setVsTestName(String vsTestName)
    {
    	this.vsTestName = vsTestName;
     }       
    @DataBoundSetter
    public void setTestFiles(String testFiles)
    {
    	this.testFiles = testFiles;
     }       
    @DataBoundSetter
    public void setSettings(String settings)
    {
           this.settings = settings;
    }       
    @DataBoundSetter
    public void setTests(String tests)
    {
            this.tests = tests;
    }       
    @DataBoundSetter
    public void setTestCaseFilter(String testCaseFilter)
    {
            this.testCaseFilter = testCaseFilter;
    }
    @DataBoundSetter
    public void setOtherPlatform(String otherPlatform)
    {
            this.otherPlatform = otherPlatform;
    }       
    @DataBoundSetter
    public void setOtherFramework(String otherFramework)
    {       
            this.otherFramework = otherFramework;
     }       
    @DataBoundSetter
    public void setOtherLogger(String otherLogger)
    {      
            this.otherLogger = otherLogger;
     } 
    @DataBoundSetter
    public void setPlatform(String platform)
    {
            this.platform = platform;
    }       
    @DataBoundSetter
    public void setFramework(String framework)
    {       
            this.framework = framework;
     }       
    @DataBoundSetter
    public void setLogger(String logger)
    {      
            this.logger = logger;
     }       
    @DataBoundSetter
    public void setCmdLineArgs(String cmdLineArgs)
    {      
            this.cmdLineArgs = cmdLineArgs;
    }       
    @DataBoundSetter
    public void setEnablecodecoverage(boolean enablecodecoverage)
    {
            this.enablecodecoverage = enablecodecoverage;
    }       
    @DataBoundSetter
    public void setInIsolation(boolean inIsolation)
    {
            this.inIsolation = inIsolation;
    }       
    @DataBoundSetter
    public void setUseVsixExtensions(boolean useVsixExtensions)
    {
            this.useVsixExtensions = useVsixExtensions;
    }       
    @DataBoundSetter
    public void setUseVs2017Plus(boolean useVs2017Plus)
    {
            this.useVs2017Plus = useVs2017Plus;
    }
    @DataBoundSetter
    public void setFailBuild(boolean failBuild)
    {
            this.failBuild = failBuild;
    }       

    
    public VsTestInstallation getVsTest() {
        for (VsTestInstallation i : DESCRIPTOR.getInstallations()) {
            if (vsTestName != null && i.getName().equals(vsTestName)) {
                return i;
            }
        }
        return null;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     *
     * @author Yasuyuki Saito
     */
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @CopyOnWrite
        private volatile VsTestInstallation[] installations = new VsTestInstallation[0];

        DescriptorImpl() {
            super(VsTestBuilder.class);
            load();
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return Messages.VsTestBuilder_DisplayName();
        }

        public VsTestInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(VsTestInstallation... antInstallations) {
            this.installations = antInstallations;
            save();
        }

        /**
         * Obtains the {@link VsTestInstallation.DescriptorImpl} instance.
         */
        public VsTestInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(VsTestInstallation.DescriptorImpl.class);
        }
    }

    /**
     *
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ArrayList<String> args = new ArrayList<String>();

        EnvVars env = build.getEnvironment(listener);

        // VsTest.console.exe path.
        String pathToVsTest = getVsTestPath(launcher, listener, env);
        if (pathToVsTest == null) {
            return false;
        }
        args.add(pathToVsTest);

        // Target dll path
        if (!StringUtils.isBlank(testFiles)) {
            List<String> targets = getTestFilesArguments(build, env);
            if (targets.size() == 0) {
                listener.getLogger().print("no file matches the pattern " + this.testFiles);
                return !this.failBuild;
            }
            args.addAll(targets);
        }

        // Run tests with additional settings such as data collectors.
        if (!StringUtils.isBlank(settings)) {
            args.add(convertArgumentWithQuote("Settings", replaceMacro(settings, env, build)));
        }

        // Run tests with names that match the provided values.
        if (!StringUtils.isBlank(tests)) {
            args.add(convertArgument("Tests", replaceMacro(tests, env, build)));
        }

        // Run tests that match the given expression.
        if (!StringUtils.isBlank(testCaseFilter)) {
            args.add(convertArgumentWithQuote("TestCaseFilter", replaceMacro(testCaseFilter, env, build)));
        }

        // Enables data diagnostic adapter CodeCoverage in the test run.
        if (enablecodecoverage) {
            args.add("/Enablecodecoverage");
        }

        // Runs the tests in an isolated process.
        if (inIsolation) {
            args.add("/InIsolation");
        }

        // This makes vstest.console.exe process use or skip the VSIX extensions installed (if any) in the test run.
        if(!useVs2017Plus)
        {
	        if (useVsixExtensions) {
	            args.add("/UseVsixExtensions:true");
	        } else {
	            args.add("/UseVsixExtensions:false");
	        }
        }

        // Target platform architecture to be used for test execution.
        String platformArg = getPlatformArgument(env, build);
        if (!StringUtils.isBlank(platformArg)) {
            args.add(convertArgument("Platform", platformArg));
        }

        // Target .NET Framework version to be used for test execution.
        String frameworkArg = getFrameworkArgument(env, build);
        if (!StringUtils.isBlank(frameworkArg)) {
            args.add(convertArgument("Framework", frameworkArg));
        }

        // Specify a logger for test results.
        String loggerArg = getLoggerArgument(env, build);
        if (!StringUtils.isBlank(loggerArg)) {
            args.add(convertArgument("Logger", loggerArg));
        }

        // Manual Command Line String
        if (!StringUtils.isBlank(cmdLineArgs)) {
            args.add(replaceMacro(cmdLineArgs, env, build));
        }

        // VSTest run.
        boolean r = execVsTest(args, build, launcher, listener, env);

        return r;
    }

    /**
     *
     * @param value
     * @param env
     * @param build
     * @return
     */
    private String replaceMacro(String value, EnvVars env, AbstractBuild<?, ?> build) {
        String result = Util.replaceMacro(value, env);
        result = Util.replaceMacro(result, build.getBuildVariables());
        return result;
    }

    /**
     *
     * @param launcher
     * @param listener
     * @param env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private String getVsTestPath(Launcher launcher, BuildListener listener, EnvVars env) throws InterruptedException, IOException {

        String execName = "vstest.console.exe";

        VsTestInstallation installation = getVsTest();
        if (installation == null) {
            listener.getLogger().println("Path To VSTest.console.exe: " + execName);
            return execName;
        } else {
            installation = installation.forNode(Computer.currentComputer().getNode(), listener);
            installation = installation.forEnvironment(env);
            String pathToVsTest = installation.getHome();
            FilePath exec = new FilePath(launcher.getChannel(), pathToVsTest);

            try {
                if (!exec.exists()) {
                    listener.fatalError(pathToVsTest + " doesn't exist");
                    return null;
                }
            } catch (IOException e) {
                listener.fatalError("Failed checking for existence of " + pathToVsTest);
                return null;
            }

            listener.getLogger().println("Path To VSTest.console.exe: " + pathToVsTest);
            return appendQuote(pathToVsTest);
        }
    }

    /**
     *
     * @param build
     * @param env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private List<String> getTestFilesArguments(AbstractBuild<?, ?> build, EnvVars env) throws InterruptedException, IOException {
        ArrayList<String> args = new ArrayList<String>();

        StringTokenizer testFilesToknzr = new StringTokenizer(testFiles, " \t\r\n");

        while (testFilesToknzr.hasMoreTokens()) {
            String testFile = testFilesToknzr.nextToken();
            testFile = replaceMacro(testFile, env, build);

            if (!StringUtils.isBlank(testFile)) {

                for (String file : expandFileSet(build, testFile)) {
                    args.add(appendQuote(file));
                }
            }
        }

        return args;
    }

    private String[] expandFileSet(AbstractBuild<?, ?> build, String pattern) {
        List<String> fileNames = new ArrayList<String>();
        try {
        for (FilePath x: build.getWorkspace().list(pattern))
            fileNames.add(x.getRemote());
        } catch (IOException ioe) {}
        catch (InterruptedException inte) {}
        return fileNames.toArray(new String[fileNames.size()]);
    }

    /**
     *
     * @param env
     * @param build
     * @return
     */
    private String getPlatformArgument(EnvVars env, AbstractBuild<?, ?> build) {
        if (PLATFORM_X86.equals(platform) || PLATFORM_X64.equals(platform) || PLATFORM_ARM.equals(platform)) {
            return platform;
        } else if (PLATFORM_OTHER.equals(platform)) {
            return replaceMacro(otherPlatform, env, build);
        } else {
            return null;
        }
    }

    /**
     *
     * @param env
     * @param build
     * @return
     */
    private String getFrameworkArgument(EnvVars env, AbstractBuild<?, ?> build) {
        if (FRAMEWORK_35.equals(framework) || FRAMEWORK_40.equals(framework) || FRAMEWORK_45.equals(framework)) {
            if(!useVs2017Plus){
            	return framework;
            }
            else {
            	char[] chars = framework.toCharArray();
            	chars[0] = Character.toUpperCase(chars[0]);
            	return new String(chars);
            }
            	
        } else if (FRAMEWORK_OTHER.equals(framework)) {
            return replaceMacro(otherFramework, env, build);
        } else {
            return null;
        }
    }

    /**
     *
     * @param env
     * @param build
     * @return
     */
    private String getLoggerArgument(EnvVars env, AbstractBuild<?, ?> build) {
        if (LOGGER_TRX.equals(logger)) {
            return logger;
        } else if (LOGGER_OTHER.equals(logger)) {
            return replaceMacro(otherLogger, env, build);
        } else {
            return null;
        }
    }

    /**
     * @param base
     * @param path
     * @return the relative path of 'path'
     * @throws InterruptedException
     * @throws IOException
     */
    private String relativize(FilePath base, String path) throws InterruptedException, IOException {
        return base.toURI().relativize(new java.io.File(path).toURI()).getPath();
    }

    /**
     *
     * @param args
     * @param build
     * @param launcher
     * @param listener
     * @param env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private boolean execVsTest(List<String> args, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, EnvVars env) throws InterruptedException, IOException {
        ArgumentListBuilder cmdExecArgs = new ArgumentListBuilder();
        FilePath tmpDir = null;
        FilePath pwd = build.getWorkspace();

        if (!launcher.isUnix()) {
            tmpDir = pwd.createTextTempFile("vstest", ".bat", concatString(args), false);
            cmdExecArgs.add("cmd.exe", "/C", tmpDir.getRemote(), "&&", "exit", "%ERRORLEVEL%");
        } else {
            for (String arg : args) {
                cmdExecArgs.add(arg);
            }
        }

        listener.getLogger().println("Executing VSTest: " + cmdExecArgs.toStringWithQuote());

        try {
            VsTestListenerDecorator parserListener = new VsTestListenerDecorator(listener);
            int r = launcher.launch().cmds(cmdExecArgs).envs(env).stdout(parserListener).pwd(pwd).join();

            String trxFullPath = parserListener.getTrxFile();
            String trxPathRelativeToWorkspace = null;
            String coverageFullPath = parserListener.getCoverageFile();
            String coveragePathRelativeToWorkspace = null;

            if (trxFullPath != null) {
                trxPathRelativeToWorkspace = relativize(build.getWorkspace(), trxFullPath);
            }
            if (coverageFullPath != null) {
                coveragePathRelativeToWorkspace = relativize(build.getWorkspace(), parserListener.getCoverageFile());
            }

            build.addAction(new AddVsTestEnvVarsAction(trxPathRelativeToWorkspace, coveragePathRelativeToWorkspace));

            if (failBuild) {
                return (r == 0);
            } else {
                if (r != 0) {
                    build.setResult(Result.UNSTABLE);
                }
                return true;
            }
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("VSTest command execution failed"));
            return false;
        } finally {
            try {
                if (tmpDir != null) {
                    tmpDir.delete();
                }
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("temporary file delete failed"));
            }
        }
    }

    /**
     *
     * @param option
     * @param param
     * @return
     */
    private String convertArgument(String option, String param) {
        return String.format("/%s:%s", option, param);
    }

    /**
     *
     * @param option
     * @param param
     * @return
     */
    private String convertArgumentWithQuote(String option, String param) {
        return String.format("/%s:\"%s\"", option, param);
    }

    /**
     *
     * @param value
     * @return
     */
    private String appendQuote(String value) {
        return String.format("\"%s\"", value);
    }

    /**
     *
     * @param args
     * @return
     */
    private String concatString(List<String> args) {
        StringBuilder buf = new StringBuilder();
        for (String arg : args) {
            if (buf.length() > 0) {
                buf.append(' ');
            }
            buf.append(arg);
        }
        return buf.toString();
    }

    private static class AddVsTestEnvVarsAction implements EnvironmentContributingAction {

        private final static String TRX_ENV = "VSTEST_RESULT_TRX";
        private final static String COVERAGE_ENV = "VSTEST_RESULT_COVERAGE";

        private final String trxEnv;
        private final String coverageEnv;

        public AddVsTestEnvVarsAction(String trxEnv, String coverageEnv) {
            this.trxEnv = trxEnv;
            this.coverageEnv = coverageEnv;
        }

        public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
            if (trxEnv != null) {
                env.put(TRX_ENV, trxEnv);
            }

            if (coverageEnv != null) {
                env.put(COVERAGE_ENV, coverageEnv);
            }
        }

        public String getDisplayName() {
            return "Add VSTestRunner Environment Variables to Build Environment";
        }

        public String getIconFileName() {
            return null;
        }

        public String getUrlName() {
            return null;
        }
    }
}
