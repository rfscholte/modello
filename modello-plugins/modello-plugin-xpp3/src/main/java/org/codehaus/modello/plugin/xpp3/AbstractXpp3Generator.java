package org.codehaus.modello.plugin.xpp3;

/*
 * Copyright (c) 2004, Jason van Zyl
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.ModelloParameterConstants;
import org.codehaus.modello.model.CodeSegment;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.model.ModelInterface;
import org.codehaus.modello.model.Version;
import org.codehaus.modello.model.VersionDefinition;
import org.codehaus.modello.plugin.java.javasource.JClass;
import org.codehaus.modello.plugin.java.javasource.JField;
import org.codehaus.modello.plugin.java.javasource.JMethod;
import org.codehaus.modello.plugin.java.javasource.JParameter;
import org.codehaus.modello.plugin.java.javasource.JSourceCode;
import org.codehaus.modello.plugin.java.javasource.JType;
import org.codehaus.modello.plugins.xml.AbstractXmlJavaGenerator;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 */
public abstract class AbstractXpp3Generator
    extends AbstractXmlJavaGenerator
{
    private List<Version> supportedVersions;

    @Override
    protected void initialize( Model model, Properties parameters )
        throws ModelloException
    {
        super.initialize( model, parameters );
        
        String versions = parameters.getProperty( ModelloParameterConstants.SUPPORTED_VERSIONS );
        
        if ( versions != null )
        {
            supportedVersions = new ArrayList<Version>();
            for( String version : versions.split( "," ) )
            {
                supportedVersions.add( new Version( version ) );
            }
        }
    }
    
    protected final List<Version> getSupportedVersions()
    {
        return supportedVersions;
    }
    
    protected boolean verifySupportedVersions()
    {
        return getSupportedVersions() != null && !getSupportedVersions().isEmpty();
    }

    protected JField writeInitializeVersionInsideVersionRange( JClass clazz, VersionDefinition versionDefinition )
    {
        if( supportedVersions == null )
        {
            return null;
        }
        
        Map<Version, Set<String>> versionMap = new HashMap<Version, Set<String>>();
        
        for( Version version : supportedVersions )
        {
            Set<String> validRanges = new HashSet<String>();
            
            for( ModelClass modelClass : getModel().getClasses( version ) )
            {
                validRanges.add( modelClass.getVersionRange().getValue() );
                
                for( CodeSegment codeSegment : modelClass.getCodeSegments( version ) )
                {
                    validRanges.add( codeSegment.getVersionRange().getValue() );
                }
                
                for( ModelField modelField : modelClass.getFields( version ) )
                {
                    validRanges.add( modelField.getVersionRange().getValue() );
                }
            }
            
            for( ModelInterface modelInterface: getModel().getInterfaces( version ) )
            {
                validRanges.add( modelInterface.getVersionRange().getValue() );
                
                for( CodeSegment codeSegment : modelInterface.getCodeSegments( version ) )
                {
                    validRanges.add( codeSegment.getVersionRange().getValue() );
                }
                
                for( ModelField modelField : modelInterface.getFields( version ) )
                {
                    validRanges.add( modelField.getVersionRange().getValue() );
                }
            }
            versionMap.put( version, validRanges );
        }
        
        JField supportedVersionRanges =  new JField( new JType( "java.util.Map<String, java.util.Set<String>>" ), "supportedVersionRanges" );  
        supportedVersionRanges.getModifiers().makePrivate();
        supportedVersionRanges.getModifiers().setStatic( true );
        supportedVersionRanges.getModifiers().setFinal( true );
        clazz.addField( supportedVersionRanges );
        
        JSourceCode sc = clazz.getStaticInitializationCode();
        sc.add( "supportedVersionRanges = new java.util.HashMap<String, java.util.Set<String>>();" );
        for( Map.Entry<Version, Set<String>> entry : versionMap.entrySet() )
        {
            String field = entry.getKey().toString( "v", "_" );
            sc.add( "java.util.Set<String> " + field + " = new java.util.HashSet<String>();" );
            sc.add( "java.util.Collections.addAll( " + field + ", \"" + StringUtils.join( entry.getValue().iterator(), "\", \"" ) + "\" );" );
            sc.add( "supportedVersionRanges.put( \"" + entry.getKey().toString() + "\", " + field + " );" );
            sc.add( "" );
        }
        
        JMethod versionInsideVersionRange = new JMethod( "versionInsideVersionRange", JType.BOOLEAN, "Check if version is inside versionRange" );
        versionInsideVersionRange.addParameter( new JParameter( new JType( "java.lang.String" ), "version" ) );
        versionInsideVersionRange.addParameter( new JParameter( new JType( "java.lang.String" ), "versionRange" ) );
        sc = versionInsideVersionRange.getSourceCode();
        sc.add( "if ( version == null || !supportedVersionRanges.containsKey( version ) )" );
        sc.add( "{" );
        String hint;
        if ( "field".equals( versionDefinition.getType() ) )
        {
            hint = "Please start the xml by specifying '" + versionDefinition+  "'.";
        }
        else if ( "namespace".equals( versionDefinition.getType() ) )
        {
            hint = "Please specify the namespace in the rootelement of the xml.";
        }
        else
        {
            hint = "";
        }
        sc.addIndented( "throw new RuntimeException( \"Can't determine version. " + hint + "\" );" );
        sc.add( "}" );
        sc.add( "else" );
        sc.add( "{" );
        sc.addIndented( "return supportedVersionRanges.get( version ).contains( versionRange );" );
        sc.add( "}" );
        clazz.addMethod( versionInsideVersionRange );
        
        return supportedVersionRanges;
    }

}
