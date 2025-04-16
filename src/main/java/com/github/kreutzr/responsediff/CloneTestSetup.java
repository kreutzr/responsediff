package com.github.kreutzr.responsediff;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.filter.request.setvariables.SetVariablesRequestFilter;
import com.github.kreutzr.responsediff.tools.Converter;
import com.github.kreutzr.responsediff.tools.JsonHelper;

/**
 * Class to clone an existing test setup (including all associated files) to a given target folder.
 */
public class CloneTestSetup
{
  private static final Pattern p1 = Pattern.compile( "<ignore([^>]*)>" );
  private static final Pattern p2 = Pattern.compile( "</ignore[^>]*>" );

  private static final Logger LOG = LoggerFactory.getLogger( CloneTestSetup.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void cloneTestSetup(
    final String  sourceFilePath,
    final String  targetFolderPath,
    final boolean disableIgnoreTags
  )
  throws Exception
  {
    final String sourcePath   = convertFilePath( sourceFilePath );
          String targetFolder = convertFilePath( targetFolderPath );
    if( !targetFolder.endsWith( File.separator ) ) {
      targetFolder = targetFolder + File.separator;
    }
    final String fileName = sourcePath.substring( sourcePath.lastIndexOf( File.separator ) + 1 );

    traverse( sourcePath, targetFolder + fileName, disableIgnoreTags );
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void traverse(
    final String  sourcePath,
    final String  targetPath,
    final boolean disableIgnoreTags
  )
  throws Exception
  {
    if( LOG.isDebugEnabled() ) {
      LOG.debug( "traverse( source=" + sourcePath + ", target=" + targetPath + ", disableIgnoreTags=" + disableIgnoreTags + " )" );
    }

    final String sourceFolder = sourcePath.substring( 0, sourcePath.lastIndexOf( File.separator ) + 1 );
    final String targetFolder = targetPath.substring( 0, targetPath.lastIndexOf( File.separator ) + 1 );

    final String xml = handleXml(
      Files.readString( Path.of( sourcePath ), StandardCharsets.UTF_8 ),
      disableIgnoreTags
    );
    new File( targetFolder ).mkdirs();
    Files.writeString( Path.of( targetPath ), xml, StandardCharsets.UTF_8 );


    // Handle test sets
    final XmlResponseDiffSetup xmlTestSetup = XmlFileHandler.readSetup( sourcePath, false );

    final Iterator< XmlTestSet > it = xmlTestSetup.getTestSet().iterator();
    while( it.hasNext() ) {
      final XmlTestSet xmlTestSet = it.next();

      for( final XmlTestSetInclude include : xmlTestSet.getTestSetInclude() ) {
        final String childPath = convertFilePath( include.getFile() );
        traverse(
          sourceFolder + childPath,
          targetFolder + childPath,
          disableIgnoreTags
        );
      }
    }

    // Copy filter source files
    if( xmlTestSetup.getFilterRegistry() != null ) {
      final Iterator< XmlFilterRegistryEntry > itf = xmlTestSetup.getFilterRegistry().getFilter().iterator();
      while( itf.hasNext() ) {
        final XmlFilterRegistryEntry entry = itf.next();

        if( entry.getParameters() == null ) {
          continue;
        }

        final List< XmlParameter > params = entry.getParameters().getParameter();
        for( final XmlParameter param : params ) {
          if( param.getId().equals( SetVariablesRequestFilter.PARAMETER_NAME__SOURCE ) ) {
            final String filePath = convertFilePath( param.getValue() );
            if( filePath.startsWith( "." ) ) {
              final String source = sourceFolder + filePath;
              final String target = targetFolder + filePath ;

              if( LOG.isDebugEnabled() ) {
                LOG.debug( "Copy \"" + source + "\" to \"" + target + "\"." );
              }
              // We only copy relative paths
              Files.copy( Path.of( source ), Path.of( target ) );
            }
            else {
              LOG.warn( "File in filter entry \"" + entry.getId() + "\" has no relative path (does not start with \"./\")."
                      + " File path was: \"" + filePath + "\"."
              );
            }

            break;
          }
        }
      }
    }

    // Copy upload parts
    for( final XmlTestSet xmlTestSet : xmlTestSetup.getTestSet() ) {
      for( final XmlTest xmlTest : xmlTestSet.getTest() ) {
        final XmlRequest xmlRequest = xmlTest.getRequest();
        if( xmlRequest == null ) {
          continue;
        }
        final XmlUploadParts xmlUploadParts = xmlRequest.getUploadParts();
        if( xmlUploadParts != null ) {
          for( final XmlFile xmlFile : xmlUploadParts.getFile() ) {
            final String filePath = convertFilePath( xmlFile.getValue() );
            if( filePath.startsWith( "." ) ) {
              final String source = sourceFolder + filePath;
              final String target = targetFolder + filePath ;

              if( LOG.isDebugEnabled() ) {
                LOG.debug( "Copy \"" + source + "\" to \"" + target + "\"." );
              }
              // We only copy relative paths
              Files.copy( Path.of( source ), Path.of( target ) );
            }
          }
        }
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static String convertFilePath( final String filePath )
  {
    String result = filePath.trim();
    result = result.replace( '/',  File.separator.charAt(0) );
    result = result.replace( '\\', File.separator.charAt(0) );

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static String handleXml( final String xml, final boolean disableIgnoreTags )
  {
    String result = xml;
    if( disableIgnoreTags ) {
      final Matcher m1 = p1.matcher( xml );
      final Matcher m2 = p2.matcher( xml );

      final StringBuilder sb = new StringBuilder();
      int offset = 0;
      while( m1.find() ) {
        m2.find(); // Keep closing tag in synch with opening tag

        final String group = m1.group();
        if( group != null) {
          if( !group.contains( "forEver=\"true\"" ) ) {
            sb.append( xml.substring( offset, m1.start() ) )
              .append( "<!-- ### " )
              .append( xml.substring( m1.start(), m2.end() ) // Entire tag structure <ignore...>...</ignore...>
                 .replaceAll("<!--", "<!-x-" ).replaceAll( "-->", "-x->" ) // Mask inner XML comments
              )
              .append( " ### -->" );
            offset = m2.end();
          }
/*
          if( LOG.isDebugEnabled() ) {
            LOG.debug( m1.start()  + " - " + m1.end() + " : " + xml.substring( m1.start(), m1.end() ) );
            LOG.debug( m2.start()  + " - " + m2.end() + " : " + xml.substring( m2.start(), m2.end() ) );
            LOG.debug( "---" );
          }
*/
        }
      }

      sb.append( xml.substring( offset ) );

      result = sb.toString();
    }
    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static boolean deleteDirectory( final File file )
  {
    final File[] children = file.listFiles();
    if( children != null ) {
      for( final File child : children ) {
        deleteDirectory( child );
      }
    }
    return file.delete();
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static final void main( final String[] args )
  {
    String  rootPath          = new File( "" ).getAbsolutePath() + File.separator;
    String  sourceFilePath    = null;
    String  targetFolderPath  = null;
    boolean overwriteTarget   = false;
    boolean disableIgnoreTags = false;

    if( args == null || args.length != 1 ) {
      LOG.error( "The configuration JSON parameter is missing. Pass it as first (only) parameter." );
      System.exit( 1 );
    }

    CloneTestSetupConfiguration config = null;
    try
    {
      config = JsonHelper.provideObjectMapper().readValue( args[ 0 ], CloneTestSetupConfiguration.class );
    }
    catch( final Exception ex )
    {
      ex.printStackTrace();
      System.exit( 1 );
    }

    // Read parameters from configuration
    rootPath          = Converter.asString ( config.getRootPath(),          rootPath );
    sourceFilePath    = Converter.asString ( config.getSourceFilePath(),    sourceFilePath );
    targetFolderPath  = Converter.asString ( config.getTargetFolderPath(),  targetFolderPath );
    overwriteTarget   = Converter.asBoolean( config.getOverwriteTarget(),   overwriteTarget );
    disableIgnoreTags = Converter.asBoolean( config.getDisableIgnoreTags(), disableIgnoreTags );


    boolean hasStarted = false;
    try {
      if( overwriteTarget ) {
        // Remove target folder if existing
        deleteDirectory( new File( rootPath + targetFolderPath ) );
      }
      else {
        final File dir = new File( targetFolderPath );
        if( dir.exists() ) {
          throw new RuntimeException( "Folder \"" + targetFolderPath + "\" already exists." );
        }
      }

      hasStarted = true;

      CloneTestSetup.cloneTestSetup(
        rootPath + sourceFilePath,
        rootPath + targetFolderPath,
        disableIgnoreTags
      );

      System.exit( 0 );
    }
    catch( final Throwable ex ) {
      final String message = "An error occurred: (" + ex.getMessage() + ")"
        + ( hasStarted
              ? " The execution was not completed. Remove the target folder before starting again."
              : ""
           );

      LOG.error( message, ex );
      System.exit( 1 );
    }
  }
}
