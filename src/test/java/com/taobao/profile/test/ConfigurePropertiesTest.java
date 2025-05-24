package com.taobao.profile.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.profile.config.ConfigureProperties;

public class ConfigurePropertiesTest {
  
  @Test
  public void testConfigureProperties(){
    Properties prop = new Properties();
    prop.put("file.name", "tprofiler.log");
    prop.put("log.file.path", "${user.home}/${file.name}");
    Properties context = System.getProperties();
    context.putAll(prop);
    
    Properties properties  = new ConfigureProperties(prop, context);
    Assert.assertEquals(properties.getProperty("log.file.path"), System.getProperty("user.home") + "/tprofiler.log" );
  }
  
  @Test
  public void testUnresolvableVariableThrowsRuntimeException() {
    Properties delegateProps = new Properties();
    delegateProps.put("test.key", "value_with_${unresolvable.var}");
    Properties contextProps = new Properties(); // Ensure unresolvable.var is not here
    
    ConfigureProperties config = new ConfigureProperties(delegateProps, contextProps);
    
    try {
      config.getProperty("test.key");
      Assert.fail("Expected RuntimeException was not thrown.");
    } catch (RuntimeException e) {
      // Optionally, check if the cause is VariableNotFoundException
      // This depends on the implementation of ConfigureProperties
      // For now, just catching RuntimeException as specified.
      Assert.assertTrue("RuntimeException should be thrown for unresolvable variable.", true);
      // To be more specific if ConfigureProperties wraps VariableNotFoundException:
      // Assert.assertTrue("Cause should be VariableNotFoundException", e.getCause() instanceof com.taobao.profile.utils.VariableNotFoundException);
    }
  }

  @Test
  public void testGetPropertyWithDefaultValue() {
    // Scenario 1: Key not found, default value returned.
    Properties delegateProps1 = new Properties();
    Properties contextProps1 = new Properties();
    ConfigureProperties config1 = new ConfigureProperties(delegateProps1, contextProps1);
    Assert.assertEquals("defaultVal", config1.getProperty("nonexistent.key", "defaultVal"));

    // Scenario 2: Key found, but unresolvable variable, RuntimeException thrown.
    Properties delegateProps2 = new Properties();
    delegateProps2.put("test.key.unresolvable", "value_with_${unresolvable.var.again}");
    Properties contextProps2 = new Properties(); // Ensure unresolvable.var.again is not here
    ConfigureProperties config2 = new ConfigureProperties(delegateProps2, contextProps2);
    
    try {
      config2.getProperty("test.key.unresolvable", "defaultVal");
      Assert.fail("Expected RuntimeException was not thrown for unresolvable variable with default value.");
    } catch (RuntimeException e) {
      // Optionally, check if the cause is VariableNotFoundException
      Assert.assertTrue("RuntimeException should be thrown for unresolvable variable.", true);
      // To be more specific if ConfigureProperties wraps VariableNotFoundException:
      // Assert.assertTrue("Cause should be VariableNotFoundException", e.getCause() instanceof com.taobao.profile.utils.VariableNotFoundException);
    }
  }
  
  @Test
  public void testConfigure() throws IOException{
    Properties properties = new Properties();
    InputStream in = getClass().getClassLoader().getResourceAsStream("profile.properties");
    properties.load(in);

    Properties context = new Properties(System.getProperties());
    context.putAll(System.getProperties());
    context.putAll(properties);
    try{
      ConfigureProperties configureProperties = new ConfigureProperties(properties, context);
      String logFilePath = configureProperties.getProperty("logFilePath");
      Assert.assertEquals(logFilePath, System.getProperty("user.home") + "/logs/tprofiler.log");
    }finally{
      in.close();
    }
  }
}
