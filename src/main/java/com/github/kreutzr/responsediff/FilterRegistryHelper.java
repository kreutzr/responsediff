package com.github.kreutzr.responsediff;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kreutzr.responsediff.filter.DiffFilter;
import com.github.kreutzr.responsediff.filter.DiffFilterException;

public class FilterRegistryHelper
{
  private static final Logger LOG = LoggerFactory.getLogger( FilterRegistryHelper.class );

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the registered filters (id, class).
   * @param xmlTestSetup The XmlResponseDiffSetup to read from. Must not be null.
   * @param testSetupPath The path to the central test setup. May be null.
   * @return A Map that holds all registered filters (ready configured). May be empty but never null.
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws DiffFilterException
   */
  public static Map<String, DiffFilter > getFilterRegistry(
     final XmlResponseDiffSetup xmlTestSetup,
     final String testSetupPath
  )
  throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, DiffFilterException
  {
    final Map< String, DiffFilter > filterRegistry = new TreeMap<>();

    // Read filters from Setup
    final XmlFilterRegistry xmlFilterRegistry = xmlTestSetup.getFilterRegistry();
    if( xmlFilterRegistry != null ) {
      for( final XmlFilterRegistryEntry xmlFilter : xmlFilterRegistry.getFilter() ) {
        filterRegistry.put( xmlFilter.getId(), createFilterInstance( xmlFilter, testSetupPath ) );
      }
    }

    // Read filters from XmlTestSets
    for( final XmlTestSet xmlTestSetChild : xmlTestSetup.getTestSet() ) {
      final String testSetChildPath = ( testSetupPath == null ? "" : testSetupPath ) + xmlTestSetChild.getFilePath();
      final Map< String, DiffFilter > newFilters = readFilters( xmlTestSetChild, testSetChildPath );

      // Avoid filter id clashes
      for( final String key : newFilters.keySet() ) {
        if( filterRegistry.containsKey( key ) ) {
          final String message = "Filter with id \"" + key + "\" is already defined. Duplicate found in \"" + xmlTestSetChild.getFileName() + "\".";
          LOG.error( message );
          throw new RuntimeException( message );
        }
        filterRegistry.put( key, newFilters.get( key ) );
      }
    }

    return filterRegistry;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Reads the registered filters (id, class).
   * @param xmlTestSet The XmlTestSet to read from. Must not be null.
   * @param testSetPath The path to the test set. Must not be null.
   * @return A Map that holds all registered filters (ready configured). May be empty but never null.
   * @throws ClassNotFoundException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws DiffFilterException
   */
  private static Map< String, DiffFilter > readFilters(
    final XmlTestSet xmlTestSet,
    final String testSetPath
  ) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, DiffFilterException
  {
    final Map< String, DiffFilter > filterRegistry = new TreeMap<>();

    final XmlFilterRegistry xmlFilterRegistry = xmlTestSet.getFilterRegistry();
    if( xmlFilterRegistry != null ) {
      for( final XmlFilterRegistryEntry xmlFilter : xmlFilterRegistry.getFilter() ) {
        filterRegistry.put( xmlFilter.getId(), createFilterInstance( xmlFilter, testSetPath ) );
      }
    }

    // Read filters from XmlTestSets
    for( final XmlTestSet xmlTestSetChild : xmlTestSet.getTestSet() ) {
      final String testSetChildPath = testSetPath + xmlTestSetChild.getFilePath();
      final Map< String, DiffFilter > newFilters = readFilters( xmlTestSetChild, testSetChildPath );

      // Avoid filter id clashes
      for( final String key : newFilters.keySet() ) {
        if( filterRegistry.containsKey( key ) ) {
          final String message = "Filter with id \"" + key + "\" is already defined. Duplicate found in \"" + xmlTestSetChild.getFileName() + "\".";
          LOG.error( message );
          throw new RuntimeException( message );
        }
        filterRegistry.put( key, newFilters.get( key ) );
      }
    }

    return filterRegistry;
  }


  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a filter instance of a specific class name.
   * @param xmlFilter The XmlFilter object that holds the class name. Must not be null.
   * @param testSetupPath The path to the test setup. May be null.
   * @return An instance of the requested filter class.
   * @throws ClassNotFoundException
   * @throws SecurityException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws DiffFilterException
   */
  private static DiffFilter createFilterInstance(
    final XmlFilterRegistryEntry xmlFilter,
    final String testSetupPath
  )
  throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, DiffFilterException

  {
    final String className = xmlFilter.getClazz();

    // Create class instance
    final DiffFilter filter = (DiffFilter) Class.forName( className ).getDeclaredConstructor().newInstance();
    filter.setTestSetupPath( testSetupPath );

    // Set parameters
    if( xmlFilter.getParameters() != null && xmlFilter.getParameters().getParameter() != null ) {
      for( final XmlParameter parameter : xmlFilter.getParameters().getParameter() ) {
        filter.setFilterParameter( parameter.getId(), parameter.getValue() );
      }
    }

    // Initialize the filter
    filter.init();

    return filter;
  }
}
