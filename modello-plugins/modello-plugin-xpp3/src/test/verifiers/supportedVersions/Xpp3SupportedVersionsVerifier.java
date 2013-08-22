package org.codehaus.modello.generator.xml.xpp3;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.File;
import java.io.Reader;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;

import junit.framework.Assert;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.modello.verifier.Verifier;
import org.codehaus.modello.verifier.VerifierException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;

/**
 * @author Robert Scholte
 */
public class Xpp3SupportedVersionsVerifier
    extends Verifier
{
    @Override
    public void verify()
        throws Throwable
    {
        verifyReaderPom400();

        verifyReaderPom500();
        
        verifyReaderPom500ModelVersionWithPom400Namespace();
        
        verifyReaderPom400ModelVersionWithPom500Namespace();
        
        verifyReaderPom400ModelVersionWithBuildSourceEncoding();

        verifyWriterPom400ModelVersionWithBuildSourceEncoding();
    }

    public void verifyReaderPom400()
        throws Exception
    {
        File file = new File( "src/test/verifiers/supportedVersions/pom400.xml" );

        Reader reader = ReaderFactory.newXmlReader( file );

        MavenXpp3Reader modelReader = new MavenXpp3Reader();

        Model model = modelReader.read( reader );

        MavenXpp3Writer writer = new MavenXpp3Writer();

        StringWriter output = new StringWriter();

        writer.write( output, model );

        XMLUnit.setIgnoreWhitespace( true );

        XMLUnit.setIgnoreComments( true );

        Diff diff = XMLUnit.compareXML( ReaderFactory.newXmlReader( file ), output.toString() );

        if ( !diff.identical() )
        {
            throw new VerifierException( "writer result is not the same as original content: " + diff );
        }

    }

    public void verifyReaderPom500()
        throws Exception
    {
        File file = new File( "src/test/verifiers/supportedVersions/pom500.xml" );

        Reader reader = ReaderFactory.newXmlReader( file );

        MavenXpp3Reader modelReader = new MavenXpp3Reader();

        Model model = modelReader.read( reader );

        MavenXpp3Writer writer = new MavenXpp3Writer();

        StringWriter output = new StringWriter();

        writer.write( output, model );

        XMLUnit.setIgnoreWhitespace( true );

        XMLUnit.setIgnoreComments( true );

        Diff diff = XMLUnit.compareXML( ReaderFactory.newXmlReader( file ), output.toString() );

        if ( !diff.identical() )
        {
            throw new VerifierException( "writer result is not the same as original content: " + diff );
        }

    }

    public void verifyReaderPom500ModelVersionWithPom400Namespace()
                    throws Exception
    {
        File file = new File( "src/test/verifiers/supportedVersions/pom500+400.xml" );

        Reader reader = ReaderFactory.newXmlReader( file );

        MavenXpp3Reader modelReader = new MavenXpp3Reader();

        try
        {
            Model model = modelReader.read( reader );
            fail( "Should fail since namespace and modelVersion are not in sync." );
        }
        catch ( XmlPullParserException e )
        {
        }
    }

    public void verifyReaderPom400ModelVersionWithPom500Namespace()
                    throws Exception
    {
        File file = new File( "src/test/verifiers/supportedVersions/pom400+500.xml" );

        Reader reader = ReaderFactory.newXmlReader( file );

        MavenXpp3Reader modelReader = new MavenXpp3Reader();

        try
        {
            Model model = modelReader.read( reader );
            fail( "Should fail since namespace and modelVersion are not in sync." );
        }
        catch ( XmlPullParserException e )
        {
        }
    }
    
    public void verifyReaderPom400ModelVersionWithBuildSourceEncoding()
                    throws Exception
    {
        File file = new File( "src/test/verifiers/supportedVersions/pom400+buildSourceEncoding.xml" );

        Reader reader = ReaderFactory.newXmlReader( file );

        MavenXpp3Reader modelReader = new MavenXpp3Reader();

        try
        {
            Model model = modelReader.read( reader );
            fail( "Should fail since sourcEncoding is supported since 5.0.0." );
        }
        catch ( XmlPullParserException e )
        {
        }
    }
    
    public void verifyWriterPom400ModelVersionWithBuildSourceEncoding()
                    throws Exception
    {
        MavenXpp3Writer modelWriter = new MavenXpp3Writer();

        Model model = new Model();
        model.setModelVersion( "4.0.0" );
        model.setGroupId( "groupId");
        model.setArtifactId( "artifactId" );
        model.setVersion( "1.0.0-SNAPSHOT" );
        model.setBuild( new Build() );
        model.getBuild().setSourceEncoding( "UTF-8" );
        try
        {
            modelWriter.write( new ByteArrayOutputStream(), model );
            fail( "Should fail since sourcEncoding is supported since 5.0.0." );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "build.getSourceEncoding() is not supported for version  4.0.0", e.getMessage() );
        }
    }

}
