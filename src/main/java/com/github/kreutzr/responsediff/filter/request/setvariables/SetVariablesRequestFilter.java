package com.github.kreutzr.responsediff.filter.request.setvariables;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.TestSetHandler;
import com.github.kreutzr.responsediff.VariablesCalculator;
import com.github.kreutzr.responsediff.VariablesHandler;
import com.github.kreutzr.responsediff.XmlHeader;
import com.github.kreutzr.responsediff.XmlParameter;
import com.github.kreutzr.responsediff.XmlRequest;
import com.github.kreutzr.responsediff.XmlTest;
import com.github.kreutzr.responsediff.XmlVariable;
import com.github.kreutzr.responsediff.XmlVariables;
import com.github.kreutzr.responsediff.filter.DiffFilterException;
import com.github.kreutzr.responsediff.filter.DiffFilterImpl;
import com.github.kreutzr.responsediff.filter.DiffRequestFilter;
import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.tools.JsonHelper;

/**
 * A filter that replaces variables in request headers, parameters and body.
 * Variables in the test's id and description are also replaced - but only for the CANDIDATE instance.
 * <p>
 * <b>Supported parameters:</b>
 * <ul>
 * <li>id="source", values=path-to-JSON-file</li>
 * <li>id="useVariables" values=[ true | false (default) ]</li>
 * </ul>
 * You may use a source file as follows:
 * <pre>
 *{
 * "variables" : {
 *   "key1" : [ "value11", "value12", "value13" ],
 *   "key2" : [ "value21", "value22", "value23" ]
 * },
 * "variableSets" : null
 *}
 * </pre>
 * for free variable combinations
 * or
 * <pre>
 *{
 * "variables" : null,
 * "variableSets" : [
 *   { "key1": "value31", "key2" : "value41" },
 *   { "key1": "value31", "key2" : "value42" },
 *   { "key1": "value31", "key2" : "value43" },
 *   { "key1": "value32", "key2" : "value41" },
 *   { "key1": "value32", "key2" : "value42" },
 *   { "key1": "value32", "key2" : "value43" },
 *   { "key1": "value33", "key2" : "value41" },
 *   { "key1": "value33", "key2" : "value42" },
 *   { "key1": "value33", "key2" : "value43" }
 * ]
 *}
 *</pre>
 *for deterministic variable combinations.
 */
