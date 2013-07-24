/*******************************************************************************
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Denise Smith - 2.6 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.oxm.record;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.xml.namespace.QName;

import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.core.helper.CoreClassConstants;
import org.eclipse.persistence.internal.oxm.ConversionManager;
import org.eclipse.persistence.internal.oxm.NamespaceResolver;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.oxm.record.JsonRecord.Level;

public class JsonBuilderRecord extends JsonRecord {

    private Level position;
    private JsonObjectBuilder rootJsonObjectBuilder;
    private JsonArrayBuilder rootJsonArrayBuilder;
 
    
    public JsonBuilderRecord(){
        super();
        isLastEventStart = false;
    }
    
    public JsonBuilderRecord(JsonObjectBuilder jsonObjectBuilder){
        this();
        rootJsonObjectBuilder = jsonObjectBuilder;
    }
    
    public JsonBuilderRecord(JsonArrayBuilder jsonArrayBuilder){
        this();
        rootJsonArrayBuilder = jsonArrayBuilder;
        isRootArray = true;
    }
      
    @Override
    public void startDocument(String encoding, String version) {      
        if(isRootArray){
            if(position == null){
                startCollection();
            }
            position.setEmptyCollection(false);
            
            Level newLevel = new Level(false, position);
            position = newLevel;
            
            isLastEventStart = true;
        }else{
            Level rootLevel = new Level(false, null);
            position = rootLevel;
            if(rootJsonObjectBuilder == null){
                rootJsonObjectBuilder = Json.createObjectBuilder();
            }  
            
            rootLevel.setJsonObjectBuilder(rootJsonObjectBuilder);
        }
    }

    @Override
    public void endDocument() {
        if(position != null){
            if(position.parentLevel != null && position.parentLevel.isCollection){
                popAndSetInParentBuilder();
            }else{
                //this is the root level list case
                position = (Level) position.parentLevel;
            }
        }
    }
    
    private void popAndSetInParentBuilder(){
        Level removedLevel = position;
        Level parentLevel = (Level) position.parentLevel;
        position = (Level) position.parentLevel;
        if(removedLevel.isCollection && removedLevel.isEmptyCollection() && removedLevel.keyName == null){
            return;
        }
      
        if(parentLevel != null){                  
            if(parentLevel.isCollection){
                if(removedLevel.isCollection){
                    parentLevel.getJsonArrayBuilder().add(removedLevel.getJsonArrayBuilder());
                }else{                   
                    parentLevel.getJsonArrayBuilder().add(removedLevel.getJsonObjectBuilder());
                }
            }else{
                if(removedLevel.isCollection){                    
                    parentLevel.getJsonObjectBuilder().add(removedLevel.getKeyName(), removedLevel.getJsonArrayBuilder());
                }else{
                    parentLevel.getJsonObjectBuilder().add(removedLevel.getKeyName(), removedLevel.getJsonObjectBuilder());
                }
            }
        }
        
    }    
    
    public void startCollection() {
        if(position == null){
             isRootArray = true;              
             Level rootLevel = new Level(true, null);
             if(rootJsonArrayBuilder == null){
                  rootJsonArrayBuilder = Json.createArrayBuilder();
             }
             rootLevel.setJsonArrayBuilder(rootJsonArrayBuilder);
             position = rootLevel;
        } else {            
            if(isLastEventStart){
                setComplex(position, true);           
            }            
            Level level = new Level(true, position);            
            position = level;
        }      
        isLastEventStart = false;
    }
    
    @Override
    public void endCollection() {
         popAndSetInParentBuilder();    
    }
    
    private void setComplex(Level level, boolean complex){
        boolean isAlreadyComplex = level.isComplex;
        level.setComplex(complex);
        if(complex && !isAlreadyComplex){
            if(complex && level.jsonObjectBuilder == null){
                level.jsonObjectBuilder = Json.createObjectBuilder();
            }
        }
    }
    
    @Override    
    public void openStartElement(XPathFragment xPathFragment, NamespaceResolver namespaceResolver) {
        super.openStartElement(xPathFragment, namespaceResolver);
        if(position != null){
            Level newLevel = new Level(false, position);            
            
            if(isLastEventStart){ 
                //this means 2 startevents in a row so the last this is a complex object
                setComplex(position, true);                                
            }
                      
            String keyName = getKeyName(xPathFragment);
           
            if(position.isCollection && position.isEmptyCollection() ){
                position.setKeyName(keyName);
            }else{
                newLevel.setKeyName(keyName);    
            }
            position = newLevel;   
            isLastEventStart = true;
        }
    }
       
    /**
     * Handle marshal of an empty collection.  
     * @param xPathFragment
     * @param namespaceResolver
     * @param openGrouping if grouping elements should be marshalled for empty collections
     * @return
     */    
    public boolean emptyCollection(XPathFragment xPathFragment, NamespaceResolver namespaceResolver, boolean openGrouping) {

         if(marshaller.isMarshalEmptyCollections()){
             super.emptyCollection(xPathFragment, namespaceResolver, true);
             
             if (null != xPathFragment) {
                 
                 if(xPathFragment.isSelfFragment() || xPathFragment.nameIsText()){
                     String keyName = position.getKeyName();
                     setComplex(position, false);
                     ((Level)position.parentLevel).getJsonObjectBuilder().add(keyName, Json.createArrayBuilder());                     
                 }else{ 
                     if(isLastEventStart){                         
                         setComplex(position, true);
                     }                 
                     String keyName =  getKeyName(xPathFragment);
                     if(keyName != null){
                        position.getJsonObjectBuilder().add(keyName, Json.createArrayBuilder());
                     }
                 }
                 isLastEventStart = false;   
             }
                  
             return true;
         }else{
             return super.emptyCollection(xPathFragment, namespaceResolver, openGrouping);
         }
    }

    @Override
    public void endElement(XPathFragment xPathFragment,NamespaceResolver namespaceResolver) {
        if(position != null){
            if(isLastEventStart){
                setComplex(position, true);
            }
            if(position.isComplex){
                popAndSetInParentBuilder();
            }else{
                position = (Level) position.parentLevel;
            }            
            isLastEventStart = false;          
        }
    }
    
    public void writeValue(Object value, QName schemaType, boolean isAttribute) {
        
        if (characterEscapeHandler != null && value instanceof String) {
            try {
                StringWriter stringWriter = new StringWriter();
                characterEscapeHandler.escape(((String)value).toCharArray(), 0, ((String)value).length(), isAttribute, stringWriter);
                value = stringWriter.toString();
            } catch (IOException e) {
                throw XMLMarshalException.marshalException(e);
            }
        }
        
        boolean textWrapperOpened = false;                       
        if(!isLastEventStart){
             openStartElement(textWrapperFragment, namespaceResolver);
             textWrapperOpened = true;
        }
      
        Level currentLevel = position;
        String keyName = position.getKeyName();
        if(!position.isComplex){           
            currentLevel = (Level) position.parentLevel;         
        }       
        addValue(currentLevel, keyName, value, schemaType);
        isLastEventStart = false;
        if(textWrapperOpened){    
             endElement(textWrapperFragment, namespaceResolver);
        }    
    }
    
    private void addValue(Level currentLevel, String keyName, Object value, QName schemaType){        
        if(currentLevel.isCollection()){
            currentLevel.setEmptyCollection(false);            
            addValueToArrayBuilder(currentLevel.getJsonArrayBuilder(), value, schemaType);
        } else {
            JsonObjectBuilder builder = currentLevel.getJsonObjectBuilder();
            addValueToObjectBuilder(builder, keyName, value, schemaType);            
        }
    }
    
    private void addValueToObjectBuilder(JsonObjectBuilder jsonObjectBuilder, String keyName, Object value, QName schemaType){
        if(value == NULL){
            jsonObjectBuilder.addNull(keyName);
        }else if(value instanceof Integer){
            jsonObjectBuilder.add(keyName, (Integer)value);  
        }else if(value instanceof BigDecimal){
            jsonObjectBuilder.add(keyName, (BigDecimal)value);   
        }else if(value instanceof BigInteger){
            jsonObjectBuilder.add(keyName, (BigInteger)value);               
        }else if(value instanceof Boolean){
            jsonObjectBuilder.add(keyName, (Boolean)value);
        }else if(value instanceof Character){
            jsonObjectBuilder.add(keyName, (Character)value);  
        }else if(value instanceof Double){
            jsonObjectBuilder.add(keyName, (Double)value);
        }else if(value instanceof Float){
            jsonObjectBuilder.add(keyName, (Float)value);
        }else if(value instanceof Long){
            jsonObjectBuilder.add(keyName, (Long)value);
        }else if(value instanceof String){
            jsonObjectBuilder.add(keyName, (String)value);                
        }else{
            String convertedValue = ((String) ((ConversionManager) session.getDatasourcePlatform().getConversionManager()).convertObject(value, CoreClassConstants.STRING, schemaType));
            Class theClass = (Class) ((XMLConversionManager) session.getDatasourcePlatform().getConversionManager()).getDefaultXMLTypes().get(schemaType);          
            if((schemaType == null || theClass == null) && (CoreClassConstants.NUMBER.isAssignableFrom(value.getClass()))){
                //if it's still a number and falls through the cracks we dont want "" around the value
                    BigDecimal convertedNumberValue = ((BigDecimal) ((ConversionManager) session.getDatasourcePlatform().getConversionManager()).convertObject(value, CoreClassConstants.BIGDECIMAL, schemaType));
                    jsonObjectBuilder.add(keyName, (BigDecimal)convertedNumberValue);
            }else{
                jsonObjectBuilder.add(keyName, convertedValue);
            }
                
        }
    }
    
    private void addValueToArrayBuilder(JsonArrayBuilder jsonArrayBuilder, Object value, QName schemaType){
        if(value == NULL){
            jsonArrayBuilder.addNull();
        }else if(value instanceof Integer){
            jsonArrayBuilder.add((Integer)value);  
        }else if(value instanceof BigDecimal){
            jsonArrayBuilder.add((BigDecimal)value);   
        }else if(value instanceof BigInteger){
            jsonArrayBuilder.add((BigInteger)value);               
        }else if(value instanceof Boolean){                
            jsonArrayBuilder.add((Boolean)value);
        }else if(value instanceof Character){
            jsonArrayBuilder.add((Character)value);  
        }else if(value instanceof Double){
            jsonArrayBuilder.add((Double)value);
        }else if(value instanceof Float){
            jsonArrayBuilder.add((Float)value);
        }else if(value instanceof Long){
            jsonArrayBuilder.add((Long)value);
        }else if(value instanceof String){
            jsonArrayBuilder.add((String)value);
        }else{
            String convertedValue = ((String) ((ConversionManager) session.getDatasourcePlatform().getConversionManager()).convertObject(value, CoreClassConstants.STRING, schemaType));
            Class theClass = (Class) ((XMLConversionManager) session.getDatasourcePlatform().getConversionManager()).getDefaultXMLTypes().get(schemaType);          
            if((schemaType == null || theClass == null) && (CoreClassConstants.NUMBER.isAssignableFrom(value.getClass()))){
                //if it's still a number and falls through the cracks we dont want "" around the value
                    BigDecimal convertedNumberValue = ((BigDecimal) ((ConversionManager) session.getDatasourcePlatform().getConversionManager()).convertObject(value, CoreClassConstants.BIGDECIMAL, schemaType));
                    jsonArrayBuilder.add((BigDecimal)convertedNumberValue);
            }else{
                jsonArrayBuilder.add(convertedValue);
            }
        }
    }
    
          
     /**
     * Instances of this class are used to maintain state about the current
     * level of the JSON message being marshalled.
     */
    protected static class Level extends JsonRecord.Level{
        
        private JsonObjectBuilder jsonObjectBuilder;
        private JsonArrayBuilder jsonArrayBuilder;
        
        public Level(boolean isCollection, Level parentLevel) {
            super(isCollection, parentLevel);
        }
     
        public void setCollection(boolean isCollection) {
            super.setCollection(isCollection);
            if(isCollection && jsonArrayBuilder == null){
                jsonArrayBuilder = Json.createArrayBuilder();
            }
        }

        public JsonObjectBuilder getJsonObjectBuilder() {
            return jsonObjectBuilder;
        }

        public void setJsonObjectBuilder(JsonObjectBuilder jsonObjectBuilder) {
            this.jsonObjectBuilder = jsonObjectBuilder;
        }

        public JsonArrayBuilder getJsonArrayBuilder() {
            return jsonArrayBuilder;
        }

        public void setJsonArrayBuilder(JsonArrayBuilder jsonArrayBuilder) {
            this.jsonArrayBuilder = jsonArrayBuilder;
        }       

    }

}
