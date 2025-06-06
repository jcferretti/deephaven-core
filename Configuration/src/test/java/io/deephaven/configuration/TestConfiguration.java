//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
package io.deephaven.configuration;

import junit.framework.TestCase;
import org.junit.After;

import java.util.List;
import java.util.Objects;

/**
 * Test suite for Configuration.
 *
 * Must provide a Configuration.rootFile property in the VM arguments when running from IntelliJ, even though we set
 * that in most of the tests. -ea -DConfiguration.rootFile=resources/lib-tests.prop -Dworkspace=/ -DcacheDir=/cache
 */
public class TestConfiguration extends TestCase {
    private final String FILENAME_PROPERTY = ConfigDir.ROOT_FILE_PROP;
    private final String oldFileLocation = System.getProperty(FILENAME_PROPERTY);

    /**
     * Restore previous configuration values after running the tests.
     * 
     * @throws Exception If reloading the old properties file somehow fails
     */
    @After
    public void tearDown() throws Exception {
        if (!Objects.equals(System.getProperty(FILENAME_PROPERTY), oldFileLocation)) {
            System.setProperty(FILENAME_PROPERTY, oldFileLocation);
            Configuration.getInstance().reloadProperties();
        }
        System.out.println("-------------- Test complete -----------------");
    }

    public void testPrint() {
        Configuration.getInstance().properties.list(System.out);
    }

    /**
     * Verify that Configuration correctly fails if it cannot locate the file to be loaded.
     *
     * Make sure that there does not exist a file named /NoSuchFileExists.prop.
     */
    public void testFileExistenceCheck() {
        // Make sure it fails if we pass in a nonexistent resource
        try {
            System.setProperty(FILENAME_PROPERTY, "NoSuchFileExists.prop");
            // Check for correct handling if the file is not found or is not accessible
            Configuration.getInstance().reloadProperties();
            fail("Expected ConfigurationException, got no exception");
        } catch (ConfigurationException e) {
            // Expected
        } catch (Exception e) {
            fail("Didn't get expected ConfigurationException, got something else instead: " + e.getMessage());
        }

        // Make sure it fails correctly if we pass in a nonexistent file
        try {
            System.setProperty(FILENAME_PROPERTY, "/NoSuchFileExists.prop");
            // Check for correct handling if the file is not found or is not accessible
            Configuration.getInstance().reloadProperties();
        } catch (ConfigurationException e) {
            // Expected
        } catch (Exception e) {
            fail("Didn't get expected ConfigurationException, got something else instead: " + e.getMessage());
        }
    }

    /**
     * Verify expected normal operation
     */
    public void testSimple() {
        final String propertyValue = Configuration.getInstance().getProperty("measurement.per_thread_cpu");
        assertEquals("false", propertyValue);
    }

    /**
     * Verify that the 'includes' mechanism works property
     * 
     * @throws Exception If the configuration cannot be loaded
     */
    public void testIncludes() throws Exception {
        System.setProperty(FILENAME_PROPERTY, "resources/test-include.prop");
        Configuration.getInstance().reloadProperties();
        assertTrue(Configuration.getInstance().getBoolean("test1"));
        assertTrue(Configuration.getInstance().getBoolean("test2"));
        assertTrue(Configuration.getInstance().getBoolean("test3"));
        assertTrue(Configuration.getInstance().getBoolean("override1"));
        assertTrue(Configuration.getInstance().getBoolean("override2"));
        assertTrue(Configuration.getInstance().getBoolean("override3"));
    }

    /**
     * Verify that specific operations of Configuration other than named properties work properly
     */
    public void testProperties() {
        try {
            // noinspection SpellCheckingInspection
            Configuration.getInstance().getProperty("NonExistant"); // Intentional misspelling
            fail("Expected exception");
        } catch (PropertyException expected) {
            // Expected
        }

        try {
            System.setProperty(FILENAME_PROPERTY, "nonexistent");
            Configuration.newStandaloneConfiguration();
            fail("Expected exception");
        } catch (ConfigurationException expected) {
            // Expected
        }

        System.setProperty(FILENAME_PROPERTY, oldFileLocation);
        System.setProperty("testconfig", "/test");
        System.out.println(Configuration.getInstance().lookupPath("testconfig"));
        assertTrue(Configuration.getInstance().lookupPath("testconfig").indexOf("test") > 0);
        try {
            // noinspection SpellCheckingInspection
            Configuration.getInstance().lookupPath("nonexistant"); // Intentional misspelling
            fail("Didn't catch expected exception");
        } catch (ConfigurationException ignored) {
            // expected
        }

    }

