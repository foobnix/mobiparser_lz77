package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.compress.utils.IOUtils;

import com.foobnix.mobi.parser.MobiParser;

public class MobiTest1 {
    public static void main(String[] args) throws IOException {

        String OUT = "/home/ivan-dev/dev/workspace/pdf4/MobiParserLZ77/output";

        // String input =
        // "/home/ivan-dev/dev/workspace/pdf4/MobiParserLZ77/input/Вожак.mobi";
        String input = "/home/ivan-dev/dev/workspace/pdf4/MobiParserLZ77/input/Abe.mobi";

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
        out.write(text);
        out.flush();
        out.close();

        int j = 0;
        for (int i = mobi.firstImageIndex; i < mobi.lastContentIndex; i++) {
            j++;
            FileOutputStream image = new FileOutputStream(new File(OUT, +j + ".jpg"));
            image.write(mobi.getRecordByIndex(i));
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

}
