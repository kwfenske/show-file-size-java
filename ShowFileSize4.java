/*
  Show File Size #4 - Show File Sizes in Bytes, Kilobytes, Megabytes
  Written by: Keith Fenske, http://kwfenske.github.io/
  Friday, 26 February 2021
  Java class name: ShowFileSize4
  Copyright (c) 2021 by Keith Fenske.  Apache License or GNU GPL.

  This is a Java 1.4 console application to print the size of each file named
  on the command line in bytes, kilobytes, megabytes, gigabytes, or terabytes.
  By default, sizes are in kilobytes with one digit after the decimal point,
  rounded to the nearest digit.  More decimal digits will give more precision,
  but beyond two digits, it is better to reduce the scale factor, for example,
  from gigabytes to megabytes.  You may specify an allocation unit or cluster
  size, and file sizes will be rounded up to the next full unit if necessary.
  Options go before file names on the command line:

    -a:4kb
      Sets the allocation unit or cluster size to 4 kilobytes.  Choose your own
      size.  Omit the "k" for bytes only, and replace "k" with "m" for
      megabytes, "g" for gigabytes, or "t" for terabytes.  The default
      allocation unit is one byte, which means no cluster size.

    -f:mb2
      Formats file sizes in megabytes with 2 decimal digits.  Metric prefixes
      above apply for kilobytes, megabytes, gigabytes, etc.  There may be 0 to
      5 decimal or fractional digits, except when the scale factor is bytes
      only (no digits allowed).  Omit the "f:" if desired.  The default is
      -f:kb1 for sizes in kilobytes with one decimal digit.

  To display the size of all ZIP files in the current directory, rounded up to
  the nearest kilobyte:

      java  ShowFileSize4  -a:1k  -k0  *.zip

  See the "-?" option for a help summary:

      java  ShowFileSize4  -?

  There is no graphical interface (GUI) for this program; it must be run from a
  command prompt, command shell, or terminal window.

  Apache License or GNU General Public License
  --------------------------------------------
  ShowFileSize4 is free software and has been released under the terms and
  conditions of the Apache License (version 2.0 or later) and/or the GNU
  General Public License (GPL, version 2 or later).  This program is
  distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY,
  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the license(s) for more details.  You should have
  received a copy of the licenses along with this program.  If not, see the
  http://www.apache.org/licenses/ and http://www.gnu.org/licenses/ web pages.
*/

import java.io.*;                 // standard I/O
import java.text.*;               // number formatting
import java.util.regex.*;         // regular expressions

public class ShowFileSize4
{
  /* constants */

  static final String COPYRIGHT_NOTICE =
    "Copyright (c) 2021 by Keith Fenske.  Apache License or GNU GPL.";
  static final int EXIT_FAILURE = -1; // incorrect request or errors found
  static final int EXIT_SUCCESS = 1; // request completed successfully
  static final int EXIT_UNKNOWN = 0; // don't know or nothing really done
  static final String PROGRAM_TITLE =
    "Show File Sizes in Bytes, Kilobytes, Megabytes - by: Keith Fenske";
  static final long SCALE_NONE = 1L; // scale factors for metric prefixes
  static final long SCALE_KILO = 1024L;
  static final long SCALE_MEGA = 1048576L;
  static final long SCALE_GIGA = 1073741824L;
  static final long SCALE_TERA = 1099511627776L;
  static final long SCALE_PETA = 1125899906842624L;

  /* class variables */