public class SetVariablesRequestFilter extends DiffFilterImpl implements DiffRequestFilter
{
  public  static final String PARAMETER_NAME__SOURCE        = "source";
  public  static final String PARAMETER_NAME__USE_VARIABLES = "useVariables";

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static final Logger LOG = LoggerFactory.getLogger( SetVariablesRequestFilter.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private SetVariablesRequestFilterSource source_ = null;

  private VariablesPermutationHandler variablesPermutationHandler_ = null;
  private int variableSetsIndex_ = 0;
  private Boolean useVariables_ = null;
  private XmlVariables xmlVariables_ = null;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected void registerFilterParameterNames()
  {
    super.registerFilterParameterNames();
    registerFilterParameterName( PARAMETER_NAME__SOURCE );
    registerFilterParameterName( PARAMETER_NAME__USE_VARIABLES );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void next()
  {
    if( useVariables_ ) {
      variablesPermutationHandler_.next();
    }
    else {
      variableSetsIndex_ += 1;
      if( variableSetsIndex_ >= source_.getVariableSets().size() ) {
        variableSetsIndex_ = 0;
      }
    }

    xmlVariables_ = null;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void apply(
    final XmlRequest xmlRequest,
    final String serviceId,
    final XmlTest xmlTest
  )
  throws DiffFilterException
  {
    if( source_ == null || useVariables_ == null ) {
      init();
    }

    if( xmlVariables_ == null ) {
      xmlVariables_ = new XmlVariables();

      try {
        if( useVariables_ ) {
          if( source_.getVariables() == null || source_.getVariables().size() == 0 ) {
            LOG.warn( "There are no variables defined." );
            return;
          }

          final Map< String, Integer > variablesIndexByName = variablesPermutationHandler_.getIndexes();
          for( final String name : variablesIndexByName.keySet() ) {
            final int variablesIndex = variablesIndexByName.get( name );
            final List< Object > variablesValues = source_.getVariables().get( name );
            final String value = variablesValues.get( variablesIndex ) != null
              ? variablesValues.get( variablesIndex ).toString()
              : null;

            final XmlVariable xmlVariable = new XmlVariable();
            xmlVariable.setId( name );
            // CAUTION: We must avoid individual calculated values per instance!
            xmlVariable.setValue( VariablesCalculator.calculateIfRequired( value ) );
            xmlVariables_.getVariable().add( xmlVariable );
          }
        }
        else {
          if( source_.getVariableSets().size() == 0 ) {
            LOG.warn( "There are no variable sets defined." );
            return;
          }

          if( LOG.isDebugEnabled() ) {
            LOG.debug( "Applying variable set at index " + variableSetsIndex_ + "." );
          }

          final Map< String, Object > variableSet = source_.getVariableSets().get( variableSetsIndex_ );
          for( final String key : variableSet.keySet() ) {
            final String value = variableSet.get( key ) != null
              ? variableSet.get( key ).toString()
              : "null";

            final XmlVariable xmlVariable = new XmlVariable();
            xmlVariable.setId( key );
            // CAUTION: We must avoid individual calculated values per instance!
            xmlVariable.setValue( VariablesCalculator.calculateIfRequired( value ) );
            xmlVariables_.getVariable().add( xmlVariable );
          }
        }
      }
      catch( final ParseException ex ) {
        throw new DiffFilterException( ex );
      }
    }

    final String testId   = xmlTest.getId();
    final String fileName = getFilterParameter( PARAMETER_NAME__SOURCE );

    // Handle endpoint
    xmlRequest.setEndpoint( VariablesHandler.applyVariables( xmlRequest.getEndpoint(), xmlVariables_, "request endpoint", serviceId, testId, fileName ) );

    // Handle body
    if( xmlRequest.getBody() != null ) {
      xmlRequest.setBody( VariablesHandler.applyVariables( xmlRequest.getBody(), xmlVariables_, "request body", serviceId, testId, fileName ) );
    }

    // Handle headers
    if( xmlRequest.getHeaders() != null ) {
       for( final XmlHeader xmlHeader : xmlRequest.getHeaders().getHeader() ) {
         xmlHeader.setValue( VariablesHandler.applyVariables( xmlHeader.getValue(), xmlVariables_, "request header", serviceId, testId, fileName ) );
       }
    }

    // Handle parameters (in case they are not set already but only by this mass test data)
    if( xmlRequest.getParameters() != null ) {
      for( final XmlParameter xmlParameter : xmlRequest.getParameters().getParameter() ) {
        xmlParameter.setValue( VariablesHandler.applyVariables( xmlParameter.getValue(), xmlVariables_, "request parameter", serviceId, testId, fileName ) );
      }
    }

    // Handle description
    xmlRequest.setDescription( VariablesHandler.applyVariables( xmlRequest.getDescription(), xmlVariables_, "request description", serviceId, testId, fileName ) );

    // Handle XmlTest (for CANDIDATE only!)
    if( serviceId != null && serviceId.equals( TestSetHandler.CANDIDATE ) ) {
      xmlTest.setId         ( VariablesHandler.applyVariables( xmlTest.getId(),          xmlVariables_, "test id", serviceId, testId, fileName ) );
      xmlTest.setDescription( VariablesHandler.applyVariables( xmlTest.getDescription(), xmlVariables_, "test id", serviceId, testId, fileName ) );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public void init() throws DiffFilterException
  {
    if( source_ == null ) {
      source_ = readSource();
    }

    useVariables_ = Converter.asBoolean( getFilterParameter( PARAMETER_NAME__USE_VARIABLES ), false );

    if( useVariables_ && variablesPermutationHandler_ == null ) {
      variablesPermutationHandler_ = new VariablesPermutationHandler();
      variablesPermutationHandler_.init( source_.getVariables() );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private SetVariablesRequestFilterSource readSource() throws DiffFilterException
  {
    final String fileName = getFilterParameter( PARAMETER_NAME__SOURCE );
    String jsonAsString = null;

    Path path = null;
    try {
      // Read as relative file from setup directory
      path = Path.of( getTestSetupPath() + fileName );
      jsonAsString = readFromFile( path );
    }
    catch( final Throwable ex ) {
      try {
        // Fall back to current working directory
        final String rootPath = new File( "" ).getAbsolutePath() + File.separator;
        path = Path.of( rootPath + fileName );
        jsonAsString = readFromFile( path );
      }
      catch( final Throwable ex1 ) {
        try {
          // Fall back to absolute file
          path = Path.of( fileName );
          jsonAsString = readFromFile( path );
        }
        catch( final Exception ex2 ) {
          throw new DiffFilterException( "Error reading file \"" + fileName + "\".", ex2 );
        }
      }
    }

    try {
      return JsonHelper.provideObjectMapper().readValue( jsonAsString, SetVariablesRequestFilterSource.class );
    }
    catch( final Exception ex ) {
      throw new DiffFilterException( "Error reading JSON from file " + path.toString() + " . (text was " + jsonAsString + ")", ex );
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private String readFromFile( final Path filePath ) throws IOException
  {
    if( LOG.isDebugEnabled() ) {
      // CAUTION: This message is mentioned in the manual. => DO NOT CHANGE THIS!
      LOG.debug( "Trying to read variables file \"" + filePath.toString() + "\"." );
    }

    final String jsonAsString =  Files.readString( filePath, StandardCharsets.UTF_8 );

    if( LOG.isDebugEnabled() ) {
      LOG.debug( "Read variables from JSON file \"" + filePath.toString() + "\" as follows: " + jsonAsString );
    }

    return jsonAsString;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * This is used for testing only
   * @param source The source to use. Must not be null.
   */
  void setSource( final SetVariablesRequestFilterSource source )
  {
    source_ = source;
  }
}
