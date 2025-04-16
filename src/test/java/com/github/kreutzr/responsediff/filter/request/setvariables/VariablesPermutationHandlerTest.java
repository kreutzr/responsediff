package com.github.kreutzr.responsediff.filter.request.setvariables;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.kreutzr.responsediff.filter.request.setvariables.VariablesPermutationHandler;

public class VariablesPermutationHandlerTest
{
  @Test
  public void testThatVariablePermutationWorks()
  {
    // Given
    final Map< String, List< Object > > variables = new HashMap<>();
    variables.put("A", Arrays.asList( "a", "b", "c" ) );
    variables.put("B", Arrays.asList( "c", "d" ) );
    variables.put("C", Arrays.asList( "e" ) );
    variables.put("D", Arrays.asList( ) );
    variables.put("E", null );
    variables.put("F", Arrays.asList( "f", "g" ) );

    final VariablesPermutationHandler var = new VariablesPermutationHandler();
    var.init( variables );

    // When / Then
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=0, B=0, C=0, D=0, E=0, F=0}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=1, B=0, C=0, D=0, E=0, F=0}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=2, B=0, C=0, D=0, E=0, F=0}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=0, B=1, C=0, D=0, E=0, F=0}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=1, B=1, C=0, D=0, E=0, F=0}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=2, B=1, C=0, D=0, E=0, F=0}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=0, B=0, C=0, D=0, E=0, F=1}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=1, B=0, C=0, D=0, E=0, F=1}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=2, B=0, C=0, D=0, E=0, F=1}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=0, B=1, C=0, D=0, E=0, F=1}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=1, B=1, C=0, D=0, E=0, F=1}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=2, B=1, C=0, D=0, E=0, F=1}" );

    // When / Then
    var.next();
    assertThat( var.getIndexes().toString() ).isEqualTo( "{A=0, B=0, C=0, D=0, E=0, F=0}" );
  }
}
