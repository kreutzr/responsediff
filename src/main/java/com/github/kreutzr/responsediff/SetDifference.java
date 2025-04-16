package com.github.kreutzr.responsediff;

import java.util.Arrays;
import java.util.LinkedList;

public class SetDifference 
{
  public static void main(String... args) 
  {
    String[] arrA = {"A", "B", "D", "F", "E", "G", "G" };
    String[] arrB = {"A", "B", "F", "C", "G", "H", "I" };
    
    differences(arrA, arrB);
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void differences(String[] first, String[] second) 
  {
    String[] sortedFirst  = Arrays.copyOf(first, first.length); // O(n)
    String[] sortedSecond = Arrays.copyOf(second, second.length); // O(m)
    Arrays.sort(sortedFirst); // O(n log n)
    Arrays.sort(sortedSecond); // O(m log m)

    int firstIndex = 0;
    int secondIndex = 0;

    LinkedList<String> additions = new LinkedList<String>();  
    LinkedList<String> deletions = new LinkedList<String>();  

    System.out.println( Arrays.toString( sortedFirst ) );
    System.out.println( Arrays.toString( sortedSecond ) );
    
    while (firstIndex < sortedFirst.length && secondIndex < sortedSecond.length) { // O(n + m)
      int compare = (int) Math.signum(sortedFirst[firstIndex].compareTo(sortedSecond[secondIndex]));

      System.out.println( first[ firstIndex ] + " " + second[ secondIndex ] +": compare=" + compare + ", firstIndex=" + firstIndex + ", secondIndex=" + secondIndex ); 

      switch(compare) {
      case -1:
        deletions.add(sortedFirst[firstIndex]);
        firstIndex++;
        break;
      case 1:
        additions.add(sortedSecond[secondIndex]);
        secondIndex++;
        break;
      default:
        firstIndex++;
        secondIndex++;
      }
      System.out.println( "  additions="+ additions );
      System.out.println( "  deletions=" + deletions );
    }

    if(firstIndex < sortedFirst.length) {
      append(additions, sortedFirst, firstIndex);
    }
    else if (secondIndex < sortedSecond.length) {
      append(additions, sortedSecond, secondIndex);
    }


    System.out.println( "additions="+ additions );
    System.out.println( "deletions=" + deletions );
  }
  
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private static void append(LinkedList<String> diffs, String[] sortedArray, int index) 
  {
    while(index < sortedArray.length) {
      diffs.add(sortedArray[index]);
      index++;
    }
  }
}