    /**
     * Test that the file will fail to load if a scope declaration has no scope items
     */
    public void testEmptyScope() {
        System.setProperty(FILENAME_PROPERTY, "test-empty-scope.prop");
        try {
            Configuration.getInstance().reloadProperties();
            fail("Expected and did not get parsing failure due to empty scope.");
        } catch (ConfigurationException e) {
            // Expected
        } catch (Exception e) {
            fail("Received unexpected error while checking for empty scope: " + e.getMessage());
        }
    }

    /**
     * Test that the file will fail to load if a scope declaration is malformed
     */
    public void testBadScope() {
        System.setProperty(FILENAME_PROPERTY, "test-bad-scope.prop");
        try {
            Configuration.getInstance().reloadProperties();
            fail("Expected and did not get parsing failure due to invalid scope.");
        } catch (ConfigurationException e) {
            // Expected
        } catch (Exception e) {
            fail("Received unexpected error while checking for invalid scope: " + e.getMessage());
        }
    }

    /**
     * Test that the file will fail to load if a '{' is included with no scope declaration before it.
     */
    public void testBadScopeOpen() {
        System.setProperty(FILENAME_PROPERTY, "test-bad-scope-open.prop");
        try {
            Configuration.getInstance().reloadProperties();
            fail("Expected and did not get parsing failure due to { with no preceding scope declaration.");
        } catch (ConfigurationException e) {
            // Expected
        } catch (Exception e) {
            fail("Received unexpected error while checking for invalid scope opener: " + e.getMessage());
        }
    }

    /**
     * Test that you can't use reserved keywords as declarations
     */
    public void testReservedKeywordAsDeclaration() {
        System.setProperty(FILENAME_PROPERTY, "test-reserved-keyword.prop");
        try {
            Configuration.getInstance().reloadProperties();
            fail("Expected and did not get parsing failure due to reserved keyword used as declaration.");
        } catch (ConfigurationException e) {
            // Expected
        } catch (Exception e) {
            fail("Received unexpected error while checking for reserved keyword used as declaration: "
                    + e.getMessage());

        }
    }

    /**
     * Verify that loading context-sensitive configuration files works correctly, both with a known system property and
     * an on-the-fly property
     */
    public void testContext() throws Exception {
        final String oldProcessName = System.getProperty(DefaultConfigurationContext.PROCESS_NAME_PROPERTY);
        final String testProp = "testproperty";
        try {
            System.setProperty(FILENAME_PROPERTY, "resources/test-context.prop");

            String procVal = "abcd";
            String propVal = "A";
            System.setProperty("process.name", procVal);
            System.setProperty(testProp, propVal);
            Configuration.getInstance().reloadProperties();
            assertEquals(procVal, Configuration.getInstance().getStringWithDefault("procval", "FAIL"));
            assertEquals(propVal, Configuration.getInstance().getStringWithDefault("propval", "FAIL"));
            assertEquals(propVal + procVal, Configuration.getInstance().getStringWithDefault("propproc", "FAIL"));
            assertEquals("changed", Configuration.getInstance().getStringWithDefault("aval", "FAIL"));
            assertEquals("nothing", Configuration.getInstance().getStringWithDefault("multiprop", "FAIL"));
            assertEquals("something", Configuration.getInstance().getStringWithDefault("multiprop2", "FAIL"));

            propVal = "B";
            System.setProperty(testProp, propVal);
            Configuration.getInstance().reloadProperties();
            assertEquals(procVal, Configuration.getInstance().getStringWithDefault("procval", "FAIL"));
            assertEquals(propVal, Configuration.getInstance().getStringWithDefault("propval", "FAIL"));
            assertEquals(propVal + procVal, Configuration.getInstance().getStringWithDefault("propproc", "FAIL"));
            assertEquals("changed", Configuration.getInstance().getStringWithDefault("aval", "FAIL"));
            assertEquals("nothing", Configuration.getInstance().getStringWithDefault("multiprop", "FAIL"));
            assertEquals("something", Configuration.getInstance().getStringWithDefault("multiprop2", "FAIL"));

            procVal = "defg";
            propVal = "A";
            System.setProperty("process.name", procVal);
            System.setProperty(testProp, propVal);
            Configuration.getInstance().reloadProperties();
            assertEquals(procVal, Configuration.getInstance().getStringWithDefault("procval", "FAIL"));
            assertEquals(propVal, Configuration.getInstance().getStringWithDefault("propval", "FAIL"));
            assertEquals(propVal + procVal, Configuration.getInstance().getStringWithDefault("propproc", "FAIL"));
            assertEquals("changed", Configuration.getInstance().getStringWithDefault("aval", "FAIL"));
            assertEquals("nothing", Configuration.getInstance().getStringWithDefault("multiprop", "FAIL"));
            assertEquals("nada", Configuration.getInstance().getStringWithDefault("multiprop2", "FAIL"));

            propVal = "B";
            System.setProperty(testProp, propVal);
            Configuration.getInstance().reloadProperties();
            assertEquals(procVal, Configuration.getInstance().getStringWithDefault("procval", "FAIL"));
            assertEquals(propVal, Configuration.getInstance().getStringWithDefault("propval", "FAIL"));
            assertEquals(propVal + procVal, Configuration.getInstance().getStringWithDefault("propproc", "FAIL"));
            assertEquals("changed", Configuration.getInstance().getStringWithDefault("aval", "FAIL"));
            assertEquals("nothing", Configuration.getInstance().getStringWithDefault("multiprop", "FAIL"));
            assertEquals("nada", Configuration.getInstance().getStringWithDefault("multiprop2", "FAIL"));

            // Make sure character escapes work
            assertEquals("\\abcd", Configuration.getInstance().getStringWithDefault("bval", "FAIL"));
            // Make sure the Unicode conversion works properly
            assertEquals("P", Configuration.getInstance().getStringWithDefault("cval", "FAIL"));

        } finally {
            if (oldProcessName != null)
                System.setProperty("process.name", oldProcessName);
            System.clearProperty(testProp);
        }

    }

