package org.jenkinsci.plugins.vstest_runner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

/**
 *
 * @author Yasuyuki Saito
 */
public class VsTestBuilder extends Builder {

    /** Platform:x86 */
    private static final String PLATFORM_X86 = "x86";

    /** Platform:x64 */
    private static final String PLATFORM_X64 = "x64";

    /** Platform:ARM */
    private static final String PLATFORM_ARM = "ARM";

    /** Platform:Other */
    private static final String PLATFORM_OTHER = "Other";

    /** .NET Framework 3.5 */
    private static final String FRAMEWORK_35 = "framework35";

    /** .NET Framework 4.0 */
    private static final String FRAMEWORK_40 = "framework40";

    /** .NET Framework 4.5 */
    private static final String FRAMEWORK_45 = "framework45";

    /** .NET Framework Other */
    private static final String FRAMEWORK_OTHER = "Other";

    /** Logger TRX */
    private static final String LOGGER_TRX = "trx";

    /** Logger Other */
    private static final String LOGGER_OTHER = "Other";


    private final String vsTestName;
    private final String testFiles;
    private final String settings;
    private final String tests;
    private final String testCaseFilter;
    private final boolean enablecodecoverage;
    private final boolean inIsolation;
    private final boolean useVsixExtensions;
    private final String platform;
    private final String otherPlatform;
    private final String framework;
    private final String otherFramework;
    private final String logger;
    private final String otherLogger;
    private final String cmdLineArgs;
    private final boolean failBuild;
    private final boolean doNotUseChcpCommand;

    /**
     *
     * @param vsTestName
     * @param testFiles
     * @param settings
     * @param tests
     * @param testCaseFilter
     * @param enablecodecoverage
     * @param inIsolation
     * @param useVsixExtensions
     * @param platform
     * @param otherPlatform
     * @param framework
     * @param otherFramework
     * @param logger
     * @param otherLogger
     * @param cmdLineArgs
     * @param failBuild
     */
    @DataBoundConstructor
    public VsTestBuilder(String vsTestName, String testFiles, String settings, String tests, String testCaseFilter
                        ,boolean enablecodecoverage, boolean inIsolation, boolean useVsixExtensions, String platform, String otherPlatform
                        ,String framework, String otherFramework, String logger, String otherLogger
                        ,String cmdLineArgs, boolean failBuild, boolean doNotUseChcpCommand) {
        this.vsTestName         = vsTestName;
        this.testFiles          = testFiles;
        this.settings           = settings;
        this.tests              = tests;
        this.testCaseFilter     = testCaseFilter;
        this.enablecodecoverage = enablecodecoverage;
        this.inIsolation        = inIsolation;
        this.useVsixExtensions  = useVsixExtensions;
        this.platform           = platform;
        this.otherPlatform      = PLATFORM_OTHER.equals(this.platform) ? otherPlatform : "";
        this.framework          = framework;
        this.otherFramework     = FRAMEWORK_OTHER.equals(this.framework) ? otherFramework : "";
        this.logger             = logger;
        this.otherLogger        = LOGGER_OTHER.equals(this.logger) ? otherLogger : "";
        this.cmdLineArgs        = cmdLineArgs;
        this.failBuild          = failBuild;
        this.doNotUseChcpCommand= doNotUseChcpCommand;
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

    @SuppressWarnings("unused")
    public boolean getDoNotUseChcpCommand() {
        return doNotUseChcpCommand;
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
        if (pathToVsTest == null) return false;
        args.add(pathToVsTest);

        // Tareget dll path
        if (!StringUtils.isBlank(testFiles))
            args.addAll(getTestFilesArguments(build, env));

        // Run tests with additional settings such as data collectors.
        if (!StringUtils.isBlank(settings))
            args.add(convertArgumentWithQuote("Settings", replaceMacro(settings, env, build)));

        // Run tests with names that match the provided values.
        if (!StringUtils.isBlank(tests))
            args.add(convertArgument("Tests", replaceMacro(tests, env, build)));

        // Run tests that match the given expression.
        if (!StringUtils.isBlank(testCaseFilter))
            args.add(convertArgumentWithQuote("TestCaseFilter", replaceMacro(testCaseFilter, env, build)));

        // Enables data diagnostic adapter CodeCoverage in the test run.
        if (enablecodecoverage)
            args.add("/Enablecodecoverage");

        // Runs the tests in an isolated process.
        if (inIsolation)
            args.add("/InIsolation");

        // This makes vstest.console.exe process use or skip the VSIX extensions installed (if any) in the test run.
        if (useVsixExtensions)
            args.add("/UseVsixExtensions:true");
        else
            args.add("/UseVsixExtensions:false");

        // Target platform architecture to be used for test execution.
        String platformArg = getPlatformArgument(env, build);
        if (!StringUtils.isBlank(platformArg))
            args.add(convertArgument("Platform", platformArg));

        // Target .NET Framework version to be used for test execution.
        String frameworkArg = getFrameworkArgument(env, build);
        if (!StringUtils.isBlank(frameworkArg))
            args.add(convertArgument("Framework", frameworkArg));

        // Specify a logger for test results.
        String loggerArg = getLoggerArgument(env, build);
        if (!StringUtils.isBlank(loggerArg))
            args.add(convertArgument("Logger", loggerArg));

        // Manual Command Line String
        if (!StringUtils.isBlank(cmdLineArgs))
            args.add(replaceMacro(cmdLineArgs, env, build));

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
                args.add(appendQuote(testFile));
            }
        }

