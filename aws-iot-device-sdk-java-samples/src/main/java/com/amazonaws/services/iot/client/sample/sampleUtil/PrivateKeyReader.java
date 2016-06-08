/****************************************************************************
 * Amazon Modifications: Copyright 2016 Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *****************************************************************************
 * Copyright (c) 1998-2010 AOL Inc. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ****************************************************************************/
// http://oauth.googlecode.com/svn/code/branches/jmeter/jmeter/src/main/java/org/apache/jmeter/protocol/oauth/sampler/PrivateKeyReader.java

package com.amazonaws.services.iot.client.sample.sampleUtil;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;

import javax.xml.bind.DatatypeConverter;

/**
 * Class for reading RSA or ECC private key from PEM file.
 * 
 * It can read PEM files with PKCS#8 or PKCS#1 encodings. It doesn't support
 * encrypted PEM files.
 */
public class PrivateKeyReader {

    // Private key file using PKCS #1 encoding
    public static final String P1_BEGIN_MARKER = "-----BEGIN RSA PRIVATE KEY"; //$NON-NLS-1$
    public static final String P1_END_MARKER = "-----END RSA PRIVATE KEY"; //$NON-NLS-1$

    // Private key file using PKCS #8 encoding
    public static final String P8_BEGIN_MARKER = "-----BEGIN PRIVATE KEY"; //$NON-NLS-1$
    public static final String P8_END_MARKER = "-----END PRIVATE KEY"; //$NON-NLS-1$

    /**
     * Get a RSA Private Key from InputStream.
     *
     * @param fileName
     *            file name
     * @return Private key
     * @throws IOException
     *             IOException resulted from invalid file IO
     * @throws GeneralSecurityException
     *             GeneralSecurityException resulted from invalid key format
     */
    public static PrivateKey getPrivateKey(String fileName) throws IOException, GeneralSecurityException {
        try (InputStream stream = new FileInputStream(fileName)) {
            return getPrivateKey(stream, null);
        }
    }

    /**
     * Get a Private Key from InputStream.
     *
     * @param fileName
     *            file name
     * @param algorithm
     *            the name of the key algorithm, for example "RSA" or "EC"
     * @return Private key
     * @throws IOException
     *             IOException resulted from invalid file IO
     * @throws GeneralSecurityException
     *             GeneralSecurityException resulted from invalid key data
     */
    public static PrivateKey getPrivateKey(String fileName, String algorithm) throws IOException,
            GeneralSecurityException {
        try (InputStream stream = new FileInputStream(fileName)) {
            return getPrivateKey(stream, algorithm);
        }
    }

    /**
     * Get a Private Key for the file.
     *
     * @param stream
     *            InputStream object
     * @param algorithm
     *            the name of the key algorithm, for example "RSA" or "EC"
     * @return Private key
     * @throws IOException
     *             IOException resulted from invalid file IO
     * @throws GeneralSecurityException
     *             GeneralSecurityException resulted from invalid key data
     */
    public static PrivateKey getPrivateKey(InputStream stream, String algorithm) throws IOException,
            GeneralSecurityException {
        PrivateKey key = null;
        boolean isRSAKey = false;

        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        boolean inKey = false;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (!inKey) {
                if (line.startsWith("-----BEGIN ") && line.endsWith(" PRIVATE KEY-----")) {
                    inKey = true;
                    isRSAKey = line.contains("RSA");
                }
                continue;
            } else {
                if (line.startsWith("-----END ") && line.endsWith(" PRIVATE KEY-----")) {
                    inKey = false;
                    isRSAKey = line.contains("RSA");
                    break;
                }
                builder.append(line);
            }
        }
        KeySpec keySpec = null;
        byte[] encoded = DatatypeConverter.parseBase64Binary(builder.toString());
        if (isRSAKey) {
            keySpec = getRSAKeySpec(encoded);
        } else {
            keySpec = new PKCS8EncodedKeySpec(encoded);
        }
        KeyFactory kf = KeyFactory.getInstance((algorithm == null) ? "RSA" : algorithm);
        key = kf.generatePrivate(keySpec);