    public void testContextIgnoreScope() throws Exception {
        final String oldProcessName = System.getProperty(DefaultConfigurationContext.PROCESS_NAME_PROPERTY);
        final String testProp = "testproperty";
        try {
            System.setProperty(FILENAME_PROPERTY, "resources/test-context.prop");
            String procVal = "notAValueFoundAnywhere";
            String propVal = "AlsoNotAValueFromAnything";
            System.setProperty("process.name", procVal);
            System.setProperty(testProp, propVal);
            Configuration.getInstance().reloadProperties(true);
            assertEquals(procVal, Configuration.getInstance().getStringWithDefault("process.name", "FAIL"));
            assertEquals(propVal, Configuration.getInstance().getStringWithDefault(testProp, "FAIL"));
            // This is the last value it was set to, so it should be this, ignoring all the 'final' declarations.
            assertEquals("Bdefg", Configuration.getInstance().getStringWithDefault("propproc", "FAIL"));

            assertEquals("changed", Configuration.getInstance().getStringWithDefault("aval", "FAIL"));

            assertEquals("multi", Configuration.getInstance().getStringWithDefault("multiprop", "FAIL"));

            assertEquals("something", Configuration.getInstance().getStringWithDefault("multiprop2", "FAIL"));
        } finally {
            if (oldProcessName != null)
                System.setProperty("process.name", oldProcessName);
            System.clearProperty(testProp);
        }
    }

    /**
     * Test that 'final' declarations are handled properly
     */
    public void testFinalDeclaration() {
        runTestsOnFinalKeyword("resources/final-test.prop", "final", "", "foo", "foo");
    }

    /**
     * Test that the 'finalize' declarations are handled properly
     */
    public void testFinalizeDeclaration() {
        runTestsOnFinalKeyword("resources/final-test.prop", "finalize", "FAIL", "foo", "foo");
    }

    /**
     * Test that both 'final' and 'finalize' can operate together.
     */
    public void testFinalAndFinalizedDeclaration() {
        runTestsOnFinalKeyword("resources/final-test.prop", "finalmixed", "", "foo", "foo");
    }

    /**
     * Test that changing a value that was declared 'final' in an already-included file causes an error.
     */
    public void testIncludeFinal() {
        // This test should set some values, then fail immediately due to changing the value of 'includetest'
        runTestsOnFinalKeyword("resources/include-test.prop", "finalinclude", "", "bar", "bar");
    }

