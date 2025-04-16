package com.github.kreutzr.responsediff;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonDiffTest
{
   @Test
   public void testThatBooleanChangesAreDetected()
   {
     // Given
     final String candidate = "{\"a\":false}";
     final String reference = "{\"a\":true}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference).calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertEquals( 1, changes.size() );
     Assertions.assertEquals( "$.a", changes.get( 0 ).getJsonPath() );
     Assertions.assertTrue( deletions.isEmpty(), "Deletions are not empty." );
     Assertions.assertTrue( additions.isEmpty(), "Additions are not empty." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatIgnoringOfJsonPathsWorks()
   {
     // Given
     final String candidate = "{\"a\":false}";
     final String reference = "{\"a\":true}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference)
           .addIgnorePath( "$.a" )
           .calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertTrue( changes.isEmpty(),   "Changes are not empty." );
     Assertions.assertTrue( deletions.isEmpty(), "Deletions are not empty." );
     Assertions.assertTrue( additions.isEmpty(), "Additions are not empty." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatNumberChangesAreDetected()
   {
     // Given
     final String candidate = "{\"a\":3.1414}";
     final String reference = "{\"a\":3.1415}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference).calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertEquals( 1, changes.size() );
     Assertions.assertEquals( "$.a", changes.get( 0 ).getJsonPath() );
     Assertions.assertTrue( deletions.isEmpty(), "Deletions are not empty." );
     Assertions.assertTrue( additions.isEmpty(), "Additions are not empty." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatNumberEpsilonComparisonWorks()
   {
     // Given
     final String candidate = "{\"a\":3.1414}";
     final String reference = "{\"a\":3.1415}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference)
           .setEpsilon( 0.001 )
           .calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertTrue( changes.isEmpty(),   "Changes are not empty." );
     Assertions.assertTrue( deletions.isEmpty(), "Deletions are not empty." );
     Assertions.assertTrue( additions.isEmpty(), "Additions are not empty." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatStringChangesAreDetected()
   {
     // Given
     final String candidate = "{\"a\":\" TEXT \"}";
     final String reference = "{\"a\":\"text\"}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference).calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertEquals( 1, changes.size() );
     Assertions.assertEquals( "$.a", changes.get( 0 ).getJsonPath() );
     Assertions.assertTrue( deletions.isEmpty(), "Deletions are not empty." );
     Assertions.assertTrue( additions.isEmpty(), "Additions are not empty." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatTrimAndIgnoreCaseInStringComparisonWorks()
   {
     // Given
     final String candidate = "{\"a\":\" TEXT \"}";
     final String reference = "{\"a\":\"text\"}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference)
           .setTrim( true )
           .setIgnoreCase( true )
           .calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertTrue( changes.isEmpty(),   "Changes are not empty." );
     Assertions.assertTrue( deletions.isEmpty(), "Deletions are not empty." );
     Assertions.assertTrue( additions.isEmpty(), "Additions are not empty." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatTypeChangesAreDetected()
   {
     // Given
     final String candidate = "{\"a\":1}";
     final String reference = "{\"a\":\"text\"}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference).calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertEquals( 1,     changes.size() );
     Assertions.assertEquals( "$.a", changes.get( 0 ).getJsonPath() );
     Assertions.assertTrue( deletions.isEmpty(), "Deletions are not empty." );
     Assertions.assertTrue( additions.isEmpty(), "Additions are not empty." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatDeletedEntriesAreDetected()
   {
     // Given
     final String candidate = "{\"a\":[1]}";
     final String reference = "{\"a\":[1,2],\"b\":3}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference).calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertTrue  ( changes.isEmpty(), "Changes are not empty." );
     Assertions.assertEquals( 2,        deletions.size() );
     Assertions.assertEquals( "$.a[1]", deletions.get( 0 ).getJsonPath() );
     Assertions.assertEquals( "$.b",    deletions.get( 1 ).getJsonPath() );
     Assertions.assertTrue  ( additions.isEmpty(), "Additions are not empty." );
   }

   /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

   @Test
   public void testThatAddedEntriesAreDetected()
   {
     // Given
     final String candidate = "{\"a\":[1,2],\"b\":3}";
     final String reference = "{\"a\":[1]}";

     // When
     JsonDiff diff = null;
     try {
        diff = JsonDiff.createInstance().setCandidate(candidate).setReference(reference).calculate();
     }
     catch( final Exception ex ) {
       ex.printStackTrace();
       Assertions.fail( "Unreachable" );
     }
     final List< JsonDiffEntry > changes   = diff.getChanges();
     final List< JsonDiffEntry > deletions = diff.getDeletions();
     final List< JsonDiffEntry > additions = diff.getAdditions();

     // Then
     Assertions.assertTrue  ( changes.isEmpty(),   "Changes are not empty." );
     Assertions.assertTrue  ( deletions.isEmpty(), "Deletions are not empty." );
     Assertions.assertEquals( 2,        additions.size() );
     Assertions.assertEquals( "$.a[1]", additions.get( 0 ).getJsonPath() );
     Assertions.assertEquals( "$.b",    additions.get( 1 ).getJsonPath() );
   }
}