        return key;
    }

    /**
     * Convert PKCS#1 encoded private key into RSAPrivateCrtKeySpec.
     * 
     * <p/>
     * The ASN.1 syntax for the private key with CRT is
     * 
     * <pre>
     * -- 
     * -- Representation of RSA private key with information for the CRT algorithm.
     * --
     * RSAPrivateKey ::= SEQUENCE {
     *   version           Version, 
     *   modulus           INTEGER,  -- n
     *   publicExponent    INTEGER,  -- e
     *   privateExponent   INTEGER,  -- d
     *   prime1            INTEGER,  -- p
     *   prime2            INTEGER,  -- q
     *   exponent1         INTEGER,  -- d mod (p-1)
     *   exponent2         INTEGER,  -- d mod (q-1) 
     *   coefficient       INTEGER,  -- (inverse of q) mod p
     *   otherPrimeInfos   OtherPrimeInfos OPTIONAL 
     * }
     * </pre>
     * 
     * @param keyBytes
     *            PKCS#1 encoded key
     * @return KeySpec
     * @throws IOException
     *             IOException resulted from invalid file IO
     */
    private static RSAPrivateCrtKeySpec getRSAKeySpec(byte[] keyBytes) throws IOException {

        DerParser parser = new DerParser(keyBytes);

        Asn1Object sequence = parser.read();
        if (sequence.getType() != DerParser.SEQUENCE)
            throw new IOException("Invalid DER: not a sequence"); //$NON-NLS-1$

        // Parse inside the sequence
        parser = sequence.getParser();

        parser.read(); // Skip version
        BigInteger modulus = parser.read().getInteger();
        BigInteger publicExp = parser.read().getInteger();
        BigInteger privateExp = parser.read().getInteger();
        BigInteger prime1 = parser.read().getInteger();
        BigInteger prime2 = parser.read().getInteger();
        BigInteger exp1 = parser.read().getInteger();
        BigInteger exp2 = parser.read().getInteger();
        BigInteger crtCoef = parser.read().getInteger();

        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1,
                exp2, crtCoef);

        return keySpec;
    }
}

/**
 * A bare-minimum ASN.1 DER decoder, just having enough functions to decode
 * PKCS#1 private keys. Especially, it doesn't handle explicitly tagged types
 * with an outer tag.
 * 
 * <p/>
 * This parser can only handle one layer. To parse nested constructs, get a new
 * parser for each layer using <code>Asn1Object.getParser()</code>.
 * 
 * <p/>
 * There are many DER decoders in JRE but using them will tie this program to a
 * specific JCE/JVM.
 * 
 * @author zhang
 *
 */
class DerParser {

    // Classes
    public final static int UNIVERSAL = 0x00;
    public final static int APPLICATION = 0x40;
    public final static int CONTEXT = 0x80;
    public final static int PRIVATE = 0xC0;

    // Constructed Flag
    public final static int CONSTRUCTED = 0x20;

    // Tag and data types
    public final static int ANY = 0x00;
    public final static int BOOLEAN = 0x01;
    public final static int INTEGER = 0x02;
    public final static int BIT_STRING = 0x03;
    public final static int OCTET_STRING = 0x04;
    public final static int NULL = 0x05;
    public final static int OBJECT_IDENTIFIER = 0x06;
    public final static int REAL = 0x09;
    public final static int ENUMERATED = 0x0a;
    public final static int RELATIVE_OID = 0x0d;

    public final static int SEQUENCE = 0x10;
    public final static int SET = 0x11;

    public final static int NUMERIC_STRING = 0x12;
    public final static int PRINTABLE_STRING = 0x13;
    public final static int T61_STRING = 0x14;
    public final static int VIDEOTEX_STRING = 0x15;
    public final static int IA5_STRING = 0x16;
    public final static int GRAPHIC_STRING = 0x19;
    public final static int ISO646_STRING = 0x1A;
    public final static int GENERAL_STRING = 0x1B;

    public final static int UTF8_STRING = 0x0C;
    public final static int UNIVERSAL_STRING = 0x1C;
    public final static int BMP_STRING = 0x1E;

    public final static int UTC_TIME = 0x17;
    public final static int GENERALIZED_TIME = 0x18;

    protected InputStream in;

    /**
     * Create a new DER decoder from an input stream.
     * 
     * @param in
     *            The DER encoded stream
     */
    public DerParser(InputStream in) throws IOException {
        this.in = in;
    }

    /**
     * Create a new DER decoder from a byte array.
     * 
     * @param The
     *            encoded bytes
     * @throws IOException
     *             IOException resulted from invalid file IO
     */
    public DerParser(byte[] bytes) throws IOException {
        this(new ByteArrayInputStream(bytes));
    }

    /**
     * Read next object. If it's constructed, the value holds encoded content
     * and it should be parsed by a new parser from
     * <code>Asn1Object.getParser</code>.
     * 
     * @return A object
     * @throws IOException
     *             IOException resulted from invalid file IO
     */
    public Asn1Object read() throws IOException {
        int tag = in.read();

        if (tag == -1)
            throw new IOException("Invalid DER: stream too short, missing tag"); //$NON-NLS-1$

        int length = getLength();

        byte[] value = new byte[length];
        int n = in.read(value);
        if (n < length)
            throw new IOException("Invalid DER: stream too short, missing value"); //$NON-NLS-1$

        Asn1Object o = new Asn1Object(tag, length, value);

        return o;
    }