    /**
     * Verify that we can either create an error or a warning when a declaration previously named 'final' is modified
     * later.
     *
     * @param filename The name of the file to load, generally from the resources directory within this project
     * @param contextName The process name to use while loading the configuration file
     * @param beforeValue The value that the 'beforetest' property should have at the end of this test. If 'beforetest'
     *        is explicitly set, then that value should appear (since it was parsed before the error happened). If
     *        'beforetest' is not explicitly set but is created by an empty declaration or the 'final' keyword, then it
     *        should have a value of empty-string. If 'beforetest' was created by a 'finalize' declaration, then it has
     *        no value at all, so the default value of 'FAIL' should be returned.
     */
    private void runTestsOnFinalKeyword(final String filename, final String contextName, final String beforeValue,
            final String finalTestValue, final String includeValue) {
        final String oldProcessName = System.getProperty(DefaultConfigurationContext.PROCESS_NAME_PROPERTY);
        final String testPropContextIdentifier = "testbatch";
        final String beforeTestProperty = "beforetest";
        final String finalTestProperty = "finaltest";
        final String afterTestProperty = "aftertest";
        try {
            System.setProperty(FILENAME_PROPERTY, filename);

            // Set the context to something that tries to re-set some final declarations
            System.setProperty(testPropContextIdentifier, contextName);

            // A ConfigurationException should be generated when trying to load a file that re-sets a final property
            try {
                Configuration.getInstance().reloadProperties();
                fail("Did not catch re-set final declaration");
            } catch (ConfigurationException e) {
                // expected; this configuration tries to re-set a final declaration or includes a file in the wrong
                // place.
                String a = e.getMessage();
            } catch (Exception e) {
                fail("Unexpected exception while checking for re-set final declaration: " + e.getMessage());
            }
            // The 'before' line should load with an empty value, unless it was declared with a 'finalize' statement and
            // so is null.
            assertEquals(beforeValue, Configuration.getInstance().getStringWithDefault(beforeTestProperty, "FAIL"));
            // the 'final' line should have the original value, unless it was not loaded
            assertEquals(finalTestValue, Configuration.getInstance().getStringWithDefault(finalTestProperty, "FAIL"));
            // The 'after' line should not be loaded, since this should always come after some exception
            assertEquals("NOTHING", Configuration.getInstance().getStringWithDefault(afterTestProperty, "NOTHING"));
            // The 'includetest' line should be loaded with its initial value, unless it was changed or failed to load
            assertEquals(includeValue, Configuration.getInstance().getStringWithDefault("includetest", "FAIL"));

        } finally {
            if (oldProcessName != null)
                System.setProperty(DefaultConfigurationContext.PROCESS_NAME_PROPERTY, oldProcessName);
            System.clearProperty(testPropContextIdentifier);
        }
    }

    /**
     * Test that an includefiles line must be the first line in the file
     */
    public void testIncludeInWrongPlace() {
        runTestsOnFinalKeyword("resources/test-include-wrong-place.prop", "finalinclude", "FAIL", "FAIL", "FAIL");
    }

    public void testFinalizedPropertyProgrammatic() {
        System.setProperty(FILENAME_PROPERTY, "resources/test-finalized-property-from-code.prop");
        try {
            Configuration.getInstance().reloadProperties();
            Configuration.getInstance().setProperty("foo", "plugh");
            fail("Expected and did not get error due to attempting to change final property.");
        } catch (ConfigurationException e) {
            // Expected
        } catch (Exception e) {
            fail("Received unexpected error while attempting to change final property: " + e.getMessage());

        }
    }

    /**
     * Test that the 'finalize' keyword can be used with an asterisk for a wildcard match.
     */
    public void testFinalizePatternDeclaration() {
        System.setProperty(FILENAME_PROPERTY, "resources/test-finalized-property-pattern.prop");
        try {
            Configuration.getInstance().reloadProperties();
            // 'fo' should be able to be set, since that doesn't match the pattern 'foo*' that was finalized
            Configuration.getInstance().setProperty("fo", "plugh");
            // Any other property starting with 'foo' should not be able to be set, even a newly-created on.
            Configuration.getInstance().setProperty("foozywhatsis", "xyzzy");
            fail("Expected and did not get error due to attempting to change finalized property.");
        } catch (ConfigurationException e) {
            // Expected
        } catch (Exception e) {
            fail("Received unexpected error while attempting to change finalized property: " + e.getMessage());
        }
    }

