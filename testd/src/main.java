import java.io.BufferedWriter;
import java.io.FileWriter;

public class main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			System.out.println("\"");
			// Create file
			String []workerHandleInArray ={"http://one.co.il","bla bla"}; 
			FileWriter fileWriter = new FileWriter("test.html");
			BufferedWriter out = new BufferedWriter(fileWriter);
			out.write("<html>\n<title>OCR</title>\n<body>");
			for (int i = 0; i < workerHandleInArray.length; i = i + 2) {
				System.out.println("dksaldksal;d");
				out.write("<p>\n");
				out.write("<img src=\"");
				out.write(workerHandleInArray[i]);
				out.write("\"><br/>\n");
				out.write(workerHandleInArray[i + 1] + "\n");
				out.write("<p>\n");

			}

			out.write("</body>\n<html>");
			out.close();		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
}
