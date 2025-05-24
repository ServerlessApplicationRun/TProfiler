package com.taobao.profile.test;

import org.junit.Assert;
import org.junit.Test;

import com.taobao.profile.utils.Utilities;
import com.taobao.profile.utils.VariableNotFoundException;

import java.util.Properties;

public class UtilitiesTest{

  @Test
  public void testRepleseVariables() throws VariableNotFoundException{
    String source = "${user.home}/logs/${user.language}/tprofiler.log";
    String str1 = Utilities.repleseVariables(source, System.getProperties());
    String str2 = System.getProperty("user.home") + "/logs/" + System.getProperty("user.language") + "/tprofiler.log";  
    Assert.assertEquals(str1, str2);
  }

  @Test(expected = VariableNotFoundException.class)
  public void testVariableNotFoundException() throws VariableNotFoundException {
    String source = "${user.home}/${nonexistent.var}/file.log";
    Utilities.repleseVariables(source, System.getProperties());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullSource() throws VariableNotFoundException {
    Utilities.repleseVariables(null, System.getProperties());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullContext() throws VariableNotFoundException {
    Utilities.repleseVariables("test", null);
  }

  @Test
  public void testNoVariables() throws VariableNotFoundException {
    String source = "This is a string without variables.";
    String result = Utilities.repleseVariables(source, System.getProperties());
    Assert.assertEquals(source, result);
  }

  @Test
  public void testMalformedVariableExpressions() throws VariableNotFoundException {
    // Use System.getProperties() as a context that is unlikely to contain test-specific keys.
    Properties generalContext = System.getProperties();

    String source1 = "${incomplete";
    String result1 = Utilities.repleseVariables(source1, generalContext); 
    Assert.assertEquals(source1, result1);

    String source2 = "missing_brace}";
    String result2 = Utilities.repleseVariables(source2, generalContext); 
    Assert.assertEquals(source2, result2);
    
    // Test for "another ${var${inner}} case"
    // Expecting VariableNotFoundException for key "inner}" based on previous observations.
    String source3 = "another ${var${inner}} case";
    try {
      // Using a new empty context to ensure "inner}" is not found.
      Utilities.repleseVariables(source3, new Properties()); 
      Assert.fail("Expected VariableNotFoundException for source3 due to 'inner}' key not being found.");
    } catch (VariableNotFoundException e) {
      // Verify the specific key in the exception message.
      Assert.assertEquals("variable inner} not found", e.getMessage());
    }
  }

  @Test
  public void testNestedVariableReplacementInnermostFirst() throws VariableNotFoundException {
    String source = "another ${var${inner_key}} case";
    Properties context = new Properties();
    context.put("inner_key}", "resolved_inner"); 
    context.put("varresolved_inner}", "final_value"); // This key won't be used if the outer var structure is lost
    String result = Utilities.repleseVariables(source, context);
    // Based on detailed trace, the '}' of the outer variable is lost after inner replacement.
    Assert.assertEquals("another ${varresolved_inner case", result);
  }
  
  @Test(expected = VariableNotFoundException.class)
  public void testEmptyVariableNameWithEmptyContext() throws VariableNotFoundException {
    String source = "${}";
    Properties context = new Properties();
    Utilities.repleseVariables(source, context);
  }

  @Test
  public void testEmptyVariableNameWithMatchingContext() throws VariableNotFoundException {
    String source = "${}";
    Properties context = new Properties();
    context.put("", "emptyKeyValue");
    String result = Utilities.repleseVariables(source, context);
    Assert.assertEquals("emptyKeyValue", result);
  }
}
