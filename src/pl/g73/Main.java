package pl.g73;

import java.io.*;
import java.util.Arrays;
import java.util.Random;

public class Main {
    //    private static String dir = "g:\\";
    private static String dir="";

    static File fileSendTextF = new File(dir + "send.txt");
    static File fileEncodedTextF = new File(dir + "encoded.txt");
    static File fileReceivedTextF = new File(dir + "received.txt");
    static File fileDecodedTextF = new File(dir + "decoded.txt");

    public static void main(String[] args) throws IOException {

        System.out.println("--------------        Program #1 double bit corection");
        //Program #1 double bit corection
        encodeDoubleBitCorrection();
        sendDoubleBitCorrection();
        decodeDoubleBitCorrection();

        System.out.println("--------------        Program #2 Hamming error-correctionn");
        //Program #2 Hamming error-correction
        encodeHamming();
        sendAndReceivingHamming();
        decodeHamming();
    }

    private static void encodeHamming() throws IOException {
        //load file
        System.out.println("LOADING DATA: " + "*".repeat(100));
        byte[] bytesFromFile = loadFile(fileSendTextF);
        printerGP(bytesFromFile);

        //hamming error-correction: stage 1 = dividing rozkładanie 4 bits on position: 3,5,6,7
        System.out.println("DIVIDING TO 4 BITS AND PUUTTING THEM IN 8 BIT in pos: 3,5,6,7: " + "*".repeat(100));
        StringBuffer sb = new StringBuffer();
        for (byte b : bytesFromFile) {
            sb.append(byte2binary(b));
        }

        byte[] array4bits = new byte[sb.length() / 4];

        for (int i = 0; i < (sb.length() / 4); i++) {
            String string4bits = sb.substring(i * 4, i * 4 + 4);
            int fourBits = (Integer.parseInt(string4bits, 2)) << 4; //moving from left to right +4 positions

            int b3 = (fourBits & (0b1000_0000)) >> 2; //moving bits nr 1 into pos. nr 3 (from pos. 1)
            int b5 = (fourBits & (0b0100_0000)) >> 3; //moving bits nr 2 into pos. nr 5 (from pos. 2)
            int b6 = (fourBits & (0b0010_0000)) >> 3; //moving bits nr 3 into pos. nr 6 (from pos. 3)
            int b7 = (fourBits & (0b0001_0000)) >> 3; //moving bits nr 4 into pos. nr 7 (from pos. 4)
            array4bits[i] = (byte) (b3 | b5 | b6 | b7);// adding all above 4 bits in one byte
        }
        printerGP(array4bits);
        System.out.println("FULL HAMMING (add parity bits in pos. 1,2,4, and 0 to pos. 8): " + "*".repeat(100));

        //stage 2 = Hamming
        for (int i = 0; i < array4bits.length; i++) {
            byte oneByte = array4bits[i];
            byte a1 = (byte) ((oneByte & 0b0010_0000) << 2 ^ (oneByte & 0b0000_1000) << 4 ^ (oneByte & 0b0000_0010) << 6);
            byte a2 = (byte) ((oneByte & 0b0010_0000) << 1 ^ (oneByte & 0b0000_0100) << 4 ^ (oneByte & 0b0000_0010) << 5);
            byte a4 = (byte) ((oneByte & 0b0000_1000) << 1 ^ (oneByte & 0b0000_0100) << 2 ^ (oneByte & 0b0000_0010) << 3);
            array4bits[i] = (byte) (oneByte | a1 | a2 | a4);
//            System.out.println(byte2binary(array4bits[i]) + " = " + byte2binary(oneByte) + ", " + byte2binary(a1) + " " + byte2binary(a2) + " " + byte2binary(a4));
        }
        printerGP(array4bits);

        //save
        System.out.println("SAVING TO FILE: " + "*".repeat(100));
        saveFile(fileEncodedTextF, array4bits);
    }

    private static void sendAndReceivingHamming() throws IOException {
        //load file
        System.out.println("LOADING: " + "*".repeat(100));
        byte[] bytesFromFile = loadFile(fileEncodedTextF);


        System.out.println("SENDING: " + "*".repeat(100));
        // virtual sending information

        // receiving information with errors
        System.out.println("RECEIVING WITH ERRORS: " + "*".repeat(100));
        byte[] receivedDataWithErrors = bytesFromFile.clone();
        for (int i = 0; i < receivedDataWithErrors.length; i++) {
            receivedDataWithErrors[i] = (byte) (receivedDataWithErrors[i] ^ (1 << new Random().nextInt(7)));
        }
        printerGP(receivedDataWithErrors);

        //save result after receiving with errors
        System.out.println("SAVING WITH ERRORS: " + "*".repeat(100));
        saveFile(fileReceivedTextF, receivedDataWithErrors);

    }



