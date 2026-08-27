package tmp;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.kreutzr.responsediff.tools.JsonHelper;

public class UserContextHandler 
{
  public static final String VERSION_0 = "V0";
  public static final String VERSION_1 = "V1";
  public static final String VERSION_2 = "V2";
  public static final String VERSION_LATEST = VERSION_2;
  
  public static UserContext getUserContextByUserId( final String userId )
  {
    try {
	    final String json = readUserContextFromDatabase( userId );
	    UserContext userContext = fromJson( json );
	    writeUserContextToDatabase( userId, userContext.getVersion(), JsonHelper.provideObjectMapper().writeValueAsString( userContext ) );
	    return userContext;
    }
	catch( final Exception ex ) {
	  // TODO: Handle exceptions properly
	  throw new RuntimeException( "Could not convert user context.", ex );
	}
  }
  
  private static String readUserContextFromDatabase( final String userId )
  {
	// TODO: Implement this
	return null;
  }
  
  private static void writeUserContextToDatabase( final String userId, final String version, final String json )
  {
	// TODO: Implement this
	System.out.println( json );
  }
	
  private static UserContext fromJson( final String json ) throws JsonMappingException, JsonProcessingException 
  {
	IUserContext userContext;
	if( json == null ) {
	  userContext = createInitialVersion();
	}
	else {
	 userContext = JsonHelper.provideObjectMapper().readValue( json, IUserContext.class );
	}
	
	return upgradeToLatest( userContext );
  }
  
  private static UserContext upgradeToLatest( final IUserContext userContext ) throws JsonProcessingException
  {
	IUserContext result = userContext;

	// Step from one version to the next
	if( result.getVersion().equals( VERSION_0 ) ) {
	  result = upgradeToVersion1( (UserContextV0) result ); 
	}
	if( result.getVersion().equals( VERSION_1 ) ) {
	  result = upgradeToVersion2( (UserContextV1) result ); 
	}
	
	// Add more conversions here if required
	
	// Final check
	if( result.getVersion().equals( VERSION_LATEST ) ) {
	  return castTo( UserContext.class, result, true );
}
	else {
	  throw new RuntimeException( "Could not upgrade usercontext " + result.getVersion() + " to " + VERSION_LATEST +"." ); 
	}
  }
  
  private static <T> T castTo( Class<T> clazz, final IUserContext userContext, final boolean failOnUnknownProperties ) throws JsonProcessingException
  {
    // NOTE: Casting won't work here.
    final String json = JsonHelper.provideObjectMapper().writeValueAsString( userContext );
    return JsonHelper.provideObjectMapper().configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties ).readValue( json, clazz );
  }
  
  private static void checkVersion( final String expected, final String actual )
  {
    if( !actual.equals( expected ) ) {
      throw new IllegalArgumentException( "User context version " + expected + " expected but was " + actual + "." );
    }
  }
  
  private static UserContextV0 createInitialVersion()
  {
    final UserContextV0 result = new UserContextV0();
    result.setVersion(VERSION_0);
    
    // TODO: Initialize static or from database
    
    return result;
  }
  
  private static UserContextV1 upgradeToVersion1( final UserContextV0 userContext ) throws JsonProcessingException
  {
    checkVersion( VERSION_0, userContext.getVersion() );

    final UserContextV1 result = castTo( UserContextV1.class, userContext, false );
	result.setVersion(VERSION_1);
	 
	// Convert
	result.setThema( "light" ); // Hard coded fallback
	 
    return result;
  }
  
  private static UserContextV2 upgradeToVersion2( final UserContextV1 userContext ) throws JsonProcessingException
  {
    checkVersion( VERSION_1, userContext.getVersion() );

    final UserContextV2 result = castTo( UserContextV2.class, userContext, false );
	result.setVersion(VERSION_2);
	 
	// Convert
	result.setTheme( userContext.getThema() ); // Fix attribute renaming
	result.setLanguage( "de_DE" ); // Hard coded fallback or read from database or user service.
	 
	return result;
  }
  
  public static void main(String[] args ) {
	final UserContext userContext = getUserContextByUserId( UUID.randomUUID().toString() );
  }
}
