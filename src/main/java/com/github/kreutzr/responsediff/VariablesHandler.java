package com.github.kreutzr.responsediff;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariablesHandler
{
  private static final Pattern VARIABLE_GROUP_PATTERN         = Pattern.compile( "\\$\\{([^\\}]+)\\}" );
  private static final Pattern VARIABLE_GROUP_PATTERN_ENCODED = Pattern.compile( "%24%7B([^%]+)%7D" );  // "${ ... }" URL encoded

  private static final Logger LOG = LoggerFactory.getLogger( VariablesHandler.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Replaces all found variable names within a given text by the defined variable values.
   * @param text      The text in which the variable name shall be replaced. May be null.
   * @param variables The variable definitions to use. Must not be null.
   * @param source    A String that indicates the source of the given text (e.g. "body", "header", "parameter")
   * @param serviceId A String that indicates the associated service (one of CANDIDATE, REFERENCE or CONTROL). May be null.
   * @param testId    The current test id. Must not be null.
   * @param testFileName The file name the current test is configured in. Must not be null.
   * @return The passed text with all matching variables being replaced. If text was null, null is returned.
   */
  public static String applyVariables(
    final String text,
    final XmlVariables xmlVariables,
    final String source,
    final String serviceId,
    final String testId,
    final String testFileName
  )
  {
    if( text == null ) {
      return null;
    }

    String result = new String( text );

    final Map< String, XmlVariable > variables = TestSetHandler.getVariablesMap( xmlVariables, null );

    final String servicePrefix = serviceId != null
      ? serviceId.trim() + "."
      : null;

    Matcher matcher = VARIABLE_GROUP_PATTERN.matcher( text );
    while( matcher.find() ) {
      result = handleMatcher( matcher, 2, 1, result, variables, servicePrefix ); // 2="${", 1="}"
    }

    matcher = VARIABLE_GROUP_PATTERN_ENCODED.matcher( text ); // Request endpoint parameters are already URL encoded here
    while( matcher.find() ) {
      result = handleMatcher( matcher, 6, 3, result, variables, servicePrefix ); // 6="%24%7B", 3="%7D"
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static String handleMatcher(
    final Matcher matcher,
    final int    offset1,
    final int    offset2,
    final String text,
    final Map< String, XmlVariable > variables,
    final String servicePrefix
  )
  {
    String result = new String( text );

    final String group = matcher.group();
    final String variableName = group.substring( offset1, group.length() - offset2 );

    // Replace by defined variable
    String source = "variables";
    String value = servicePrefix != null && variables.get( servicePrefix + variableName ) != null
      ? variables.get( servicePrefix + variableName ).getValue()
      : variables.get( variableName ) != null
        ? variables.get( variableName ).getValue()
        : null;

    // Fallback to system property
    if( value == null ) {
      source = "system properties";
      value = System.getProperty(variableName);
    }
    // Fallback system environment
    if( value == null ) {
      source = "environment properties";
      value = System.getenv(variableName);
    }

    if( LOG.isTraceEnabled() ) {
      if( !variableName.endsWith( ")" ) ) { // Don't log function calls here (they are not held inside the variables map).
        if( value != null ) {
          source = "(found in " + source + ")";
        }
        else {
          source = "(not found)";
        }

        LOG.trace( "Value of variable ${" + variableName + "}: " + value + " " + source );
      }
    }

    // Fallback to "${variableName}" (itself)
    if( value == null ) {
      value = "${" + variableName + "}"; // This will be found by the findUnresolvedVariables() method.
    }

    result = result.replace( "${"     + variableName + "}",   value )  // Plain
                   .replace( "%24%7B" + variableName + "%7D", value ); // URL encoded

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Finds all unresolved variables within a given XmlRequest.
   * @param xmlRequest The XmlRequest to inspect. May be null.
   * @return A List that holds the ids of all unresolved variables. May be empty but never null.
   */
  public static Set<String> findUnresolvedVariables( final XmlRequest xmlRequest )
  {
    final Set< String > result = new TreeSet<>();

    if( xmlRequest != null ) {
      final String text = ToJson.fromXmlRequest( xmlRequest );
      final Matcher matcher = VARIABLE_GROUP_PATTERN.matcher( text );
      while( matcher.find() ) {
        result.add( matcher.group() );
      }
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/*
  public static void main( String[] args )
  {
    String text ="AAA";
    System.out.println( text.replace( "A", "${a}" ) );
  }
*/
}