    private static void decodeHamming() throws IOException {
        //load file
        System.out.println("LOADING DATA WITH ERRORS: " + "*".repeat(80));
        byte[] receivedData = loadFile(fileReceivedTextF);

        //decoding, correction
        System.out.println("DECODING: " + "*".repeat(80));
        byte[] corectedData = new byte[receivedData.length];
        for (int i = 0; i < receivedData.length; i++) {
            byte oneByte = receivedData[i];
            int bitWithError = 0;
            byte a1 = (byte) ((oneByte & 0b0010_0000) << 2 ^ (oneByte & 0b0000_1000) << 4 ^ (oneByte & 0b0000_0010) << 6); //1-357
            byte a2 = (byte) ((oneByte & 0b0010_0000) << 1 ^ (oneByte & 0b0000_0100) << 4 ^ (oneByte & 0b0000_0010) << 5); //2-357
            byte a4 = (byte) ((oneByte & 0b0000_1000) << 1 ^ (oneByte & 0b0000_0100) << 2 ^ (oneByte & 0b0000_0010) << 3); //4-567

            if ((a1 & 0b1000_0000) != (oneByte & 0b1000_0000)) bitWithError += 1;
            if ((a2 & 0b0100_0000) != (oneByte & 0b0100_0000)) bitWithError += 2;
            if ((a4 & 0b0001_0000) != (oneByte & 0b0001_0000)) bitWithError += 4;

//            System.out.println((bitWithError > 0 ? "error in bit nr: " + bitWithError : "errors in 8th bit"));
            corectedData[i] = ((bitWithError != 0) ? (byte) (oneByte ^ (0b0000_0001 << (8 - bitWithError))) : oneByte);
        }


        System.out.println("CORECTING: " + "*".repeat(80));
        printerGP(corectedData);
        // teaking 4 bits from 8bits (3, 5, 6, 7)

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < corectedData.length; i++) {

            int b1 = (corectedData[i] & (0b0010_0000)) << 2; //moving bits nr 3 into pos. nr 1
            int b2 = (corectedData[i] & (0b0000_1000)) << 3; //moving bits nr 5 into pos. nr 2
            int b3 = (corectedData[i] & (0b0000_0100)) << 3; //moving bits nr 6 into pos. nr 3
            int b4 = (corectedData[i] & (0b0000_0010)) << 3; //moving bits nr 7 into pos. nr 4

            sb.append(byte2binary((byte) (b1 | b2 | b3 | b4)));
        }

        System.out.println("FINAL STRING: " + "*".repeat(80));
        StringBuffer finalString = new StringBuffer();
        for (int i = 0; i < sb.toString().length(); i += 16) {
            finalString.append(sb.substring(i, i + 4)).append(sb.substring(i + 8, i + 12));
        }
        byte[] bytes = binaryString2ByteArray(finalString.toString());
        printerGP(bytes);

//        save result after receiving with errors
        System.out.println("SAVING: " + "*".repeat(80));
        saveFile(fileDecodedTextF, bytes);
    }

