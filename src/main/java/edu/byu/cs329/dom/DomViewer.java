package edu.byu.cs329.dom;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
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
 * TODO.
 * 
 * @author James Wasson
 * @author Eric Mercer
 *
 */
public class DomViewer {

  static final Logger log = LoggerFactory.getLogger(DomViewer.class);

  /**
   * TODO.
   * 
   * @param node TODO
   * @param file TODO
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
        + "ul, #myUL {\n" + "  list-style-type: none;\n" + "}\n" + "\n" + "#myUL {\n"
        + "  margin: 0;\n" + "  padding: 0;\n" + "}\n" + "\n" + ".caret {\n"
        + "  cursor: pointer;\n" + "  -webkit-user-select: none; /* Safari 3.1+ */\n"
        + "  -moz-user-select: none; /* Firefox 2+ */\n" + "  -ms-user-select: none; /* IE 10+ */\n"
        + "  user-select: none;\n" + "}\n" + "\n" + ".caret::before {\n"
        + "  content: \"\\25B6\";\n" + "  color: black;\n" + "  display: inline-block;\n"
        + "  margin-right: 6px;\n" + "}\n" + "\n" + ".caret-down::before {\n"
        + "  -ms-transform: rotate(90deg); /* IE 9 */\n"
        + "  -webkit-transform: rotate(90deg); /* Safari */'\n" + "  transform: rotate(90deg);  \n"
        + "}\n" + "\n" + ".nested {\n" + "  display: none;\n" + "}\n" + "\n" + ".active {\n"
        + "  display: block;\n" + "}\n" + "</style>\n" + "</head>\n" + "<body>\n" + "\n"
        + "<h2>Tree View</h2>\n"
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
   * TODO.
   * 
   * @param node TODO
   * @return TODO
   */
  private static String astNodeAsHtmlInner(ASTNode node) {

    String output = "";
    String itemHeader = "<li><span class=\"caret\">";
    String nestedListHeader = "</span>\n<ul class=\"nested\">\n";
    String nestedListFooterItemFooter = "</ul>\n</li>\n";

    output += itemHeader + node.getClass().getSimpleName() + nestedListHeader;

    List<?> properties = node.structuralPropertiesForType();
    for (Iterator<?> iterator = properties.iterator(); iterator.hasNext();) {
      Object obj = iterator.next();
      assert obj instanceof StructuralPropertyDescriptor;
      StructuralPropertyDescriptor descriptor = (StructuralPropertyDescriptor) obj;

      if (descriptor instanceof SimplePropertyDescriptor) {
        
        SimplePropertyDescriptor simple = (SimplePropertyDescriptor) descriptor;
        Object value = node.getStructuralProperty(simple);
        
        // JavaDoc is part of the AST and their children have no value
        if (value == null) {
          continue;
        }
        
        output += "<li>" + value.getClass().getSimpleName() + " " + simple.getId() + ": \'"
            + value.toString() + "\'</li>\n";
      
      } else if (descriptor instanceof ChildPropertyDescriptor) {
      
        ChildPropertyDescriptor child = (ChildPropertyDescriptor) descriptor;
        ASTNode childNode = (ASTNode) node.getStructuralProperty(child);
        if (childNode != null) {
          output += astNodeAsHtmlInner(childNode);
        }
      
      } else {
      
        ChildListPropertyDescriptor list = (ChildListPropertyDescriptor) descriptor;
        output += itemHeader + list.getElementType().getSimpleName() + nestedListHeader;
        output += itemHeader + list.getId() + nestedListHeader;

        for (Object j : (List<?>) node.getStructuralProperty(list)) {
          output += astNodeAsHtmlInner((ASTNode) j);
        }

        output += nestedListFooterItemFooter;
        output += nestedListFooterItemFooter;
      }

    }

    output += nestedListFooterItemFooter;
    return output;
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
      log.error(ioe.getMessage());
    }
    return "";
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
   * TODO.
   * 
   * @param args TODO
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
    writeDomToFile(node, args[1]);
  }
}
