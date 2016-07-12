package com.foobnix.mobi.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobiParser {
    public String name;
    public int recordsCount;
    List<Integer> recordsOffset = new ArrayList<Integer>();
    public int mobiType;
    public String encoding;
    public String fullName;
    private byte[] raw;
    public int firstImageIndex;
    public int lastContentIndex;
    public EXTH exth = new EXTH();
    private int firstContentIndex;

    // http://wiki.mobileread.com/wiki/MOBI#MOBI_Header
    class EXTH {
        public String identifier;
        public int len;
        public int count;
        public Map<Integer, byte[]> headers = new HashMap<Integer, byte[]>();

        public EXTH parse(byte[] raw) {
            int offset = indexOf(raw, "EXTH".getBytes());
            identifier = asString(raw, offset, 4);
            len = asInt(raw, offset + 4, 4);
            count = asInt(raw, offset + 8, 4);

            int rOffset = offset + 12;

            for (int i = 0; i < count; i++) {
                int rType = asInt(raw, rOffset, 4);
                int rLen = asInt(raw, rOffset + 4, 4);
                byte[] data = Arrays.copyOfRange(raw, rOffset + 8, rOffset + rLen);
                rOffset = rOffset + rLen;
                headers.put(rType, data);
            }
            return this;
        }

    }

    public static int byteArrayToInt(byte[] buffer) {
        int total = 0;
        int len = buffer.length;
        for (int i = 0; i < len; i++) {
            total = (total << 8) + (buffer[i] & 0xff);
        }
        return total;
    }

    public static String asString(byte[] raw, int offset, int len) {
        return new String(Arrays.copyOfRange(raw, offset, offset + len));
    }

    public static int asInt(byte[] raw, int offset, int len) {
        byte[] range = Arrays.copyOfRange(raw, offset, offset + len);
        return byteArrayToInt(range);
    }

    public static byte[] lz77(byte[] bytes) {
        ByteArrayBuffer outputStream = new ByteArrayBuffer(bytes.length);

        int i = 0;
        while (i < bytes.length - 4) {// try 2,4,8,10
            int b = bytes[i++] & 0x00FF;
            try {
                if (b == 0x0) {
                    outputStream.write(b);
                } else if (b <= 0x08) {
                    for (int j = 0; j < b; j++)
                        outputStream.write(bytes[i + j]);
                    i += b;
                }

                else if (b <= 0x7f) {
                    outputStream.write(b);
                } else if (b <= 0xbf) {
                    b = b << 8 | bytes[i++] & 0xFF;
                    int length = (b & 0x0007) + 3;
                    int location = (b >> 3) & 0x7FF;

                    for (int j = 0; j < length; j++)
                        outputStream.write(outputStream.getRawData()[outputStream.size() - location]);
                } else {
                    outputStream.write(' ');
                    outputStream.write(b ^ 0x80);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return outputStream.getRawData();
    }

    public int indexOf(byte[] input, byte[] search) {
        for (int i = 0; i < input.length - search.length + 1; ++i) {
            boolean found = true;
            for (int j = 0; j < search.length; ++j) {
                if (input[i + j] != search[j]) {
                    found = false;
                    break;
                }
            }
            if (found)
                return i;
        }
        return -1;
    }

    public MobiParser(byte[] file) throws IOException {
        raw = file;

        name = asString(raw, 0, 32);
        recordsCount = asInt(raw, 76, 2);
        for (int i = 78; i < 78 + recordsCount * 8; i += 8) {
            int recordOffset = byteArrayToInt(Arrays.copyOfRange(raw, i, i + 4));
            int recordID = byteArrayToInt(Arrays.copyOfRange(raw, i + 5, i + 5 + 3));
            recordsOffset.add(recordOffset);
        }
        int mobiOffset = recordsOffset.get(0);

        mobiType = asInt(raw, mobiOffset + 24, 4);
        encoding = asInt(raw, mobiOffset + 28, 4) == 1252 ? "cp1251" : "UTF-8";

        int fullNameOffset = asInt(raw, mobiOffset + 84, 4);
        int fullNameLen = asInt(raw, mobiOffset + 88, 4);
        fullName = asString(raw, mobiOffset + fullNameOffset, fullNameLen);

        firstImageIndex = asInt(raw, mobiOffset + 108, 4);
        boolean isEXTHFlag = (asInt(raw, mobiOffset + 128, 4) & 0x40) != 0;
        firstContentIndex = asInt(raw, mobiOffset + 192, 2);
        lastContentIndex = asInt(raw, mobiOffset + 194, 2);

        if (isEXTHFlag) {
            exth = new EXTH().parse(raw);
        }
    }

    public String getTextContent() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (int i = firstContentIndex; i < lastContentIndex - 1 && i < firstImageIndex; i++) {
            int start = recordsOffset.get(i);
            int end = recordsOffset.get(i + 1);
            byte[] coded = Arrays.copyOfRange(raw, start, end);
            byte[] decoded = lz77(coded);
            for (int n = 0; n < decoded.length; n++) {
                if (decoded[n] != 0x00) {
                    outputStream.write(decoded[n]);
                }
            }
        }
        try {
            return outputStream.toString(encoding);
        } catch (UnsupportedEncodingException e) {
            return outputStream.toString();
        }
    }

    public String getTitle() {
        return fullName;
    }

    public String getAuthor() {
        return new String(exth.headers.get(100));
    }

    public byte[] getCoverOrThumb() {
        byte[] imgNumber = exth.headers.get(201);
        if (imgNumber == null) {
            imgNumber = exth.headers.get(202);
        }

        if (imgNumber != null) {
            int index = byteArrayToInt(imgNumber);
            return getRecordByIndex(index + firstImageIndex);
        } else {
            for (int i = firstImageIndex; i < lastContentIndex; i++) {
                byte[] img = getRecordByIndex(i);
                if ((img[0] & 0xff) == 0xFF && (img[1] & 0xff) == 0xD8) {
                    return img;
                }
            }
        }
        return null;
    }

    public byte[] getRecordByIndex(int index) {
        return Arrays.copyOfRange(raw, recordsOffset.get(index), recordsOffset.get(index + 1));
    }

}
