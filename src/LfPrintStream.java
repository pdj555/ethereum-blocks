import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * A PrintStream whose println ends lines with "\n" on every platform.
 *
 * The stock println writes the host's line separator, so the same command
 * printed different bytes on Windows or under -Dline.separator. Output that
 * scripts compare byte for byte must not depend on the machine.
 */
final class LfPrintStream extends PrintStream {
    LfPrintStream(OutputStream out, Charset charset) {
        super(out, false, charset);
    }

    @Override
    public void println() {
        print('\n');
        flush();
    }

    @Override
    public void println(boolean x) {
        line(String.valueOf(x));
    }

    @Override
    public void println(char x) {
        line(String.valueOf(x));
    }

    @Override
    public void println(int x) {
        line(String.valueOf(x));
    }

    @Override
    public void println(long x) {
        line(String.valueOf(x));
    }

    @Override
    public void println(float x) {
        line(String.valueOf(x));
    }

    @Override
    public void println(double x) {
        line(String.valueOf(x));
    }

    @Override
    public void println(char[] x) {
        line(String.valueOf(x));
    }

    @Override
    public void println(String x) {
        line(String.valueOf(x));
    }

    @Override
    public void println(Object x) {
        line(String.valueOf(x));
    }

    private void line(String text) {
        print(text + "\n");
        flush();
    }
}
