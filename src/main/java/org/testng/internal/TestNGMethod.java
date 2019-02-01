package org.testng.internal;

import org.testng.IRetryAnalyzer;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.annotations.CustomAttribute;
import org.testng.annotations.ITestAnnotation;
import org.testng.internal.annotations.AnnotationHelper;
import org.testng.internal.annotations.IAnnotationFinder;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlTest;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/** This class represents a test method. */
public class TestNGMethod extends BaseTestMethod {

  private int m_threadPoolSize = 0;
  private int m_invocationCount = 1;
  private int m_successPercentage = 100;
  private boolean isDataDriven = false;
  private CustomAttribute[] m_attributes = {};

  /** Constructs a <code>TestNGMethod</code> */
  public TestNGMethod(Method method, IAnnotationFinder finder, XmlTest xmlTest, Object instance) {
    this(method, finder, true, xmlTest, instance);
  }

  private TestNGMethod(
      Method method,
      IAnnotationFinder finder,
      boolean initialize,
      XmlTest xmlTest,
      Object instance) {
    super(method.getName(), new ConstructorOrMethod(method), finder, instance);
    setXmlTest(xmlTest);

    if (initialize) {
      init(xmlTest);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getInvocationCount() {
    return m_invocationCount;
  }

  /** {@inheritDoc} */
  @Override
  public int getSuccessPercentage() {
    return m_successPercentage;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTest() {
    return true;
  }

  private void init(XmlTest xmlTest) {
    setXmlTest(xmlTest);
    setInvocationNumbers(
        xmlTest.getInvocationNumbers(
            m_method.getDeclaringClass().getName() + "." + m_method.getName()));
    {
      ITestAnnotation testAnnotation =
          AnnotationHelper.findTest(getAnnotationFinder(), m_method.getMethod());

      if (testAnnotation == null) {
        // Try on the class
        testAnnotation =
            AnnotationHelper.findTest(getAnnotationFinder(), m_method.getDeclaringClass());
      }

      if (null != testAnnotation) {
        setTimeOut(testAnnotation.getTimeOut());
        m_successPercentage = testAnnotation.getSuccessPercentage();
        isDataDriven = doesTestAnnotationHaveADataProvider(testAnnotation);

        setInvocationCount(testAnnotation.getInvocationCount());
        setThreadPoolSize(testAnnotation.getThreadPoolSize());
        setAlwaysRun(testAnnotation.getAlwaysRun());
        setDescription(findDescription(testAnnotation, xmlTest));
        setEnabled(testAnnotation.getEnabled());
        setRetryAnalyzer(cloneInstance(testAnnotation.getRetryAnalyzer()));
        setRetryAnalyzerClass(testAnnotation.getRetryAnalyzerClass());
        setSkipFailedInvocations(testAnnotation.skipFailedInvocations());
        setInvocationTimeOut(testAnnotation.invocationTimeOut());
        setIgnoreMissingDependencies(testAnnotation.ignoreMissingDependencies());
        setPriority(testAnnotation.getPriority());
        m_attributes = testAnnotation.getAttributes();
      }

      // Groups
      {
        initGroups(ITestAnnotation.class);
      }
    }
  }

  private static boolean doesTestAnnotationHaveADataProvider(ITestAnnotation testAnnotation) {
    return !testAnnotation.getDataProvider().trim().isEmpty()
        || testAnnotation.getDataProviderClass() != null;
  }

  private String findDescription(ITestAnnotation testAnnotation, XmlTest xmlTest) {
    String result = testAnnotation.getDescription();
    if (result == null) {
      List<XmlClass> classes = xmlTest.getXmlClasses();
      for (XmlClass c : classes) {
        if (c.getName().equals(m_method.getMethod().getDeclaringClass().getName())) {
          for (XmlInclude include : c.getIncludedMethods()) {
            if (include.getName().equals(m_method.getName())) {
              result = include.getDescription();
              if (result != null) {
                break;
              }
            }
          }
        }
      }
    }
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public int getThreadPoolSize() {
    return m_threadPoolSize;
  }

  /** Sets the number of threads on which this method should be invoked. */
  @Override
  public void setThreadPoolSize(int threadPoolSize) {
    m_threadPoolSize = threadPoolSize;
  }

  /** Sets the number of invocations for this method. */
  @Override
  public void setInvocationCount(int counter) {
    m_invocationCount = counter;
  }

  /**
   * Clones the current <code>TestNGMethod</code> and its @BeforeMethod and @AfterMethod methods.
   *
   * @see org.testng.internal.BaseTestMethod#clone()
   */
  @Override
  public BaseTestMethod clone() {
    TestNGMethod clone =
        new TestNGMethod(
            getConstructorOrMethod().getMethod(),
            getAnnotationFinder(),
            false,
            getXmlTest(),
            getInstance());
    ITestClass tc = getTestClass();
    NoOpTestClass testClass = new NoOpTestClass(tc);
    testClass.setBeforeTestMethods(clone(tc.getBeforeTestMethods()));
    testClass.setAfterTestMethod(clone(tc.getAfterTestMethods()));
    clone.m_testClass = testClass;
    clone.setDate(getDate());
    clone.setGroups(getGroups());
    clone.setGroupsDependedUpon(getGroupsDependedUpon(), Collections.emptyList());
    clone.setMethodsDependedUpon(getMethodsDependedUpon());
    clone.setAlwaysRun(isAlwaysRun());
    clone.m_beforeGroups = getBeforeGroups();
    clone.m_afterGroups = getAfterGroups();
    clone.m_currentInvocationCount = m_currentInvocationCount;
    clone.setMissingGroup(getMissingGroup());
    clone.setThreadPoolSize(getThreadPoolSize());
    clone.setDescription(getDescription());
    clone.setEnabled(getEnabled());
    clone.setParameterInvocationCount(getParameterInvocationCount());
    clone.setInvocationCount(getInvocationCount());
    clone.m_successPercentage = getSuccessPercentage();
    clone.setTimeOut(getTimeOut());
    clone.setRetryAnalyzer(getRetryAnalyzer());
    clone.setRetryAnalyzerClass(getRetryAnalyzerClass());
    clone.setSkipFailedInvocations(skipFailedInvocations());
    clone.setInvocationNumbers(getInvocationNumbers());
    clone.setPriority(getPriority());

    return clone;
  }

  private ITestNGMethod[] clone(ITestNGMethod[] sources) {
    ITestNGMethod[] clones = new ITestNGMethod[sources.length];
    for (int i = 0; i < sources.length; i++) {
      clones[i] = sources[i].clone();
    }

    return clones;
  }

  private static IRetryAnalyzer cloneInstance(IRetryAnalyzer instance) {
    if (instance == null) {
      return null;
    }
    return InstanceCreator.newInstance(instance.getClass());
  }

  @Override
  public boolean isDataDriven() {
    return isDataDriven;
  }

  @Override
  public CustomAttribute[] getAttributes() {
    return m_attributes;
  }
}
