package edu.byu.cs329.dom;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.SimplePropertyDescriptor;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a HTML file that displays the JDT AST extracted from a Java source file.
 * 
 * @author James Wasson
 * @author Eric Mercer
 *
 */
public class DomViewer {

  private static final Logger log = LoggerFactory.getLogger(DomViewer.class);
  
  private static final String ITEM_HEADER = "<li><span class=\"caret\">";
  private static final String NESTED_LIST_HEADER = "</span>\n<ul class=\"nested\">\n";
  private static final String NESTED_LIST_AND_ITEM_FOOTER = "</ul>\n</li>\n";

  /**
   * Given the ASTNode instance, print the HTML tree representation to a file.
   * 
   * @param node The ASTNode instance to print in a tree view.
   * @param file The file to output the HTML.
   */
  public static void writeDomToFile(ASTNode node, String file) {
    String nodeAsHtml = writeAsHtml2(node);
    try {
      PrintWriter writer = new PrintWriter(file, "UTF-8");
      writer.print(nodeAsHtml);
      writer.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static String getHead() {
    return "<!DOCTYPE html>\n" + "<html>\n" + "<head>\n"
        + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" + "<style>\n"
        + "ul, #myUL {\n" + "  list-style-type: none;\n" 
        + "  font-family: 'DejaVu Sans Mono', monospace;\n" + "}\n" + "\n" + "#myUL {\n"
        + "  margin: 0;\n" + "  padding: 0;\n" + "}\n" + "\n" + ".caret {\n"
        + "  cursor: pointer;\n" + "  -webkit-user-select: none; /* Safari 3.1+ */\n"
        + "  -moz-user-select: none; /* Firefox 2+ */\n" + "  -ms-user-select: none; /* IE 10+ */\n"
        + "  user-select: none;\n" + "}\n" + "\n" + ".caret::before {\n"
        + "  content: \"\\25B6\";\n" + "  color: black;\n" + "  display: inline-block;\n"
        + "  margin-right: 6px;\n" + "}\n" + "\n" + ".caret-down::before {\n"
        + "  -ms-transform: rotate(90deg); /* IE 9 */\n"
        + "  -webkit-transform: rotate(90deg); /* Safari */'\n" + "  transform: rotate(90deg);  \n"
        + "}\n" + "\n" + ".nested {\n" + "  display: none;\n" + "}\n" + "\n" + ".active {\n"
        + "  display: block;\n" + "}\n" + "\n" + "body {\n" 
        + "  font-family: Arial, Helvetica, sans-serif;\n" + "}\n" + "</style>\n" + "</head>\n" 
        + "<body>\n" + "\n" + "<h2>Tree View</h2>\n"
        + "<p>Click on the arrow(s) to open or close the tree branches.</p>\n" + "\n";
  }

  private static String getFoot() {
    return "<script>\n" + "var toggler = document.getElementsByClassName(\"caret\");\n" + "var i;\n"
        + "\n" + "for (i = 0; i < toggler.length; i++) {\n"
        + "  toggler[i].addEventListener(\"click\", function() {\n"
        + "    this.parentElement.querySelector(\".nested\").classList.toggle(\"active\");\n"
        + "    this.classList.toggle(\"caret-down\");\n" + "  });\n" + "}\n" + "</script>\n" + "\n"
        + "</body>\n" + "</html>\n";
  }

  private static String writeAsHtml2(ASTNode node) {
    String output = "";
    output += getHead();
    output += "<ul id=\"myUL\">\n";
    output += astNodeAsHtmlInner(node);
    output += "</ul>\n\n";
    output += getFoot();
    return output;
  }

  /**
   * Recursive function that creates a nested HTML tree.
   * 
   * @param node The ASTNode to display.
   * @return The HTML representation of the ASTNode.
   */
  private static String astNodeAsHtmlInner(ASTNode node) {

    String output = ITEM_HEADER + node.getClass().getSimpleName() + NESTED_LIST_HEADER;

    for (Object obj : node.structuralPropertiesForType()) {
      StructuralPropertyDescriptor descriptor = (StructuralPropertyDescriptor) obj;

      if (descriptor instanceof SimplePropertyDescriptor) {
        Object value = node.getStructuralProperty(descriptor);
        
        if (value == null) {
          log.warn("Ignoring null StructuralProperty from SimplePropertyDescriptor {} for node {}", descriptor, node);
          continue;
        }
        
        output += "<li>" + value.getClass().getSimpleName() + " " 
            + getMethodName(descriptor, value.getClass()) + "() =&gt; \'" + value.toString() 
            + "\'</li>\n";
      
      } else if (descriptor instanceof ChildPropertyDescriptor) {
        ASTNode childNode = (ASTNode) node.getStructuralProperty(descriptor);
        String methodName = getMethodName(descriptor, null);
        String methodReturnType = getMethodReturnType(node, methodName);
        log.trace("Descriptor methodName: {} returnType {}", methodName, methodReturnType);

        // Ignore JavaDoc Property. JavaDoc is part of the AST and their children have no value
        if (methodReturnType.equals("Javadoc")) {
          continue;
        }

        output += ITEM_HEADER + methodReturnType + " " + methodName + "()" + NESTED_LIST_HEADER;
        if (childNode != null) {
          output += astNodeAsHtmlInner(childNode);
        }
        output += NESTED_LIST_AND_ITEM_FOOTER;
      
      } else {
        ChildListPropertyDescriptor list = (ChildListPropertyDescriptor) descriptor;
        output += ITEM_HEADER + "List&lt;" + list.getElementType().getSimpleName() + "&gt; " 
            + list.getId() + "()" + NESTED_LIST_HEADER;

        for (Object j : (List<?>) node.getStructuralProperty(list)) {
          output += astNodeAsHtmlInner((ASTNode) j);
        }

        output += NESTED_LIST_AND_ITEM_FOOTER;
      }

    }

    output += NESTED_LIST_AND_ITEM_FOOTER;
    return output;
  }

  private static String getMethodReturnType(ASTNode node, String methodName) {
    String methodReturnType = "*";
    try {
      methodReturnType = node.getClass()
          .getMethod(methodName)
          .getReturnType()
          .getSimpleName();
    } catch (NoSuchMethodException | SecurityException e) {
      log.warn("Method name {} does not exist for class {}", methodName, node.getClass());
    }
    return methodReturnType;
  }

  private static String getMethodName(StructuralPropertyDescriptor descriptor, Class<?> clazz) {
    String prefix = clazz != null && clazz.isAssignableFrom(Boolean.class) ? "is" : "get";
    return prefix + Character.toUpperCase(descriptor.getId().charAt(0)) 
        + descriptor.getId().substring(1);
  }

  /**
   * Read the file at path and return its contents as a String.
   * 
   * @param path The location of the file to be read.
   * @return The contents of the file as a String.
   */
  public static String readFile(final String path) {
    try {
      return String.join("\n", Files.readAllLines(Paths.get(path)));
    } catch (IOException ioe) {
      throw new RuntimeException("Error reading input file. Check input file path", ioe);
    }
  }

  /**
   * Parse the given source.
   * 
   * @param sourceString The contents of some set of Java files.
   * @return An ASTNode representing the entire program.
   */
  public static ASTNode parse(final String sourceString) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setSource(sourceString.toCharArray());
    Map<?, ?> options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
    parser.setCompilerOptions(options);
    return parser.createAST(null);
  }

  /**
   * Main method to execute DomViewer.
   * 
   * @param args Input file string and output file string.
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      log.error("Missing Java input file or output file on command line");
      System.out.println("usage: java DomViewer <java file to parse> <html file to write>");
      System.exit(1);
    }

    File inputFile = new File(args[0]);
    String inputFileAsString = readFile(inputFile.getPath());
    ASTNode node = parse(inputFileAsString);

    log.info("Node parsed from {}. Writing to file {}. Parsed node: {}", inputFile, args[1], node);
    writeDomToFile(node, args[1]);
  }
}