  static boolean mswinFlag;       // true if running on Microsoft Windows

/*
  main() method

  We run as a console application.  There is no graphical interface.
*/
  public static void main(String[] args)
  {
    int decimalDigits;            // number of digits after decimal point
    NumberFormat formatComma;     // formats with commas (digit grouping)
    int i;                        // index variable
    Matcher matcher;              // matches elements of regular expression
    long sizeFactor;              // scale factor for formatted size
    Pattern sizePattern;          // regular expression for formatted size
    String sizeSuffix;            // tag appended to formatted size
    long totalBad;                // number of files with errors
    long totalGood;               // number of files with size reported
    Pattern unitPattern;          // regular expression for allocation unit
    long unitSize;                // allocation unit or cluster size in bytes
    String word;                  // one parameter from command line

    /* Initialize global and local variables. */

    decimalDigits = 1;            // default to one decimal digit
    formatComma = NumberFormat.getInstance(); // current locale
    formatComma.setGroupingUsed(true); // use commas or digit groups
    formatComma.setMaximumFractionDigits(decimalDigits); // default format
    formatComma.setMinimumFractionDigits(decimalDigits);
    mswinFlag = System.getProperty("os.name").startsWith("Windows");
    sizeFactor = SCALE_KILO;      // default scale factor for kilobytes
    sizePattern = Pattern.compile("[-/](?:f:?)?(?:(?:b0?)|(?:([gkmpt])b?([0-5]?)))");
                                  // regular expression for formatted size
    sizeSuffix = " KB";           // default tag for size in kilobytes
    totalBad = totalGood = 0;     // no files found yet
    unitPattern = Pattern.compile("[-/](?:a:?)?(\\d+)([gkmpt]?)b?");
                                  // regular expression for allocation unit
    unitSize = 1;                 // no allocation unit or cluster size

    /* Check command-line parameters for options.  Anything we don't recognize
    as an option is assumed to be a file name. */

    for (i = 0; i < args.length; i ++)
    {
      word = args[i].toLowerCase(); // easier to process if consistent case
      if (word.length() == 0)
      {
        /* Ignore empty parameters, which are more common than you might think,
        when programs are being run from inside scripts (command files). */
      }

      else if (word.equals("?") || word.equals("-?") || word.equals("/?")
        || word.equals("-h") || word.equals("/h") || word.equals("-help")
        || word.equals("/help"))
      {
        showHelp();               // show help summary
        System.exit(EXIT_UNKNOWN); // exit application after printing help
      }

      /* Allocation unit or cluster size in bytes with metric prefix.  Our
      regular expression allows the metric prefix to be an empty string; its
      capturing group will never be <null>. */

      else if ((matcher = unitPattern.matcher(word)).matches())
      {
        unitSize = -1;            // set result to an illegal value
        try                       // there can be too many digits
        {
          long factor = SCALE_NONE; // default scale factor
          long number = Long.parseLong(matcher.group(1)); // parse digits
          String prefix = matcher.group(2); // metric prefix
          if (prefix.equals("k")) { factor = SCALE_KILO; }
          else if (prefix.equals("m")) { factor = SCALE_MEGA; }
          else if (prefix.equals("g")) { factor = SCALE_GIGA; }
          else if (prefix.equals("t")) { factor = SCALE_TERA; }
          else if (prefix.equals("p")) { factor = SCALE_PETA; }
          else { /* do nothing, assume bytes */ }
          if (number <= (Long.MAX_VALUE / factor)) // prevent overflow
            unitSize = number * factor;
        }
        catch (NumberFormatException nfe) { /* do nothing */ }
        if (unitSize <= 0)        // must be positive to be useful
        {
          System.err.println("Invalid allocation unit or cluster size: "
            + args[i]);           // notify user of our arbitrary limits
          showHelp();             // show help summary
          System.exit(EXIT_FAILURE); // exit application after printing help
        }
      }

      /* Scale factor and number of decimal digits for formatted size.  This
      regular expression is more complex than above, so some capturing groups
      may be <null> or an empty string. */

      else if ((matcher = sizePattern.matcher(word)).matches())
      {
        String digits = matcher.group(2); // get number of decimal digits
        if ((digits == null) || (digits.length() == 0)) digits = "1";
        decimalDigits = Integer.parseInt(digits); // always safe to parse

        String prefix = matcher.group(1); // metric prefix
        if ((prefix == null) || (prefix.length() == 0)) prefix = "b";
        if (prefix.equals("k")) { sizeFactor = SCALE_KILO; sizeSuffix = " KB"; }
        else if (prefix.equals("m")) { sizeFactor = SCALE_MEGA; sizeSuffix = " MB"; }
        else if (prefix.equals("g")) { sizeFactor = SCALE_GIGA; sizeSuffix = " GB"; }
        else if (prefix.equals("t")) { sizeFactor = SCALE_TERA; sizeSuffix = " TB"; }
        else if (prefix.equals("p")) { sizeFactor = SCALE_PETA; sizeSuffix = " PB"; }
        else                      // bytes only or unknown prefix string
        {
          decimalDigits = 0;      // no need for decimal digits
          sizeFactor = SCALE_NONE; // scale factor for bytes
          sizeSuffix = " bytes";  // format tag for size in bytes
        }
        formatComma.setMaximumFractionDigits(decimalDigits);
        formatComma.setMinimumFractionDigits(decimalDigits);
      }

      /* Anything that looks like an option but which we don't recognize. */

      else if (word.startsWith("-") || (mswinFlag && word.startsWith("/")))
      {
        System.err.println("Option not recognized: " + args[i]);
        showHelp();               // show help summary
        System.exit(EXIT_FAILURE); // exit application after printing help
      }

      /* Parameter does not look like an option.  Assume this is a file. */

      else
      {
        File fp = new File(args[i]); // always successful even if bad file name
        if (fp.isFile())          // must be a file, safe to get size
        {
          long fileSize = fp.length(); // get size of file in bytes

          /* Round up the file size to a full allocation unit or cluster.  The
          code below works correctly when the unit size is one byte (default),
          but may overflow if the file size is close to Long.MAX_VALUE and the
          unit size is greater than one. */

          long sizeQuo = fileSize / unitSize; // whole units or clusters
          long sizeRem = fileSize % unitSize; // zero or a partial unit
          if (sizeRem > 0) sizeQuo ++; // round up if necessary
          fileSize = sizeQuo * unitSize; // restore to proper range

          /* Use floating-point arithmetic only if there is a scale factor, as
          double-precision floating-point errors come into play when file sizes
          are in the petabyte range or larger (10**15). */

          if (sizeFactor > SCALE_NONE)
          {
            System.out.println(args[i] + " - " + formatComma.format(((double)
              fileSize) / ((double) sizeFactor)) + sizeSuffix);
          }
          else
          {
            System.out.println(args[i] + " - " + formatComma.format(fileSize)
              + sizeSuffix);
          }
          totalGood ++;           // one more good file with size reported
        }
        else                      // we can't do folders, non-file objects
        {
          System.out.println(args[i] + " - not a file");
          totalBad ++;            // one more "file" with errors
        }
      }
    }

    /* All command-line parameters have been successfully parsed.  Set the exit
    status from this program. */

    if (totalBad > 0)             // did any files have errors?
      System.exit(EXIT_FAILURE);
    else if (totalGood > 0)       // were there any good files reported?
      System.exit(EXIT_SUCCESS);
    else                          // if there were no files at all
    {
      showHelp();                 // show help summary
      System.exit(EXIT_UNKNOWN);  // exit application after printing help
    }

  } // end of main() method


/*
  showHelp() method

  Show the help summary.  This is a UNIX standard and is expected for all
  console applications, even very simple ones.
*/
  static void showHelp()
  {
    System.err.println();
    System.err.println(PROGRAM_TITLE);
    System.err.println();
    System.err.println("  java  ShowFileSize4  [options]  filenames");
    System.err.println();
    System.err.println("Options:");
    System.err.println("  -? = -help = show summary of command-line syntax");
    System.err.println("  -a:4kb (example) = allocation unit or cluster size in bytes, KB, MB, etc.");
    System.err.println("  -b = show sizes in bytes, with commas but no decimal digits");
    System.err.println("  -kb -kb1 -kb2 = show sizes in kilobytes with 1 or 2 decimal digits");
    System.err.println("  -mb -mb1 -mb2 = show sizes in megabytes with 1 or 2 decimal digits");
    System.err.println("  -gb -gb1 -gb2 = show sizes in gigabytes with 1 or 2 decimal digits");
    System.err.println("  -tb -tb1 -tb2 = show sizes in terabytes with 1 or 2 decimal digits");
//  System.err.println("  -pb -pb1 -pb2 = show sizes in petabytes with 1 or 2 decimal digits");
    System.err.println();
    System.err.println("The number of decimal digits (fractional digits) above can be from 0 to 5.");
    System.err.println();
    System.err.println(COPYRIGHT_NOTICE);
//  System.err.println();

  } // end of showHelp() method

} // end of ShowFileSize4 class

/* Copyright (c) 2021 by Keith Fenske.  Apache License or GNU GPL. */
