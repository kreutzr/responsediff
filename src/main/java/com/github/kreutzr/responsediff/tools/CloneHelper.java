package com.github.kreutzr.responsediff.tools;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.util.JAXBSource;

public class CloneHelper
{
  /**
   * Creates a deep copy (clone) using JaxB
   * @param <T> The type of the object to clone.
   * @param object The object to clone. May be null.
   * @param clazz The class of the object to clone. Must not be null.
   * @return A deep copy (clone) of the passed object.
   * @throws RuntimeException If an error occurs a RuntimeException is thrown.
   */
  public static <T> T deepCopyJAXB( final T object, final Class<T> clazz )
  {
    if( object == null ) {
      return null;
    }

    try {
      final JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
      final JAXBElement< T > contentObject = new JAXBElement< T >(new QName( clazz.getSimpleName() ), clazz, object );
      final JAXBSource source = new JAXBSource( jaxbContext, contentObject );
      return jaxbContext.createUnmarshaller().unmarshal( source, clazz ).getValue();
    }
    catch( final JAXBException ex ) {
        throw new RuntimeException( ex );
    }
  }
}
