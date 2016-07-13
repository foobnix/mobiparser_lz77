package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.compress.utils.IOUtils;

import com.foobnix.mobi.parser.MobiParser;

public class MobiTest1 {


    public static void main(String[] args) throws IOException {

        String OUT = "/home/ivan-dev/git/mobilz77/MobiParserLZ77/output";

        String input = "/home/ivan-dev/git/mobilz77/MobiParserLZ77/input/Вожак.mobi";

        byte[] raw = IOUtils.toByteArray(new FileInputStream(new File(input)));
        MobiParser mobi = new MobiParser(raw);

        System.out.println(mobi.getTitle());
        System.out.println(mobi.getAuthor());

        for (File item : new File(OUT).listFiles()) {
            item.delete();
        }
        FileWriter out = new FileWriter(new File(OUT, "0_BOOK1.html"));
        String text = mobi.getTextContent().replace("<head>", "<head><meta charset='utf-8'>");
        text = text.replaceAll("recindex=\"([0]*)([0-9]+)\"", "src=\"$2.jpg\"");
        text = text.replaceAll("kindle:embed:([0]*)([0-9A-Z]+)([?]{1})mime=image/([a-z]+)", "$2.$4");
        out.write(text);
        out.flush();
        out.close();

        int j = 0;

        for (int i = mobi.firstImageIndex; i < mobi.lastContentIndex; i++) {
            j++;

            byte[] data = mobi.getRecordByIndex(i);
            FileOutputStream image = new FileOutputStream(new File(OUT, Integer.toHexString(j).toUpperCase() + "." + getDataExt(data)));
            image.write(data);
            image.flush();
            image.close();
        }

        FileOutputStream image = new FileOutputStream(new File(OUT, "cover.jpg"));
        if (mobi.getCoverOrThumb() != null) {
            image.write(mobi.getCoverOrThumb());
            image.flush();
            image.close();
        }

    }

    private final static byte[] JPEG = new byte[] { (byte) 0xFF, (byte) 0xD8 };
    private final static byte[] PNG = new byte[] { (byte) 0x89, (byte) 0x50 };
    private final static byte[] GIF = new byte[] { (byte) 0x47, (byte) 0x49 };

    public static String getDataExt(byte[] input) {
        byte[] in = Arrays.copyOf(input, 2);
        if (Arrays.equals(JPEG, in)) {
            return "jpeg";
        }
        if (Arrays.equals(PNG, in)) {
            return "png";
        }
        if (Arrays.equals(GIF, in)) {
            return "gif";
        }
        return "unknown";

    }

}