// #############################################################################################


    private static void encodeDoubleBitCorrection() throws IOException {
        //load file
        byte[] bytesFromFile = loadFile(fileSendTextF);
        printerGP(bytesFromFile);

        //doubler
        StringBuilder doubledBits = new StringBuilder();
        for (byte b : bytesFromFile) {
            String bits = String.format("%8s", Integer.toBinaryString(b)).replace(" ", "0");
            for (int i = 0; i < 8; i++) {
                doubledBits.append(bits.charAt(i)).append(bits.charAt(i));
            }
        }

        //parity
        StringBuilder parity = new StringBuilder();
        int amountOfSets = doubledBits.length() / 6;  //full sets of 6 plus last npt full
        for (int nrSetOf6 = 0; nrSetOf6 <= amountOfSets; nrSetOf6++) {
            int rightValue = Math.min(doubledBits.length(), nrSetOf6 * 6 + 6);
            String set = doubledBits.substring(nrSetOf6 * 6, rightValue);
            if (set.length() == 0) break;
            if (set.length() < 6) set = String.format("%-6s", set).replace(" ", "0");

            int b1 = Integer.parseInt(String.valueOf(set.charAt(0)));
            int b2 = Integer.parseInt(String.valueOf(set.charAt(2)));
            int b3 = Integer.parseInt(String.valueOf(set.charAt(4)));
            parity.append(set).append(b1 ^ b2 ^ b3).append(b1 ^ b2 ^ b3);
        }
        printerGP(binaryString2ByteArray(parity.toString()));
        //save result of parity
        saveFile(fileEncodedTextF, binaryString2ByteArray(parity.toString()));
    }

    private static void sendDoubleBitCorrection() throws IOException {
        //load file
        byte[] encodedBytesFromFile = loadFile(fileEncodedTextF);
        printerGP(encodedBytesFromFile);

        // virtual sending information

        // receiving information with errors
        byte[] receivedDataWithErrors = encodedBytesFromFile.clone();
        for (int i = 0; i < receivedDataWithErrors.length; i++) {
            receivedDataWithErrors[i] = (byte) (receivedDataWithErrors[i] ^ (1 << new Random().nextInt(7)));
        }

        //save result after receiving with errors
        saveFile(fileReceivedTextF, receivedDataWithErrors);
    }


    private static void decodeDoubleBitCorrection() throws IOException {
        //load file
        byte[] receivedData = loadFile(fileReceivedTextF);

        //decoding
        System.out.println("DECODING   ".repeat(5));

        //zamiana na string bitów z bajtów
        StringBuilder fullChain = new StringBuilder();
        for (int i = 0; i < receivedData.length; i++) {
            fullChain.append(String.format("%8s", Integer.toBinaryString(receivedData[i] & 0xFF)).replace(" ", "0"));
        }

        // errors correction
        StringBuilder correctedChain = new StringBuilder();
        for (int i = 0; i < fullChain.length(); i += 8) {
            String oneByte1 = fullChain.substring(i, i + 8);

            for (int j = 0; j < 5; j += 2) {
                if (oneByte1.charAt(j) == oneByte1.charAt(j + 1)) {
                    correctedChain.append(oneByte1.charAt(j));
                } else {
                    if (j == 0) {
                        correctedChain.append((char) ((int) oneByte1.charAt(2) ^ (int) oneByte1.charAt(4)
                                ^ (int) oneByte1.charAt(6)));
                    }

                    if (j == 2) {
                        correctedChain.append((char) ((int) oneByte1.charAt(0) ^ (int) oneByte1.charAt(4)
                                ^ (int) oneByte1.charAt(6)));
                    }
                    if (j == 4) {
                        correctedChain.append((char) ((int) oneByte1.charAt(0) ^ (int) oneByte1.charAt(2)
                                ^ (int) oneByte1.charAt(6)));
                    }
                }
            }
        }

        int modulo = (correctedChain.length() % 8);
        if (modulo > 0) {
            correctedChain.delete(correctedChain.length() - modulo, correctedChain.length());
        }
        byte[] correctedData = binaryString2ByteArray(correctedChain.toString());
        printerGP(correctedData);

        //removing
        if (correctedData[correctedData.length - 1] == 0) {
            correctedData = Arrays.copyOf(correctedData, correctedData.length - 1);
        }
        printerGP(correctedData);

        //save result after receiving with errors
        saveFile(fileDecodedTextF, correctedData);
    }

    private static void printerGP(byte[] bytesArray) {
        String GREEN_BOLD = "\033[1;32m";
        String WHITE_BOLD = "\033[1;37m";

        // Bits
        String bits = Arrays.toString(bytesArray);

        // Asci text
        String text = new String(bytesArray);

        //Hex
        StringBuilder hex = new StringBuilder();
        for (byte b : bytesArray) {
            hex.append(Integer.toHexString(b & 0xFF) + "/");
        }

        //Binary
        StringBuilder binary = new StringBuilder();
        for (byte b : bytesArray) {
            binary.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(" ", "0"));
        }
        System.out.println("-----------");
        System.out.print(GREEN_BOLD);
        System.out.println("Bits: " + Arrays.toString(bytesArray));
        System.out.println("Text: " + new String(bytesArray));
        System.out.println("Hex: " + hex.toString());
        System.out.println("Binary: " + stringWithSpacePrinter(binary.toString(), 8));
        System.out.print(WHITE_BOLD);
        System.out.println("-----------");
    }

    private static String byte2binary(byte bytes) {
        return String.format("%8s", Integer.toBinaryString(bytes & 0xFF)).replace(" ", "0");
    }

    private static byte[] binaryString2ByteArray(String binary) {
        String[] binaryTextArray = stringWithSpacePrinter(binary, 8).split(" ");
        byte[] byteArray = new byte[binaryTextArray.length];

        for (int j = 0; j < binaryTextArray.length; j++) {
            int byteLength = binaryTextArray[j].length() - 1;
            int decimalValue = 0;
            for (int i = 0; i <= byteLength; i++) {
                decimalValue += Math.pow(2, i) * (binaryTextArray[j].charAt(byteLength - i) == '1' ? 1 : 0);
            }
            byteArray[j] = (byte) decimalValue;
        }
        return byteArray;
    }

    private static byte[] loadFile(File file) throws IOException {
//        System.out.println(new File("").getAbsolutePath());
        try (FileInputStream fis = new FileInputStream(file)) {
            return fis.readAllBytes();
        }
    }

    private static void saveFile(File file, byte[] array) throws IOException {
        try (FileOutputStream fis = new FileOutputStream(file)) {
            fis.write(array);
        }
    }

    private static String stringWithSpacePrinter(String string, int setLength) {
        String s = Arrays.toString(string.split("(?<=\\G.{" + setLength + "})"))
                .replace("[", "")
                .replace("]", "")
                .replace(", ", " ");
        return s;
    }
}