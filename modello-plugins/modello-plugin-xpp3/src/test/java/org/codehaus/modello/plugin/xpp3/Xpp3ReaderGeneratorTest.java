package org.codehaus.modello.plugin.xpp3;

import junit.framework.TestCase;

public class Xpp3ReaderGeneratorTest
    extends TestCase
{

    public void testReplaceAndEscape()
    {
        assertEquals( "\\QHello World\\E", Xpp3ReaderGenerator.toVersionGroupedRegexp( "Hello World" ) );
        assertEquals( "\\QHello World \\E([0-9]+(\\.[0-9]+){0,2})", Xpp3ReaderGenerator.toVersionGroupedRegexp( "Hello World ${version}" ) );
        assertEquals( "([0-9]+(\\.[0-9]+){0,2})\\Q Hello World\\E", Xpp3ReaderGenerator.toVersionGroupedRegexp( "${version} Hello World" ) );
        assertEquals( "([0-9]+(\\.[0-9]+){0,2})\\Q Hello World \\E([0-9]+(\\.[0-9]+){0,2})", Xpp3ReaderGenerator.toVersionGroupedRegexp( "${version} Hello World ${version}" ) );
    }

}
