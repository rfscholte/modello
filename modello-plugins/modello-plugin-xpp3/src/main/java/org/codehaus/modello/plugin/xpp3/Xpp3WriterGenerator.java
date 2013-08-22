package org.codehaus.modello.plugin.xpp3;

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

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.codehaus.modello.ModelloException;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.model.ModelAssociation;
import org.codehaus.modello.model.ModelClass;
import org.codehaus.modello.model.ModelDefault;
import org.codehaus.modello.model.ModelField;
import org.codehaus.modello.plugin.java.javasource.JClass;
import org.codehaus.modello.plugin.java.javasource.JField;
import org.codehaus.modello.plugin.java.javasource.JMethod;
import org.codehaus.modello.plugin.java.javasource.JParameter;
import org.codehaus.modello.plugin.java.javasource.JSourceCode;
import org.codehaus.modello.plugin.java.javasource.JSourceWriter;
import org.codehaus.modello.plugin.java.javasource.JType;
import org.codehaus.modello.plugin.java.metadata.JavaFieldMetadata;
import org.codehaus.modello.plugin.model.ModelClassMetadata;
import org.codehaus.modello.plugins.xml.metadata.XmlAssociationMetadata;
import org.codehaus.modello.plugins.xml.metadata.XmlFieldMetadata;
import org.codehaus.modello.plugins.xml.metadata.XmlModelMetadata;

/**
 * @author <a href="mailto:jason@modello.org">Jason van Zyl </a>
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse </a>
 */
