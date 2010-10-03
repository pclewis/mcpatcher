import javax.swing.*;
import java.io.OutputStream;
import java.io.PrintStream;

public class JTextAreaPrintStream extends PrintStream {

    private static final int DELAY = 750;

    public JTextAreaPrintStream(JTextArea textArea){
        super(new JTextAreaOutputStream(textArea));
    }

    private static class JTextAreaOutputStream extends OutputStream {
        private JTextArea textArea;

        private JTextAreaOutputStream(JTextArea textArea) {
            this.textArea = textArea;
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setEditable(false);
        }

        public void write(int i){
            textArea.append(new String(new char[] {(char) i}));
        }

        public void write(byte[] b){
            textArea.append(new String(b));
        }

        public void write(byte[] b, int offset, int len){
            textArea.append(new String(b, offset, len));
        }
    }
}