        return args;
    }

    /**
     *
     * @param env
     * @param build
     * @return
     */
    private String getPlatformArgument(EnvVars env, AbstractBuild<?, ?> build) {
        if (PLATFORM_X86.equals(platform) || PLATFORM_X64.equals(platform) || PLATFORM_ARM.equals(platform))
            return platform;
        else if (PLATFORM_OTHER.equals(platform))
            return replaceMacro(otherPlatform, env, build);
        else
            return null;
    }

    /**
     *
     * @param env
     * @param build
     * @return
     */
    private String getFrameworkArgument(EnvVars env, AbstractBuild<?, ?> build) {
        if (FRAMEWORK_35.equals(framework) || FRAMEWORK_40.equals(framework) || FRAMEWORK_45.equals(framework))
            return framework;
        else if (FRAMEWORK_OTHER.equals(framework))
            return replaceMacro(otherFramework, env, build);
        else
            return null;
    }

    /**
     *
     * @param env
     * @param build
     * @return
     */
    private String getLoggerArgument(EnvVars env, AbstractBuild<?, ?> build) {
        if (LOGGER_TRX.equals(logger))
            return logger;
        else if (LOGGER_OTHER.equals(logger))
            return replaceMacro(otherLogger, env, build);
        else
            return null;
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
            final int cpi = doNotUseChcpCommand ? 0 : getCodePageIdentifier(build.getCharset());
            if (cpi != 0) {
                args.add(0, "&");
                args.add(0, String.valueOf(cpi));
                args.add(0, "chcp");
            }
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

            build.addAction(new AddVsTestEnvVarsAction(parserListener.getTrxFile(), parserListener.getCoverageFile()));

            if (failBuild)
                return (r == 0);
            else {
                if (r != 0)
                    build.setResult(Result.UNSTABLE);
                return true;
            }
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("VSTest command execution failed"));
            return false;
        } finally {
            try {
                if (tmpDir != null) tmpDir.delete();
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
            if(buf.length() > 0)  buf.append(' ');
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
            if (trxEnv != null)
            {
                env.put(TRX_ENV, trxEnv);
            }

            if (coverageEnv != null)
            {
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

    private static int getCodePageIdentifier(Charset charset) {
        final String s_charset = charset.name();
        if(s_charset.equalsIgnoreCase("utf-8"))             // Unicode
            return 65001;
        else if(s_charset.equalsIgnoreCase("ibm437"))       // US
            return 437;
        else if(s_charset.equalsIgnoreCase("ibm850"))       // OEM Multilingual Latin 1
            return 850;
        else if(s_charset.equalsIgnoreCase("ibm852"))       // OEM Latin2
            return 852;
        else if(s_charset.equalsIgnoreCase("shift_jis") || s_charset.equalsIgnoreCase("windows-31j"))//Japanese
            return 932;
        else if(s_charset.equalsIgnoreCase("us-ascii"))     // US-ASCII
            return 20127;
        else if(s_charset.equalsIgnoreCase("euc-jp"))       // Japanese
            return 20932;
        else if(s_charset.equalsIgnoreCase("iso-8859-1"))   // Latin 1
            return 28591;
        else if(s_charset.equalsIgnoreCase("iso-8859-2"))   // Latin 2
            return 28592;
        else if(s_charset.equalsIgnoreCase("IBM00858"))
            return 858;
        else if(s_charset.equalsIgnoreCase("IBM775"))
            return 775;
        else if(s_charset.equalsIgnoreCase("IBM855"))
            return 855;
        else if(s_charset.equalsIgnoreCase("IBM857"))
            return 857;
        else if(s_charset.equalsIgnoreCase("ISO-8859-4"))
            return 28594;
        else if(s_charset.equalsIgnoreCase("ISO-8859-5"))
            return 28595;
        else if(s_charset.equalsIgnoreCase("ISO-8859-7"))
            return 28597;
        else if(s_charset.equalsIgnoreCase("ISO-8859-9"))
            return 28599;
        else if(s_charset.equalsIgnoreCase("ISO-8859-13"))
            return 28603;
        else if(s_charset.equalsIgnoreCase("ISO-8859-15"))
            return 28605;
        else if(s_charset.equalsIgnoreCase("KOI8-R"))
            return 20866;
        else if(s_charset.equalsIgnoreCase("KOI8-U"))
            return 21866;
        else if(s_charset.equalsIgnoreCase("UTF-16"))
            return 1200;
        else if(s_charset.equalsIgnoreCase("UTF-32"))
            return 12000;
        else if(s_charset.equalsIgnoreCase("UTF-32BE"))
            return 12001;
        else if(s_charset.equalsIgnoreCase("windows-1250"))
            return 1250;
        else if(s_charset.equalsIgnoreCase("windows-1251"))
            return 1251;
        else if(s_charset.equalsIgnoreCase("windows-1252"))
            return 1252;
        else if(s_charset.equalsIgnoreCase("windows-1253"))
            return 1253;
        else if(s_charset.equalsIgnoreCase("windows-1254"))
            return 1254;
        else if(s_charset.equalsIgnoreCase("windows-1257"))
            return 1257;
        else if(s_charset.equalsIgnoreCase("Big5"))
            return 950;
        else if(s_charset.equalsIgnoreCase("EUC-KR"))
            return 51949;
        else if(s_charset.equalsIgnoreCase("GB18030"))
            return 54936;
        else if(s_charset.equalsIgnoreCase("GB2312"))
            return 936;
        else if(s_charset.equalsIgnoreCase("IBM-Thai"))
            return 20838;
        else if(s_charset.equalsIgnoreCase("IBM01140"))
            return 1140;
        else if(s_charset.equalsIgnoreCase("IBM01141"))
            return 1141;
        else if(s_charset.equalsIgnoreCase("IBM01142"))
            return 1142;
        else if(s_charset.equalsIgnoreCase("IBM01143"))
            return 1143;
        else if(s_charset.equalsIgnoreCase("IBM01144"))
            return 1144;
        else if(s_charset.equalsIgnoreCase("IBM01145"))
            return 1145;
        else if(s_charset.equalsIgnoreCase("IBM01146"))
            return 1146;
        else if(s_charset.equalsIgnoreCase("IBM01147"))
            return 1147;
        else if(s_charset.equalsIgnoreCase("IBM01148"))
            return 1148;
        else if(s_charset.equalsIgnoreCase("IBM01149"))
            return 1149;
        else if(s_charset.equalsIgnoreCase("IBM037"))
            return 37;
        else if(s_charset.equalsIgnoreCase("IBM1026"))
            return 1026;
        else if(s_charset.equalsIgnoreCase("IBM273"))
            return 20273;
        else if(s_charset.equalsIgnoreCase("IBM277"))
            return 20277;
        else if(s_charset.equalsIgnoreCase("IBM278"))
            return 20278;
        else if(s_charset.equalsIgnoreCase("IBM280"))
            return 20280;
        else if(s_charset.equalsIgnoreCase("IBM284"))
            return 20284;
        else if(s_charset.equalsIgnoreCase("IBM285"))
            return 20285;
        else if(s_charset.equalsIgnoreCase("IBM297"))
            return 20297;
        else if(s_charset.equalsIgnoreCase("IBM420"))
            return 20420;
        else if(s_charset.equalsIgnoreCase("IBM424"))
            return 20424;
        else if(s_charset.equalsIgnoreCase("IBM500"))
            return 500;
        else if(s_charset.equalsIgnoreCase("IBM860"))
            return 860;
        else if(s_charset.equalsIgnoreCase("IBM861"))
            return 861;
        else if(s_charset.equalsIgnoreCase("IBM863"))
            return 863;
        else if(s_charset.equalsIgnoreCase("IBM864"))
            return 864;
        else if(s_charset.equalsIgnoreCase("IBM865"))
            return 865;
        else if(s_charset.equalsIgnoreCase("IBM869"))
            return 869;
        else if(s_charset.equalsIgnoreCase("IBM870"))
            return 870;
        else if(s_charset.equalsIgnoreCase("IBM871"))
            return 20871;
        else if(s_charset.equalsIgnoreCase("ISO-2022-JP"))
            return 50220;
        else if(s_charset.equalsIgnoreCase("ISO-2022-KR"))
            return 50225;
        else if(s_charset.equalsIgnoreCase("ISO-8859-3"))
            return 28593;
        else if(s_charset.equalsIgnoreCase("ISO-8859-6"))
            return 28596;
        else if(s_charset.equalsIgnoreCase("ISO-8859-8"))
            return 28598;
        else if(s_charset.equalsIgnoreCase("windows-1255"))
            return 1255;
        else if(s_charset.equalsIgnoreCase("windows-1256"))
            return 1256;
        else if(s_charset.equalsIgnoreCase("windows-1258"))
            return 1258;
        else
            return 0;
    }