public class Xpp3WriterGenerator
    extends AbstractXpp3Generator
{
    private boolean requiresDomSupport;
    
    private static final String IGNOREVERSIONCONFLICT_PARAM = "ignoreVersionConflicts";

    
    public void generate( Model model, Properties parameters )
        throws ModelloException
    {
        initialize( model, parameters );

        requiresDomSupport = false;
        
        try
        {
            generateXpp3Writer();
        }
        catch ( IOException ex )
        {
            throw new ModelloException( "Exception while generating XPP3 Writer.", ex );
        }
    }

    private void generateXpp3Writer()
        throws ModelloException, IOException
    {
        Model objectModel = getModel();

        String packageName = objectModel.getDefaultPackageName( isPackageWithVersion(), getGeneratedVersion() )
            + ".io.xpp3";

        String marshallerName = getFileName( "Xpp3Writer" );

        JSourceWriter sourceWriter = newJSourceWriter( packageName, marshallerName );

        JClass jClass = new JClass( packageName + '.' + marshallerName );
        initHeader( jClass );
        suppressAllWarnings( objectModel, jClass );

        jClass.addImport( "org.codehaus.plexus.util.xml.pull.XmlSerializer" );
        jClass.addImport( "org.codehaus.plexus.util.xml.pull.MXSerializer" );
        jClass.addImport( "java.io.OutputStream" );
        jClass.addImport( "java.io.Writer" );
        jClass.addImport( "java.util.Iterator" );

        JField namespaceField = new JField( new JClass( "String" ), "NAMESPACE" );
        namespaceField.getModifiers().setFinal( true );
        namespaceField.getModifiers().setStatic( true );
        namespaceField.setInitString( "null" );
        jClass.addField( namespaceField );

        addModelImports( jClass, null );

        String root = objectModel.getRoot( getGeneratedVersion() );

        ModelClass rootClass = objectModel.getClass( root, getGeneratedVersion() );

        String rootElement = resolveTagName( rootClass );

        // ----------------------------------------------------------------------
        // Write the write( Writer, Model ) method which will do the unmarshalling.
        // ----------------------------------------------------------------------
        
        String writeRootArgs = verifySupportedVersions() ? ", " + IGNOREVERSIONCONFLICT_PARAM : "";

        String rootElementParameterName = uncapitalise( root );

        JMethod marshall = new JMethod( "write" );

        marshall.addParameter( new JParameter( new JClass( "Writer" ), "writer" ) );
        marshall.addParameter( new JParameter( new JClass( root ), rootElementParameterName ) );

        marshall.addException( new JClass( "java.io.IOException" ) );

        JSourceCode sc;
        
        if ( verifySupportedVersions() )
        {
            sc = marshall.getSourceCode();
            sc.add( "write( writer, " + rootElementParameterName + ", false );" );
            jClass.addMethod( marshall );
            
            marshall = new JMethod( "write" );

            marshall.addParameter( new JParameter( new JClass( "Writer" ), "writer" ) );
            marshall.addParameter( new JParameter( new JClass( root ), rootElementParameterName ) );
            marshall.addParameter( new JParameter( JType.BOOLEAN, IGNOREVERSIONCONFLICT_PARAM ) );
            marshall.addException( new JClass( "java.io.IOException" ) );
            marshall.addException( new JClass( "java.lang.IllegalArgumentException" ) );
        }
        
        sc = marshall.getSourceCode();

        sc.add( "XmlSerializer serializer = new MXSerializer();" );

        sc.add(
            "serializer.setProperty( \"http://xmlpull.org/v1/doc/properties.html#serializer-indentation\", \"  \" );" );

        sc.add(
            "serializer.setProperty( \"http://xmlpull.org/v1/doc/properties.html#serializer-line-separator\", \"\\n\" );" );

        sc.add( "serializer.setOutput( writer );" );

        sc.add( "serializer.startDocument( " + rootElementParameterName + ".getModelEncoding(), null );" );

        sc.add( "write" + root + "( " + rootElementParameterName + ", \"" + rootElement + "\", serializer" + writeRootArgs + " );" );

        sc.add( "serializer.endDocument();" );

        jClass.addMethod( marshall );

        // ----------------------------------------------------------------------
        // Write the write( OutputStream, Model ) method which will do the unmarshalling.
        // ----------------------------------------------------------------------

        marshall = new JMethod( "write" );

        marshall.addParameter( new JParameter( new JClass( "OutputStream" ), "stream" ) );
        marshall.addParameter( new JParameter( new JClass( root ), rootElementParameterName ) );
        marshall.addException( new JClass( "java.io.IOException" ) );
        
        if( verifySupportedVersions() )
        {
            sc = marshall.getSourceCode();
            sc.add( "write( stream, " + rootElementParameterName + ", false );" );
            jClass.addMethod( marshall );
            
            marshall = new JMethod( "write" );
//            marshall.getJDocComment().appendComment( "if {@code ignoreVersionConflicts} is {@code false}, "
//                + "an IllegalArgumentException is thrown when there's a version conflict." );

            marshall.addParameter( new JParameter( new JClass( "OutputStream" ), "stream" ) );
            marshall.addParameter( new JParameter( new JClass( root ), rootElementParameterName ) );
            marshall.addParameter( new JParameter( JType.BOOLEAN, IGNOREVERSIONCONFLICT_PARAM ) );
            marshall.addException( new JClass( "java.io.IOException" ) );
            marshall.addException( new JClass( "java.lang.IllegalArgumentException" ) );
        }

        sc = marshall.getSourceCode();

        sc.add( "XmlSerializer serializer = new MXSerializer();" );

        sc.add(
            "serializer.setProperty( \"http://xmlpull.org/v1/doc/properties.html#serializer-indentation\", \"  \" );" );

        sc.add(
            "serializer.setProperty( \"http://xmlpull.org/v1/doc/properties.html#serializer-line-separator\", \"\\n\" );" );

        sc.add( "serializer.setOutput( stream, " + rootElementParameterName + ".getModelEncoding() );" );

        sc.add( "serializer.startDocument( " + rootElementParameterName + ".getModelEncoding(), null );" );

        sc.add( "write" + root + "( " + rootElementParameterName + ", \"" + rootElement + "\", serializer" + writeRootArgs + " );" );

        sc.add( "serializer.endDocument();" );

        jClass.addMethod( marshall );

        writeAllClasses( objectModel, jClass );

        if( verifySupportedVersions() )
        {
            writeInitializeVersionInsideVersionRange( jClass, objectModel.getVersionDefinition() );
        }
        
        if ( requiresDomSupport )
        {
            createWriteDomMethod( jClass );
        }

        // @since 1.9
        if ( objectModel.getVersionDefinition() != null && verifySupportedVersions() )
        {
            XmlModelMetadata xmlModelMetadata = (XmlModelMetadata) objectModel.getMetadata( XmlModelMetadata.ID );
            
            JType stringType = new JType( "java.lang.String" );
            
            JMethod utilityMethod =
                new JMethod( "getNamespace", stringType, "the namespace of this model" );
            utilityMethod.getModifiers().makePrivate();
            
            utilityMethod.addParameter( new JParameter( stringType, "version" ) );
            
            sc = utilityMethod.getSourceCode();
            
            sc.add( "return \"" + xmlModelMetadata.getNamespace() + "\".replace( \"${version}\", version );" );

            jClass.addMethod( utilityMethod );

            utilityMethod = new JMethod( "getSchemaLocation", stringType, "the schemaLocation of this model" );
            utilityMethod.getModifiers().makePrivate();

            utilityMethod.addParameter( new JParameter( stringType, "version" ) );

            sc = utilityMethod.getSourceCode();

            sc.add( "return \"" + xmlModelMetadata.getSchemaLocation() + "\".replace( \"${version}\", version );" );

            jClass.addMethod( utilityMethod );
        }
        
        jClass.print( sourceWriter );

        sourceWriter.close();
    }

    private void writeAllClasses( Model objectModel, JClass jClass )
        throws ModelloException
    {
        for ( ModelClass clazz : getClasses( objectModel ) )
        {
            writeClass( clazz, jClass );
        }
    }

    private void writeClass( ModelClass modelClass, JClass jClass )
        throws ModelloException
    {
        String className = modelClass.getName();

        String uncapClassName = uncapitalise( className );

        JMethod marshall = new JMethod( "write" + className );

        marshall.addParameter( new JParameter( new JClass( className ), uncapClassName ) );
        marshall.addParameter( new JParameter( new JClass( "String" ), "tagName" ) );
        marshall.addParameter( new JParameter( new JClass( "XmlSerializer" ), "serializer" ) );
        
        marshall.addException( new JClass( "java.io.IOException" ) );
        
        if( verifySupportedVersions() )
        {
            marshall.addParameter( new JParameter( JType.BOOLEAN, IGNOREVERSIONCONFLICT_PARAM ) );
            marshall.addException( new JClass( "java.lang.IllegalArgumentException" ) );
        }

        marshall.getModifiers().makePrivate();

        JSourceCode sc = marshall.getSourceCode();

        ModelClassMetadata classMetadata = (ModelClassMetadata) modelClass.getMetadata( ModelClassMetadata.ID );

        if( !classMetadata.isRootElement() && verifySupportedVersions() )
        {
            marshall.addParameter( new JParameter( new JClass( "String" ), VERSION_PARAM ) );
        }

        
        String namespaceValue = null;
        String namespaceMethod = null;

        XmlModelMetadata xmlModelMetadata = (XmlModelMetadata) modelClass.getModel().getMetadata( XmlModelMetadata.ID );

        // add namespace information for root element only
        if ( classMetadata.isRootElement() && ( xmlModelMetadata.getNamespace() != null ) )
        {
            if ( getModel().getVersionDefinition() != null && verifySupportedVersions() )
            {
                sc.add( "String " + VERSION_PARAM + ";" );
                if ( getModel().getVersionDefinition().isFieldType() )
                {
                    String modelVersionGetter =
                        uncapClassName + ".get" + capitalise( getModel().getVersionDefinition().getValue() ) + "()";

                    namespaceMethod = "getNamespace( " + VERSION_PARAM + " = " + modelVersionGetter + " )";

                    sc.add( "serializer.setPrefix( \"\", " + namespaceMethod + " );" );
                }
                else if ( getModel().getVersionDefinition().isNamespaceType() )
                {
                    // we need to know the original namespace...
                    getLogger().warn( "supportedVersions only works with versionDefinition of type field." );

                    // default
                    namespaceValue = xmlModelMetadata.getNamespace( getGeneratedVersion() );
                    sc.add( "serializer.setPrefix( \"\", \"" + namespaceValue + "\" );" );
                }
                else
                {
                    // unknown type
                    getLogger().warn( "unknown versionDefinition type: " + getModel().getVersionDefinition().getType() );

                    // default
                    namespaceValue = xmlModelMetadata.getNamespace( getGeneratedVersion() );
                    sc.add( "serializer.setPrefix( \"\", \"" + namespaceValue + "\" );" );
                }
            }
            else
            {
                namespaceValue = xmlModelMetadata.getNamespace( getGeneratedVersion() );
                sc.add( "serializer.setPrefix( \"\", \"" + namespaceValue + "\" );" );
            }
        }

        if ( ( namespaceValue != null || namespaceMethod != null ) && ( xmlModelMetadata.getSchemaLocation() != null ) )
        {
            sc.add( "serializer.setPrefix( \"xsi\", \"http://www.w3.org/2001/XMLSchema-instance\" );" );

            sc.add( "serializer.startTag( NAMESPACE, tagName );" );

            String url = xmlModelMetadata.getSchemaLocation( getGeneratedVersion() );

            if ( getModel().getVersionDefinition() != null && verifySupportedVersions() )
            {
                // developer expects to be able to

                if ( getModel().getVersionDefinition().isFieldType() )
                {
                    String modelVersionGetter =
                        uncapClassName + ".get" + capitalise( getModel().getVersionDefinition().getValue() ) + "()";

                    String urlMethod = "getSchemaLocation( " + modelVersionGetter + " )";

                    sc.add( "serializer.attribute( \"\", \"xsi:schemaLocation\", " + namespaceMethod + " + \" \" + "
                        + urlMethod + " );" );
                }
                else if ( getModel().getVersionDefinition().isNamespaceType() )
                {
                    // we need to know the original schemaLocation...
                    getLogger().warn( "supportedVersions only works with versionDefinition of type field." );

                    // default
                    sc.add( "serializer.attribute( \"\", \"xsi:schemaLocation\", \"" + namespaceValue + " " + url
                        + "\" );" );
                }
                else
                {
                    // unknown type
                    getLogger().warn( "unknown versionDefinition type: " + getModel().getVersionDefinition().getType() );

                    // default
                    sc.add( "serializer.attribute( \"\", \"xsi:schemaLocation\", \"" + namespaceValue + " " + url
                        + "\" );" );
                }
            }
            else
            {
                sc.add( "serializer.attribute( \"\", \"xsi:schemaLocation\", \"" + namespaceValue + " " + url + "\" );" );
            }
        }
        else
        {
            sc.add( "serializer.startTag( NAMESPACE, tagName );" );
        }

        ModelField contentField = null;

        String contentValue = null;

        List<ModelField> modelFields = getFieldsForXml( modelClass, getGeneratedVersion() );

        // XML attributes
        for ( ModelField field : modelFields )
        {
            XmlFieldMetadata xmlFieldMetadata = (XmlFieldMetadata) field.getMetadata( XmlFieldMetadata.ID );

            JavaFieldMetadata javaFieldMetadata = (JavaFieldMetadata) field.getMetadata( JavaFieldMetadata.ID );

            String fieldTagName = resolveTagName( field, xmlFieldMetadata );

            String type = field.getType();

            String value = uncapClassName + "." + getPrefix( javaFieldMetadata ) + capitalise( field.getName() ) + "()";

            if ( xmlFieldMetadata.isContent() )
            {
                contentField = field;
                contentValue = value;
                continue;
            }

            if ( xmlFieldMetadata.isAttribute() )
            {
                sc.add( getValueChecker( type, value, field ) );
                
                JSourceCode codeblock = new JSourceCode( "serializer.attribute( NAMESPACE, \"" + fieldTagName + "\", "
                                + getValue( field.getType(), value, xmlFieldMetadata ) + " );" );
                
                sc.add( "{" );
                sc.indent();
                writeVersionCheck( sc, field, value, codeblock );
                sc.unindent();
                sc.add( "}" );
            }

        }

        if ( contentField != null )
        {
            XmlFieldMetadata xmlFieldMetadata = (XmlFieldMetadata) contentField.getMetadata( XmlFieldMetadata.ID );
            sc.add( "serializer.text( " + getValue( contentField.getType(), contentValue, xmlFieldMetadata ) + " );" );
        }

        // XML tags
        for ( ModelField field : modelFields )
        {
            XmlFieldMetadata xmlFieldMetadata = (XmlFieldMetadata) field.getMetadata( XmlFieldMetadata.ID );

            if ( xmlFieldMetadata.isContent() )
            {
                // skip field with type Content
                continue;
            }

            JavaFieldMetadata javaFieldMetadata = (JavaFieldMetadata) field.getMetadata( JavaFieldMetadata.ID );

            String fieldTagName = resolveTagName( field, xmlFieldMetadata );

            String type = field.getType();

            String value = uncapClassName + "." + getPrefix( javaFieldMetadata ) + capitalise( field.getName() ) + "()";

            if ( xmlFieldMetadata.isAttribute() )
            {
                continue;
            }

            String writeArgs = verifySupportedVersions() ? ", " + IGNOREVERSIONCONFLICT_PARAM + ", " + VERSION_PARAM : "";
            
            if ( field instanceof ModelAssociation )
            {
                ModelAssociation association = (ModelAssociation) field;

                String associationName = association.getName();

                if ( association.isOneMultiplicity() )
                {
                    JSourceCode codeblock = new JSourceCode( "write" + association.getTo() + "( (" + association.getTo() + ") " + value + ", \""
                                    + fieldTagName + "\", serializer" + writeArgs + " );" );
                    sc.add( getValueChecker( type, value, association ) );
                    sc.add( "{" );
                    sc.indent();
                    writeVersionCheck( sc, field, value, codeblock );
                    sc.unindent();
                    sc.add( "}" );
                }
                else
                {
                    //MANY_MULTIPLICITY

                    XmlAssociationMetadata xmlAssociationMetadata =
                        (XmlAssociationMetadata) association.getAssociationMetadata( XmlAssociationMetadata.ID );

                    String valuesTagName = resolveTagName( fieldTagName, xmlAssociationMetadata );
                    
                    

                    type = association.getType();
                    String toType = association.getTo();

                    boolean wrappedItems = xmlAssociationMetadata.isWrappedItems();

                    if ( ModelDefault.LIST.equals( type ) || ModelDefault.SET.equals( type ) )
                    {
                        sc.add( getValueChecker( type, value, association ) );
                        sc.add( "{" );
                        sc.indent();

                        JSourceCode codeblock = new JSourceCode();

                        if ( wrappedItems )
                        {
                            codeblock.add( "serializer.startTag( NAMESPACE, " + "\"" + fieldTagName + "\" );" );
                        }

                        codeblock.add( "for ( Iterator iter = " + value + ".iterator(); iter.hasNext(); )" );

                        codeblock.add( "{" );
                        codeblock.indent();

                        if ( isClassInModel( association.getTo(), modelClass.getModel() ) )
                        {
                            codeblock.add( toType + " o = (" + toType + ") iter.next();" );

                            codeblock.add( "write" + toType + "( o, \"" + valuesTagName + "\", serializer" + writeArgs + " );" );
                        }
                        else
                        {
                            codeblock.add( toType + " " + singular( uncapitalise( field.getName() ) ) + " = (" + toType
                                + ") iter.next();" );

                            codeblock.add( "serializer.startTag( NAMESPACE, " + "\"" + valuesTagName + "\" ).text( "
                                + singular( uncapitalise( field.getName() ) ) + " ).endTag( NAMESPACE, " + "\""
                                + valuesTagName + "\" );" );
                        }

                        codeblock.unindent();
                        codeblock.add( "}" );

                        if ( wrappedItems )
                        {
                            codeblock.add( "serializer.endTag( NAMESPACE, " + "\"" + fieldTagName + "\" );" );
                        }

                        writeVersionCheck( sc, field, value, codeblock );
                        
                        sc.unindent();
                        sc.add( "}" );
                    }
                    else
                    {
                        //Map or Properties

                        sc.add( getValueChecker( type, value, field ) );

                        sc.add( "{" );
                        sc.indent();

                        JSourceCode codeblock = new JSourceCode();
                        if ( wrappedItems )
                        {
                            codeblock.add( "serializer.startTag( NAMESPACE, " + "\"" + fieldTagName + "\" );" );
                        }

                        codeblock.add( "for ( Iterator iter = " + value + ".keySet().iterator(); iter.hasNext(); )" );

                        codeblock.add( "{" );
                        codeblock.indent();

                        codeblock.add( "String key = (String) iter.next();" );

                        codeblock.add( "String value = (String) " + value + ".get( key );" );

                        if ( xmlAssociationMetadata.isMapExplode() )
                        {
                            codeblock.add( "serializer.startTag( NAMESPACE, \"" + singular( associationName ) + "\" );" );
                            codeblock.add(
                                "serializer.startTag( NAMESPACE, \"key\" ).text( key ).endTag( NAMESPACE, \"key\" );" );
                            codeblock.add(
                                "serializer.startTag( NAMESPACE, \"value\" ).text( value ).endTag( NAMESPACE, \"value\" );" );
                            codeblock.add( "serializer.endTag( NAMESPACE, \"" + singular( associationName ) + "\" );" );
                        }
                        else
                        {
                            codeblock.add(
                                "serializer.startTag( NAMESPACE, \"\" + key + \"\" ).text( value ).endTag( NAMESPACE, \"\" + key + \"\" );" );
                        }

                        codeblock.unindent();
                        codeblock.add( "}" );

                        if ( wrappedItems )
                        {
                            codeblock.add( "serializer.endTag( NAMESPACE, " + "\"" + fieldTagName + "\" );" );
                        }

                        writeVersionCheck( sc, field, value, codeblock );
                        
                        sc.unindent();
                        sc.add( "}" );
                    }
                }
            }
            else
            {
                sc.add( getValueChecker( type, value, field ) );

                sc.add( "{" );
                sc.indent();

                JSourceCode codeblock = new JSourceCode();

                if ( "DOM".equals( field.getType() ) )
                {
                    if ( domAsXpp3 )
                    {
                        jClass.addImport( "org.codehaus.plexus.util.xml.Xpp3Dom" );
                        
    
                        codeblock = new JSourceCode( "((Xpp3Dom) " + value + ").writeToSerializer( NAMESPACE, serializer );" );
                    }
                    else
                    {
                        codeblock = new JSourceCode( "writeDom( (org.w3c.dom.Element) " + value + ", serializer );" );
                    }

                    requiresDomSupport = true;
                }
                else
                {
                    codeblock = new JSourceCode( "serializer.startTag( NAMESPACE, " + "\"" + fieldTagName + "\" ).text( "
                                    + getValue( field.getType(), value, xmlFieldMetadata ) + " ).endTag( NAMESPACE, " + "\""
                                    + fieldTagName + "\" );" );

                }
                writeVersionCheck( sc, field, value, codeblock );

                sc.unindent();
                sc.add( "}" );
            }
        }

        sc.add( "serializer.endTag( NAMESPACE, tagName );" );

        jClass.addMethod( marshall );
    }

    private void writeVersionCheck( JSourceCode sc, ModelField field, String value, JSourceCode serializeCodeblock )
    {
        if ( verifySupportedVersions() && !getIgnoreableRanges().contains( field.getVersionRange().getValue() ) )
        {
            sc.add( "if ( versionInsideVersionRange(  " + VERSION_PARAM + ", \"" + field.getVersionRange().getValue() + "\" ) )" );
            sc.add( "{" );
            sc.indent();
            serializeCodeblock.copyInto( sc );
            sc.unindent();
            sc.add( "}" );
            sc.add( "else if ( !" + IGNOREVERSIONCONFLICT_PARAM + "  )" );
            sc.add( "{" );
            sc.addIndented( "throw new IllegalArgumentException( \"" + value + " is not supported for version  \" + " + VERSION_PARAM + " );" );
            sc.add( "}" );
            sc.add( "else" );
            sc.add( "{" );
            sc.addIndented( "// silently ignore" );
            sc.add( "}" );
        }
        else
        {
            serializeCodeblock.copyInto( sc );
        }
    }

    private void createWriteDomMethod( JClass jClass )
    {
        if ( domAsXpp3 )
        {
            return;
        }
        String type = "org.w3c.dom.Element";
        JMethod method = new JMethod( "writeDom" );
        method.getModifiers().makePrivate();

        method.addParameter( new JParameter( new JType( type ), "dom" ) );
        method.addParameter( new JParameter( new JClass( "XmlSerializer" ), "serializer" ) );

        method.addException( new JClass( "java.io.IOException" ) );

        JSourceCode sc = method.getSourceCode();

        // start element
        sc.add( "serializer.startTag( NAMESPACE, dom.getTagName() );" );

        // attributes
        sc.add( "org.w3c.dom.NamedNodeMap attributes = dom.getAttributes();" );
        sc.add( "for ( int i = 0; i < attributes.getLength(); i++ )" );
        sc.add( "{" );

        sc.indent();
        sc.add( "org.w3c.dom.Node attribute = attributes.item( i );" );
        sc.add( "serializer.attribute( NAMESPACE, attribute.getNodeName(), attribute.getNodeValue() );" );
        sc.unindent();

        sc.add( "}" );

        // child nodes & text
        sc.add( "org.w3c.dom.NodeList children = dom.getChildNodes();" );
        sc.add( "for ( int i = 0; i < children.getLength(); i++ )" );
        sc.add( "{" );
        sc.indent();
        sc.add( "org.w3c.dom.Node node = children.item( i );" );
        sc.add( "if ( node instanceof org.w3c.dom.Element)" );
        sc.add( "{" );
        sc.addIndented( "writeDom( (org.w3c.dom.Element) children.item( i ), serializer );" );
        sc.add( "}" );
        sc.add( "else" );
        sc.add( "{" );
        sc.addIndented( "serializer.text( node.getTextContent() );" );
        sc.add( "}" );
        sc.unindent();
        sc.add( "}" );

        sc.add( "serializer.endTag( NAMESPACE, dom.getTagName() );" );

        jClass.addMethod( method );
    }
}