    /**
     * Test that multiline files process properly, including Windows filepaths
     */
    public void testMultiline() {
        System.out.println("-------------- Multiline test -----------------");
        System.setProperty(FILENAME_PROPERTY, "resources/test-multiline.prop");
        try {
            Configuration.getInstance().reloadProperties();
            assertEquals("abcdefghi", Configuration.getInstance().getStringWithDefault("foo", "FAIL"));
            assertEquals("C:\\a\\b\\c\\",
                    Configuration.getInstance().getStringWithDefault("research.dbCacheDir", "FAIL"));
            assertEquals("C:\\d\\e\\",
                    Configuration.getInstance().getStringWithDefault("research.stockPriceEstimator", "FAIL"));
        } catch (Exception e) {
            fail("Received unexpected error while checking multiline properties: " + e.getMessage());
        }
    }

    public void testShowHistory() {
        System.out.println("-------------- Show history -----------------");
        ParsedProperties properties = new ParsedProperties();
        properties.setProperty("someVal", "ABCD");
        properties.setProperty("someVal", "EFGH");
        List<PropertyHistory> history = properties.getLineNumbers().get("someVal");
        assertEquals(2, history.size());
        assertEquals(0, history.get(0).lineNumber);
        assertEquals("ABCD", history.get(1).value);
        assertEquals("EFGH", history.get(0).value);
        assertEquals("[]", history.get(0).context);
        final String javaVersion = System.getProperty("java.specification.version");
        if ("11".equals(javaVersion)) {
            assertEquals(
                    "<not from configuration file>: io.deephaven.configuration.TestConfiguration.testShowHistory(TestConfiguration.java:428)\n"
                            +
                            "java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                            "java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n",
                    history.get(0).fileName);
        } else if ("17".equals(javaVersion)) {
            assertEquals(
                    "<not from configuration file>: io.deephaven.configuration.TestConfiguration.testShowHistory(TestConfiguration.java:428)\n"
                            +
                            "java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
                            "java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)\n",
                    history.get(0).fileName);
        } else if ("21".equals(javaVersion)) {
            assertEquals(
                    "<not from configuration file>: io.deephaven.configuration.TestConfiguration.testShowHistory(TestConfiguration.java:428)\n"
                            +
                            "java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)\n"
                            +
                            "java.base/java.lang.reflect.Method.invoke(Method.java:580)\n",
                    history.get(0).fileName);
        } else if ("24".equals(javaVersion)) {
            assertEquals(
                    "<not from configuration file>: io.deephaven.configuration.TestConfiguration.testShowHistory(TestConfiguration.java:428)\n"
                            +
                            "java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)\n"
                            +
                            "java.base/java.lang.reflect.Method.invoke(Method.java:565)\n",
                    history.get(0).fileName);
        } else {
            fail("Must add specific test for java version " + javaVersion);
        }
        System.out.println("-------------- End show history -----------------");
    }

