package com.github.kreutzr.responsediff;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader
{
  private Properties properties_;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public PropertyReader( final String fileName )
  throws IOException
  {
    final InputStream is = getClass().getClassLoader().getResourceAsStream( fileName );
    properties_ = new Properties();
    properties_.load( is );
    is.close();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getProperty( final String propertyName )
  {
      return properties_.getProperty( propertyName );
  }
}