    /**
     * Decode the length of the field. Can only support length encoding up to 4
     * octets.
     * 
     * <p/>
     * In BER/DER encoding, length can be encoded in 2 forms,
     * <ul>
     * <li>Short form. One octet. Bit 8 has value "0" and bits 7-1 give the
     * length.
     * <li>Long form. Two to 127 octets (only 4 is supported here). Bit 8 of
     * first octet has value "1" and bits 7-1 give the number of additional
     * length octets. Second and following octets give the length, base 256,
     * most significant digit first.
     * </ul>
     * 
     * @return The length as integer
     * @throws IOException
     *             IOException resulted from invalid file IO
     */
    private int getLength() throws IOException {

        int i = in.read();
        if (i == -1)
            throw new IOException("Invalid DER: length missing"); //$NON-NLS-1$

        // A single byte short length
        if ((i & ~0x7F) == 0)
            return i;

        int num = i & 0x7F;

        // We can't handle length longer than 4 bytes
        if (i >= 0xFF || num > 4)
            throw new IOException("Invalid DER: length field too big (" //$NON-NLS-1$
                    + i + ")"); //$NON-NLS-1$

        byte[] bytes = new byte[num];
        int n = in.read(bytes);
        if (n < num)
            throw new IOException("Invalid DER: length too short"); //$NON-NLS-1$

        return new BigInteger(1, bytes).intValue();
    }

}

/**
 * An ASN.1 TLV. The object is not parsed. It can only handle integers and
 * strings.
 * 
 * @author zhang
 *
 */
class Asn1Object {

    protected final int type;
    protected final int length;
    protected final byte[] value;
    protected final int tag;

    /**
     * Construct a ASN.1 TLV. The TLV could be either a constructed or primitive
     * entity.
     * 
     * <p/>
     * The first byte in DER encoding is made of following fields,
     * 
     * <pre>
     * -------------------------------------------------
     * |Bit 8|Bit 7|Bit 6|Bit 5|Bit 4|Bit 3|Bit 2|Bit 1|
     * -------------------------------------------------
     * |  Class    | CF  |     +      Type             |
     * -------------------------------------------------
     * </pre>
     * 
     * <ul>
     * <li>Class: Universal, Application, Context or Private
     * <li>CF: Constructed flag. If 1, the field is constructed.
     * <li>Type: This is actually called tag in ASN.1. It indicates data type
     * (Integer, String) or a construct (sequence, choice, set).
     * </ul>
     * 
     * @param tag
     *            Tag or Identifier
     * @param length
     *            Length of the field
     * @param value
     *            Encoded octet string for the field.
     */
    public Asn1Object(int tag, int length, byte[] value) {
        this.tag = tag;
        this.type = tag & 0x1F;
        this.length = length;
        this.value = value;
    }

    public int getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public byte[] getValue() {
        return value;
    }

    public boolean isConstructed() {
        return (tag & DerParser.CONSTRUCTED) == DerParser.CONSTRUCTED;
    }

    /**
     * For constructed field, return a parser for its content.
     * 
     * @return A parser for the construct.
     * @throws IOException
     *             IOException resulted from invalid file IO
     */
    public DerParser getParser() throws IOException {
        if (!isConstructed())
            throw new IOException("Invalid DER: can't parse primitive entity"); //$NON-NLS-1$

        return new DerParser(value);
    }

    /**
     * Get the value as integer
     * 
     * @return BigInteger
     * @throws IOException
     *             IOException resulted from invalid file IO
     */
    public BigInteger getInteger() throws IOException {
        if (type != DerParser.INTEGER)
            throw new IOException("Invalid DER: object is not integer"); //$NON-NLS-1$

        return new BigInteger(value);
    }

    /**
     * Get value as string. Most strings are treated as Latin-1.
     * 
     * @return Java string
     * @throws IOException
     *             IOException resulted from invalid file IO
     */
    public String getString() throws IOException {

        String encoding;

        switch (type) {

        // Not all are Latin-1 but it's the closest thing
        case DerParser.NUMERIC_STRING:
        case DerParser.PRINTABLE_STRING:
        case DerParser.VIDEOTEX_STRING:
        case DerParser.IA5_STRING:
        case DerParser.GRAPHIC_STRING:
        case DerParser.ISO646_STRING:
        case DerParser.GENERAL_STRING:
            encoding = "ISO-8859-1"; //$NON-NLS-1$
            break;

        case DerParser.BMP_STRING:
            encoding = "UTF-16BE"; //$NON-NLS-1$
            break;

        case DerParser.UTF8_STRING:
            encoding = "UTF-8"; //$NON-NLS-1$
            break;

        case DerParser.UNIVERSAL_STRING:
            throw new IOException("Invalid DER: can't handle UCS-4 string"); //$NON-NLS-1$

        default:
            throw new IOException("Invalid DER: object is not a string"); //$NON-NLS-1$
        }

        return new String(value, encoding);
    }
}