    public void testNamedConfiguration() {
        System.setProperty(FILENAME_PROPERTY, "resources/lib-tests.prop");
        System.setProperty("Configuration.Test1.rootFile", "resources/test1.prop");
        System.setProperty("Configuration.Test2.rootFile", "resources/test2.prop");
        System.setProperty("Configuration.Test3.rootFile", "resources/test3.prop");

        Configuration dc = Configuration.getInstance();
        Configuration c1 = Configuration.getNamed("Test1");
        Configuration c2 = Configuration.getNamed("Test2");
        Configuration c3 = Configuration.getNamed("Test3");

        assertEquals("cache", dc.getProperty("cacheDir"));
        assertNull(c1.getStringWithDefault("cacheDir", null));
        assertNull(c2.getStringWithDefault("cacheDir", null));
        assertNull(c3.getStringWithDefault("cacheDir", null));

        assertNull(dc.getStringWithDefault("test1", null));
        assertEquals("true", c1.getProperty("test1"));
        assertNull(c2.getStringWithDefault("test1", null));
        assertNull(c3.getStringWithDefault("test1", null));

        assertNull(dc.getStringWithDefault("test2", null));
        assertNull(c1.getStringWithDefault("test2", null));
        assertEquals("true", c2.getProperty("test2"));
        assertNull(c3.getStringWithDefault("test2", null));

        assertNull(dc.getStringWithDefault("test3", null));
        // test1 imports test3, so expected.
        assertEquals("true", c1.getProperty("test3"));
        assertNull(c2.getStringWithDefault("test3", null));
        assertEquals("true", c3.getProperty("test3"));

        // We'll try a reset of one of them
        System.setProperty("Configuration.Test1.rootFile", "resources/test2.prop");
        Configuration.reset("Test1");
        dc = Configuration.getInstance();
        c1 = Configuration.getNamed("Test1");
        c2 = Configuration.getNamed("Test2");
        c3 = Configuration.getNamed("Test3");

        assertEquals("cache", dc.getProperty("cacheDir"));
        assertNull(c1.getStringWithDefault("cacheDir", null));
        assertNull(c2.getStringWithDefault("cacheDir", null));
        assertNull(c3.getStringWithDefault("cacheDir", null));

        assertNull(dc.getStringWithDefault("test1", null));
        assertNull(c1.getStringWithDefault("test1", null));
        assertNull(c2.getStringWithDefault("test1", null));
        assertNull(c3.getStringWithDefault("test1", null));

        assertNull(dc.getStringWithDefault("test2", null));
        assertEquals("true", c1.getProperty("test2"));
        assertEquals("true", c2.getProperty("test2"));
        assertNull(c3.getStringWithDefault("test2", null));

        assertNull(dc.getStringWithDefault("test3", null));
        assertNull(c1.getStringWithDefault("test3", null));
        assertNull(c2.getStringWithDefault("test3", null));
        assertEquals("true", c3.getProperty("test3"));

        // Reset all of them
        System.setProperty(FILENAME_PROPERTY, "resources/test-multiline.prop");
        System.setProperty("Configuration.Test1.rootFile", "resources/lib-tests.prop");
        System.setProperty("Configuration.Test2.rootFile", "resources/test3.prop");
        System.setProperty("Configuration.Test3.rootFile", "resources/test2.prop");
        Configuration.reset();
        dc = Configuration.getInstance();
        c1 = Configuration.getNamed("Test1");
        c2 = Configuration.getNamed("Test2");
        c3 = Configuration.getNamed("Test3");

        assertEquals("abcdefghi", dc.getProperty("foo"));
        assertNull(c1.getStringWithDefault("foo", null));
        assertNull(c2.getStringWithDefault("foo", null));
        assertNull(c3.getStringWithDefault("foo", null));

        assertNull(dc.getStringWithDefault("cache", null));
        assertEquals("cache", c1.getProperty("cacheDir"));
        assertNull(c2.getStringWithDefault("cache", null));
        assertNull(c3.getStringWithDefault("cache", null));

        assertNull(dc.getStringWithDefault("test3", null));
        assertNull(c1.getStringWithDefault("test3", null));
        assertEquals("true", c2.getProperty("test3"));
        assertNull(c3.getStringWithDefault("test3", null));

        assertNull(dc.getStringWithDefault("test2", null));
        assertNull(c1.getStringWithDefault("test2", null));
        assertNull(c2.getStringWithDefault("test2", null));
        assertEquals("true", c3.getProperty("test2"));
    }

    public void testNamedConfigurationSimple() {
        System.setProperty(FILENAME_PROPERTY, "resources/lib-tests.prop");
        System.clearProperty("Configuration.Test1.rootFile");
        System.setProperty("Test1.rootFile", "resources/test1.prop");

        Configuration.reset();
        Configuration dc = Configuration.getInstance();
        Configuration c1 = Configuration.getNamed("Test1");

        assertEquals("cache", dc.getProperty("cacheDir"));
        assertNull(c1.getStringWithDefault("cacheDir", null));

        assertNull(dc.getStringWithDefault("test1", null));
        assertEquals("true", c1.getProperty("test1"));

        assertNull(dc.getStringWithDefault("test3", null));
        // test1 imports test3, so expected.
        assertEquals("true", c1.getProperty("test3"));

        // Make sure we can't try to load a Configuration with a null name, or `Configuration`
        try {
            final Configuration c = Configuration.getNamed(null);
            fail("Should have rejected a null name");
        } catch (ConfigurationException ignored) {

        }

        try {
            final Configuration c = Configuration.getNamed("Configuration");
            fail("Should have rejected the name `Configuration`");
        } catch (ConfigurationException ignored) {

        }

        try {
            final Configuration c = Configuration.getNamed("NonExistent");
            fail("Should have rejected a named config that doesnt exist");
        } catch (ConfigurationException ignored) {

        }
    }
}
